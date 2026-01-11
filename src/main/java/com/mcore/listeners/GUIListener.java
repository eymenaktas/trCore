package com.mcore.listeners;
import com.mcore.managers.DuelManager;
import com.mcore.managers.MenuManager;
import com.mcore.managers.TPAManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GUIListener implements Listener {
    private final MenuManager menuManager;
    private final TPAManager tpaManager;
    private final DuelManager duelManager;

    public GUIListener(MenuManager m, TPAManager t, DuelManager d) {
        this.menuManager = m; this.tpaManager = t; this.duelManager = d;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof MenuManager.CustomHolder holder) {
            e.setCancelled(true);
            Player p = (Player) e.getWhoClicked();
            String id = holder.getId();

            String action = menuManager.getAction(id, e.getRawSlot());
            if (action == null) return;

            if (action.equals("accept")) {
                if (id.contains("duel")) duelManager.accept(p);
                else tpaManager.accept(p);
            } else if (action.equals("deny")) {
                if (id.contains("duel")) { p.closeInventory(); duelManager.remove(p); }
                else tpaManager.deny(p);
            }
        }
    }
}