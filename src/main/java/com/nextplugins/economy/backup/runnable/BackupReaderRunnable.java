package com.nextplugins.economy.backup.runnable;

import com.google.common.base.Stopwatch;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nextplugins.economy.NextEconomy;
import com.nextplugins.economy.backup.BackupManager;
import com.nextplugins.economy.backup.response.ResponseType;
import com.nextplugins.economy.util.ColorUtil;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.command.CommandSender;

import java.io.File;

/**
 * @author Yuhtin
 * Github: https://github.com/Yuhtin
 */
@RequiredArgsConstructor
public final class BackupReaderRunnable implements Runnable {

    private static final Gson PARSER = new GsonBuilder().setPrettyPrinting().create();

    private final CommandSender commandSender;
    private final BackupManager backupManager;
    private final boolean restauration;
    private final File file;

    @Override
    public void run() {
        File restaurationFile = null;

        val logger = NextEconomy.getInstance().getLogger();
        try {
            val accountRepository = NextEconomy.getInstance().getAccountRepository();

            if (!restauration) {
                backupManager.setBackuping(false);

                logger.info("Criando um ponto de restauração para caso ocorra um erro.");

                val response = backupManager.createBackup(null, null, accountRepository, true, true);

                if (response.getResponseType() == ResponseType.SUCCESS) {
                    restaurationFile = response.getFile();
                } else {
                    logger.warning("O ponto de restauração não pode ser criado. (" + response.getResponseType() + ")");
                    logger.warning("Cancelando o carregamento do backup.");

                    commandSender.sendMessage(ColorUtil.colored("&cOcorreu um erro, observe o console!"));
                    return;
                }
            }

            backupManager.setBackuping(true);

            accountRepository.recreateTable();
            logger.warning("Tabela com as contas do servidor foi apagada!");

            val type = restauration ? "ponto de restauração" : "backup";
            logger.info("Iniciando leitura do " + type + ".");

            val stopwatch = Stopwatch.createStarted();

            logger.info("A leitura do " + type + " '" + this.file.getName()
                    + "' foi finalizada e os valores da tabela alterados. (" + stopwatch + ")");
            backupManager.setBackuping(false);

        } catch (Throwable t) {
            backupManager.setBackuping(false);

            Thread.currentThread().interrupt();

            t.printStackTrace();
            logger.severe("Não foi possível ler os dados do arquivo.");

            if (restaurationFile != null) {
                logger.severe("Tentando utilizar o backup de restauração!");
                backupManager.loadBackup(commandSender, restaurationFile, true, true);
            }
        }
    }
}
