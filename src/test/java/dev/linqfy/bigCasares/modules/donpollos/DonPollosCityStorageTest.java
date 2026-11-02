package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosCityStorageTest {

    private static final String WORLD = "3f0a2b1c-0000-4000-8000-000000000001";
    private static final DonPollosCityLayout.CityPlan PLAN =
        new DonPollosCityLayout.CityPlan(Integer.MIN_VALUE, Integer.MIN_VALUE, -120, 4500, 3, 56, 45);

    private static YamlConfiguration yaml(String text) throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(text);
        return yaml;
    }

    @Test
    void savesEachCityWithNamedFieldsAndAVersion() throws InvalidConfigurationException {
        Map<String, String> status = new LinkedHashMap<>();
        status.put(WORLD + ";4;-2", "hecha");
        status.put(WORLD + ";manual;1760000000000", "pendiente");
        YamlConfiguration out = new YamlConfiguration();
        DonPollosCityStorage.save(out, status, Map.of(WORLD + ";manual;1760000000000", PLAN));

        YamlConfiguration saved = yaml(out.saveToString());
        assertEquals(2, saved.getInt("version"));
        assertEquals("mundo", saved.getString("ciudades.c0.tipo"));
        assertEquals(4, saved.getInt("ciudades.c0.celda-x"));
        assertEquals(-2, saved.getInt("ciudades.c0.celda-z"));
        assertEquals("manual", saved.getString("ciudades.c1.tipo"));
        assertEquals(-120, saved.getInt("ciudades.c1.x"));
        assertEquals(45, saved.getInt("ciudades.c1.largo"));
        assertEquals("pendiente", saved.getString("ciudades.c1.estado"));

        DonPollosCityStorage.Loaded loaded = DonPollosCityStorage.load(saved, name -> null);
        assertEquals(status, loaded.status());
        assertEquals(Map.of(WORLD + ";manual;1760000000000", PLAN), loaded.manualPlans());
        assertTrue(loaded.problems().isEmpty(), loaded.problems().toString());
        assertEquals(2, loaded.version());
    }

    @Test
    void theOldFormatIsReadAndMigrated() throws InvalidConfigurationException {
        YamlConfiguration old = yaml(String.join("\n",
            "ciudades:",
            "- " + WORLD + ";4;-2=hecha",
            "- world;1;1=agua",
            "- " + WORLD + ";manual;1760000000000=pendiente",
            "- sin-igual",
            "manuales:",
            "- " + WORLD + ";manual;1760000000000=-120,4500,3,56,45",
            "- " + WORLD + ";manual;99=roto"));

        DonPollosCityStorage.Loaded loaded = DonPollosCityStorage.load(old, name -> name.equals("world") ? WORLD : null);

        assertEquals(1, loaded.version());
        assertEquals("hecha", loaded.status().get(WORLD + ";4;-2"));
        assertEquals("agua", loaded.status().get(WORLD + ";1;1"), "registro por nombre de mundo pasado al uuid");
        assertEquals(PLAN, loaded.manualPlans().get(WORLD + ";manual;1760000000000"));
        assertEquals(2, loaded.problems().size(), loaded.problems().toString());
    }

    @Test
    void brokenRecordsAreSkippedAndReported() throws InvalidConfigurationException {
        YamlConfiguration broken = yaml(String.join("\n",
            "version: 2",
            "ciudades:",
            "  ok: {mundo: " + WORLD + ", tipo: mundo, celda-x: 1, celda-z: 2, estado: hecha}",
            "  sin-mundo: {tipo: mundo, celda-x: 1, celda-z: 2, estado: hecha}",
            "  estado-raro: {mundo: " + WORLD + ", tipo: mundo, celda-x: 1, celda-z: 3, estado: quemada}",
            "  sin-celda: {mundo: " + WORLD + ", tipo: mundo, estado: hecha}",
            "  manual-sin-plano: {mundo: " + WORLD + ", tipo: manual, id: '5', x: 1, estado: pendiente}",
            "  giro-malo: {mundo: " + WORLD + ", tipo: manual, id: '6', x: 1, z: 1, giro: 7, ancho: 4, largo: 4, estado: hecha}",
            "  tipo-raro: {mundo: " + WORLD + ", tipo: castillo, estado: hecha}",
            "  vacio: algo"));

        DonPollosCityStorage.Loaded loaded = DonPollosCityStorage.load(broken, name -> null);

        assertEquals(Map.of(WORLD + ";1;2", "hecha"), loaded.status());
        assertTrue(loaded.manualPlans().isEmpty());
        assertEquals(7, loaded.problems().size(), loaded.problems().toString());
    }

    @Test
    void aNewerFileIsReadWithAWarning() throws InvalidConfigurationException {
        DonPollosCityStorage.Loaded loaded = DonPollosCityStorage.load(yaml(String.join("\n",
            "version: 3",
            "ciudades:",
            "  c0: {mundo: " + WORLD + ", tipo: mundo, celda-x: 0, celda-z: 0, estado: hecha}")), name -> null);
        assertEquals(Map.of(WORLD + ";0;0", "hecha"), loaded.status());
        assertEquals(1, loaded.problems().size());
    }

    @Test
    void anEmptyOrMissingFileHasNoCities() throws InvalidConfigurationException {
        DonPollosCityStorage.Loaded loaded = DonPollosCityStorage.load(yaml(""), name -> null);
        assertTrue(loaded.status().isEmpty());
        assertTrue(loaded.problems().isEmpty());
    }
}
