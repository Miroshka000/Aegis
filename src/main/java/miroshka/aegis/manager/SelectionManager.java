package miroshka.aegis.manager;

import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {
    private final Map<UUID, Selection> selections = new HashMap<>();

    public void setPos1(UUID player, Vector3i pos) {
        selections.computeIfAbsent(player, k -> new Selection()).pos1 = pos;
    }

    public void setPos2(UUID player, Vector3i pos) {
        selections.computeIfAbsent(player, k -> new Selection()).pos2 = pos;
    }

    public Selection getSelection(UUID player) {
        return selections.get(player);
    }

    public static class Selection {
        public Vector3i pos1;
        public Vector3i pos2;

        public boolean isComplete() {
            return pos1 != null && pos2 != null;
        }
    }
}
