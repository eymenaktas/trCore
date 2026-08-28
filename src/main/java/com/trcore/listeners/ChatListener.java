package com.trcore.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatListener implements Listener {

    private final Map<UUID, LinkedList<String>> lastMessages = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        lastMessages.computeIfAbsent(uuid, k -> new LinkedList<>()).add(e.getMessage());
        if (lastMessages.get(uuid).size() > 5) {
            lastMessages.get(uuid).removeFirst();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastMessages.remove(e.getPlayer().getUniqueId());
    }

    public List<String> getLastMessages(UUID uuid) {
        if (!lastMessages.containsKey(uuid)) return new ArrayList<>();
        return new ArrayList<>(lastMessages.get(uuid));
    }
}
