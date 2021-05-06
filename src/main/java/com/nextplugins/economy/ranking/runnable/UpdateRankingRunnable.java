package com.nextplugins.economy.ranking.runnable;

import com.google.common.collect.Lists;
import com.nextplugins.economy.api.event.operations.MoneyTopPlayerUpdateEvent;
import com.nextplugins.economy.api.model.account.Account;
import com.nextplugins.economy.configuration.FeatureValue;
import com.nextplugins.economy.configuration.RankingValue;
import com.nextplugins.economy.dao.repository.AccountRepository;
import com.nextplugins.economy.ranking.storage.RankingStorage;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Bukkit;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public final class UpdateRankingRunnable implements Runnable {

    private final AccountRepository accountRepository;
    private final RankingStorage rankingStorage;

    @Override
    public void run() {

        List<Account> accounts = Lists.newLinkedList(accountRepository.selectAll(
                "ORDER BY balance DESC LIMIT 10" + RankingValue.get(RankingValue::rankingLimit)
        ));

        List<Account> accountsMovimentation = Lists.newLinkedList(accountRepository.selectAll(
                "ORDER BY movimentedBalance DESC LIMIT 10" + RankingValue.get(RankingValue::rankingLimit)
        ));

        if (!accounts.isEmpty()) {

            Account lastAccount = null;
            if (!rankingStorage.getRankByCoin().isEmpty()) {

                lastAccount = rankingStorage.getRankByCoin().get(0);
                rankingStorage.getRankByCoin().clear();

            }

            accounts.forEach(rankingStorage.getRankByCoin()::add);

            if (lastAccount != null) {

                Account topAccount = rankingStorage.getRankByCoin().get(0);
                if (lastAccount.getUserName().equals(topAccount.getUserName())) return;

                Bukkit.getPluginManager().callEvent(
                        MoneyTopPlayerUpdateEvent.builder()
                                .lastMoneyTop(lastAccount)
                                .moneyTop(topAccount)
                                .updateInstant(Instant.now())
                                .async(true)
                                .build()
                );

            }

        }

        if (!accountsMovimentation.isEmpty()) {

            rankingStorage.getRankByMovimentation().clear();

            List<Account> accounts1 = rankingStorage.getRankByMovimentation();
            accounts1.addAll(accountsMovimentation);
        }

        val updateDelayMillis = TimeUnit.SECONDS.toMillis(RankingValue.get(RankingValue::updateDelay));

        rankingStorage.setNextUpdateMillis(
                System.currentTimeMillis() + updateDelayMillis
        );

    }

}