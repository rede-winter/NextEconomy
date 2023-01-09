package com.nextplugins.economy.model;

import com.nextplugins.economy.NextEconomy;
import com.nextplugins.economy.api.event.operations.MoneyChangeEvent;
import com.nextplugins.economy.configuration.FeatureValue;
import com.nextplugins.economy.model.transaction.Transaction;
import com.nextplugins.economy.model.transaction.TransactionType;
import com.nextplugins.economy.util.NumberUtils;
import lombok.*;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

@Data
@Builder(builderMethodName = "generate", buildMethodName = "result")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    private String username;
    private final String uuid;
    private double balance;

    @Builder.Default
    private boolean receiveCoins = true;

    public static Account createDefault(OfflinePlayer player) {
        val accountBuilder =
                Account.generate().username(player.getName()).balance(FeatureValue.get(FeatureValue::initialBalance));

        if (!NextEconomy.getInstance().getAccountStorage().isNickMode()) {
            accountBuilder.uuid(player.getUniqueId().toString());
        }

        return accountBuilder.result();
    }

    /**
     * Create account
     *
     * @param name    of player
     * @param balance start balance
     * @return a new {@link Account}
     * @deprecated Since 2.0.0
     */
    @Deprecated
    public static Account create(@NotNull String name, double balance) {
        return new Account(name, "", balance, true);
    }

    public String getIdentifier() {
        return uuid == null ? username : uuid;
    }

    public synchronized double getBalance() {
        return this.balance;
    }

    public synchronized String getBalanceFormatted() {
        return NumberUtils.format(getBalance());
    }

    public synchronized void setBalance(double quantity) {
        if (NumberUtils.isInvalid(quantity)) return;
        this.balance = quantity;
        fastSave();
    }

    public synchronized void deposit(double quantity) {
        if (NumberUtils.isInvalid(quantity)) return;
        this.balance += quantity;
        fastSave();
    }

    public synchronized EconomyResponse createTransaction(@NotNull Transaction transaction) {
        val amount = transaction.amount();
        if (NumberUtils.isInvalid(amount)) {
            return new EconomyResponse(
                    amount, balance, EconomyResponse.ResponseType.FAILURE, "O valor inserido é inválido.");
        }

        val transactionType = transaction.transactionType();
        if (transactionType == TransactionType.WITHDRAW) {
            if (!hasAmount(amount)) {
                return new EconomyResponse(
                        amount,
                        balance,
                        EconomyResponse.ResponseType.FAILURE,
                        "Não foi possível terminar esta operação. "
                                + "(A conta requisitada não possui quantia suficiente para completar esta transação).");
            }

            this.balance -= amount;
        } else this.balance += amount;
        if (this.balance < 0) this.balance = 0;

        if (transaction.owner() != null) {
            save();
        } else {
            fastSave();
        }

        increaseData(transactionType);

        val player = transaction.player();
        if (player != null) {
            val moneyChangeEvent = new MoneyChangeEvent(player, this, balance, NumberUtils.format(balance));
            Bukkit.getScheduler().runTask(NextEconomy.getInstance(), () -> Bukkit.getPluginManager()
                    .callEvent(moneyChangeEvent));
        }

        return new EconomyResponse(
                amount, balance, EconomyResponse.ResponseType.SUCCESS, "Operação realizada com sucesso.");
    }

    private void increaseData(TransactionType transactionType) {
        val accountStorage = NextEconomy.getInstance().getAccountStorage();
        accountStorage.increaseTransactionCount(transactionType);
    }

    public void save() {
        val accountStorage = NextEconomy.getInstance().getAccountStorage();
        Bukkit.getScheduler().runTaskAsynchronously(NextEconomy.getInstance(), () -> accountStorage.saveOne(this));
    }

    /**
     * Only saves the account balance
     */
    public void fastSave() {
        val accountStorage = NextEconomy.getInstance().getAccountStorage();
        Bukkit.getScheduler()
                .runTaskAsynchronously(
                        NextEconomy.getInstance(), () -> accountStorage.fastSaveOne(getIdentifier(), getBalance()));
    }

    public synchronized boolean hasAmount(double amount) {
        if (NumberUtils.isInvalid(amount)) return false;
        return this.balance >= amount;
    }
}
