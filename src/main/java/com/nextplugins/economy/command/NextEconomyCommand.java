package com.nextplugins.economy.command;

import com.nextplugins.economy.NextEconomy;
import com.nextplugins.economy.backup.BackupManager;
import com.nextplugins.economy.backup.response.ResponseType;
import com.nextplugins.economy.configuration.MessageValue;
import com.nextplugins.economy.model.storage.AccountStorage;
import com.nextplugins.economy.ranking.RankingStorage;
import com.nextplugins.economy.util.ColorUtil;
import lombok.RequiredArgsConstructor;
import lombok.val;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.annotation.Optional;
import me.saiintbrisson.minecraft.command.command.Context;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.Arrays;

/**
 * @author Yuhtin
 * Github: https://github.com/Yuhtin
 */
@RequiredArgsConstructor
public final class NextEconomyCommand {

    private final BackupManager backupManager;
    private final RankingStorage rankingStorage;
    private final AccountStorage accountStorage;

    @Command(
            name = "nexteconomy",
            aliases = {"economy", "neco", "eco", "ne"},
            permission = "nexteconomy.admin",
            async = true)
    public void onCommand(Context<CommandSender> context) {
        for (String s : ColorUtil.colored(MessageValue.get(MessageValue::adminCommand))) {
            context.sendMessage(s);
        }
    }

    @Command(name = "nexteconomy.backup", permission = "nexteconomy.admin", async = true)
    public void onBackupCommand(Context<CommandSender> context, @Optional String name) {
        context.sendMessage(ColorUtil.colored("&aIniciando criação do backup."));

        val backup = backupManager.createBackup(
                context.getSender(), name, accountStorage.getAccountRepository(), false, true);

        if (backup.getResponseType() == ResponseType.NAME_IN_USE) {
            context.sendMessage(ColorUtil.colored(
                    "&cJá existe um backup com este nome",
                    "&a&LDICA: &fDeixe o nome vazio para gerar um backup com a data e hora atual."));
            return;
        }

        if (backup.getResponseType() == ResponseType.BACKUP_IN_PROGRESS) {
            context.sendMessage(ColorUtil.colored("&cJá existe um backup em andamento."));
        }
    }

    @Command(name = "nexteconomy.forceupdate", permission = "nexteconomy.admin", async = true)
    public void onForceRankingUpdate(Context<CommandSender> context) {
        context.sendMessage(ColorUtil.colored("&aIniciando atualização do ranking"));

        if (!rankingStorage.updateRanking(true)) {
            context.sendMessage(ColorUtil.colored("&cNão foi possível atualizar o ranking"));
            return;
        }

        context.sendMessage(ColorUtil.colored("&aRanking atualizado com sucesso."));
    }

    @Command(name = "nexteconomy.rankingdebug", permission = "nexteconomy.admin", async = true)
    public void onRankingDebug(Context<CommandSender> context) {
        val pluginManager = Bukkit.getPluginManager();
        if (!pluginManager.isPluginEnabled("HolographicDisplays")) {
            context.sendMessage(
                    ColorUtil.colored("&cO plugin HolographicDisplays precisa estar ativo para usar esta função."));
            return;
        }

        for (val world : Bukkit.getWorlds()) {
            for (val entity : world.getEntities()) {
                if (!entity.hasMetadata("nexteconomy")) continue;
                entity.remove();
            }
        }

        context.sendMessage(ColorUtil.colored("&aTodos os NPCs, ArmorStands e Hologramas foram limpos com sucesso."));
    }

    @Command(
            name = "nexteconomy.read",
            permission = "nexteconomy.admin",
            usage = "/ne read (nome) (backup ou restaurar)",
            async = true)
    public void onReadBackupCommand(Context<CommandSender> context, String name, String type) {

        if (!type.equalsIgnoreCase("backup") && !type.equalsIgnoreCase("restaurar")) {

            context.sendMessage(ColorUtil.colored("&cTipo inválido."));
            return;
        }

        var folderName = type.equalsIgnoreCase("restaurar") ? "restauration" : "backups";
        var typeFancy = type.equalsIgnoreCase("restaurar") ? "ponto de restauração" : "backup";

        var fileName = name.endsWith(".json") ? name : name + ".json";

        val file = new File(NextEconomy.getInstance().getDataFolder(), folderName + "/" + fileName);
        if (!file.exists()) {

            val folder = new File(NextEconomy.getInstance().getDataFolder(), folderName);
            val files = folder.list();
            if (files == null) {

                context.sendMessage(ColorUtil.colored("&cVocê não possui nenhum " + typeFancy + " criado."));
                return;
            }

            context.sendMessage(ColorUtil.colored(
                    "&cO nome do " + typeFancy + " inserido é inválido, valores válidos:",
                    "&e" + Arrays.asList(files)));
            return;
        }

        context.sendMessage(ColorUtil.colored("&aIniciando leitura do " + typeFancy + " &2" + fileName + "&a."));

        backupManager.loadBackup(context.getSender(), file, type.equalsIgnoreCase("restaurar"), true);
    }
}
