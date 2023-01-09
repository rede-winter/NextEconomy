package com.nextplugins.economy.ranking;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nextplugins.economy.NextEconomy;
import com.nextplugins.economy.api.event.operations.AsyncMoneyTopPlayerChangedEvent;
import com.nextplugins.economy.api.event.operations.AsyncRankingUpdateEvent;
import com.nextplugins.economy.configuration.RankingValue;
import com.nextplugins.economy.dao.repository.AccountRepository;
import com.nextplugins.economy.group.Group;
import com.nextplugins.economy.group.GroupWrapperManager;
import com.nextplugins.economy.model.Account;
import com.nextplugins.economy.util.ColorUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

@Data
@RequiredArgsConstructor
public final class RankingStorage {

    private final LinkedHashMap<String, Account> rankByCoin = Maps.newLinkedHashMap();

    private final AccountRepository repository;
    private final GroupWrapperManager groupManager;
    private final RankingChatBody rankingChatBody;

    private String topPlayer;
    private long nextUpdateMillis;
    private boolean updating;

    public boolean updateRanking(boolean force) {
        if (!force && nextUpdateMillis > System.currentTimeMillis()) return false;

        val updateDelayMillis = TimeUnit.MINUTES.toMillis(10);

        nextUpdateMillis = System.currentTimeMillis() + updateDelayMillis;

        update();

        return true;
    }

    public String getTycoonTag(String playerName) {
        if (!rankByCoin.containsKey(playerName)) return "";

        return topPlayer.equals(playerName)
                ? RankingValue.get(RankingValue::tycoonTagValue)
                : RankingValue.get(RankingValue::tycoonRichTagValue);
    }

    private void update() {
        Bukkit.getScheduler().runTaskAsynchronously(NextEconomy.getInstance(), () -> {
            val pluginManager = Bukkit.getPluginManager();

            Account lastAccount = null;
            if (!getRankByCoin().isEmpty()) {
                lastAccount = getTopPlayer();
            }

            NextEconomy.getInstance().getAccountStorage().flushData();

            getRankByCoin().clear();

            val accounts =
                    repository.selectAll("ORDER BY balance DESC LIMIT " + RankingValue.get(RankingValue::rankingLimit));

            if (!accounts.isEmpty()) {
                val rankingType = RankingValue.get(RankingValue::rankingType);
                val tycoonTag = RankingValue.get(RankingValue::tycoonTagValue);
                val chatRanking = rankingType.equals("CHAT");

                val bodyLines = new LinkedList<String>();
                int position = 1;
                for (val account : accounts) {
                    if (position == 1) setTopPlayer(account.getUsername());
                    getRankByCoin().put(account.getUsername(), account);

                    final Group group = groupManager.getGroup(account.getUsername());
                    if (chatRanking) {
                        val body = RankingValue.get(RankingValue::chatModelBody);
                        bodyLines.add(body.replace("$position", String.valueOf(position))
                                .replace("$prefix", group.getPrefix())
                                .replace("$suffix", group.getSuffix())
                                .replace("$player", account.getUsername())
                                .replace("$tycoon", position == 1 ? tycoonTag : "")
                                .replace("$amount", account.getBalanceFormatted()));
                    }

                    position++;
                }

                rankingChatBody.setBodyLines(bodyLines.toArray(new String[] {}));

                if (lastAccount != null) {
                    val topAccount = getTopPlayer();
                    if (!lastAccount.getUsername().equals(topAccount))
                        pluginManager.callEvent(new AsyncMoneyTopPlayerChangedEvent(lastAccount, getTopPlayer()));
                }

            } else {
                rankingChatBody.setBodyLines(new String[] {ColorUtil.colored("  &cNenhum jogador está no ranking!")});
            }
        });
    }

    /**
     * Retrieve top player
     *
     * @return player's account
     */
    public @Nullable Account getTopPlayer() {
        if (rankByCoin.isEmpty()) return null;
        else return rankByCoin.get(topPlayer);
    }
}
