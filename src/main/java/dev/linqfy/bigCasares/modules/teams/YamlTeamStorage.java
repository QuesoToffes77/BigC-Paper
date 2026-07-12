package dev.linqfy.bigCasares.modules.teams;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class YamlTeamStorage implements TeamStorage {
    private final File file;
    private final InMemoryTeamStorage delegate = new InMemoryTeamStorage();
    private final TeamYamlMapper mapper = new TeamYamlMapper();

    public YamlTeamStorage(File file) {
        this.file = file;
        load();
    }

    @Override
    public synchronized Collection<Team> findAll() {
        return delegate.findAll();
    }

    @Override
    public synchronized Optional<Team> findById(TeamId teamId) {
        return delegate.findById(teamId);
    }

    @Override
    public synchronized void save(Team team) {
        delegate.save(team);
        flush();
    }

    @Override
    public synchronized void delete(TeamId teamId) {
        delegate.delete(teamId);
        flush();
    }

    private void load() {
        if (!file.isFile()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection teams = yaml.getConfigurationSection("teams");
        if (teams == null) {
            return;
        }
        for (String key : teams.getKeys(false)) {
            ConfigurationSection section = teams.getConfigurationSection(key);
            if (section != null) {
                Map<String, Object> values = new LinkedHashMap<>(section.getValues(false));
                ConfigurationSection members = section.getConfigurationSection("members");
                if (members != null) {
                    values.put("members", members.getValues(false));
                }
                delegate.save(mapper.fromMap(values));
            }
        }
    }

    private void flush() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Team team : delegate.findAll()) {
            yaml.createSection("teams." + team.id(), mapper.toMap(team));
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("No se pudo crear el directorio de equipos: " + parent);
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudieron guardar los equipos en " + file, ex);
        }
    }
}
