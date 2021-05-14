package com.nextplugins.economy.command;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.henryfabio.sqlprovider.executor.SQLExecutor;
import com.nextplugins.economy.NextEconomy;
import com.nextplugins.economy.api.backup.BackupManager;
import com.nextplugins.economy.api.conversor.ConversorManager;
import com.nextplugins.economy.api.model.account.Account;
import com.nextplugins.economy.configuration.MessageValue;
import com.nextplugins.economy.dao.SQLProvider;
import com.nextplugins.economy.dao.repository.AccountRepository;
import com.nextplugins.economy.util.ColorUtil;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.var;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.annotation.Optional;
import me.saiintbrisson.minecraft.command.command.Context;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

/**
 * @author Yuhtin
 * Github: https://github.com/Yuhtin
 */

@RequiredArgsConstructor
public final class NextEconomyCommand {

    private final BackupManager backupManager;
    private final AccountRepository accountRepository;
    private final ConversorManager conversorManager;

    @Command(
            name = "nexteconomy",
            aliases = {"economy", "eco", "ne"},
            permission = "nexteconomy.admin",
            async = true
    )
    public void onCommand(Context<CommandSender> context) {
        for (String s : ColorUtil.colored(MessageValue.get(MessageValue::adminCommand))) {
            context.sendMessage(s);
        }
    }

    @Command(
            name = "nexteconomy.backup",
            permission = "nexteconomy.admin",
            async = true
    )
    public void onBackupCommand(Context<CommandSender> context,
                                @Optional String name) {

        val accounts = accountRepository.selectAll("");

        context.sendMessage(ColorUtil.colored("&aIniciando criação do backup."));

        val backup = backupManager.createBackup(
                context.getSender(),
                name,
                Lists.newArrayList(accounts),
                false, true
        );

        if (backup != null) return;

        context.sendMessage(ColorUtil.colored(
                "&cJá existe um backup com este nome",
                "&a&LDICA: &fDeixe o nome vazio para gerar um backup com a data e hora atual."
        ));

    }

    @Command(
            name = "nexteconomy.readbackup",
            permission = "nexteconomy.admin",
            usage = "/ne readbackup (nome)",
            async = true
    )
    public void onReadBackupCommand(Context<CommandSender> context,
                                    String name) {

        var fileName = name.endsWith(".json") ? name : name + ".json";

        val file = new File(NextEconomy.getInstance().getDataFolder(), "backups/" + fileName);
        if (!file.exists()) {

            val folder = new File(NextEconomy.getInstance().getDataFolder(), "backups/");
            val files = folder.list();
            if (files == null) {

                context.sendMessage(ColorUtil.colored(
                        "&cVocê não possui nenhum backup criado."
                ));
                return;

            }

            context.sendMessage(ColorUtil.colored(
                    "&cO nome de backup inserido é inválido, valores válidos:",
                    "&e" + Arrays.asList(files)
            ));
            return;

        }

        context.sendMessage(ColorUtil.colored(
                "&aIniciando leitura do backup &2" + fileName + "&a."
        ));

        backupManager.loadBackup(context.getSender(), file, false, true);

    }

    @Command(
            name = "nexteconomy.converter",
            permission = "nexteconomy.admin",
            usage = "/ne converter <tomysql, tosqlite>"
    )
    public void onConverterCommand(Context<CommandSender> context,
                                   String option) {

        if (!conversorManager.checkConversorAvaility(context.getSender())) {

            context.sendMessage(ColorUtil.colored(
                    "&cNenhum jogador pode estar online para efetuar esta ação."
            ));
            return;

        }

        if (!option.equalsIgnoreCase("tomysql") && !option.equalsIgnoreCase("tosqlite")) {

            context.sendMessage(ColorUtil.colored(
                    "&cConversores válidos: tomysql, tosqlite"
            ));
            return;

        }

        val stopwatch = Stopwatch.createStarted();
        conversorManager.setConverting(true);

        val jdbcUrl = accountRepository.getSqlExecutor()
                .getSqlConnector()
                .getDatabaseType()
                .getJdbcUrl();

        if (option.equalsIgnoreCase("tomysql") && !jdbcUrl.contains("mysql")) {

            context.sendMessage(ColorUtil.colored(
                    "&cVocê precisa habilitar o mysql na config e reiniciar o servidor antes."
            ));
            return;

        } else if (option.equalsIgnoreCase("tosqlite") && !jdbcUrl.contains("sqlite")) {

            context.sendMessage(ColorUtil.colored(
                    "&cVocê precisa desabilitar o mysql na config e reiniciar o servidor antes."
            ));
            return;

        }

        val sqlProvider = SQLProvider.of(NextEconomy.getInstance());
        val sqlConnector = sqlProvider.setup(option.replace("to", ""));
        val repository = new AccountRepository(new SQLExecutor(sqlConnector));

        val accounts = new HashSet<Account>();
        for (Account account : repository.selectAll("")) {

            if (account == null) continue;
            accounts.add(account);

        }

        if (accounts.isEmpty()) {

            context.sendMessage(ColorUtil.colored(
                    "&aNão tem nenhum dado para converter."
            ));
            return;

        }

        conversorManager.startConversion(
                context.getSender(),
                accounts,
                option,
                stopwatch
        );

    }

}
