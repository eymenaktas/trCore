package com.trcore.managers;

import com.trcore.TRCore;
import com.trcore.utils.CC;
import com.trcore.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TrimGUI implements InventoryHolder, Listener {

    private final TRCore plugin;
    private final TrimManager trimManager;
    private final Player player;
    private final ItemStack originalItem;
    private Inventory inventory;

    private List<TrimPattern> allowedPatterns;
    private List<TrimMaterial> allowedMaterials;

    private int patternIndex = 0;
    private int materialIndex = 0;

    public TrimGUI(TRCore plugin, Player player, ItemStack originalItem) {
        this.plugin = plugin;
        this.trimManager = plugin.getTrimManager();
        this.player = player;
        this.originalItem = originalItem;
        
        this.allowedPatterns = trimManager.getAllowedPatterns(player);
        this.allowedMaterials = trimManager.getAllowedMaterials(player);
        
        initializeIndices();
        createInventory();
    }

    private void initializeIndices() {
        if (originalItem.getItemMeta() instanceof ArmorMeta meta && meta.hasTrim()) {
            TrimPattern currentPattern = meta.getTrim().getPattern();
            TrimMaterial currentMaterial = meta.getTrim().getMaterial();
            
            for (int i = 0; i < allowedPatterns.size(); i++) {
                if (allowedPatterns.get(i).equals(currentPattern)) {
                    patternIndex = i;
                    break;
                }
            }
            for (int i = 0; i < allowedMaterials.size(); i++) {
                if (allowedMaterials.get(i).equals(currentMaterial)) {
                    materialIndex = i;
                    break;
                }
            }
        }
    }

    private void createInventory() {
        FileConfiguration config = plugin.getConfigManager().getTrimsConfig();
        Component title = CC.parse(config.getString("gui.title", "&dArmor Trimer"));
        this.inventory = Bukkit.createInventory(this, config.getInt("gui.size", 27), title);
        updateInventory();
    }

    public void updateInventory() {
        FileConfiguration config = plugin.getConfigManager().getTrimsConfig();
        Material fillerMat = Material.valueOf(config.getString("gui.filler_material", "GRAY_STAINED_GLASS_PANE"));
        ItemStack filler = new ItemBuilder(fillerMat).name(" ").build();

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        if (!allowedPatterns.isEmpty()) {
            TrimPattern current = allowedPatterns.get(patternIndex);
            inventory.setItem(11, getPatternItem(current));
        }

        if (!allowedMaterials.isEmpty()) {
            TrimMaterial current = allowedMaterials.get(materialIndex);
            inventory.setItem(15, getMaterialItem(current));
        }

        ItemStack preview = originalItem.clone();
        if (!allowedPatterns.isEmpty() && !allowedMaterials.isEmpty()) {
            trimManager.applyTrim(preview, allowedPatterns.get(patternIndex), allowedMaterials.get(materialIndex));
        }
        inventory.setItem(13, preview);

        inventory.setItem(22, new ItemBuilder(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
                .name(me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, config.getString("messages.apply-button", "&a[ UYGULA ]")))
                .lore(me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, List.of("<!italic>&7Tıkla ve zırhını süsle!", "<!italic>&7Seçilen: &f" + trimManager.getPatternName(allowedPatterns.get(patternIndex)) + " &7+ &f" + trimManager.getMaterialName(allowedMaterials.get(materialIndex)))))
                .build());

        inventory.setItem(18, new ItemBuilder(Material.BARRIER)
                .name(me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, config.getString("messages.remove-button", "&c[ KALDIR ]")))
                .build());
    }

    private ItemStack getPatternItem(TrimPattern pattern) {
        Material mat = trimManager.getPatternIcon(pattern);
        String name = plugin.getConfigManager().getTrimsConfig().getString("messages.pattern-select", "&bDesen: &f%pattern%")
                .replace("%pattern%", trimManager.getPatternName(pattern));
        
        name = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, name);
        return new ItemBuilder(mat).name(name).glow().build();
    }

    private ItemStack getMaterialItem(TrimMaterial material) {
        Material mat = trimManager.getMaterialIcon(material);
        String name = plugin.getConfigManager().getTrimsConfig().getString("messages.material-select", "&bMateryal: &f%material%")
                .replace("%material%", trimManager.getMaterialName(material));
        
        name = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, name);
        return new ItemBuilder(mat).name(name).glow().build();
    }

    public void open() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        player.getScheduler().run(plugin, t -> {
            player.openInventory(inventory);
        }, null);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory)) {
            HandlerList.unregisterAll(this);
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!plugin.checkGuiCooldown(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);

        int slot = event.getSlot();
        boolean changed = false;

        if (slot == 11) {
            patternIndex = (patternIndex + 1) % allowedPatterns.size();
            changed = true;
            CC.playSound(player, "UI_BUTTON_CLICK", 0.5f, 1.2f);
        } else if (slot == 15) {
            materialIndex = (materialIndex + 1) % allowedMaterials.size();
            changed = true;
            CC.playSound(player, "UI_BUTTON_CLICK", 0.5f, 1.2f);
        } else if (slot == 22) {
            trimManager.applyTrim(originalItem, allowedPatterns.get(patternIndex), allowedMaterials.get(materialIndex));
            player.sendMessage(CC.parse(plugin.getConfigManager().getTrimsConfig().getString("messages.applied", "&aTrim uygulandı!")));
            CC.playSound(player, "BLOCK_SMITHING_TABLE_USE", 1.0f, 1.0f);
            player.closeInventory();
        } else if (slot == 18) {
            trimManager.removeTrim(originalItem);
            player.sendMessage(CC.parse(plugin.getConfigManager().getTrimsConfig().getString("messages.removed", "&eTrim kaldırıldı!")));
            CC.playSound(player, "BLOCK_GRINDSTONE_USE", 1.0f, 1.0f);
            player.closeInventory();
        }

        if (changed) {
            updateInventory();
        }
    }
}
