package com.trcore.listeners.pda;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;
import com.trcore.TRCore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class OutgoingPlayerDeathListener extends PacketListenerAbstract {
    private static final byte ENTITY_DEATH_EVENT_ID = 3;
    private final TRCore plugin;
    private final DeathListener deathListener;
    private final double cancelDistanceSquared;

    public OutgoingPlayerDeathListener(TRCore plugin, DeathListener deathListener) {
        this.plugin = plugin;
        this.deathListener = deathListener;
        this.cancelDistanceSquared = Math.pow(Bukkit.getSimulationDistance() * 16.0, 2.0);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.DESTROY_ENTITIES) return;

        Player playerSendingTo = (Player) event.getPlayer();

        WrapperPlayServerDestroyEntities outgoingPacket = new WrapperPlayServerDestroyEntities(event);

        List<Integer> remainingToSend = new ArrayList<>(Arrays.stream(outgoingPacket.getEntityIds())
            .boxed()
            .toList());

        ArrayList<DeathListener.DeathCache> delayedSend = new ArrayList<>();

        for (int entityId : outgoingPacket.getEntityIds()) {
            DeathListener.DeathCache cached = deathListener.getCachedDeath(entityId);

            // Sadece sistemimizde kayıtlı olan ölü oyuncuları geciktiriyoruz.
            // Diğer tüm entity ID'leri (Moblar, Itemlar vb.) hemen gönderilecek listesine kalır.
            if (cached != null && cached.getMillisSinceDeath() <= 1000) {
                delayedSend.add(cached);
                remainingToSend.removeAll(List.of(entityId));
                
                // Simulate the player dying for the user
                WrapperPlayServerEntityMetadata healthPacket = new WrapperPlayServerEntityMetadata(
                    entityId, List.of(new EntityData(9, EntityDataTypes.FLOAT, 0.0f))
                );
                WrapperPlayServerEntityStatus deathPacket = new WrapperPlayServerEntityStatus(entityId, ENTITY_DEATH_EVENT_ID);
                PacketEvents.getAPI().getPlayerManager().sendPacket(playerSendingTo, healthPacket);
                PacketEvents.getAPI().getPlayerManager().sendPacket(playerSendingTo, deathPacket);
            }
        }

        if (delayedSend.isEmpty()) {
            event.setCancelled(false);
            return;
        } else {
            event.setCancelled(true);
        }

        Bukkit.getAsyncScheduler().runDelayed(plugin, (task) -> {
            int[] entityIds = delayedSend.stream()
                .filter(cache -> !shouldCancelRemovePacket(playerSendingTo, cache.player()))
                .mapToInt(DeathListener.DeathCache::entityId)
                .toArray();

            WrapperPlayServerDestroyEntities delayedRemovePacket = new WrapperPlayServerDestroyEntities(entityIds);
            PacketEvents.getAPI().getPlayerManager().sendPacket(playerSendingTo, delayedRemovePacket);
        }, 1L, TimeUnit.SECONDS);

        if (!remainingToSend.isEmpty()) {
            int[] entityIds = remainingToSend.stream()
                .mapToInt(i -> i)
                .toArray();

            WrapperPlayServerDestroyEntities notModifiedRemovePacket = new WrapperPlayServerDestroyEntities(entityIds);
            PacketEvents.getAPI().getPlayerManager().sendPacket(playerSendingTo, notModifiedRemovePacket);
        }
    }

    private boolean shouldCancelRemovePacket(Player sending, Player dead) {
        boolean world = sending.getWorld() == dead.getWorld();

        if (!world) {
            return false;
        }

        boolean distance = sending.getLocation().distanceSquared(dead.getLocation()) <= cancelDistanceSquared;
        boolean canSee = sending.canSee(dead) && dead.getGameMode() != GameMode.SPECTATOR;

        return distance && canSee;
    }
}
