package com.nextplugins.economy.listener.events.chat;

import br.com.devpaulo.legendchat.api.events.ChatMessageEvent;
import com.nextplugins.economy.ranking.RankingStorage;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@RequiredArgsConstructor
public final class LegendChatListener implements Listener {

    private final RankingStorage rankingStorage;

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerChat(ChatMessageEvent event) {
        if (event.isCancelled()) return;

        val player = event.getSender();
        event.setTagValue("tycoon", rankingStorage.getTycoonTag(player.getName()));
    }
}
