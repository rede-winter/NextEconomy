package com.nextplugins.economy.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import static java.lang.Double.parseDouble;
import static java.lang.Float.parseFloat;

public final class LocationSerializer {

    private LocationSerializer() {}

    public static Location from(String info) {
        final String[] data = info.split("\t");

        final World world = Bukkit.getWorld(data[0]);

        final double x = parseDouble(data[1]);
        final double y = parseDouble(data[2]);
        final double z = parseDouble(data[3]);

        final float yaw = parseFloat(data[4]);
        final float pitch = parseFloat(data[5]);

        return new Location(world, x, y, z, yaw, pitch);
    }

    public static String to(Location info) {
        return String.format(
                "%s\t%s\t%s\t%s\t%s\t%s",
                info.getWorld().getName(), info.getX(), info.getY(), info.getZ(), info.getYaw(), info.getPitch());
    }
}
