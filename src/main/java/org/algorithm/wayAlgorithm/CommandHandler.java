package org.algorithm.wayAlgorithm;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandHandler implements CommandExecutor {

    private final WayAlgorithm plugin;
    private Location startPoint;
    private Location endPoint;

    public CommandHandler(WayAlgorithm plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (label.equalsIgnoreCase("set1")) {
            if (args.length != 3) {
                player.sendMessage("Usage: /set1 <x> <y> <z>");
                return true;
            }
            startPoint = parseLocation(player.getWorld(), args);
            player.sendMessage("Start point set to: " + startPoint);
            return true;
        }

        if (label.equalsIgnoreCase("set2")) {
            if (args.length != 3) {
                player.sendMessage("Usage: /set2 <x> <y> <z>");
                return true;
            }
            endPoint = parseLocation(player.getWorld(), args);
            player.sendMessage("End point set to: " + endPoint);
            return true;
        }

        if (label.equalsIgnoreCase("calc")) {
            if (startPoint == null || endPoint == null) {
                player.sendMessage("Please set both start and end points using /set1 and /set2.");
                return true;
            }

            AStarAlgorithm algorithm = new AStarAlgorithm();
            List<Location> path = algorithm.calculatePath(player.getWorld(), startPoint, endPoint);
            if (path.isEmpty()) {
                player.sendMessage("No path found!");
                return true;
            }

            player.sendMessage("Path calculated and armor stands placed!");
            return true;
        }

        if (label.equalsIgnoreCase("reset")) {
             if (args.length >= 1) {
                 player.sendMessage("Usage: /reset");
                 return true;
             } else {
                 killArmorStandsWithPathTag(player.getWorld());
                 return true;
             }

        }

        return false;
    }

    private Location parseLocation(World world, String[] args) {
        try {
            double x = Double.parseDouble(args[0]);
            double y = Double.parseDouble(args[1]);
            double z = Double.parseDouble(args[2]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void killArmorStandsWithPathTag(World world) {
        // 모든 엔티티를 탐색
        for (org.bukkit.entity.Entity entity : world.getEntities()) {
            // ArmorStand인지 확인
            if (entity.getType() == EntityType.ARMOR_STAND) {
                ArmorStand armorStand = (ArmorStand) entity;

                // path라는 태그가 있는지 확인
                if (armorStand.getScoreboardTags().contains("path")) {
                    // 태그가 있으면 해당 ArmorStand를 죽임
                    armorStand.remove();
                }
            }
        }
    }
}
