package miroshka.aegis.manager;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import miroshka.aegis.config.AegisConfig;
import miroshka.aegis.utils.Messages;
import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.debugshape.DebugBox;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.message.I18n;
import org.allaymc.api.pdc.PersistentDataType;
import org.allaymc.api.player.Player;
import org.allaymc.api.server.Server;
import org.allaymc.api.utils.identifier.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SelectionManager {
    static final Identifier WAND_TAG = new Identifier("aegis", "selection_wand");

    Map<UUID, LocalSelection> localSelections = new HashMap<>();
    AegisConfig config;
    WorldEditBridge worldEditBridge = new WorldEditBridge();

    public SelectionManager(AegisConfig config) {
        this.config = config;
    }

    public boolean isUsingWorldEdit() {
        return config.isPreferWorldEditSelection() && worldEditBridge.isEnabled();
    }

    public String getSelectionSourceName() {
        return isUsingWorldEdit() ? "WorldEdit" : "Aegis";
    }

    public boolean giveWand(EntityPlayer player) {
        ItemStack wand = ItemTypes.WOODEN_AXE.createItemStack();
        wand.setCustomName(isUsingWorldEdit() ? I18n.get().tr("worldedit:selection.wand.name") : Messages.get("tool.wand.name"));
        wand.setLore(List.of(Messages.get("tool.wand.lore", getSelectionSourceName())));
        wand.getPersistentDataContainer().set(WAND_TAG, PersistentDataType.BOOLEAN, true);
        return player.getContainer(ContainerTypes.INVENTORY).tryAddItem(wand) != -1;
    }

    public boolean isSelectionTool(EntityPlayer player) {
        ItemStack itemInHand = player.getItemInHand();
        if (itemInHand.getPersistentDataContainer().has(WAND_TAG, PersistentDataType.BOOLEAN)) {
            return true;
        }
        return isUsingWorldEdit() && worldEditBridge.isWorldEditWand(itemInHand);
    }

    public void setPos1(EntityPlayer player, Vector3ic pos) {
        if (isUsingWorldEdit() && worldEditBridge.setFirst(player, pos)) {
            return;
        }

        LocalSelection selection = localSelections.computeIfAbsent(player.getUniqueId(), ignored -> new LocalSelection());
        selection.pos1 = new Vector3i(pos);
        notifySelectionUpdate(player, selection, true);
    }

    public void setPos2(EntityPlayer player, Vector3ic pos) {
        if (isUsingWorldEdit() && worldEditBridge.setSecond(player, pos)) {
            return;
        }

        LocalSelection selection = localSelections.computeIfAbsent(player.getUniqueId(), ignored -> new LocalSelection());
        selection.pos2 = new Vector3i(pos);
        notifySelectionUpdate(player, selection, false);
    }

    public Selection getSelection(EntityPlayer player) {
        if (isUsingWorldEdit()) {
            Selection worldEditSelection = worldEditBridge.getSelection(player);
            if (worldEditSelection != null) {
                return worldEditSelection;
            }
        }

        LocalSelection selection = localSelections.get(player.getUniqueId());
        return selection == null ? null : selection.snapshot();
    }

    private void notifySelectionUpdate(EntityPlayer player, LocalSelection selection, boolean first) {
        Player controller = player.getController();
        if (controller != null) {
            controller.sendTip(Messages.get(first ? "command.select_pos1" : "command.select_pos2",
                    formatPosition(first ? selection.pos1 : selection.pos2)));
            if (selection.isComplete()) {
                controller.sendTip(Messages.get("command.select_volume", selection.snapshot().volume()));
            }
        }
        refreshLocalVisual(player, selection);
    }

    private void refreshLocalVisual(EntityPlayer player, LocalSelection selection) {
        if (!config.isLocalSelectionVisualization()) {
            clearLocalVisual(player, selection);
            return;
        }

        BoxData boxData = calcBoxData(selection);
        if (boxData == null) {
            clearLocalVisual(player, selection);
            return;
        }

        Player controller = player.getController();
        if (controller == null) {
            return;
        }

        DebugBox debugBox = selection.visual;
        if (debugBox == null) {
            debugBox = new DebugBox(boxData.position(), Color.RED, 1.0f, boxData.bounds());
            selection.visual = debugBox;
        } else {
            debugBox.setColor(Color.RED);
            debugBox.setPosition(boxData.position());
            debugBox.setScale(1.0f);
            debugBox.setBoxBounds(boxData.bounds());
        }
        controller.viewDebugShape(debugBox);
    }

    private void clearLocalVisual(EntityPlayer player, LocalSelection selection) {
        if (selection.visual == null) {
            return;
        }

        Player controller = player.getController();
        if (controller != null) {
            controller.removeDebugShape(selection.visual);
            selection.visual.removeViewer(controller, false);
        }
        selection.visual = null;
    }

    private BoxData calcBoxData(LocalSelection selection) {
        if (selection.pos1 == null && selection.pos2 == null) {
            return null;
        }
        if (selection.pos1 != null && selection.pos2 == null) {
            return new BoxData(new Vector3f(selection.pos1.x, selection.pos1.y, selection.pos1.z), new Vector3f(1f, 1f, 1f));
        }
        if (selection.pos1 == null) {
            return new BoxData(new Vector3f(selection.pos2.x, selection.pos2.y, selection.pos2.z), new Vector3f(1f, 1f, 1f));
        }

        int minX = Math.min(selection.pos1.x, selection.pos2.x);
        int minY = Math.min(selection.pos1.y, selection.pos2.y);
        int minZ = Math.min(selection.pos1.z, selection.pos2.z);
        int maxX = Math.max(selection.pos1.x, selection.pos2.x);
        int maxY = Math.max(selection.pos1.y, selection.pos2.y);
        int maxZ = Math.max(selection.pos1.z, selection.pos2.z);

        return new BoxData(
                new Vector3f(minX, minY, minZ),
                new Vector3f(maxX - minX + 1f, maxY - minY + 1f, maxZ - minZ + 1f)
        );
    }

    private String formatPosition(Vector3ic pos) {
        return pos.x() + ", " + pos.y() + ", " + pos.z();
    }

    public record Selection(Vector3i pos1, Vector3i pos2) {
        public boolean isComplete() {
            return pos1 != null && pos2 != null;
        }

        public int volume() {
            if (!isComplete()) {
                return 0;
            }
            return (Math.abs(pos1.x - pos2.x) + 1)
                    * (Math.abs(pos1.y - pos2.y) + 1)
                    * (Math.abs(pos1.z - pos2.z) + 1);
        }
    }

    private record BoxData(Vector3f position, Vector3f bounds) {
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    static final class LocalSelection {
        Vector3i pos1;
        Vector3i pos2;
        DebugBox visual;

        boolean isComplete() {
            return pos1 != null && pos2 != null;
        }

        Selection snapshot() {
            return new Selection(
                    pos1 == null ? null : new Vector3i(pos1),
                    pos2 == null ? null : new Vector3i(pos2)
            );
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    static final class WorldEditBridge {
        static final String PLUGIN_NAME = "WorldEdit";
        static final String SELECTION_CLASS = "xyz.zernix.worldedit.Selection";
        static final String WAND_LISTENER_CLASS = "xyz.zernix.worldedit.listener.WandListener";

        ClassLoader classLoader;
        Method selectionOfMethod;
        Method selectionSetFirstMethod;
        Method selectionSetSecondMethod;
        Method statePos1Method;
        Method statePos2Method;
        Field wandTagField;

        boolean isEnabled() {
            return Server.getInstance().getPluginManager().isPluginEnabled(PLUGIN_NAME);
        }

        boolean isWorldEditWand(ItemStack itemStack) {
            if (!resolve()) {
                return false;
            }

            try {
                Identifier wandTag = (Identifier) wandTagField.get(null);
                return itemStack.getPersistentDataContainer().has(wandTag, PersistentDataType.BOOLEAN);
            } catch (ReflectiveOperationException exception) {
                return false;
            }
        }

        boolean setFirst(EntityPlayer player, Vector3ic pos) {
            if (!resolve()) {
                return false;
            }

            try {
                selectionSetFirstMethod.invoke(null, player, new Vector3i(pos), true);
                return true;
            } catch (ReflectiveOperationException exception) {
                return false;
            }
        }

        boolean setSecond(EntityPlayer player, Vector3ic pos) {
            if (!resolve()) {
                return false;
            }

            try {
                selectionSetSecondMethod.invoke(null, player, new Vector3i(pos), true);
                return true;
            } catch (ReflectiveOperationException exception) {
                return false;
            }
        }

        Selection getSelection(EntityPlayer player) {
            if (!resolve()) {
                return null;
            }

            try {
                Object state = selectionOfMethod.invoke(null, player);
                Vector3ic pos1 = (Vector3ic) statePos1Method.invoke(state);
                Vector3ic pos2 = (Vector3ic) statePos2Method.invoke(state);
                if (pos1 == null && pos2 == null) {
                    return null;
                }
                return new Selection(
                        pos1 == null ? null : new Vector3i(pos1),
                        pos2 == null ? null : new Vector3i(pos2)
                );
            } catch (ReflectiveOperationException exception) {
                return null;
            }
        }

        private boolean resolve() {
            var pluginContainer = Server.getInstance().getPluginManager().getEnabledPlugin(PLUGIN_NAME);
            if (pluginContainer == null) {
                return false;
            }

            ClassLoader pluginClassLoader = pluginContainer.plugin().getClass().getClassLoader();
            if (pluginClassLoader == classLoader && selectionOfMethod != null) {
                return true;
            }

            try {
                Class<?> selectionClass = pluginClassLoader.loadClass(SELECTION_CLASS);
                Class<?> stateClass = pluginClassLoader.loadClass(SELECTION_CLASS + "$State");
                Class<?> wandListenerClass = pluginClassLoader.loadClass(WAND_LISTENER_CLASS);

                selectionOfMethod = selectionClass.getMethod("of", EntityPlayer.class);
                selectionSetFirstMethod = selectionClass.getMethod("setFirst", EntityPlayer.class, Vector3ic.class, boolean.class);
                selectionSetSecondMethod = selectionClass.getMethod("setSecond", EntityPlayer.class, Vector3ic.class, boolean.class);
                statePos1Method = stateClass.getMethod("pos1");
                statePos2Method = stateClass.getMethod("pos2");
                wandTagField = wandListenerClass.getField("WAND_TAG");
                classLoader = pluginClassLoader;
                return true;
            } catch (ReflectiveOperationException exception) {
                classLoader = null;
                selectionOfMethod = null;
                selectionSetFirstMethod = null;
                selectionSetSecondMethod = null;
                statePos1Method = null;
                statePos2Method = null;
                wandTagField = null;
                return false;
            }
        }
    }
}
