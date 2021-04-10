package com.nextplugins.economy.dao.adapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.henryfabio.sqlprovider.executor.adapter.SQLResultAdapter;
import com.henryfabio.sqlprovider.executor.result.SimpleResultSet;
import com.nextplugins.economy.api.model.account.Account;
import com.nextplugins.economy.util.LinkedListHelper;

public final class AccountAdapter implements SQLResultAdapter<Account> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public Account adaptResult(SimpleResultSet resultSet) {

        String accountOwner = resultSet.get("owner");
        double accountBalance = resultSet.get("balance");
        double movimentedBalance = resultSet.get("movimentedBalance");
        String transactions = resultSet.get("transactions");

        return Account.create(
                accountOwner,
                accountBalance,
                movimentedBalance,
                LinkedListHelper.fromJson(transactions)
        );

    }

}
