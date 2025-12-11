package miroshka.aegis.manager;

import miroshka.aegis.region.Region;
import org.allaymc.api.math.position.Position3ic;
import org.allaymc.api.utils.hash.HashUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RegionManager {
    private final Map<String, Region> regions = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, List<Region>>> spatialIndex = new ConcurrentHashMap<>();

    public void addRegion(Region region) {
        regions.put(region.getName(), region);

        int minCx = region.getMin().x >> 4;
        int minCz = region.getMin().z >> 4;
        int maxCx = region.getMax().x >> 4;
        int maxCz = region.getMax().z >> 4;

        String world = region.getWorldName();
        spatialIndex.putIfAbsent(world, new ConcurrentHashMap<>());
        Map<Long, List<Region>> worldIndex = spatialIndex.get(world);

        for (int x = minCx; x <= maxCx; x++) {
            for (int z = minCz; z <= maxCz; z++) {
                long key = HashUtils.hashXZ(x, z);
                worldIndex.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>())).add(region);
            }
        }
    }

    public void removeRegion(String name) {
        Region region = regions.remove(name);
        if (region == null)
            return;

        int minCx = region.getMin().x >> 4;
        int minCz = region.getMin().z >> 4;
        int maxCx = region.getMax().x >> 4;
        int maxCz = region.getMax().z >> 4;

        String world = region.getWorldName();
        Map<Long, List<Region>> worldIndex = spatialIndex.get(world);
        if (worldIndex == null)
            return;

        for (int x = minCx; x <= maxCx; x++) {
            for (int z = minCz; z <= maxCz; z++) {
                long key = HashUtils.hashXZ(x, z);
                List<Region> list = worldIndex.get(key);
                if (list != null) {
                    list.remove(region);
                    if (list.isEmpty()) {
                        worldIndex.remove(key);
                    }
                }
            }
        }
    }

    public Region getRegion(String name) {
        return regions.get(name);
    }

    public List<Region> getRegionsAt(Position3ic pos) {
        if (pos.dimension() == null || pos.dimension().getWorld() == null)
            return Collections.emptyList();
        String world = pos.dimension().getWorld().getName();
        Map<Long, List<Region>> worldIndex = spatialIndex.get(world);
        if (worldIndex == null)
            return Collections.emptyList();

        int cx = pos.x() >> 4;
        int cz = pos.z() >> 4;
        long key = HashUtils.hashXZ(cx, cz);

        List<Region> candidates = worldIndex.get(key);
        if (candidates == null)
            return Collections.emptyList();

        List<Region> result = new ArrayList<>();
        synchronized (candidates) {
            for (Region region : candidates) {
                if (region.contains(pos)) {
                    result.add(region);
                }
            }
        }

        result.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));
        return result;
    }

    public Collection<Region> getAllRegions() {
        return regions.values();
    }
}
