package com.nextplugins.economy.listener.events.chat;

import com.nextplugins.economy.ranking.RankingStorage;
import com.nextplugins.economy.util.ColorUtil;
import com.nickuc.chat.api.events.PublicMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@RequiredArgsConstructor
public final class OpeNChatListener implements Listener {

    private final RankingStorage rankingStorage;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(PublicMessageEvent event) {
        if (event.isCancelled()) return;

        val player = event.getSender();
        val textComponent = new TextComponent(rankingStorage.getTycoonTag(player.getName()));
        val hoverEvent = new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                TextComponent.fromLegacyText(ColorUtil.colored("&7Um dos jogadores mais ricos.")));

        textComponent.setHoverEvent(hoverEvent);

        event.setTag("tycoon", textComponent);
    }
}
