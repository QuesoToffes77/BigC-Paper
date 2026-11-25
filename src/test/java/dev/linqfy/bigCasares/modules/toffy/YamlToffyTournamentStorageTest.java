package dev.linqfy.bigCasares.modules.toffy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlToffyTournamentStorageTest {
    private static final UUID ANA = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BEN = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @TempDir
    Path directory;

    @Test
    void roundTripsEverythingThroughTheService() throws IOException {
        Path file = directory.resolve("data").resolve("torneos.yml");
        YamlToffyTournamentStorage storage = new YamlToffyTournamentStorage(file);
        ToffyTournamentService service = new ToffyTournamentService(storage, List.of());
        service.create("Copa-1");
        service.register("copa-1", ANA, "Ana");
        service.register("copa-1", BEN, "Ben");
        service.close("copa-1");
        service.setMessage("copa-1", "cierre", "&cCerrado {torneo}: x");
        service.recordDeparture("copa-1", ANA, new ToffyStoredLocation("world.nether", 1.5, 64, -2.25, 90f, 10.5f));
        service.markPending("copa-1", ANA);
        service.create("liga");

        List<ToffyTournament> loaded = new YamlToffyTournamentStorage(file).load();

        assertEquals(List.of("copa-1", "liga"), loaded.stream().map(ToffyTournament::key).toList());
        ToffyTournament copa = loaded.getFirst();
        assertEquals("Copa-1", copa.displayName());
        assertEquals(false, copa.open());
        assertEquals(List.of(ANA, BEN), List.copyOf(copa.participants().keySet()));
        assertEquals("Ben", copa.participants().get(BEN));
        assertEquals("&cCerrado {torneo}: x", copa.message("cierre"));
        assertEquals(new ToffyStoredLocation("world.nether", 1.5, 64, -2.25, 90f, 10.5f), copa.returns().get(ANA));
        assertTrue(copa.pending().contains(ANA));
        assertTrue(loaded.get(1).open());
        assertTrue(loaded.get(1).participants().isEmpty());
    }

    @Test
    void thePlaceAndTheScheduleSurviveARestart() throws IOException {
        Path file = directory.resolve("torneos.yml");
        ToffyTournamentService service = new ToffyTournamentService(new YamlToffyTournamentStorage(file), List.of());
        service.create("copa");
        ToffyStoredLocation place = new ToffyStoredLocation("world_nether", -12.5, 70.25, 300.0, -45.5f, 12f);
        service.setPlace("copa", place);
        long at = 1_900_000_000_000L;
        service.schedule("copa", at);
        service.create("liga");
        service.setPlace("liga", new ToffyStoredLocation("world", 0, 64, 0, 0f, 0f));
        service.schedule("liga", at + 60_000L);
        service.cancelSchedule("liga");

        List<ToffyTournament> loaded = new YamlToffyTournamentStorage(file).load();

        ToffyTournament copa = loaded.getFirst();
        assertEquals(place, copa.place());
        assertEquals(Long.valueOf(at), copa.scheduledAt());
        ToffyTournament liga = loaded.get(1);
        assertEquals(new ToffyStoredLocation("world", 0, 64, 0, 0f, 0f), liga.place());
        assertEquals(null, liga.scheduledAt());
    }

    @Test
    void aTournamentWithoutPlaceOrScheduleLoadsWithoutThem() throws IOException {
        Path file = directory.resolve("torneos.yml");
        ToffyTournamentService service = new ToffyTournamentService(new YamlToffyTournamentStorage(file), List.of());
        service.create("copa");

        ToffyTournament copa = new YamlToffyTournamentStorage(file).load().getFirst();

        assertEquals(null, copa.place());
        assertEquals(null, copa.scheduledAt());
    }

    @Test
    void aMissingFileIsEmpty() throws IOException {
        assertTrue(new YamlToffyTournamentStorage(directory.resolve("torneos.yml")).load().isEmpty());
    }

    @Test
    void brokenFilesAreQuarantinedBeforeFailing() throws IOException {
        Path file = directory.resolve("torneos.yml");
        YamlToffyTournamentStorage storage = new YamlToffyTournamentStorage(file);

        for (String broken : new String[] {"tournaments: [unclosed\n",
            "tournaments:\n  copa:\n    participants:\n      no-es-uuid: Ana\n",
            "tournaments:\n  'mal nombre':\n    open: true\n"}) {
            Files.writeString(file, broken);
            assertThrows(IOException.class, storage::load);
            assertTrue(Files.notExists(file));
            assertTrue(storage.load().isEmpty());
        }
        try (var kept = Files.list(directory)) {
            assertEquals(3, kept.filter(path -> path.getFileName().toString().startsWith("torneos.yml.broken-")).count());
        }
    }
}
