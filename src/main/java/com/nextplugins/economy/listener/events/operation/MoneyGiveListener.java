package com.nextplugins.economy.listener.events.operation;

import com.nextplugins.economy.NextEconomy;
import com.nextplugins.economy.api.event.operations.MoneyGiveEvent;
import com.nextplugins.economy.api.model.account.Account;
import com.nextplugins.economy.api.model.account.transaction.TransactionType;
import com.nextplugins.economy.configuration.values.MessageValue;
import com.nextplugins.economy.storage.AccountStorage;
import com.nextplugins.economy.util.NumberUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class MoneyGiveListener implements Listener {

    protected final AccountStorage accountStorage = NextEconomy.getInstance().getAccountStorage();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeposit(MoneyGiveEvent event) {

        CommandSender sender = event.getSender();
        OfflinePlayer target = event.getTarget();
        double amount = event.getAmount();

        Account targetAccount = accountStorage.findOfflineAccount(target.getName());
        if (targetAccount == null) {

            sender.sendMessage(MessageValue.get(MessageValue::invalidTarget));
            return;

        }

        if (Double.isNaN(amount) || amount < 1) {

            sender.sendMessage(MessageValue.get(MessageValue::invalidMoney));
            return;

        }

        targetAccount.createTransaction(
                MessageValue.get(MessageValue::mainAccountName),
                amount,
                TransactionType.DEPOSIT
        );

        sender.sendMessage(MessageValue.get(MessageValue::addAmount)
                .replace("$player", target.getName())
                .replace("$amount", NumberUtils.format(amount))
        );
    }

}