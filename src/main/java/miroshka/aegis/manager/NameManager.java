package miroshka.aegis.manager;

import org.allaymc.api.server.Server;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NameManager {
    private final File file;
    private final Map<UUID, String> names = new HashMap<>();

    public NameManager(File dataFolder) {
        this.file = new File(dataFolder, "names.dat");
        load();
    }

    public void addName(UUID uuid, String name) {
        names.put(uuid, name);
    }

    public String getName(UUID uuid) {
        if (names.containsKey(uuid)) {
            return names.get(uuid);
        }
        var player = Server.getInstance().getPlayerManager().getPlayers().get(uuid);
        if (player != null) {
            String name = player.getOriginName();
            names.put(uuid, name);
            return name;
        }
        return uuid.toString();
    }

    public String getName(String uuidStr) {
        try {
            return getName(UUID.fromString(uuidStr));
        } catch (IllegalArgumentException e) {
            return uuidStr;
        }
    }

    public void save() {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            out.writeInt(names.size());
            for (Map.Entry<UUID, String> entry : names.entrySet()) {
                out.writeLong(entry.getKey().getMostSignificantBits());
                out.writeLong(entry.getKey().getLeastSignificantBits());
                out.writeUTF(entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        if (!file.exists())
            return;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                long most = in.readLong();
                long least = in.readLong();
                String name = in.readUTF();
                names.put(new UUID(most, least), name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
