package miroshka.aegis.region;

import lombok.Getter;
import lombok.Setter;
import miroshka.aegis.flags.Flag;
import miroshka.aegis.flags.FlagRegistry;
import org.allaymc.api.math.position.Position3ic;
import org.allaymc.api.player.Player;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
public class Region {
    private final String name;
    private final Vector3i min;
    private final Vector3i max;
    private final String worldName;
    @Setter
    private int priority;

    private final Set<String> owners = new HashSet<>();
    private final Set<String> members = new HashSet<>();
    private final Map<String, Object> flags = new HashMap<>();

    public Region(String name, Vector3i p1, Vector3i p2, String worldName, int priority) {
        this.name = name;
        this.worldName = worldName;
        this.priority = priority;
        this.min = new Vector3i(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y), Math.min(p1.z, p2.z));
        this.max = new Vector3i(Math.max(p1.x, p2.x), Math.max(p1.y, p2.y), Math.max(p1.z, p2.z));
    }

    public boolean contains(Position3ic pos) {
        if (pos.dimension() == null || pos.dimension().getWorld() == null)
            return false;
        if (!pos.dimension().getWorld().getName().equals(worldName))
            return false;
        return pos.x() >= min.x && pos.x() <= max.x &&
                pos.y() >= min.y && pos.y() <= max.y &&
                pos.z() >= min.z && pos.z() <= max.z;
    }

    public boolean canBuild(Player player) {
        if (player.getControlledEntity() == null)
            return false;
        if (player.getControlledEntity().hasPermission("aegis.admin").asBoolean())
            return true;
        if (isMember(player))
            return true;

        return getFlag(FlagRegistry.BUILD);
    }

    public boolean isMember(Player player) {
        String uuid = player.getControlledEntity().getUniqueId().toString();
        return owners.contains(uuid) || members.contains(uuid);
    }

    public boolean isOwner(Player player) {
        String uuid = player.getControlledEntity().getUniqueId().toString();
        return owners.contains(uuid);
    }

    public void addOwner(String uuid) {
        owners.add(uuid);
    }

    public void removeOwner(String uuid) {
        owners.remove(uuid);
    }

    public void addMember(String uuid) {
        members.add(uuid);
    }

    public void removeMember(String uuid) {
        members.remove(uuid);
    }

    public void setFlag(String flag, Object value) {
        flags.put(flag, value);
    }

    public <T> void setFlag(Flag<T> flag, T value) {
        flags.put(flag.getName(), flag.serialize(value));
    }

    public Object getFlag(String flag) {
        return flags.get(flag);
    }

    @SuppressWarnings("unchecked")
    public <T> T getFlag(Flag<T> flag) {
        Object display = flags.get(flag.getName());
        if (display == null) {
            return flag.getDefaultValue();
        }
        try {
            return flag.parse(display);
        } catch (Exception e) {
            return flag.getDefaultValue();
        }
    }
}
