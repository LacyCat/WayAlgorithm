package org.algorithm.wayAlgorithm;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class AStarAlgorithm {
    public ItemStack helmet = new ItemStack(Material.COMMAND_BLOCK);
    public ItemMeta meta = helmet.getItemMeta();
    Plugin plugin = Bukkit.getPluginManager().getPlugin("WayAlgorithm");

    // 성능 최적화를 위한 캐시
    private final Map<Location, Boolean> passabilityCache = new HashMap<>();
    private final Map<Location, Boolean> heightValidityCache = new HashMap<>();
    private final Set<Location> closedSet = new HashSet<>();

    // 문 관련 재료들 미리 정의
    private final Set<Material> DOOR_MATERIALS = EnumSet.of(
            Material.OAK_DOOR, Material.BIRCH_DOOR, Material.SPRUCE_DOOR,
            Material.JUNGLE_DOOR, Material.ACACIA_DOOR, Material.DARK_OAK_DOOR,
            Material.CRIMSON_DOOR, Material.WARPED_DOOR, Material.IRON_DOOR
    );

    // 통과 가능한 재료들 미리 정의 (성능 최적화)
    private final Set<Material> PASSABLE_MATERIALS = EnumSet.of(
            Material.AIR, Material.CAVE_AIR, Material.VOID_AIR,
            Material.LADDER, Material.VINE, Material.SCAFFOLDING,
            Material.WATER, Material.LAVA, // 필요시 제거 가능
            Material.TALL_GRASS, Material.GRASS, Material.FERN,
            Material.DEAD_BUSH, Material.WHEAT, Material.CARROTS, Material.POTATOES
    );

    public List<Location> calculatePath(World world, Location start, Location end) {
        // 캐시 초기화
        passabilityCache.clear();
        heightValidityCache.clear();
        closedSet.clear();

        Bukkit.getLogger().info("[DEBUG] Enhanced A* Algorithm - Starting Path Calculation");
        Bukkit.getLogger().info("[DEBUG] Starting Path Calculation from " + start + " to " + end);

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(Node::getF));
        Map<Location, Node> allNodes = new HashMap<>();

        Node startNode = new Node(start, null, 0, calculateHeuristic(start, end));
        openSet.add(startNode);
        allNodes.put(start, startNode);

        Bukkit.getLogger().info("[DEBUG] Start node: " + startNode.getLocation() + " with g=" + startNode.getG() + " and h=" + startNode.getH());

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            Location currentLoc = current.getLocation();

            // 이미 처리된 노드는 스킵
            if (closedSet.contains(currentLoc)) {
                continue;
            }
            closedSet.add(currentLoc);

            Bukkit.getLogger().info("[DEBUG] Processing node: " + currentLoc + " with f=" + current.getF());

            if (isGoalReached(currentLoc, end)) {
                Bukkit.getLogger().info("[DEBUG] Found the end node: " + end);
                List<Location> path = reconstructPath(current);
                spawnArmorStands(path);
                return path;
            }

            for (Location neighbor : getEnhancedNeighbors(world, currentLoc)) {
                if (closedSet.contains(neighbor)) {
                    continue;
                }

                double movementCost = calculateMovementCost(currentLoc, neighbor);
                double tentativeG = current.getG() + movementCost;
                Node neighborNode = allNodes.getOrDefault(neighbor, new Node(neighbor));

                if (tentativeG < neighborNode.getG()) {
                    neighborNode.setParent(current);
                    neighborNode.setG(tentativeG);
                    neighborNode.setH(calculateHeuristic(neighbor, end));

                    allNodes.put(neighbor, neighborNode);
                    if (!openSet.contains(neighborNode)) {
                        openSet.add(neighborNode);
                    }

                    Bukkit.getLogger().info("[DEBUG] Updated neighbor: " + neighbor + " with g=" + neighborNode.getG() + " h=" + neighborNode.getH());
                }
            }
        }

        Bukkit.getLogger().severe("[DEBUG] No path found from " + start + " to " + end);
        return Collections.emptyList();
    }

    private List<Location> reconstructPath(Node node) {
        List<Location> path = new ArrayList<>();
        while (node != null) {
            path.add(0, node.getLocation());
            node = node.getParent();
        }
        Bukkit.getLogger().info("[DEBUG] Reconstructed path with " + path.size() + " nodes");
        return path;
    }

    private boolean isPassable(Material material, Block block) {
        // 캐시 확인
        Location loc = block.getLocation();
        if (passabilityCache.containsKey(loc)) {
            return passabilityCache.get(loc);
        }

        boolean passable = false;

        // 기본 통과 가능 블록들
        if (PASSABLE_MATERIALS.contains(material)) {
            passable = true;
        }
        // 모든 종류의 문 처리 (자동으로 열기)
        else if (DOOR_MATERIALS.contains(material)) {
            passable = handleDoor(block);
        }
        // 문자열 기반 문 감지 (추가 보안)
        else if (material.name().endsWith("_DOOR")) {
            passable = handleDoor(block);
        }
        // 사다리
        else if (material == Material.LADDER || material == Material.VINE) {
            passable = true;
        }
        // 압력판
        else if (material == Material.STONE_PRESSURE_PLATE || material == Material.OAK_PRESSURE_PLATE
                || material.name().endsWith("_PRESSURE_PLATE")) {
            passable = handlePressurePlate(block);
        }

        // 캐시에 저장
        passabilityCache.put(loc, passable);

        Bukkit.getLogger().info("[DEBUG] Block at " + loc + " (" + material + ") is passable: " + passable);
        return passable;
    }

    private boolean handleDoor(Block block) {
        BlockData blockData = block.getBlockData();
        if (blockData instanceof Openable door) {
            if (!door.isOpen()) {
                // 문을 자동으로 열기
                door.setOpen(true);
                block.setBlockData(door);

                // 일정 시간 후 문 닫기 (선택사항)
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    door.setOpen(false);
                    block.setBlockData(door);
                }, 40L); // 2초 후 닫기

                Bukkit.getLogger().info("[DEBUG] Opened door at " + block.getLocation());
            }
            return true;
        }
        return false;
    }

    private boolean handlePressurePlate(Block block) {
        BlockData blockData = block.getBlockData();
        if (blockData instanceof Powerable plate) {
            if (!plate.isPowered()) {
                plate.setPowered(true);
                block.setBlockData(blockData);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plate.setPowered(false);
                    block.setBlockData(blockData);
                }, 20L);

                Bukkit.getLogger().info("[DEBUG] Activated pressure plate at " + block.getLocation());
            }
            return true;
        }
        return false;
    }

    private boolean isValidHeight(World world, Location location) {
        // 캐시 확인
        if (heightValidityCache.containsKey(location)) {
            return heightValidityCache.get(location);
        }

        Block below = world.getBlockAt(location.clone().add(0, -1, 0));
        Block current = world.getBlockAt(location);
        Block above = world.getBlockAt(location.clone().add(0, 1, 0));

        // 특별한 경우들 처리
        boolean valid = false;

        // 사다리나 덩굴의 경우 특별 처리
        if (current.getType() == Material.LADDER || current.getType() == Material.VINE) {
            valid = true; // 사다리는 공중에 있어도 됨
        }
        // 일반적인 경우: 아래가 단단하고 위와 현재가 통과 가능
        else {
            boolean belowSolid = below.getType().isSolid() || below.getType() == Material.LADDER;
            boolean currentPassable = isPassable(current.getType(), current);
            boolean abovePassable = isPassable(above.getType(), above);
            valid = belowSolid && currentPassable && abovePassable;
        }

        // 캐시에 저장
        heightValidityCache.put(location, valid);

        Bukkit.getLogger().info("[DEBUG] Location " + location + " height validity: " + valid);
        return valid;
    }

    private List<Location> getEnhancedNeighbors(World world, Location location) {
        List<Location> neighbors = new ArrayList<>();

        // 수평 이동 (4방향)
        int[][] horizontalDirs = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};

        for (int[] dir : horizontalDirs) {
            Location neighbor = location.clone().add(dir[0], dir[1], dir[2]);
            if (isValidMovement(world, location, neighbor)) {
                neighbors.add(neighbor);
                Bukkit.getLogger().info("[DEBUG] Adding horizontal neighbor at " + neighbor);
            }
        }

        // 사다리/덩굴 수직 이동 처리
        Block currentBlock = world.getBlockAt(location);
        if (currentBlock.getType() == Material.LADDER || currentBlock.getType() == Material.VINE) {
            // 사다리 위로
            Location ladderUp = location.clone().add(0, 1, 0);
            if (isValidLadderMovement(world, ladderUp)) {
                neighbors.add(ladderUp);
                Bukkit.getLogger().info("[DEBUG] Adding ladder upward neighbor at " + ladderUp);
            }

            // 사다리 아래로
            Location ladderDown = location.clone().add(0, -1, 0);
            if (isValidLadderMovement(world, ladderDown)) {
                neighbors.add(ladderDown);
                Bukkit.getLogger().info("[DEBUG] Adding ladder downward neighbor at " + ladderDown);
            }
        }

        // 점프 가능한 위치 확인 (1블록 위)
        Location jumpUp = location.clone().add(0, 1, 0);
        if (isValidHeight(world, jumpUp)) {
            for (int[] dir : horizontalDirs) {
                Location jumpNeighbor = jumpUp.clone().add(dir[0], 0, dir[2]);
                if (isValidHeight(world, jumpNeighbor)) {
                    neighbors.add(jumpNeighbor);
                    Bukkit.getLogger().info("[DEBUG] Adding jump neighbor at " + jumpNeighbor);
                }
            }
        }

        Bukkit.getLogger().info("[DEBUG] Total neighbors found: " + neighbors.size());
        return neighbors;
    }

    private boolean isValidMovement(World world, Location from, Location to) {
        return isValidHeight(world, to);
    }

    private boolean isValidLadderMovement(World world, Location location) {
        Block block = world.getBlockAt(location);
        // 사다리나 덩굴이 있거나, 통과 가능한 공간이어야 함
        return block.getType() == Material.LADDER ||
                block.getType() == Material.VINE ||
                isPassable(block.getType(), block);
    }

    // 개선된 휴리스틱 함수
    private double calculateHeuristic(Location from, Location to) {
        double dx = Math.abs(from.getX() - to.getX());
        double dy = Math.abs(from.getY() - to.getY());
        double dz = Math.abs(from.getZ() - to.getZ());

        // 맨해튼 거리와 유클리드 거리의 조합
        double manhattan = dx + dy + dz;
        double euclidean = Math.sqrt(dx*dx + dy*dy + dz*dz);

        return euclidean * 1.001; // 약간의 가중치로 최적화
    }

    // 이동 비용 계산
    private double calculateMovementCost(Location from, Location to) {
        double baseCost = from.distance(to);

        // 수직 이동에 추가 비용
        double yDiff = Math.abs(to.getY() - from.getY());
        if (yDiff > 0) {
            baseCost += yDiff * 0.5; // 수직 이동 패널티
        }

        return baseCost;
    }

    // 목표 도달 확인 (약간의 여유 허용)
    private boolean isGoalReached(Location current, Location goal) {
        return current.getBlockX() == goal.getBlockX() &&
                current.getBlockY() == goal.getBlockY() &&
                current.getBlockZ() == goal.getBlockZ();
    }

    private void spawnArmorStands(List<Location> path) {
        for (int i = 0; i < path.size(); i++) {
            Location loc = path.get(i);
            Block block = loc.getBlock();

            // 사다리나 문은 스킵
            if (block.getType() == Material.LADDER ||
                    block.getType() == Material.VINE ||
                    DOOR_MATERIALS.contains(block.getType())) {
                continue;
            }

            Location adjustedLoc = loc.clone().add(0.5, 0.1, 0.5);
            Location targetLoc = null;

            if (i < path.size() - 1) {
                targetLoc = path.get(i + 1);
            } else if (i > 0) {
                targetLoc = path.get(i - 1);
            }

            if (targetLoc != null) {
                float[] yawAndPitch = calculateYawAndPitch(adjustedLoc, targetLoc);

                loc.getWorld().spawn(adjustedLoc, ArmorStand.class, armorStand -> {
                    armorStand.setInvisible(true);
                    armorStand.setGlowing(true);
                    armorStand.setGravity(false);
                    armorStand.setSmall(true);
                    armorStand.addScoreboardTag("path");

                    if (meta != null) {
                        meta.setDisplayName(ChatColor.MAGIC+"P"+ChatColor.RESET+" [Enhanced Path] "+ChatColor.MAGIC+"P");
                        List<String> lore = new ArrayList<>();
                        lore.add("Version: "+ plugin.getDescription().getVersion());
                        lore.add("Enhanced with door & ladder support");
                        meta.setLore(lore);
                        meta.addEnchant(new PathEnchant(),1,true);
                        helmet.setItemMeta(meta);
                    }
                    armorStand.setItem(EquipmentSlot.HEAD, helmet);
                });
            }
        }
    }

    private float[] calculateYawAndPitch(Location from, Location to) {
        double deltaX = to.getX() - from.getX();
        double deltaY = to.getY() - from.getY();
        double deltaZ = to.getZ() - from.getZ();

        float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX));
        float pitch = (float) Math.toDegrees(Math.atan2(deltaY, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)));

        return new float[]{yaw, pitch};
    }

    private static class Node {
        private final Location location;
        private Node parent;
        private double g;
        private double h;

        public Node(Location location) {
            this.location = location;
            this.g = Double.MAX_VALUE;
            this.h = 0;
        }

        public Node(Location location, Node parent, double g, double h) {
            this.location = location;
            this.parent = parent;
            this.g = g;
            this.h = h;
        }

        public Location getLocation() { return location; }
        public Node getParent() { return parent; }
        public void setParent(Node parent) { this.parent = parent; }
        public double getG() { return g; }
        public void setG(double g) { this.g = g; }
        public double getH() { return h; }
        public void setH(double h) { this.h = h; }
        public double getF() { return g + h; }
    }
}