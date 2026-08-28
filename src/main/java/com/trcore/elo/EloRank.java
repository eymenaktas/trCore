package com.trcore.elo;

public class EloRank {
    private final String id;
    private final String displayName;
    private final String numberColor;
    private final String suffix;
    private final int minElo;

    public EloRank(String id, String displayName, String numberColor, String suffix, int minElo) {
        this.id = id;
        this.displayName = displayName;
        this.numberColor = numberColor;
        this.suffix = suffix;
        this.minElo = minElo;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getNumberColor() {
        return numberColor;
    }

    public String getSuffix() {
        return suffix;
    }

    public int getMinElo() {
        return minElo;
    }
}
