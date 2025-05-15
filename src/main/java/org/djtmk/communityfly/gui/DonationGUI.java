package org.djtmk.communityfly.gui;

import org.bukkit.Bukkit;
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

public class DonationGUI implements Listener {
    private final CommunityFly plugin;
    private final Player player;
    private final Inventory inventory;

    public DonationGUI(CommunityFly plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(null, 9, "Community Flight Donation");
        initializeItems();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void initializeItems() {
        for (int i = 0; i < 3; i++) {
            String path = "donation.tier" + (i + 1);
            double cost = plugin.getConfig().getDouble(path + ".cost", 100.0 * (i + 1));
            double time = plugin.getConfig().getDouble(path + ".time", 60.0 * (i + 1));
            ItemStack item = new ItemStack(Material.EMERALD);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§aDonate $" + cost);
            meta.setLore(Arrays.asList("§7Grants " + time + "s flight time", "§7to all online players"));
            item.setItemMeta(meta);
            inventory.setItem(i * 2, item);
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != inventory) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot >= 0 && slot < 9 && inventory.getItem(slot) != null) {
            int tier = (slot / 2) + 1;
            String path = "donation.tier" + tier;
            double cost = plugin.getConfig().getDouble(path + ".cost", 100.0 * tier);
            double time = plugin.getConfig().getDouble(path + ".time", 60.0 * tier);

            if (plugin.getEconomy() != null && plugin.getEconomy().has(p, cost)) {
                plugin.getEconomy().withdrawPlayer(p, cost);
                plugin.addFlightTimeToAll(time);
                p.sendMessage("§aThank you for donating! Everyone received " + time + " seconds of flight time!");
            } else {
                p.sendMessage("§cYou don't have enough money!");
            }
            p.closeInventory();
        }
    }
}