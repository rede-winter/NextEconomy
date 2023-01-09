package com.nextplugins.economy.ranking;

import com.nextplugins.economy.NextEconomy;
import com.nextplugins.economy.model.Account;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class RankingRunnable implements Runnable {

    private final NextEconomy plugin;
    private final RankingHologram ranking;
    private final RankingStorage rankingStorage;

    public RankingRunnable(RankingBootstrap bootstrap) {
        this.plugin = bootstrap.getPlugin();
        this.ranking = new RankingHologram(bootstrap.getPlugin(), bootstrap.getLocations());
        this.rankingStorage = bootstrap.getPlugin().getRankingStorage();

        rankingStorage.updateRanking(true);
    }

    public void register() {
        final int updateDelay = 10 * 60; // 10 minutes

        Bukkit.getScheduler().runTaskTimer(plugin, this, 20L, updateDelay * 20L);
    }

    @Override
    public void run() {
        update();
    }

    public void update() {
        final List<Account> accounts =
                new ArrayList<>(rankingStorage.getRankByCoin().values());

        ranking.update(accounts);
    }

    public void destroy() {
        ranking.clear();
    }
}
