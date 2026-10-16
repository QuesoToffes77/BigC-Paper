package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Como se guardan las ciudades en ciudades.yml (sin servidor). Version 2: cada ciudad con sus campos con nombre.
 * <pre>
 * version: 2
 * ciudades:
 *   c0: { mundo: uuid, tipo: mundo, celda-x: 4, celda-z: -2, estado: hecha }
 *   c1: { mundo: uuid, tipo: manual, id: '1760000000000', x: -120, z: 4500, giro: 3, ancho: 56, largo: 45,
 *         estado: pendiente }
 * </pre>
 * La version 1 (texto armado a mano: {@code uuid;4;-2=hecha} y la lista {@code manuales}) se lee y se pasa a la 2.
 * Un registro roto se anota en {@link Loaded#problems()} y se saltea: nunca impide que arranque el modulo.
 */
final class DonPollosCityStorage {

    static final int VERSION = 2;
    static final Set<String> STATES = Set.of("pendiente", "hecha", "agua");

    /** Lo leido: estado por clave, planos de las ciudades manuales, problemas y la version que tenia el archivo. */
    record Loaded(Map<String, String> status, Map<String, DonPollosCityLayout.CityPlan> manualPlans,
                  List<String> problems, int version) {
    }

    private DonPollosCityStorage() {
    }

    /** worldNameToId: para registros muy viejos guardados por nombre de mundo (devuelve null si no existe). */
    static Loaded load(ConfigurationSection yaml, UnaryOperator<String> worldNameToId) {
        Map<String, String> status = new LinkedHashMap<>();
        Map<String, DonPollosCityLayout.CityPlan> manual = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        int version = yaml.getInt("version", 1);
        if (version <= 1) {
            loadVersion1(yaml, worldNameToId, status, manual, problems);
        } else {
            if (version > VERSION) {
                problems.add("ciudades.yml es de una version mas nueva (" + version + "): se lee como la " + VERSION);
            }
            loadVersion2(yaml, status, manual, problems);
        }
        return new Loaded(status, manual, problems, version);
    }

    private static void loadVersion1(ConfigurationSection yaml, UnaryOperator<String> worldNameToId,
                                     Map<String, String> status, Map<String, DonPollosCityLayout.CityPlan> manual,
                                     List<String> problems) {
        for (String line : yaml.getStringList("ciudades")) {
            int split = line.lastIndexOf('=');
            if (split <= 0) {
                problems.add("registro viejo invalido: " + line);
                continue;
            }
            String key = line.substring(0, split);
            int semicolon = key.indexOf(';');
            if (semicolon > 0) {
                String byName = worldNameToId.apply(key.substring(0, semicolon));
                if (byName != null) {
                    key = byName + key.substring(semicolon);
                }
            }
            status.put(key, line.substring(split + 1));
        }
        for (String line : yaml.getStringList("manuales")) {
            int split = line.lastIndexOf('=');
            Optional<DonPollosCityLayout.CityPlan> plan = split > 0
                ? DonPollosCityRecords.decodePlan(line.substring(split + 1)) : Optional.empty();
            if (plan.isPresent()) {
                manual.put(line.substring(0, split), plan.get());
            } else {
                problems.add("ciudad manual vieja invalida: " + line);
            }
        }
    }

    private static void loadVersion2(ConfigurationSection yaml, Map<String, String> status,
                                     Map<String, DonPollosCityLayout.CityPlan> manual, List<String> problems) {
        ConfigurationSection cities = yaml.getConfigurationSection("ciudades");
        if (cities == null) {
            return;
        }
        for (String id : cities.getKeys(false)) {
            ConfigurationSection city = cities.getConfigurationSection(id);
            if (city == null) {
                problems.add("ciudad " + id + ": no tiene campos");
                continue;
            }
            String world = city.getString("mundo");
            String state = city.getString("estado");
            String type = city.getString("tipo");
            if (world == null || world.isBlank()) {
                problems.add("ciudad " + id + ": falta el mundo");
            } else if (state == null || !STATES.contains(state)) {
                problems.add("ciudad " + id + ": estado invalido (" + state + ")");
            } else if ("mundo".equals(type)) {
                if (!city.isInt("celda-x") || !city.isInt("celda-z")) {
                    problems.add("ciudad " + id + ": falta la celda");
                    continue;
                }
                status.put(world + ";" + city.getInt("celda-x") + ";" + city.getInt("celda-z"), state);
            } else if (DonPollosCityRecords.MANUAL.equals(type)) {
                String manualId = city.getString("id");
                boolean complete = manualId != null && !manualId.isBlank();
                for (String field : List.of("x", "z", "giro", "ancho", "largo")) {
                    complete &= city.isInt(field);
                }
                Optional<DonPollosCityLayout.CityPlan> plan = complete
                    ? DonPollosCityRecords.decodePlan(city.getInt("x") + "," + city.getInt("z") + "," + city.getInt("giro")
                        + "," + city.getInt("ancho") + "," + city.getInt("largo"))
                    : Optional.empty();
                if (plan.isEmpty()) {
                    problems.add("ciudad " + id + ": la ciudad manual no tiene un plano valido");
                    continue;
                }
                String key = world + ";" + DonPollosCityRecords.MANUAL + ";" + manualId;
                status.put(key, state);
                manual.put(key, plan.get());
            } else {
                problems.add("ciudad " + id + ": tipo desconocido (" + type + ")");
            }
        }
    }

    /** Escribe la version 2. Las ciudades manuales sin plano no se guardan (no hay forma de usarlas). */
    static void save(ConfigurationSection yaml, Map<String, String> status,
                     Map<String, DonPollosCityLayout.CityPlan> manualPlans) {
        yaml.set("version", VERSION);
        yaml.set("ciudades", null);
        int index = 0;
        for (Map.Entry<String, String> entry : status.entrySet()) {
            String key = entry.getKey();
            String path = "ciudades.c" + index;
            if (DonPollosCityRecords.isManual(key)) {
                DonPollosCityLayout.CityPlan plan = manualPlans.get(key);
                if (plan == null) {
                    continue;
                }
                yaml.set(path + ".mundo", DonPollosCityRecords.worldId(key));
                yaml.set(path + ".tipo", DonPollosCityRecords.MANUAL);
                yaml.set(path + ".id", key.substring(key.lastIndexOf(';') + 1));
                yaml.set(path + ".x", plan.x());
                yaml.set(path + ".z", plan.z());
                yaml.set(path + ".giro", plan.rotation());
                yaml.set(path + ".ancho", plan.sizeX());
                yaml.set(path + ".largo", plan.sizeZ());
            } else {
                Optional<int[]> cell = DonPollosCityRecords.seededCell(key);
                if (cell.isEmpty()) {
                    continue;
                }
                yaml.set(path + ".mundo", DonPollosCityRecords.worldId(key));
                yaml.set(path + ".tipo", "mundo");
                yaml.set(path + ".celda-x", cell.get()[0]);
                yaml.set(path + ".celda-z", cell.get()[1]);
            }
            yaml.set(path + ".estado", entry.getValue());
            index++;
        }
    }
}
