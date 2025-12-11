package miroshka.aegis.storage;

import miroshka.aegis.manager.RegionManager;
import miroshka.aegis.region.Region;
import org.joml.Vector3i;

import java.io.*;
import java.util.Map;

public class StorageManager {
    private final RegionManager regionManager;
    private final File dataFolder;

    public StorageManager(RegionManager regionManager, File dataFolder) {
        this.regionManager = regionManager;
        this.dataFolder = dataFolder;
    }

    public void save() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File file = new File(dataFolder, "regions.dat");
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            out.writeUTF("AEGIS");
            out.writeInt(1); // Version
            out.writeInt(regionManager.getAllRegions().size());

            for (Region region : regionManager.getAllRegions()) {
                out.writeUTF(region.getName());
                out.writeUTF(region.getWorldName());
                out.writeInt(region.getPriority());

                out.writeInt(region.getMin().x);
                out.writeInt(region.getMin().y);
                out.writeInt(region.getMin().z);

                out.writeInt(region.getMax().x);
                out.writeInt(region.getMax().y);
                out.writeInt(region.getMax().z);

                out.writeInt(region.getOwners().size());
                for (String owner : region.getOwners()) {
                    out.writeUTF(owner);
                }

                out.writeInt(region.getMembers().size());
                for (String member : region.getMembers()) {
                    out.writeUTF(member);
                }

                out.writeInt(region.getFlags().size());
                for (Map.Entry<String, Object> entry : region.getFlags().entrySet()) {
                    out.writeUTF(entry.getKey());
                    Object val = entry.getValue();
                    if (val instanceof Boolean) {
                        out.writeByte(1);
                        out.writeBoolean((Boolean) val);
                    } else if (val instanceof Integer) {
                        out.writeByte(2);
                        out.writeInt((Integer) val);
                    } else {
                        out.writeByte(3);
                        out.writeUTF(val.toString());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        File file = new File(dataFolder, "regions.dat");
        if (!file.exists())
            return;

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            String header = in.readUTF();
            if (!header.equals("AEGIS"))
                throw new IOException("Invalid file header");
            int version = in.readInt();
            int count = in.readInt();

            for (int i = 0; i < count; i++) {
                String name = in.readUTF();
                String world = in.readUTF();
                int priority = in.readInt();

                int minX = in.readInt();
                int minY = in.readInt();
                int minZ = in.readInt();

                int maxX = in.readInt();
                int maxY = in.readInt();
                int maxZ = in.readInt();

                Region region = new Region(name, new Vector3i(minX, minY, minZ), new Vector3i(maxX, maxY, maxZ), world,
                        priority);

                int ownersCount = in.readInt();
                for (int j = 0; j < ownersCount; j++) {
                    region.addOwner(in.readUTF());
                }

                int membersCount = in.readInt();
                for (int j = 0; j < membersCount; j++) {
                    region.addMember(in.readUTF());
                }

                int flagsCount = in.readInt();
                for (int j = 0; j < flagsCount; j++) {
                    String key = in.readUTF();
                    byte type = in.readByte();
                    Object value = null;
                    if (type == 1)
                        value = in.readBoolean();
                    else if (type == 2)
                        value = in.readInt();
                    else if (type == 3)
                        value = in.readUTF();

                    if (value != null)
                        region.setFlag(key, value);
                }

                regionManager.addRegion(region);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
