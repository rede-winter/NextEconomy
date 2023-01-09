package com.nextplugins.economy.ranking;

import com.nextplugins.economy.NextEconomy;
import lombok.Getter;

@Getter
public class RankingBootstrap {

    private final NextEconomy plugin;
    private final RankingLocations locations;
    private final RankingRunnable rankingRunnable;

    public RankingBootstrap(NextEconomy plugin) {
        this.plugin = plugin;

        this.locations = new RankingLocations(plugin);
        this.rankingRunnable = new RankingRunnable(this);
        this.rankingRunnable.register();
    }

    public void shutdown() {
        rankingRunnable.destroy();
    }
}
