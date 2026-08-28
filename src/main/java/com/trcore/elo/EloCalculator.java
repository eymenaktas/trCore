package com.trcore.elo;

import org.bukkit.configuration.file.FileConfiguration;

public class EloCalculator {

    private final EloManager eloManager;

    public EloCalculator(EloManager eloManager) {
        this.eloManager = eloManager;
    }

    /**
     * Calculates and applies new Elo ratings for winner and loser.
     * HasPlayedRanked flag is guaranteed to be set to true for both.
     *
     * @param winner The player who won
     * @param loser  The player who lost
     */
    public void calculateAndApply(EloPlayerData winner, EloPlayerData loser) {
        RankedMatchManager.EloOutcome outcome = calculateOutcome(winner, loser);
        applyOutcome(winner, loser, outcome.getWinnerGain(), outcome.getLoserLoss());
    }

    public RankedMatchManager.EloOutcome calculateOutcome(EloPlayerData winner, EloPlayerData loser) {
        FileConfiguration config = eloManager.getEloConfig().get();

        int baseGain = config.getInt("elo.calculation.base-gain", 25);
        int baseLoss = config.getInt("elo.calculation.base-loss", 25);
        int diffThreshold = config.getInt("elo.calculation.difference-threshold", 200);
        int minElo = config.getInt("elo.min-elo", 0);

        int winnerOld = winner.getElo();
        int loserOld = loser.getElo();
        int difference = Math.abs(winnerOld - loserOld);

        int gain = baseGain;
        int loss = baseLoss;

        if (difference >= diffThreshold) {
            double lowWinsMult = config.getDouble("elo.calculation.big-difference.low-wins-multiplier", 0.5);
            double highWinsMult = config.getDouble("elo.calculation.big-difference.high-wins-multiplier", 0.5);
            double highLosesMult = config.getDouble("elo.calculation.big-difference.high-loses-multiplier", 1.5);
            double lowLosesMult = config.getDouble("elo.calculation.big-difference.low-loses-multiplier", 0.5);

            if (winnerOld < loserOld) {
                // Low ELO player won against a much higher ELO player
                gain = (int) (difference * lowWinsMult);
                int maxGain = config.getInt("elo.calculation.big-difference.low-wins-max-gain", 200);
                if (gain > maxGain) {
                    gain = maxGain;
                }
                
                // The higher ELO player lost against a much lower ELO player
                loss = (int) (baseLoss * highLosesMult);
            } else {
                // High ELO player won against a much lower ELO player
                gain = (int) (baseGain * highWinsMult);
                // The lower ELO player lost against a much higher ELO player
                loss = (int) (baseLoss * lowLosesMult);
            }
            
            // Safety cap
            if (gain < 1) gain = 1;
            if (loss < 1) loss = 1;
        }

        // Streak Bonus logic
        int winStreak = winner.getWinStreak();
        boolean streakEnabled = config.getBoolean("streak-system.enabled", true);
        if (streakEnabled && winStreak >= config.getInt("streak-system.min-streak-for-bonus", 4)) {
            double bonusPerWin = config.getDouble("streak-system.bonus-per-win", 0.1);
            // Example: 4 streak = 0.1 bonus, 5 streak = 0.2 bonus -> (streak - 3) * 0.1
            double multiplier = 1.0 + ((winStreak - 3) * bonusPerWin);
            gain = (int) (gain * multiplier);
        }

        // Loss Protection logic
        if (loser.hasLossProtection()) {
            loss = 0;
            loser.setLossProtection(false); // Use protection
        }

        // Safety cap (final pass)
        if (gain < 1) gain = 1;
        if (loss < 0) loss = 0; // Loss can be 0 now due to protection

        // Upper League (2200+) Difficulty Adjustments
        // Note: Using 2200 as hardcoded boundary for extra difficulty, but leagues config handles totem limits.
        if (winnerOld >= 2200) {
            double mult = config.getDouble("elo.calculation.upper-league.gain-multiplier", 0.8);
            gain = (int) (gain * mult);
        }
        if (loserOld >= 2200) {
            double mult = config.getDouble("elo.calculation.upper-league.loss-multiplier", 1.2);
            loss = (int) (loss * mult);
        }

        // Final safety cap
        if (gain < 1) gain = 1;
        if (loss < 0) loss = 0;

        if (loserOld - loss < minElo) {
            loss = loserOld - minElo;
            if (loss < 0) loss = 0;
        }

        return new RankedMatchManager.EloOutcome(gain, loss);
    }

    public void applyOutcome(EloPlayerData winner, EloPlayerData loser, int gain, int loss) {
        FileConfiguration config = eloManager.getEloConfig().get();
        int minElo = config.getInt("elo.min-elo", 0);

        int winnerOld = winner.getElo();
        int loserOld = loser.getElo();

        int winnerNew = winnerOld + Math.max(1, gain);
        int loserNew = loserOld - Math.max(0, loss);
        if (loserNew < minElo) loserNew = minElo;

        winner.setElo(winnerNew);
        winner.setHasPlayedRanked(true);

        loser.setElo(loserNew);
        loser.setHasPlayedRanked(true);
    }
}
