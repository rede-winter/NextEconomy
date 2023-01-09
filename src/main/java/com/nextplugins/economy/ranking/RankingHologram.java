package com.nextplugins.economy.ranking;

import com.nextplugins.economy.NextEconomy;
import com.nextplugins.economy.model.Account;
import com.nextplugins.libs.hologramwrapper.Holograms;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class RankingHologram {

    private final Holograms holograms;
    private final RankingLocations locations;

    public RankingHologram(NextEconomy instance, RankingLocations locations) {
        this.holograms = Holograms.get(instance);
        this.locations = locations;
    }

    public void update(List<Account> accounts) {
        if (holograms == null) return;

        clear();

        System.out.println(System.currentTimeMillis() + " - ATUALIZADO");

        final Location location = locations.getLocation(1);
        if (location == null) return;

        final Location baseLocation = location.clone().add(0, (12 * 0.3), 0);

        final List<String> lines = new ArrayList<>();

        lines.add("&a&lRANKING DE BALINHAS");
        lines.add("&7Atualizado a cada 10 minutos");
        lines.add("");

        for (int index = 0; index < accounts.size(); index++) {
            final Account account = accounts.get(index);
            final int position = index + 1;

            lines.add("&b&l" + position + "º &8-&7 " + account.getUsername() + " &8-&2 ❁&a"
                    + account.getBalanceFormatted());
        }

        lines.add("");

        holograms.create(baseLocation, lines);
    }

    public void clear() {
        if (holograms != null) holograms.clear();
    }
}
