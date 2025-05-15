package org.djtmk.communityfly.api;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.djtmk.communityfly.CommunityFly;

public class FlightPlaceholders extends PlaceholderExpansion {
    private final CommunityFly plugin;

    public FlightPlaceholders(CommunityFly plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "communityfly";
    }

    @Override
    public String getAuthor() {
        return "djtmk";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }
        if (identifier.equals("time")) {
            return String.format("%.1f", plugin.getFlightTime(player));
        }
        return null;
    }
}