package miroshka.aegis.listener;

import miroshka.aegis.flags.Flag;
import miroshka.aegis.flags.FlagRegistry;
import miroshka.aegis.form.AegisForms;
import miroshka.aegis.manager.NameManager;
import miroshka.aegis.manager.RegionManager;
import miroshka.aegis.manager.SelectionManager;
import miroshka.aegis.region.Region;
import miroshka.aegis.utils.Messages;
import org.allaymc.api.block.dto.PlayerInteractInfo;
import org.allaymc.api.blockentity.BlockEntity;
import org.allaymc.api.blockentity.component.BlockEntityContainerHolderComponent;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.eventbus.event.block.BlockBreakEvent;
import org.allaymc.api.eventbus.event.block.BlockPlaceEvent;
import org.allaymc.api.eventbus.event.entity.EntityDamageEvent;
import org.allaymc.api.eventbus.event.player.PlayerInteractBlockEvent;
import org.allaymc.api.eventbus.event.player.PlayerMoveEvent;
import org.allaymc.api.eventbus.event.server.PlayerJoinEvent;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.math.position.Position3i;
import org.allaymc.api.math.position.Position3ic;
import org.allaymc.api.player.Player;
import org.joml.Vector3i;

import java.util.List;

public class AegisListener {
    private final RegionManager regionManager;
    private final SelectionManager selectionManager;
    private final NameManager nameManager;
    private final AegisForms aegisForms;

    public AegisListener(RegionManager regionManager, SelectionManager selectionManager, NameManager nameManager) {
        this.regionManager = regionManager;
        this.selectionManager = selectionManager;
        this.nameManager = nameManager;
        this.aegisForms = new AegisForms(regionManager, selectionManager, nameManager);
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
            resendBlock(player, event.getBlock().getPosition());
            return;
        }

        var extraTag = entityPlayer.getItemInHand().saveExtraTag();
        if (extraTag.containsKey("BlockEntityTag")
                && "info".equals(extraTag.getCompound("BlockEntityTag").getString("aegis_tool"))) {
            event.setCancelled(true);
            resendBlock(player, event.getBlock().getPosition());
            return;
        }

        List<Region> regions = regionManager.getRegionsAt(event.getBlock().getPosition());
        for (Region region : regions) {
            if (!region.canBuild(player)) {
                event.setCancelled(true);
                player.sendTip(Messages.get("protection.break"));
                resendBlock(player, event.getBlock().getPosition());
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
                resendBlock(player, event.getBlock().getPosition());
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
            if (region.canBuild(player))
                continue;

            if (!getEffectiveFlag(region, FlagRegistry.USE)) {
                event.setCancelled(true);
                player.sendTip(Messages.get("protection.interact"));
                return;
            }

            BlockEntity blockEntity = info.getClickedBlock().getBlockEntity();
            if (blockEntity instanceof BlockEntityContainerHolderComponent) {
                if (!getEffectiveFlag(region, FlagRegistry.CHEST_ACCESS)) {
                    event.setCancelled(true);
                    player.sendTip(Messages.get("protection.interact"));
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof EntityPlayer))
            return;
        EntityPlayer victim = (EntityPlayer) event.getEntity();

        Object attackerObj = event.getDamageContainer().getAttacker();
        if (!(attackerObj instanceof EntityPlayer))
            return;

        Position3ic pos = new Position3i((int) victim.getLocation().x(), (int) victim.getLocation().y(),
                (int) victim.getLocation().z(), victim.getLocation().dimension());
        boolean pvpAllowed = true;
        List<Region> regions = regionManager.getRegionsAt(pos);
        for (Region region : regions) {
            if (region.getFlags().containsKey(FlagRegistry.PVP.getName())) {
                pvpAllowed = region.getFlag(FlagRegistry.PVP);
                break;
            }
        }

        if (!pvpAllowed) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if ((int) event.getFrom().x() == (int) event.getTo().x() &&
                (int) event.getFrom().y() == (int) event.getTo().y() &&
                (int) event.getFrom().z() == (int) event.getTo().z()) {
            return;
        }

        if (event.getTo().dimension() == null)
            return;

        Position3ic toPos = new Position3i((int) event.getTo().x(), (int) event.getTo().y(), (int) event.getTo().z(),
                event.getTo().dimension());
        Position3ic fromPos = new Position3i((int) event.getFrom().x(), (int) event.getFrom().y(),
                (int) event.getFrom().z(), event.getFrom().dimension());

        List<Region> toRegions = regionManager.getRegionsAt(toPos);
        List<Region> fromRegions = regionManager.getRegionsAt(fromPos);

        for (Region region : toRegions) {
            if (!fromRegions.contains(region)) {
                boolean entryAllowed = true;
                if (region.getFlags().containsKey(FlagRegistry.ENTRY.getName())) {
                    entryAllowed = region.getFlag(FlagRegistry.ENTRY);
                } else {
                    entryAllowed = FlagRegistry.ENTRY.getDefaultValue();
                }

                Player player = event.getPlayer().getController();
                if (!entryAllowed && player != null && !region.isMember(player)
                        && !player.getControlledEntity().hasPermission("aegis.admin").asBoolean()) {
                    event.setCancelled(true);
                    player.sendTip(Messages.get("protection.entry"));
                    return;
                }
            }
        }

        for (Region region : fromRegions) {
            if (!toRegions.contains(region)) {
                boolean exitAllowed = true;
                if (region.getFlags().containsKey(FlagRegistry.EXIT.getName())) {
                    exitAllowed = region.getFlag(FlagRegistry.EXIT);
                } else {
                    exitAllowed = FlagRegistry.EXIT.getDefaultValue();
                }

                Player player = event.getPlayer().getController();
                if (!exitAllowed && player != null && !region.isMember(player)
                        && !player.getControlledEntity().hasPermission("aegis.admin").asBoolean()) {
                    event.setCancelled(true);
                    player.sendTip(Messages.get("protection.exit"));
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onChat(org.allaymc.api.eventbus.event.player.PlayerChatEvent event) {
        Player player = event.getPlayer().getController();
        if (player == null)
            return;

        if (player.getControlledEntity().hasPermission("aegis.admin").asBoolean())
            return;

        Position3ic pos = new Position3i((int) player.getControlledEntity().getLocation().x(),
                (int) player.getControlledEntity().getLocation().y(),
                (int) player.getControlledEntity().getLocation().z(),
                player.getControlledEntity().getLocation().dimension());

        List<Region> regions = regionManager.getRegionsAt(pos);
        for (Region region : regions) {
            if (region.isMember(player))
                continue;

            if (!getEffectiveFlag(region, FlagRegistry.CHAT)) {
                event.setCancelled(true);
                player.sendTip(Messages.get("protection.chat"));
                return;
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        nameManager.addName(event.getPlayer().getLoginData().getUuid(), event.getPlayer().getOriginName());
    }

    private void resendBlock(Player player, Position3ic pos) {
        if (pos.dimension() == null) {
            return;
        }
        var blockState = pos.dimension().getBlockState(pos);
        var chunk = pos.dimension().getChunkManager().getChunkByDimensionPos(pos.x(), pos.z());
        if (chunk != null) {
            chunk.setBlockState(pos.x() & 15, pos.y(), pos.z() & 15, blockState, 0, true);
        }
    }

    private <T> T getEffectiveFlag(Region region, Flag<T> flag) {
        return region.getFlag(flag);
    }
}
