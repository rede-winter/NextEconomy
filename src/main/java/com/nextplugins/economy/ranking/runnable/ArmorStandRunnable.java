package com.nextplugins.economy.ranking.runnable;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.google.common.collect.Lists;
import com.nextplugins.economy.NextEconomy;
import com.nextplugins.economy.api.model.account.Account;
import com.nextplugins.economy.configuration.RankingValue;
import com.nextplugins.economy.ranking.manager.LocationManager;
import com.nextplugins.economy.ranking.storage.RankingStorage;
import com.nextplugins.economy.util.ItemBuilder;
import com.nextplugins.economy.util.NumberUtils;
import com.nextplugins.economy.util.TypeUtil;
import lombok.RequiredArgsConstructor;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Yuhtin
 * Github: https://github.com/Yuhtin
 */
@RequiredArgsConstructor
public final class ArmorStandRunnable implements Runnable {

    public static final List<ArmorStand> STANDS = Lists.newLinkedList();
    public static final List<Hologram> HOLOGRAM = Lists.newLinkedList();

    private static final Material[] SWORDS = new Material[] {
            Material.DIAMOND_SWORD, TypeUtil.getType("GOLD_SWORD", "GOLDEN_SWORD"),
            Material.IRON_SWORD, Material.STONE_SWORD,
            TypeUtil.getType("WOOD_SWORD", "WOODEN_SWORD")
    };

    private final NextEconomy plugin;
    private final LocationManager locationManager;
    private final RankingStorage rankingStorage;

    @Override
    public void run() {
        List<Account> accounts = rankingStorage.getRankByCoin();

        if (accounts.size() <= 0) return;

        STANDS.forEach(ArmorStand::remove);
        HOLOGRAM.forEach(Hologram::delete);

        AtomicInteger position = new AtomicInteger(1);

        for (Account account : accounts) {
            if (!locationManager.getLocationMap().containsKey(position.get())) return;

            Location location = locationManager.getLocation(position.get());
            Chunk chunk = location.getChunk();
            if (!chunk.isLoaded()) chunk.load();

            List<String> hologramLines = RankingValue.get(RankingValue::hologramLines);
            double hologramHeight = RankingValue.get(RankingValue::hologramHeight);

            if (!hologramLines.isEmpty()) {
                Location hologramLocation = location.clone().add(0, hologramHeight, 0);
                Hologram hologram = HologramsAPI.createHologram(plugin, hologramLocation);

                String format = NumberUtils.format(account.getBalance());
                for (int i = 0; i < hologramLines.size(); i++) {
                    String replacedLine = hologramLines.get(i);

                    replacedLine = replacedLine.replace("$position", String.valueOf(position.get()));
                    replacedLine = replacedLine.replace("$player", account.getUserName());
                    replacedLine = replacedLine.replace("$amount", format);

                    hologram.insertTextLine(i, replacedLine);
                }

                HOLOGRAM.add(hologram);
            }

            ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class);
            stand.setVisible(false); // show only after configuration
            stand.setSmall(true);
            stand.setCustomNameVisible(false);
            stand.setGravity(false);
            stand.setArms(true);

            int swordNumber = Math.min(SWORDS.length, position.get());

            Material sword = SWORDS[swordNumber - 1];
            stand.setItemInHand(new ItemStack(sword));

            stand.setHelmet(new ItemBuilder(account.getUserName()).wrap());
            stand.setChestplate(createDyeItem(Material.LEATHER_CHESTPLATE, getColorByHex(RankingValue.get(RankingValue::chestplateRGB))));
            stand.setLeggings(createDyeItem(Material.LEATHER_LEGGINGS, getColorByHex(RankingValue.get(RankingValue::leggingsRGB))));
            stand.setBoots(createDyeItem(Material.LEATHER_BOOTS, getColorByHex(RankingValue.get(RankingValue::bootsRGB))));

            stand.setVisible(true); // configuration finished, show stand

            STANDS.add(stand);
            position.getAndIncrement();
        }

    }

    private ItemStack createDyeItem(Material leatherPiece, Color color) {

        ItemStack item = new ItemStack(leatherPiece);

        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color);
        item.setItemMeta(meta);

        return item;

    }

    private Color getColorByHex(String hex) {

        java.awt.Color decode = java.awt.Color.decode(hex);
        return Color.fromRGB(decode.getRed(), decode.getGreen(), decode.getBlue());

    }

}

