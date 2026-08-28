package com.trcore.elo;

import com.trcore.TRCore;
import com.trcore.utils.CC;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EloPlaceholderExpansion extends PlaceholderExpansion implements Relational {

    private final EloManager eloManager;

    public EloPlaceholderExpansion(EloManager eloManager) {
        this.eloManager = eloManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "trcore";
    }

    @Override
    public @NotNull String getAuthor() {
        return "TRCore";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.2";
    }

    @Override
    public boolean persist() {
        return true; 
    }

    @Override
    public String onPlaceholderRequest(Player one, Player two, String params) {
        if (one == null || two == null) return "";

        EloPlayerData targetData = eloManager.getPlayerData(two.getUniqueId());
        if (targetData == null) {
            if (params.endsWith("_raw") || params.equalsIgnoreCase("elo") || params.equalsIgnoreCase("winstreak") || params.equalsIgnoreCase("kills") || params.equalsIgnoreCase("deaths") || params.equalsIgnoreCase("kd")) {
                return "0";
            }
            return "";
        }

        EloPlayerData viewerData = eloManager.getPlayerData(one.getUniqueId());

        String hiddenVal = eloManager.getEloConfig().get().getString("messages.not-visible", "-");

        if (params.equalsIgnoreCase("elo_formatted") || params.equalsIgnoreCase("elo")) {
            if (!targetData.hasPlayedRanked()) return "0";
            
            EloPlayerData.DisplaySetting setting = (viewerData != null) ? viewerData.getDisplaySetting() : EloPlayerData.DisplaySetting.NUMBER;
            EloRank rank = eloManager.getEloConfig().getRankForElo(targetData.getElo());
            
            if (setting == EloPlayerData.DisplaySetting.RANK) {
                return CC.translate(rank.getDisplayName());
            } else {
                return CC.translate(rank.getNumberColor() + targetData.getElo());
            }
        }

        if (params.equalsIgnoreCase("elo_suffix")) {
            if (!targetData.hasPlayedRanked()) return "";
            EloRank rank = eloManager.getEloConfig().getRankForElo(targetData.getElo());
            String suf = rank.getSuffix().replace("{lp_suffix}", getLuckPermsSuffix(two));
            return CC.translate(suf);
        }

        if (params.equalsIgnoreCase("winstreak")) {
            return String.valueOf(targetData.getWinStreak());
        }

        if (params.equalsIgnoreCase("winstreak_raw")) {
            return String.valueOf(targetData.getWinStreak());
        }

        return null;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        EloPlayerData data = eloManager.getPlayerData(player.getUniqueId());
        
        if (data == null) {
            if (params.endsWith("_raw") || params.equalsIgnoreCase("elo") || params.equalsIgnoreCase("winstreak") || params.equalsIgnoreCase("kills") || params.equalsIgnoreCase("deaths") || params.equalsIgnoreCase("kd")) {
                return "0";
            }
            return "";
        }

        String hiddenVal = eloManager.getEloConfig().get().getString("messages.not-visible", "-");
        
        if (params.equalsIgnoreCase("elo_raw")) {
            return String.valueOf(data.getElo());
        }

        if (params.equalsIgnoreCase("elo_rank")) {
            if (!data.hasPlayedRanked()) return CC.translate(hiddenVal);
            EloRank rank = eloManager.getEloConfig().getRankForElo(data.getElo());
            return CC.translate(rank.getDisplayName());
        }

        if (params.equalsIgnoreCase("player")) {
            if (TRCore.getInstance().getDisguiseManager() != null) {
                if (player.isOnline()) {
                    return TRCore.getInstance().getDisguiseManager().getDisguise((Player) player);
                }
            }
            return player.getName() != null ? player.getName() : "";
        }

        if (params.equalsIgnoreCase("elo_formatted") || params.equalsIgnoreCase("elo")) {
            if (!data.hasPlayedRanked()) return "0";
            
            EloRank rank = eloManager.getEloConfig().getRankForElo(data.getElo());
            if (data.getDisplaySetting() == EloPlayerData.DisplaySetting.RANK) {
                return CC.translate(rank.getDisplayName());
            } else {
                return CC.translate(rank.getNumberColor() + data.getElo());
            }
        }

        if (params.equalsIgnoreCase("elo_suffix")) {
            if (!data.hasPlayedRanked()) return "";
            EloRank rank = eloManager.getEloConfig().getRankForElo(data.getElo());
            String suf = rank.getSuffix().replace("{lp_suffix}", getLuckPermsSuffix(player));
            return CC.translate(suf);
        }
        
        if (params.equalsIgnoreCase("elo_setting_formatted")) {
            if (data.getDisplaySetting() == EloPlayerData.DisplaySetting.RANK) {
                return CC.translate("<light_purple>Rütbe");
            } else {
                return CC.translate("<yellow>Sayısal");
            }
        }

        if (params.equalsIgnoreCase("kills")) {
            return String.valueOf(data.getKills());
        }

        if (params.equalsIgnoreCase("deaths")) {
            return String.valueOf(data.getDeaths());
        }

        if (params.equalsIgnoreCase("kd")) {
            if (data.getDeaths() == 0) {
                return String.valueOf((double) data.getKills());
            }
            double kdr = (double) data.getKills() / data.getDeaths();
            return String.format("%.2f", kdr);
        }

        if (params.equalsIgnoreCase("winstreak")) {
            return String.valueOf(data.getWinStreak());
        }

        if (params.equalsIgnoreCase("winstreak_raw")) {
            return String.valueOf(data.getWinStreak());
        }

        return null; 
    }

    private String getLuckPermsSuffix(OfflinePlayer player) {
        if (player == null) return "";
        try {
            if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
                LuckPerms lp = LuckPermsProvider.get();
                User user = lp.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    String suffix = user.getCachedData().getMetaData().getSuffix();
                    return suffix != null ? suffix : "";
                }
            }
        } catch (Exception ignored) {}
        return "";
    }
}
