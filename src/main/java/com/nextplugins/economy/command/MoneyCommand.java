package com.nextplugins.economy.command;

import com.nextplugins.economy.NextEconomy;
import com.nextplugins.economy.api.event.operations.MoneyChangeEvent;
import com.nextplugins.economy.api.event.operations.MoneyGiveEvent;
import com.nextplugins.economy.api.event.operations.MoneySetEvent;
import com.nextplugins.economy.api.event.operations.MoneyWithdrawEvent;
import com.nextplugins.economy.api.event.transaction.TransactionRequestEvent;
import com.nextplugins.economy.configuration.MessageValue;
import com.nextplugins.economy.configuration.RankingValue;
import com.nextplugins.economy.model.storage.AccountStorage;
import com.nextplugins.economy.ranking.RankingBootstrap;
import com.nextplugins.economy.util.ColorUtil;
import com.nextplugins.economy.util.NumberUtils;
import lombok.RequiredArgsConstructor;
import lombok.val;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.annotation.Optional;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.target.CommandTarget;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class MoneyCommand {

    private final NextEconomy plugin;
    private final AccountStorage accountStorage;
    private final RankingBootstrap rankingBootstrap;

    @Command(
            name = "money",
            aliases = {"balinhas", "balas"},
            description = "Veja seu saldo de balinhas.",
            async = true)
    public void moneyCommand(Context<CommandSender> context, @Optional OfflinePlayer target) {
        if (target == null) {
            if (context.getSender() instanceof ConsoleCommandSender) {
                context.sendMessage(MessageValue.get(MessageValue::invalidTarget));
                return;
            }

            target = (OfflinePlayer) context.getSender();
        }

        val offlineAccount = accountStorage.findAccount(target);
        if (offlineAccount == null) {
            context.sendMessage(MessageValue.get(MessageValue::invalidTarget));
            return;
        }

        context.sendMessage(MessageValue.get(MessageValue::seeOtherBalance)
                .replace("$player", target.getName())
                .replace("$amount", offlineAccount.getBalanceFormatted()));
    }

    @Command(
            name = "money.toggle",
            aliases = {"recebimento"},
            description = "Desative/ative o recebimento de money",
            permission = "nexteconomy.togglemoney",
            target = CommandTarget.PLAYER,
            async = true)
    public void toggleMoney(Context<Player> context) {
        val account = accountStorage.findAccount(context.getSender());
        account.setReceiveCoins(!account.isReceiveCoins());

        val toggleMessage = account.isReceiveCoins()
                ? MessageValue.get(MessageValue::enabledReceiveCoins)
                : MessageValue.get(MessageValue::disabledReceiveCoins);

        context.sendMessage(
                MessageValue.get(MessageValue::receiveCoinsToggled).replace("$toggleMessage", toggleMessage));
    }

    @Command(
            name = "money.pay",
            aliases = {"enviar"},
            usage = "/money enviar {jogador} {quantia}",
            description = "Utilize para enviar uma quantia da sua conta para outra.",
            permission = "nexteconomy.command.pay",
            target = CommandTarget.PLAYER,
            async = true)
    public void moneyPayCommand(Context<Player> context, OfflinePlayer target, String amount) {
        val player = context.getSender();

        val parse = NumberUtils.parse(amount);
        if (parse < 1) {
            player.sendMessage(MessageValue.get(MessageValue::invalidMoney));
            return;
        }

        val offlineAccount = accountStorage.findAccount(target);
        if (offlineAccount == null) {
            player.sendMessage(MessageValue.get(MessageValue::invalidTarget));
            return;
        }

        if (!player.hasPermission("nexteconony.bypass") && !offlineAccount.isReceiveCoins()) {
            player.sendMessage(MessageValue.get(MessageValue::disabledCoins));
            return;
        }

        val transactionRequestEvent = new TransactionRequestEvent(player, target, offlineAccount, parse);
        Bukkit.getPluginManager().callEvent(transactionRequestEvent);
    }

    @Command(
            name = "money.help",
            aliases = {"ajuda", "comandos"},
            description = "Utilize para receber ajuda com os comandos do plugin.",
            async = true)
    public void moneyHelpCommand(Context<CommandSender> context) {
        val sender = context.getSender();
        if (sender.hasPermission("nexteconomy.command.help.staff")) {
            for (String s : MessageValue.get(MessageValue::helpCommandStaff)) {
                sender.sendMessage(s);
            }
        } else {
            for (String s : MessageValue.get(MessageValue::helpCommand)) {
                sender.sendMessage(s);
            }
        }
    }

    @Command(
            name = "money.set",
            aliases = {"alterar", "setar"},
            usage = "/money set {jogador} {quantia}",
            description = "Utilize para alterar a quantia de dinheiro de alguém.",
            permission = "nexteconomy.command.set",
            async = true)
    public void moneySetCommand(Context<CommandSender> context, OfflinePlayer target, String amount) {
        val sender = context.getSender();
        val parse = NumberUtils.parse(amount);

        if (parse < 1) {
            sender.sendMessage(MessageValue.get(MessageValue::invalidMoney));
            return;
        }

        val offlineAccount = accountStorage.findAccount(target);
        if (offlineAccount == null) {
            sender.sendMessage(MessageValue.get(MessageValue::invalidTarget));
            return;
        }

        val moneySetEvent = new MoneySetEvent(sender, target, parse);
        Bukkit.getPluginManager().callEvent(moneySetEvent);
    }

    @Command(
            name = "money.add",
            aliases = {"adicionar", "deposit", "depositar", "give"},
            usage = "/money give {jogador} {quantia} ",
            description = "Utilize para adicionar uma quantia de dinheiro para alguém.",
            permission = "nexteconomy.command.add",
            async = true)
    public void moneyAddCommand(Context<CommandSender> context, OfflinePlayer target, String amount) {
        val sender = context.getSender();
        val parse = NumberUtils.parse(amount);

        if (parse < 1) {
            sender.sendMessage(MessageValue.get(MessageValue::invalidMoney));
            return;
        }

        val offlineAccount = accountStorage.findAccount(target);
        if (offlineAccount == null) {
            sender.sendMessage(MessageValue.get(MessageValue::invalidTarget));
            return;
        }

        val moneyGiveEvent = new MoneyGiveEvent(sender, target, parse, 0);
        Bukkit.getPluginManager().callEvent(moneyGiveEvent);
    }

    @Command(
            name = "money.remove",
            aliases = {"remover", "withdraw", "retirar", "take"},
            usage = "/money remover {jogador} {quantia}",
            description = "Utilize para remover uma quantia de dinheiro de alguém.",
            permission = "nexteconomy.command.remove",
            async = true)
    public void moneyRemoveCommand(Context<CommandSender> context, OfflinePlayer target, String amount) {
        val sender = context.getSender();
        val parse = NumberUtils.parse(amount);

        if (parse < 1) {
            sender.sendMessage(MessageValue.get(MessageValue::invalidMoney));
            return;
        }

        val offlineAccount = accountStorage.findAccount(target);
        if (offlineAccount == null) {
            sender.sendMessage(MessageValue.get(MessageValue::invalidTarget));
            return;
        }

        val moneyWithdrawEvent = new MoneyWithdrawEvent(sender, target, parse);
        Bukkit.getPluginManager().callEvent(moneyWithdrawEvent);
    }

    @Command(
            name = "money.reset",
            aliases = {"zerar", "resetar"},
            usage = "/money reset {jogador}",
            description = "Utilize para zerar a quantia de dinheiro de alguém.",
            permission = "nexteconomy.command.reset")
    public void moneyResetCommand(Context<CommandSender> context, OfflinePlayer target) {
        val sender = context.getSender();
        val offlineAccount = accountStorage.findAccount(target);

        if (offlineAccount == null) {
            sender.sendMessage(MessageValue.get(MessageValue::invalidTarget));
            return;
        }

        offlineAccount.setBalance(0);

        sender.sendMessage(MessageValue.get(MessageValue::resetBalance).replace("$player", target.getName()));

        if (!target.isOnline()) return;

        val player = target.getPlayer();
        val moneyChangeEvent = new MoneyChangeEvent(
                player, offlineAccount, offlineAccount.getBalance(), offlineAccount.getBalanceFormatted());

        Bukkit.getPluginManager().callEvent(moneyChangeEvent);
    }

    @Command(
            name = "money.top",
            aliases = {"ranking", "podio"},
            description = "Utilize para ver os jogadores com mais dinheiro do servidor.",
            async = true)
    public void moneyTopCommand(Context<CommandSender> context) {
        val rankingStorage = plugin.getRankingStorage();
        val sender = context.getSender();

        if (rankingStorage.updateRanking(false)) {
            sender.sendMessage(ColorUtil.colored("&aAtualizando o ranking, aguarde alguns segundos."));
            return;
        }

        val rankingType = RankingValue.get(RankingValue::rankingType);

        if (rankingType.equalsIgnoreCase("CHAT")) {
            val header = RankingValue.get(RankingValue::chatModelHeader);
            val body = plugin.getRankingChatBody();
            val footer = RankingValue.get(RankingValue::chatModelFooter);

            header.forEach(sender::sendMessage);
            sender.sendMessage(body.getBodyLines());
            footer.forEach(sender::sendMessage);
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ColorUtil.colored("&cEste tipo de ranking não é suportado via console."));
                return;
            }
        }
    }

    @Command(
            name = "money.npc",
            usage = "/money npc",
            description = "Utilize para ver a ajuda para os comandos do sistema de NPC.",
            permission = "nexteconomy.command.npc.help",
            target = CommandTarget.PLAYER,
            async = true)
    public void npcCommand(Context<Player> context) {
        val player = context.getSender();

        for (String s : ColorUtil.colored(MessageValue.get(MessageValue::npcHelp))) {
            player.sendMessage(s);
        }
    }

    @Command(
            name = "money.npc.add",
            aliases = {"npc.adicionar"},
            usage = "/money npc add {posição}",
            description = "Utilize para definir uma localização de spawn de NPC de certa posição.",
            permission = "nexteconomy.command.npc.add",
            target = CommandTarget.PLAYER,
            async = true)
    public void npcAddCommand(Context<Player> context, int position) {
        val player = context.getSender();

        if (position <= 0) {
            player.sendMessage(MessageValue.get(MessageValue::wrongPosition));
            return;
        }

        val limit = RankingValue.get(RankingValue::rankingLimit);
        if (position > limit) {
            player.sendMessage(
                    MessageValue.get(MessageValue::positionReachedLimit).replace("$limit", String.valueOf(limit)));
            return;
        }

        val location = player.getLocation();
        location.setX(location.getBlockX() + .5);
        location.setZ(location.getBlockZ() + .5);

        rankingBootstrap.getLocations().setLocation(position, location);

        player.sendMessage(MessageValue.get(MessageValue::positionSuccessfulCreated)
                .replace("$position", String.valueOf(position)));
    }

    @Command(
            name = "money.npc.remove",
            aliases = {"npc.remover"},
            usage = "/money npc remover {posição}",
            description = "Utilize para remover uma localização de spawn de NPC de certa posição.",
            permission = "nexteconomy.command.npc.remove",
            target = CommandTarget.PLAYER,
            async = true)
    public void npcRemoveCommand(Context<Player> context, int position) {
        val player = context.getSender();

        if (rankingBootstrap.getLocations().getLocation(position) == null) {
            player.sendMessage(MessageValue.get(MessageValue::positionNotYetDefined));
            return;
        }

        try {
            rankingBootstrap.getLocations().setLocation(position, null);

            player.sendMessage(MessageValue.get(MessageValue::positionSuccessfulRemoved)
                    .replace("$position", String.valueOf(position)));
            rankingBootstrap.getRankingRunnable().run();
        } catch (Exception exception) {
            player.sendMessage(ColorUtil.colored("&cOcorreu um erro ao salvar o arquivo de localizações."));
        }
    }
}
