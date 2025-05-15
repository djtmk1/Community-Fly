package org.djtmk.communityfly.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.djtmk.communityfly.CommunityFly;
import org.djtmk.communityfly.gui.DonationGUI;

public class FlyCommand implements CommandExecutor {
    private final CommunityFly plugin;

    public FlyCommand(CommunityFly plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            if (sender instanceof Player && sender.hasPermission("communityfly.donate")) {
                new DonationGUI(plugin, (Player) sender).open();
                return true;
            }
            sender.sendMessage("§cUsage: /cfly [give|take|set|check|reload|trade]");
            return true;
        }

        if (!sender.hasPermission("communityfly.admin") && !args[0].equalsIgnoreCase("trade")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                if (args.length == 3) {
                    Player target = plugin.getServer().getPlayer(args[1]);
                    if (target != null) {
                        try {
                            double time = Double.parseDouble(args[2]);
                            plugin.addFlightTime(target, time);
                            sender.sendMessage("§aGave " + time + " seconds of flight time to " + target.getName());
                        } catch (NumberFormatException e) {
                            sender.sendMessage("§cInvalid time format!");
                        }
                    } else {
                        sender.sendMessage("§cPlayer not found!");
                    }
                }
                break;

            case "take":
                if (args.length == 3) {
                    Player target = plugin.getServer().getPlayer(args[1]);
                    if (target != null) {
                        try {
                            double time = Double.parseDouble(args[2]);
                            plugin.removeFlightTime(target, time);
                            sender.sendMessage("§aTook " + time + " seconds of flight time from " + target.getName());
                        } catch (NumberFormatException e) {
                            sender.sendMessage("§cInvalid time format!");
                        }
                    } else {
                        sender.sendMessage("§cPlayer not found!");
                    }
                }
                break;

            case "set":
                if (args.length == 3) {
                    Player target = plugin.getServer().getPlayer(args[1]);
                    if (target != null) {
                        try {
                            double time = Double.parseDouble(args[2]);
                            plugin.setFlightTime(target, time);
                            sender.sendMessage("§aSet " + target.getName() + "'s flight time to " + time + " seconds");
                        } catch (NumberFormatException e) {
                            sender.sendMessage("§cInvalid time format!");
                        }
                    } else {
                        sender.sendMessage("§cPlayer not found!");
                    }
                }
                break;

            case "check":
                if (args.length == 2) {
                    Player target = plugin.getServer().getPlayer(args[1]);
                    if (target != null) {
                        double time = plugin.getFlightTime(target);
                        sender.sendMessage("§a" + target.getName() + " has " + time + " seconds of flight time");
                    } else {
                        sender.sendMessage("§cPlayer not found!");
                    }
                }
                break;

            case "reload":
                plugin.reloadConfig();
                plugin.getDatabase().initialize();
                sender.sendMessage("§aConfiguration and database reloaded!");
                break;

            case "trade":
                if (!sender.hasPermission("communityfly.trade")) {
                    sender.sendMessage("§cNo permission!");
                    return true;
                }
                if (args.length == 3 && sender instanceof Player) {
                    Player senderPlayer = (Player) sender;
                    Player target = plugin.getServer().getPlayer(args[1]);
                    if (target != null) {
                        try {
                            double time = Double.parseDouble(args[2]);
                            if (plugin.getFlightTime(senderPlayer) >= time) {
                                plugin.removeFlightTime(senderPlayer, time);
                                plugin.addFlightTime(target, time);
                                sender.sendMessage("§aTransferred " + time + " seconds to " + target.getName());
                                target.sendMessage("§aReceived " + time + " seconds from " + senderPlayer.getName());
                            } else {
                                sender.sendMessage("§cNot enough flight time!");
                            }
                        } catch (NumberFormatException e) {
                            sender.sendMessage("§cInvalid time format!");
                        }
                    } else {
                        sender.sendMessage("§cPlayer not found!");
                    }
                }
                break;

            default:
                sender.sendMessage("§cUnknown subcommand!");
                break;
        }
        return true;
    }
}