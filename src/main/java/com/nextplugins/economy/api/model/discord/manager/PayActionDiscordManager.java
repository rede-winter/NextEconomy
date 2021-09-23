package com.nextplugins.economy.api.model.discord.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.nextplugins.economy.api.model.account.storage.AccountStorage;
import com.nextplugins.economy.api.model.account.transaction.TransactionType;
import com.nextplugins.economy.api.model.discord.PayActionDiscord;
import com.nextplugins.economy.configuration.DiscordValue;
import com.nextplugins.economy.configuration.MessageValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;

import java.util.concurrent.TimeUnit;

/**
 * @author Yuhtin
 * Github: https://github.com/Yuhtin
 */
@AllArgsConstructor
public class PayActionDiscordManager {

    @Getter
    private final Cache<Long, PayActionDiscord> cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(30, TimeUnit.SECONDS)
            .removalListener((RemovalListener<Long, PayActionDiscord>) (key, value, cause) -> deny(value, cause))
            .build();

    private final AccountStorage accountStorage;

    private void deny(PayActionDiscord payActionDiscord, RemovalCause cause) {
        if (cause != RemovalCause.EXPIRED) return;
        payActionDiscord.getMessage().queue(message -> message.reply(DiscordValue.get(DiscordValue::errorEmoji) + " OPS! A transação foi cancelada. (Limite de tempo atingido)").queue());
    }

    public void confirm(PayActionDiscord payActionDiscord) {
        val player = payActionDiscord.player();
        val target = payActionDiscord.target();

        val account = accountStorage.findAccount(player);
        val targetAccount = accountStorage.findAccount(target);
        val amount = payActionDiscord.value();

        payActionDiscord.getMessage().queue(message -> {
            if (account == null || targetAccount == null) {
                // 1.10^-11 chance
                message.reply("⁉️ 404: Ocorreu um erro ao obter as contas dos jogadores envolvidos.").queue();
                return;
            }

            if (!account.hasAmount(amount)) {
                message.reply(MessageValue.get(MessageValue::noCoinsDiscord)).queue();
                return;
            }

            account.createTransaction(
                    payActionDiscord.player().isOnline() ? payActionDiscord.player().getPlayer() : null,
                    target.getName(),
                    amount,
                    0,
                    TransactionType.WITHDRAW
            );

            targetAccount.createTransaction(
                    target.isOnline() ? target.getPlayer() : null,
                    player.getName(),
                    amount,
                    0,
                    TransactionType.DEPOSIT
            );

            message.reply(MessageValue.get(MessageValue::sendedMoneyDiscord)
                    .replace("$coins", payActionDiscord.valueFormated())
                    .replace("$player", target.getName())
                    .replace("$discord", payActionDiscord.targetDiscordName())
            ).queue(message1 -> message1.addReaction("\uD83D\uDC99").queue());
        });
    }

}
