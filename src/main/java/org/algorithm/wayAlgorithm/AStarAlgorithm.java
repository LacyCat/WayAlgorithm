package org.algorithm.wayAlgorithm;

import io.papermc.paper.enchantments.EnchantmentRarity;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityCategory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AStarAlgorithm {
    public ItemStack helmet = new ItemStack(Material.COMMAND_BLOCK);
    public ItemMeta meta = helmet.getItemMeta();
    Plugin plugin = Bukkit.getPluginManager().getPlugin("WayAlgorithm");
    public List<Location> calculatePath(World world, Location start, Location end) {
        // Start debugging
        Bukkit.getLogger().info("[DEBUG] A* Algorithm - Starting Path Calculation");
        Bukkit.getLogger().info("[DEBUG] Starting Path Calculation from " + start + " to " + end);

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(Node::getF));
        Map<Location, Node> allNodes = new HashMap<>();

        Node startNode = new Node(start, null, 0, start.distance(end));
        openSet.add(startNode);
        allNodes.put(start, startNode);

        Bukkit.getLogger().info("[DEBUG] Start node: " + startNode.getLocation() + " with g=" + startNode.getG() + " and h=" + startNode.getH());

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            // Debugging current node
            Bukkit.getLogger().info("[DEBUG] Processing node: " + current.getLocation() + " with f=" + current.getF() + " g=" + current.getG() + " h=" + current.getH());

            if (current.getLocation().equals(end)) {
                Bukkit.getLogger().info("[DEBUG] Found the end node: " + end);
                List<Location> path = reconstructPath(current);
                spawnArmorStands(path);
                return path;
            }

            for (Location neighbor : getNeighbors(world, current.getLocation())) {
                double tentativeG = current.getG() + current.getLocation().distance(neighbor);
                Node neighborNode = allNodes.getOrDefault(neighbor, new Node(neighbor));

                // Debugging the neighbor node calculations
                Bukkit.getLogger().info("[DEBUG] Checking neighbor: " + neighbor + " with tentative g=" + tentativeG);

                if (tentativeG < neighborNode.getG()) {
                    neighborNode.setParent(current);
                    neighborNode.setG(tentativeG);
                    neighborNode.setH(neighbor.distance(end));

                    allNodes.put(neighbor, neighborNode);
                    if (!openSet.contains(neighborNode)) {
                        openSet.add(neighborNode);
                    }

                    // Debugging node updates
                    Bukkit.getLogger().info("[DEBUG] Updated neighbor: " + neighbor + " with g=" + neighborNode.getG() + " h=" + neighborNode.getH());
                }
            }
        }

        Bukkit.getLogger().severe("[DEBUG] No path found.");
        Bukkit.getLogger().severe("[DEBUG] No path found from " + start + " to " + end);
        return Collections.emptyList(); // No path found
    }

    private List<Location> reconstructPath(Node node) {
        List<Location> path = new ArrayList<>();
        while (node != null) {
            path.add(0, node.getLocation());
            node = node.getParent();
        }
        Bukkit.getLogger().info("[DEBUG] Reconstructed path: " + path);
        return path;
    }

    private boolean isPassable(Material material, Block block) {
        // 기본 통과 가능 조건
        boolean passable = material == Material.AIR || material == Material.OAK_DOOR || material == Material.LADDER;

        // 문이 열려 있을 때 통과 가능
        if (material.name().endsWith("_DOOR") && block.getBlockData() instanceof org.bukkit.block.data.Openable) {
            org.bukkit.block.data.Openable door = (org.bukkit.block.data.Openable) block.getBlockData();
            passable = door.isOpen();
            Bukkit.getLogger().info("[DEBUG] Door at " + block.getLocation() + " is open: " + door.isOpen());
        }
        Bukkit.getLogger().info("[DEBUG] Block at " + block.getLocation() + " with material " + material + " is passable: " + passable);
        return passable;
    }

    private boolean isValidHeight(World world, Location location) {
        Block below = world.getBlockAt(location.clone().add(0, -1, 0));
        Block current = world.getBlockAt(location);
        Block above = world.getBlockAt(location.clone().add(0, 1, 0));

        // 아래 블록이 반드시 단단한 블록이어야 함
        boolean belowSolid = below.getType().isSolid();

        // 현재와 위의 블록은 통과 가능해야 함
        boolean passable = isPassable(current.getType(), current) && isPassable(above.getType(), above);
        Bukkit.getLogger().info("[DEBUG] Location " + location + " - Below solid: " + belowSolid + ", Current passable: " + isPassable(current.getType(), current) + ", Above passable: " + isPassable(above.getType(), above));
        return belowSolid && passable;
    }

    private List<Location> getNeighbors(World world, Location location) {
        List<Location> neighbors = new ArrayList<>();
        int[][] directions = {
                {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, // Horizontal directions
                {0, 1, 0}, {0, -1, 0}                         // Vertical directions
        };

        for (int[] dir : directions) {
            Location neighbor = location.clone().add(dir[0], dir[1], dir[2]);
            Block block = world.getBlockAt(neighbor);
            Bukkit.getLogger().info("[DEBUG] Checking neighbor at " + neighbor + " with material " + block.getType());
            // 사다리를 타고 위/아래로 이동
            if (block.getType() == Material.LADDER) {
                Location ladderUp = neighbor.clone().add(0, 1, 0);
                Location ladderDown = neighbor.clone().add(0, -1, 0);
                if (isValidHeight(world, ladderUp)) {
                    neighbors.add(ladderUp);
                    Bukkit.getLogger().info("[DEBUG] Adding ladder upward neighbor at " + ladderUp);
                }
                if (isValidHeight(world, ladderDown)) {
                    neighbors.add(ladderDown);
                    Bukkit.getLogger().info("[DEBUG] Adding ladder downward neighbor at " + ladderDown);
                }
            }

            // 일반 블록 이동
            if (isPassable(block.getType(), block) && isValidHeight(world, neighbor)) {
                neighbors.add(neighbor);
                Bukkit.getLogger().info("[DEBUG] Adding valid neighbor at " + neighbor);
            }
        }
        Bukkit.getLogger().info("[DEBUG] Total neighbors found: " + neighbors.size());
        return neighbors;
    }


    private void spawnArmorStands(List<Location> path) {
        for (int i = 0; i < path.size(); i++) {
            Location loc = path.get(i);
            Block block = loc.getBlock();

            // 사다리나 문은 스킵
            if (block.getType() == Material.LADDER || block.getType().name().endsWith("_DOOR")) {
                continue;
            }

            Location adjustedLoc = loc.clone().add(0.5, 0.1, 0.5); // 중앙 정렬 및 높이 조정
            Location targetLoc = null;

            // 진행 방향 계산 (마지막 노드가 아닐 때)
            if (i < path.size() - 1) {
                targetLoc = path.get(i + 1);  // 다음 위치
            } else if (i > 0) {
                targetLoc = path.get(i - 1);  // 이전 위치 (종료 점을 바라보게)
            }

            if (targetLoc != null) {
                // 각도 계산 메서드 호출
                float[] yawAndPitch = calculateYawAndPitch(adjustedLoc, targetLoc);

                loc.getWorld().spawn(adjustedLoc, ArmorStand.class, armorStand -> {
                    armorStand.setInvisible(true);
                    armorStand.setGlowing(true);
                    armorStand.setGravity(false);
                    armorStand.setSmall(true);
                    armorStand.addScoreboardTag("path");

                    if (meta != null) {
                        // 아이템에 이름 설정
                        meta.setDisplayName(ChatColor.MAGIC+"P"+ChatColor.RESET+" [Path] "+ChatColor.MAGIC+"P");
                        List<String> lore = new ArrayList<>();
                        lore.add("Version: "+ plugin.getDescription().getVersion());
                        meta.setLore(lore);
                        meta.addEnchant(new PathEnchant(),1,true);

                        // 아이템 메타 설정
                        helmet.setItemMeta(meta);
                    }
                    armorStand.setItem(EquipmentSlot.HEAD,helmet);


                });
            }
        }
    }
    /**
     * 두 위치 간의 yaw와 pitch 값 계산
     *
     * @param from 시작 위치
     * @param to 목표 위치
     * @return yaw와 pitch 값이 담긴 배열
     */
    private float[] calculateYawAndPitch(Location from, Location to) {
        double deltaX = to.getX() - from.getX();
        double deltaY = to.getY() - from.getY();
        double deltaZ = to.getZ() - from.getZ();

        // yaw 계산 (수평 방향)
        float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX));

        // pitch 계산 (수직 방향)
        float pitch = (float) Math.toDegrees(Math.atan2(deltaY, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)));

        return new float[]{yaw, pitch};
    }


    private static class Node {
        private final Location location;
        private Node parent;
        private double g; // Cost from start
        private double h; // Heuristic to end

        public Node(Location location) {
            this.location = location;
            this.g = Double.MAX_VALUE; // Default to a very high value
            this.h = 0;
        }

        public Node(Location location, Node parent, double g, double h) {
            this.location = location;
            this.parent = parent;
            this.g = g;
            this.h = h;
        }

        public Location getLocation() {
            return location;
        }

        public Node getParent() {
            return parent;
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }

        public double getG() {
            return g;
        }

        public void setG(double g) {
            this.g = g;
        }

        public double getH() {
            return h;
        }

        public void setH(double h) {
            this.h = h;
        }

        public double getF() {
            return g + h;
        }
    }
}