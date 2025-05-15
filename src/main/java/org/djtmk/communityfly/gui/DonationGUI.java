package org.djtmk.communityfly.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.djtmk.communityfly.CommunityFly;

import java.util.Arrays;
import java.util.List;

public class DonationGUI implements Listener {

    private final CommunityFly plugin;

    public DonationGUI(CommunityFly plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.GREEN + "Donate for Flight Time");

        inv.addItem(createItem(Material.FEATHER, "30 seconds - $500", "500", "30"));
        inv.addItem(createItem(Material.FEATHER, "1 minute - $800", "800", "60"));
        inv.addItem(createItem(Material.FEATHER, "5 minutes - $2000", "2000", "300"));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material material, String displayName, String cost, String time) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + displayName);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Cost: $" + cost,
                    ChatColor.GRAY + "Time: " + time + " seconds"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!event.getView().getTitle().equals(ChatColor.GREEN + "Donate for Flight Time")) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta() || clicked.getItemMeta() == null) return;

        ItemMeta meta = clicked.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore == null || lore.size() < 2) return;

        String costLine = ChatColor.stripColor(lore.get(0)).replace("Cost: $", "");
        String timeLine = ChatColor.stripColor(lore.get(1)).replace("Time: ", "").replace(" seconds", "");

        try {
            double cost = Double.parseDouble(costLine);
            int time = Integer.parseInt(timeLine);

            if (plugin.getEconomy() != null &&
                    plugin.getEconomy().has(player, cost)) {
                plugin.getEconomy().withdrawPlayer(player, cost);
                plugin.addFlightTimeToAll(time);
                player.sendMessage(ChatColor.GREEN + "Thanks! Everyone received " + time + " seconds of flight time.");
            } else {
                player.sendMessage(ChatColor.RED + "You don't have enough money or an economy account.");
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "An error occurred processing your donation.");
            plugin.getLogger().warning("Failed to parse cost/time from GUI item: " + e.getMessage());
        }

        player.closeInventory();
    }
}