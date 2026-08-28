package com.trcore.elo;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;

public class ActiveMatch {
    private final UUID matchId;
    private final UUID player1;
    private final UUID player2;
    private final Location p1StartLoc;
    private final Location p2StartLoc;
    private final String worldName;
    private final long startTime;

    private final String p1IP;
    private final String p2IP;

    private volatile String p1Name;
    private volatile String p2Name;

    private int p1AnchorExplosions;
    private int p2AnchorExplosions;
    private int p1CrystalExplosions;
    private int p2CrystalExplosions;
    private int p1TotemPops;
    private int p2TotemPops;
    private double p1DamageToOpponent;
    private double p2DamageToOpponent;

    private double p1FinalHealth = -1.0;
    private double p2FinalHealth = -1.0;

    private ItemStack[] p1InvSnapshot;
    private ItemStack[] p2InvSnapshot;
    private ItemStack[] p1ArmorSnapshot;
    private ItemStack[] p2ArmorSnapshot;
    private ItemStack p1OffhandSnapshot;
    private ItemStack p2OffhandSnapshot;

    public ActiveMatch(UUID player1, UUID player2, Location p1StartLoc, Location p2StartLoc, String worldName) {
        this.matchId = UUID.randomUUID();
        this.player1 = player1;
        this.player2 = player2;
        this.p1StartLoc = p1StartLoc;
        this.p2StartLoc = p2StartLoc;
        this.worldName = worldName;
        this.startTime = System.currentTimeMillis();
        
        Player p1 = Bukkit.getPlayer(player1);
        Player p2 = Bukkit.getPlayer(player2);
        this.p1IP = (p1 != null && p1.getAddress() != null) ? p1.getAddress().getAddress().getHostAddress() : "";
        this.p2IP = (p2 != null && p2.getAddress() != null) ? p2.getAddress().getAddress().getHostAddress() : "";
        this.p1Name = p1 != null ? p1.getName() : player1.toString();
        this.p2Name = p2 != null ? p2.getName() : player2.toString();
    }

    public UUID getMatchId() {
        return matchId;
    }

    public UUID getPlayer1() {
        return player1;
    }

    public UUID getPlayer2() {
        return player2;
    }

    public Location getP1StartLoc() {
        return p1StartLoc;
    }

    public Location getP2StartLoc() {
        return p2StartLoc;
    }

    public String getWorldName() {
        return worldName;
    }

    public long getStartTime() {
        return startTime;
    }
    
    public UUID getOpponent(UUID player) {
        if (player.equals(player1)) return player2;
        if (player.equals(player2)) return player1;
        return null;
    }
    
    public String getP1IP() { return p1IP; }
    public String getP2IP() { return p2IP; }

    public synchronized void addAnchorExplosion(UUID player) {
        if (player.equals(player1)) p1AnchorExplosions++;
        else if (player.equals(player2)) p2AnchorExplosions++;
    }

    public synchronized void addCrystalExplosion(UUID player) {
        if (player.equals(player1)) p1CrystalExplosions++;
        else if (player.equals(player2)) p2CrystalExplosions++;
    }

    public synchronized void addTotemPop(UUID player) {
        if (player.equals(player1)) p1TotemPops++;
        else if (player.equals(player2)) p2TotemPops++;
    }

    public synchronized void addDamage(UUID damager, double damage) {
        if (damage <= 0) return;
        if (damager.equals(player1)) p1DamageToOpponent += damage;
        else if (damager.equals(player2)) p2DamageToOpponent += damage;
    }

    public synchronized int getAnchorExplosions(UUID player) {
        if (player.equals(player1)) return p1AnchorExplosions;
        if (player.equals(player2)) return p2AnchorExplosions;
        return 0;
    }

    public synchronized int getCrystalExplosions(UUID player) {
        if (player.equals(player1)) return p1CrystalExplosions;
        if (player.equals(player2)) return p2CrystalExplosions;
        return 0;
    }

    public synchronized int getTotemPops(UUID player) {
        if (player.equals(player1)) return p1TotemPops;
        if (player.equals(player2)) return p2TotemPops;
        return 0;
    }

    public synchronized double getDamageToOpponent(UUID player) {
        if (player.equals(player1)) return p1DamageToOpponent;
        if (player.equals(player2)) return p2DamageToOpponent;
        return 0;
    }

    public synchronized void snapshotPlayer(Player player) {
        if (player == null) return;
        ItemStack[] inv = cloneItems(player.getInventory().getStorageContents());
        ItemStack[] armor = cloneItems(player.getInventory().getArmorContents());
        ItemStack offhand = player.getInventory().getItemInOffHand();
        ItemStack offhandClone = offhand == null ? null : offhand.clone();

        if (player.getUniqueId().equals(player1)) {
            p1Name = player.getName();
            p1FinalHealth = Math.max(0, player.getHealth());
            p1InvSnapshot = inv;
            p1ArmorSnapshot = armor;
            p1OffhandSnapshot = offhandClone;
        } else if (player.getUniqueId().equals(player2)) {
            p2Name = player.getName();
            p2FinalHealth = Math.max(0, player.getHealth());
            p2InvSnapshot = inv;
            p2ArmorSnapshot = armor;
            p2OffhandSnapshot = offhandClone;
        }
    }

    private ItemStack[] cloneItems(ItemStack[] src) {
        if (src == null) return new ItemStack[0];
        ItemStack[] out = Arrays.copyOf(src, src.length);
        for (int i = 0; i < out.length; i++) {
            if (out[i] != null) out[i] = out[i].clone();
        }
        return out;
    }

    public String getP1Name() { return p1Name; }
    public String getP2Name() { return p2Name; }
    public double getP1FinalHealth() { return p1FinalHealth; }
    public double getP2FinalHealth() { return p2FinalHealth; }
    public ItemStack[] getP1InvSnapshot() { return p1InvSnapshot; }
    public ItemStack[] getP2InvSnapshot() { return p2InvSnapshot; }
    public ItemStack[] getP1ArmorSnapshot() { return p1ArmorSnapshot; }
    public ItemStack[] getP2ArmorSnapshot() { return p2ArmorSnapshot; }
    public ItemStack getP1OffhandSnapshot() { return p1OffhandSnapshot; }
    public ItemStack getP2OffhandSnapshot() { return p2OffhandSnapshot; }
}
