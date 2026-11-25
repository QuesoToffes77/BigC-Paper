package dev.linqfy.bigCasares.modules.toffy;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

interface ToffyTournamentStorage {
    List<ToffyTournament> load() throws IOException;

    void save(Collection<ToffyTournament> tournaments) throws IOException;
}
