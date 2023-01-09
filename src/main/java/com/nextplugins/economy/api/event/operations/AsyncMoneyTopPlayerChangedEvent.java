package com.nextplugins.economy.api.event.operations;

import com.nextplugins.economy.api.event.EconomyEvent;
import com.nextplugins.economy.model.Account;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;

import java.time.Instant;

/**
 * @author Yuhtin
 * Github: https://github.com/Yuhtin
 */
@Getter
@Setter
public final class AsyncMoneyTopPlayerChangedEvent extends EconomyEvent implements Cancellable {

    private final Account lastMoneyTop;
    private final Account moneyTop;

    private final Instant updateInstant = Instant.now();
    private boolean cancelled;

    public AsyncMoneyTopPlayerChangedEvent(Account lastMoneyTop, Account moneyTop) {
        super(true);
        this.lastMoneyTop = lastMoneyTop;
        this.moneyTop = moneyTop;
    }
}
