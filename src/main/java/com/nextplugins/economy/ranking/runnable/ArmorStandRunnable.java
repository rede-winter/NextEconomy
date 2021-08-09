package com.nextplugins.economy.ranking.runnable;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.google.common.collect.Lists;
import com.nextplugins.economy.NextEconomy;
import com.nextplugins.economy.configuration.RankingValue;
import com.nextplugins.economy.ranking.manager.LocationManager;
import com.nextplugins.economy.ranking.storage.RankingStorage;
import com.nextplugins.economy.util.ColorUtil;
import com.nextplugins.economy.util.ItemBuilder;
import com.nextplugins.economy.util.TypeUtil;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Yuhtin
 * Github: https://github.com/Yuhtin
 */
@RequiredArgsConstructor
public final class ArmorStandRunnable implements Runnable {

    public static final List<UUID> STANDS = Lists.newLinkedList();

    private static final Material[] SWORDS = new Material[]{
            Material.DIAMOND_SWORD, TypeUtil.swapLegacy("GOLDEN_SWORD", "GOLD_SWORD"),
            Material.IRON_SWORD, Material.STONE_SWORD,
            TypeUtil.swapLegacy("WOODEN_SWORD", "WOOD_SWORD")
    };

    private final NextEconomy plugin;
    private final LocationManager locationManager;
    private final RankingStorage rankingStorage;

    @Override
    public void run() {

        getArmorStands().forEach(ArmorStand::remove);
        HologramsAPI.getHolograms(plugin).forEach(Hologram::delete);

        STANDS.clear();

        if (locationManager.getLocationMap().isEmpty()) return;

        val accounts = rankingStorage.getRankByCoin();
        if (accounts.isEmpty()) return;

        val position = new AtomicInteger(1);

        val hologramLines = RankingValue.get(RankingValue::hologramArmorStandLines);
        for (val account : accounts) {

            val location = locationManager.getLocation(position.get());
            if (location == null || location.getWorld() == null) {

                plugin.getLogger().warning("A localização " + position.get() + " do ranking é inválida.");
                continue;

            }

            val chunk = location.getChunk();
            if (!chunk.isLoaded()) chunk.load(true);

            if (!hologramLines.isEmpty()) {
                val hologramLocation = location.clone().add(0, 2.15, 0);
                val hologram = HologramsAPI.createHologram(plugin, hologramLocation);

                val format = account.getBalanceFormated();
                for (int i = 0; i < hologramLines.size(); i++) {
                    hologram.insertTextLine(i, hologramLines.get(i)
                            .replace("$position", String.valueOf(position.get()))
                            .replace("$player", account.getUsername())
                            .replace("$prefix", plugin.getGroupWrapperManager().getPrefix(account.getUsername()))
                            .replace("$amount", format)
                    );
                }

            }

            val stand = location.getWorld().spawn(location, ArmorStand.class);
            stand.setVisible(false); // show only after configuration
            stand.setMetadata("nexteconomy", new FixedMetadataValue(plugin, true));
            stand.setSmall(RankingValue.get(RankingValue::hologramHeight).equalsIgnoreCase("SMALL"));
            stand.setCustomNameVisible(false);
            stand.setGravity(false);
            stand.setArms(true);

            val swordNumber = Math.min(SWORDS.length, position.get());

            val sword = SWORDS[swordNumber - 1];
            stand.setItemInHand(new ItemStack(sword));

            stand.setHelmet(new ItemBuilder(account.getUsername()).wrap());

            stand.setChestplate(new ItemBuilder(
                    Material.LEATHER_CHESTPLATE,
                    ColorUtil.getBukkitColorByHex(RankingValue.get(RankingValue::chestplateRGB))
            ).wrap());

            stand.setLeggings(new ItemBuilder(
                    Material.LEATHER_LEGGINGS,
                    ColorUtil.getBukkitColorByHex(RankingValue.get(RankingValue::leggingsRGB))
            ).wrap());

            stand.setBoots(new ItemBuilder(
                    Material.LEATHER_BOOTS,
                    ColorUtil.getBukkitColorByHex(RankingValue.get(RankingValue::bootsRGB))
            ).wrap());

            stand.setVisible(true); // configuration finished, show stand

            STANDS.add(stand.getUniqueId());
            position.getAndIncrement();
        }

    }

    public static List<ArmorStand> getArmorStands() {
        return STANDS.stream()
                .map(Bukkit::getEntity)
                .map(ArmorStand.class::cast)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

}

