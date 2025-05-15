package org.djtmk.communityfly;

import com.earth2me.essentials.Essentials;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Particle;
import org.djtmk.communityfly.api.FlightPlaceholders;
import org.djtmk.communityfly.command.FlyCommand;
import org.djtmk.communityfly.database.Database;
import org.djtmk.communityfly.database.MySQLDatabase;
import org.djtmk.communityfly.database.SQLiteDatabase;
import org.djtmk.communityfly.gui.DonationGUI;

import java.util.HashMap;
import java.util.UUID;
import java.util.List;

public class CommunityFly extends JavaPlugin implements Listener {
    private HashMap<UUID, Double> flightTimes;
    private HashMap<UUID, Long> lastMoved;
    private HashMap<UUID, Long> flightDisabledTime;
    private FileConfiguration config;
    private Database database;
    private Economy economy;
    private Essentials essentials;
    private DonationGUI donationGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        flightTimes = new HashMap<>();
        lastMoved = new HashMap<>();
        flightDisabledTime = new HashMap<>();

        String dbType = config.getString("database.type", "sqlite");
        if (dbType.equalsIgnoreCase("mysql")) {
            database = new MySQLDatabase(this);
        } else {
            database = new SQLiteDatabase(this);
        }
        database.initialize();

        for (Player player : getServer().getOnlinePlayers()) {
            flightTimes.put(player.getUniqueId(), database.getFlightTime(player.getUniqueId()));
        }

        if (!setupEconomy()) {
            getLogger().warning("Vault not found! Donation feature disabled.");
        }

        essentials = (Essentials) getServer().getPluginManager().getPlugin("Essentials");
        donationGUI = new DonationGUI(this);

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("cfly").setExecutor(new FlyCommand(this));

        startFlightConsumptionTask();
        startIdlePenaltyTask();
        startParticleTask();
        startFlightMeterTask();

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new FlightPlaceholders(this).register();
        }
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void startFlightConsumptionTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    if (isPlayerFlying(player) && player.hasPermission("communityfly.fly")) {
                        UUID uuid = player.getUniqueId();
                        double currentTime = flightTimes.getOrDefault(uuid, 0.0);
                        List<String> disabledWorlds = config.getStringList("restrictions.disabled-worlds");
                        double maxHeight = config.getDouble("restrictions.max-height", 256.0);

                        if (disabledWorlds.contains(player.getWorld().getName()) || player.getLocation().getY() > maxHeight) {
                            disableFlight(player, "§cFlight disabled in this area or too high!");
                            continue;
                        }

                        if (currentTime > 0) {
                            flightTimes.put(uuid, currentTime - 1.0);
                            database.setFlightTime(uuid, currentTime - 1.0);
                            if (currentTime <= config.getDouble("aesthetics.low-time-warning.threshold", 30.0)) {
                                player.sendMessage("§cLow flight time: " + String.format("%.1f", currentTime) + " seconds remaining!");
                            }
                            if (currentTime <= 0) {
                                disableFlight(player, "§cYour flight time has run out!");
                            }
                        } else {
                            disableFlight(player, "§cNo flight time remaining!");
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void startIdlePenaltyTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    long lastMove = lastMoved.getOrDefault(player.getUniqueId(), System.currentTimeMillis());
                    if (System.currentTimeMillis() - lastMove > config.getLong("inactivity.timeout", 300000)) {
                        if (isPlayerFlying(player)) {
                            if (config.getBoolean("idle.disable-flight", true)) {
                                disableFlight(player, "§cFlight disabled due to inactivity!");
                            }
                        }
                        double penalty = config.getDouble("inactivity.penalty", 1.0);
                        removeFlightTime(player, penalty);
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void startParticleTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    if (isPlayerFlying(player) && config.getBoolean("aesthetics.particles.enabled", true)) {
                        try {
                            player.getWorld().spawnParticle(
                                    Particle.valueOf(config.getString("aesthetics.particles.type", "CLOUD")),
                                    player.getLocation(), 5, 0.3, 0.3, 0.3, 0.0
                            );
                        } catch (IllegalArgumentException e) {
                            getLogger().warning("Invalid particle type: " + config.getString("aesthetics.particles.type"));
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 5L);
    }

    private void startFlightMeterTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    if (isPlayerFlying(player) && config.getBoolean("aesthetics.flight-meter.enabled", true)) {
                        player.sendActionBar(Component.text("§aFlight Time: §e" + String.format("%.1f", getFlightTime(player)) + "s"));
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private boolean isPlayerFlying(Player player) {
        if (essentials != null) {
            try {
                Object user = essentials.getClass().getMethod("getUser", Player.class).invoke(essentials, player);
                if (user != null && (Boolean) user.getClass().getMethod("isFlying").invoke(user)) {
                    return false; // Don't manage Essentials flight
                }
            } catch (Exception e) {
                getLogger().warning("Error checking Essentials fly status: " + e.getMessage());
            }
        }
        return player.isFlying();
    }

    private void disableFlight(Player player, String message) {
        if (essentials != null) {
            try {
                Object user = essentials.getClass().getMethod("getUser", Player.class).invoke(essentials, player);
                if (user != null && (Boolean) user.getClass().getMethod("isFlying").invoke(user)) {
                    return; // Don't disable if Essentials is managing flight
                }
            } catch (Exception e) {
                getLogger().warning("Error during Essentials flight check in disableFlight: " + e.getMessage());
            }
        }

        if (player.isFlying()) {
            player.setFlying(false);
            player.setAllowFlight(false);
            flightDisabledTime.put(player.getUniqueId(), System.currentTimeMillis());
            player.sendMessage(message);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        double dailyBonus = config.getDouble("bonuses.daily", 300.0);
        double firstJoinBonus = config.getDouble("bonuses.first-join", 600.0);

        flightTimes.put(uuid, database.getFlightTime(uuid));
        if (!player.hasPlayedBefore()) {
            addFlightTime(player, firstJoinBonus);
            player.sendMessage("§aWelcome! You received " + firstJoinBonus + " seconds of flight time!");
        } else {
            addFlightTime(player, dailyBonus);
            player.sendMessage("§aDaily bonus: +" + dailyBonus + " seconds of flight time!");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        lastMoved.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isPlayerFlying(player)) {
                disableFlight(player, "§cFlight disabled during combat!");
            }
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Player player = (Player) event.getEntity();
            Long lastDisabled = flightDisabledTime.get(player.getUniqueId());
            if (lastDisabled != null && System.currentTimeMillis() - lastDisabled < 5000) {
                event.setCancelled(true);
            }
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public double getFlightTime(Player player) {
        return flightTimes.getOrDefault(player.getUniqueId(), 0.0);
    }

    public void setFlightTime(Player player, double time) {
        UUID uuid = player.getUniqueId();
        double newTime = Math.max(0, time);
        flightTimes.put(uuid, newTime);
        database.setFlightTime(uuid, newTime);
    }

    public void addFlightTime(Player player, double time) {
        UUID uuid = player.getUniqueId();
        double newTime = flightTimes.getOrDefault(uuid, 0.0) + time;
        flightTimes.put(uuid, newTime);
        database.setFlightTime(uuid, newTime);
    }

    public void removeFlightTime(Player player, double time) {
        UUID uuid = player.getUniqueId();
        double newTime = Math.max(0, flightTimes.getOrDefault(uuid, 0.0) - time);
        flightTimes.put(uuid, newTime);
        database.setFlightTime(uuid, newTime);
    }

    public void addFlightTimeToAll(double time) {
        for (Player player : getServer().getOnlinePlayers()) {
            addFlightTime(player, time);
            player.sendMessage("§aCommunity donation granted +" + time + " seconds of flight time to everyone!");
        }
    }

    public Economy getEconomy() {
        return economy;
    }

    public Database getDatabase() {
        return database;
    }

    public DonationGUI getDonationGUI() {
        return donationGUI;
    }
}