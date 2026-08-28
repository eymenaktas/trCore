package com.trcore.elo;

import java.util.UUID;

public class EloPlayerData {
    private final UUID uuid;
    private int elo;
    private int kills;
    private int deaths;
    private boolean hasPlayedRanked;
    private String lastName;
    private DisplaySetting displaySetting;
    private int winStreak;
    private boolean lossProtection;

    // Transient Streak tracking (not saved to YAML by default)
    private UUID lastOpponent;
    private int consecutiveKillsAgainstOpponent;

    public enum DisplaySetting {
        NUMBER,
        RANK
    }

    public EloPlayerData(UUID uuid, int elo, int kills, int deaths, boolean hasPlayedRanked, DisplaySetting displaySetting, String lastName) {
        this.uuid = uuid;
        this.elo = elo;
        this.kills = kills;
        this.deaths = deaths;
        this.hasPlayedRanked = hasPlayedRanked;
        this.displaySetting = displaySetting;
        this.lastName = lastName;
        this.winStreak = 0;
        this.lossProtection = false;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getElo() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo = elo;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public boolean hasPlayedRanked() {
        return hasPlayedRanked;
    }

    public void setHasPlayedRanked(boolean hasPlayedRanked) {
        this.hasPlayedRanked = hasPlayedRanked;
    }

    public DisplaySetting getDisplaySetting() {
        return displaySetting;
    }

    public void setDisplaySetting(DisplaySetting displaySetting) {
        this.displaySetting = displaySetting;
    }

    public UUID getLastOpponent() {
        return lastOpponent;
    }

    public void setLastOpponent(UUID lastOpponent) {
        this.lastOpponent = lastOpponent;
    }

    public int getConsecutiveKillsAgainstOpponent() {
        return consecutiveKillsAgainstOpponent;
    }

    public void setConsecutiveKillsAgainstOpponent(int consecutiveKillsAgainstOpponent) {
        this.consecutiveKillsAgainstOpponent = consecutiveKillsAgainstOpponent;
    }

    public int getWinStreak() {
        return winStreak;
    }

    public void setWinStreak(int winStreak) {
        this.winStreak = winStreak;
    }

    public boolean hasLossProtection() {
        return lossProtection;
    }

    public void setLossProtection(boolean lossProtection) {
        this.lossProtection = lossProtection;
    }
}
