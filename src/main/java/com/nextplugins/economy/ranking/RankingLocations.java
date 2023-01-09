package com.nextplugins.economy.ranking;

import com.nextplugins.economy.util.LocationSerializer;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RankingLocations {
    private final Plugin plugin;
    private final File file;
    private final FileConfiguration configuration;

    /**
     * Creates default configuration
     *
     * @param plugin the plugin
     */
    public RankingLocations(Plugin plugin) {
        this.plugin = plugin;
        this.file = Objects.requireNonNull(createFile());
        this.configuration = getAsConfiguration();
    }

    private File createFile() {
        final File file = new File(plugin.getDataFolder(), "ranking-location.yml");

        try {
            file.createNewFile();
        } catch (Exception exception) {
            exception.printStackTrace();

            return null;
        }

        return file;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public File getFile() {
        return file;
    }

    public FileConfiguration getAsConfiguration() {
        return YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Update all positions available within the list
     *
     * @param locations the location list
     */
    public void setPositions(List<String> locations) {
        configuration.set("available", locations);

        try {
            configuration.save(file);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Set all positions available within the list
     *
     * @param locations the location list
     */
    public void savePositions(List<Location> locations) {
        setPositions(locations.stream().map(LocationSerializer::to).collect(Collectors.toList()));
    }

    /**
     * Save position location to list
     * if position is less than 0, it would be 0
     * else, would be saved as position - 1
     *
     * @param position the position
     * @param location the location
     */
    public void setLocation(int position, Location location) {
        final List<String> locations = configuration.getStringList("available");

        final int index = position <= 0 ? 0 : position - 1;
        final String data = LocationSerializer.to(location);

        if (index > locations.size() - 1) locations.add(data);
        else locations.set(index, data);

        setPositions(locations);
    }

    /**
     * Gets the position location
     *
     * @param position the position
     * @return the position location or null if there's none
     */
    public Location getLocation(int position) {
        return getLocations().getOrDefault((position - 1), null);
    }

    /**
     * Gets all available locations at the moment
     *
     * @return the locations
     */
    public Map<Integer, Location> getLocations() {
        final Map<Integer, Location> positions = new LinkedHashMap<>();
        final List<String> available = configuration.getStringList("available");

        for (int index = 0; index < available.size(); index++) {
            final String location = available.get(index);

            positions.put(index, LocationSerializer.from(location));
        }

        return positions;
    }
}
