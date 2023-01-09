package com.nextplugins.economy.dao.repository.adapter;

import com.henryfabio.sqlprovider.executor.adapter.SQLResultAdapter;
import com.henryfabio.sqlprovider.executor.result.SimpleResultSet;
import com.nextplugins.economy.model.Account;

public final class AccountAdapter implements SQLResultAdapter<Account> {

    @Override
    public Account adaptResult(SimpleResultSet resultSet) {
        String accountOwner = resultSet.get("owner");
        double accountBalance = resultSet.get("balance");
        int receiveCoins = resultSet.get("receiveCoins");

        return Account.generate()
                .username(accountOwner)
                .balance(accountBalance)
                .receiveCoins(receiveCoins != 0)
                .result();
    }
}
