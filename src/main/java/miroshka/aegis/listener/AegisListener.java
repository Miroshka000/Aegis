package miroshka.aegis.listener;

import miroshka.aegis.form.AegisForms;
import miroshka.aegis.manager.RegionManager;
import miroshka.aegis.manager.SelectionManager;
import miroshka.aegis.region.Region;
import miroshka.aegis.utils.Messages;
import org.allaymc.api.block.dto.PlayerInteractInfo;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.eventbus.event.block.BlockBreakEvent;
import org.allaymc.api.eventbus.event.block.BlockPlaceEvent;
import org.allaymc.api.eventbus.event.player.PlayerInteractBlockEvent;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.player.Player;
import org.joml.Vector3i;

import java.util.List;

public class AegisListener {
    private final RegionManager regionManager;
    private final SelectionManager selectionManager;

    private final AegisForms aegisForms;

    public AegisListener(RegionManager regionManager, SelectionManager selectionManager) {
        this.regionManager = regionManager;
        this.selectionManager = selectionManager;
        this.aegisForms = new AegisForms(regionManager, selectionManager);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!(event.getEntity() instanceof EntityPlayer))
            return;
        EntityPlayer entityPlayer = (EntityPlayer) event.getEntity();
        Player player = entityPlayer.getController();
        if (player == null)
            return;

        if (entityPlayer.getItemInHand().getItemType() == ItemTypes.WOODEN_AXE) {
            event.setCancelled(true);
            selectionManager.setPos1(player.getControlledEntity().getUniqueId(),
                    (Vector3i) event.getBlock().getPosition());
            player.sendTip(Messages.get("command.select_pos1",
                    event.getBlock().getPosition().toString()));
            return;
        }

        var extraTag = entityPlayer.getItemInHand().saveExtraTag();
        if (extraTag.containsKey("BlockEntityTag")
                && "info".equals(extraTag.getCompound("BlockEntityTag").getString("aegis_tool"))) {
            event.setCancelled(true);
            return;
        }

        List<Region> regions = regionManager.getRegionsAt(event.getBlock().getPosition());

        for (Region region : regions) {
            if (!region.canBuild(player)) {
                event.setCancelled(true);
                player.sendTip(Messages.get("protection.break"));
                return;
            }
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (event.getInteractInfo() == null)
            return;
        EntityPlayer entityPlayer = event.getInteractInfo().player();
        Player player = entityPlayer.getController();
        if (player == null)
            return;

        List<Region> regions = regionManager.getRegionsAt(event.getBlock().getPosition());

        for (Region region : regions) {
            if (!region.canBuild(player)) {
                event.setCancelled(true);
                player.sendTip(Messages.get("protection.place"));
                return;
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractBlockEvent event) {
        PlayerInteractInfo info = event.getInteractInfo();
        if (info == null)
            return;

        EntityPlayer entityPlayer = event.getPlayer();
        Player player = entityPlayer.getController();
        if (player == null)
            return;

        var extraTag = entityPlayer.getItemInHand().saveExtraTag();
        if (extraTag.containsKey("BlockEntityTag")
                && "info".equals(extraTag.getCompound("BlockEntityTag").getString("aegis_tool"))) {
            event.setCancelled(true);
            if (event.getAction() == PlayerInteractBlockEvent.Action.LEFT_CLICK ||
                    event.getAction() == PlayerInteractBlockEvent.Action.RIGHT_CLICK) {

                List<Region> regions = regionManager.getRegionsAt(info.getClickedBlock().getPosition());
                if (regions.isEmpty()) {
                    player.sendMessage(Messages.get("info.no_region"));
                } else {
                    for (Region region : regions) {
                        player.sendMessage(Messages.get("info.found", region.getName()));
                    }
                }
                aegisForms.openRegionInfo(player, regions);
            }
            return;
        }

        if (entityPlayer.getItemInHand().getItemType() == ItemTypes.WOODEN_AXE) {
            event.setCancelled(true);
            if (event.getAction() == PlayerInteractBlockEvent.Action.LEFT_CLICK) {
                selectionManager.setPos1(player.getControlledEntity().getUniqueId(),
                        (Vector3i) info.getClickedBlock().getPosition());
                player.sendTip(Messages.get("command.select_pos1",
                        info.getClickedBlock().getPosition().toString()));
            } else if (event.getAction() == PlayerInteractBlockEvent.Action.RIGHT_CLICK) {
                selectionManager.setPos2(player.getControlledEntity().getUniqueId(),
                        (Vector3i) info.getClickedBlock().getPosition());
                player.sendTip(Messages.get("command.select_pos2",
                        info.getClickedBlock().getPosition().toString()));
            }
            return;
        }

        List<Region> regions = regionManager.getRegionsAt(info.getClickedBlock().getPosition());

        for (Region region : regions) {
            if (!region.canBuild(player)) {
                event.setCancelled(true);
                player.sendTip(Messages.get("protection.interact"));
                return;
            }
        }
    }
}
