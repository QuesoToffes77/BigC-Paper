package dev.linqfy.bigCasares.modules.donpollos;

import java.util.Optional;

/**
 * Claves y registros guardados de las ciudades en data/don-pollos/ciudades.yml (sin Bukkit).
 * <ul>
 *   <li>Ciudad del mundo: {@code <uid del mundo>;<celdaX>;<celdaZ>}.</li>
 *   <li>Ciudad manual ({@code /donpollo ciudad aqui}): {@code <uid del mundo>;manual;<momento>}, con su plan guardado
 *       aparte como {@code x,z,giro,anchoX,largoZ} para poder terminarla si el servidor se cae.</li>
 * </ul>
 * Un registro roto devuelve vacio en vez de tirar una excepcion, asi no impide que arranque el modulo.
 */
final class DonPollosCityRecords {

    static final String MANUAL = "manual";

    private DonPollosCityRecords() {
    }

    static String worldId(String key) {
        int split = key.indexOf(';');
        return split < 0 ? key : key.substring(0, split);
    }

    static boolean isManual(String key) {
        String[] parts = key.split(";");
        return parts.length == 3 && MANUAL.equals(parts[1]);
    }

    /** Celda (x, z) de una ciudad del mundo, o vacio si la clave es manual o esta rota. */
    static Optional<int[]> seededCell(String key) {
        String[] parts = key.split(";");
        if (parts.length != 3 || MANUAL.equals(parts[1])) {
            return Optional.empty();
        }
        try {
            return Optional.of(new int[]{Integer.parseInt(parts[1]), Integer.parseInt(parts[2])});
        } catch (NumberFormatException invalid) {
            return Optional.empty();
        }
    }

    static String encodePlan(DonPollosCityLayout.CityPlan plan) {
        return plan.x() + "," + plan.z() + "," + plan.rotation() + "," + plan.sizeX() + "," + plan.sizeZ();
    }

    static Optional<DonPollosCityLayout.CityPlan> decodePlan(String text) {
        String[] parts = text.split(",");
        if (parts.length != 5) {
            return Optional.empty();
        }
        try {
            int rotation = Integer.parseInt(parts[2].trim());
            int sizeX = Integer.parseInt(parts[3].trim());
            int sizeZ = Integer.parseInt(parts[4].trim());
            if (rotation < 0 || rotation > 3 || sizeX < 1 || sizeZ < 1) {
                return Optional.empty();
            }
            return Optional.of(new DonPollosCityLayout.CityPlan(Integer.MIN_VALUE, Integer.MIN_VALUE,
                Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()), rotation, sizeX, sizeZ));
        } catch (NumberFormatException invalid) {
            return Optional.empty();
        }
    }
}
