package com.nextplugins.economy.ranking.types;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.nextplugins.economy.NextEconomy;
import com.nextplugins.economy.api.model.account.SimpleAccount;
import com.nextplugins.economy.configuration.RankingValue;
import com.nextplugins.economy.ranking.manager.LocationManager;
import com.nextplugins.economy.ranking.storage.RankingStorage;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.var;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Yuhtin
 * Github: https://github.com/Yuhtin
 */

@RequiredArgsConstructor
public final class HologramRunnable implements Runnable {

    private final NextEconomy plugin;
    private final LocationManager locationManager;
    private final RankingStorage rankingStorage;

    @Override
    public void run() {

        HologramsAPI.getHolograms(plugin).forEach(Hologram::delete);

        if (locationManager.getLocationMap().isEmpty()) return;

        val accounts = rankingStorage.getRankByCoin();
        val location = locationManager.getLocation(1);
        if (location == null || location.getWorld() == null) {

            plugin.getLogger().warning("A localização 1 do ranking é inválida.");
            return;

        }

        val chunk = location.getChunk();
        if (!chunk.isLoaded()) chunk.load(true);

        val hologramLocation = location.clone().add(0, 5, 0);
        val hologram = HologramsAPI.createHologram(plugin, hologramLocation);

        for (val line : RankingValue.get(RankingValue::hologramDefaultLines)) {

            if (line.equalsIgnoreCase("@players")) playersLines(accounts.values()).forEach(hologram::appendTextLine);
            else hologram.appendTextLine(line);

        }


    }

    public List<String> playersLines(Collection<SimpleAccount> accounts) {

        val lines = new ArrayList<String>();
        val line = RankingValue.get(RankingValue::hologramDefaultLine);

        var position = 1;
        for (val account : accounts) {
            if (position > RankingValue.get(RankingValue::hologramDefaultLimit)) break;

            lines.add(line
                    .replace("$position", String.valueOf(position))
                    .replace("$player", account.getUsername())
                    .replace("$prefix", plugin.getGroupWrapperManager().getPrefix(account.getUsername()))
                    .replace("$amount", account.getBalanceFormated())
            );

            ++position;
        }

        return lines;

    }
}