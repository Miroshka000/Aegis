package miroshka.aegis.listener;

import miroshka.aegis.config.AegisConfig;
import miroshka.aegis.flags.FlagRegistry;
import miroshka.aegis.flags.StateFlag;
import miroshka.aegis.flags.StringFlag;
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
import org.allaymc.api.eventbus.event.player.PlayerChatEvent;
import org.allaymc.api.eventbus.event.player.PlayerCommandEvent;
import org.allaymc.api.eventbus.event.player.PlayerDropItemEvent;
import org.allaymc.api.eventbus.event.player.PlayerInteractBlockEvent;
import org.allaymc.api.eventbus.event.player.PlayerInteractEntityEvent;
import org.allaymc.api.eventbus.event.player.PlayerMoveEvent;
import org.allaymc.api.eventbus.event.player.PlayerPickupItemEvent;
import org.allaymc.api.eventbus.event.server.PlayerJoinEvent;
import org.allaymc.api.math.position.Position3i;
import org.allaymc.api.math.position.Position3ic;
import org.allaymc.api.player.Player;
import org.allaymc.api.utils.TextFormat;

import java.util.List;

public class AegisListener {
    private final RegionManager regionManager;
    private final SelectionManager selectionManager;
    private final NameManager nameManager;
    private final AegisForms aegisForms;

    public AegisListener(
            RegionManager regionManager,
            SelectionManager selectionManager,
            NameManager nameManager,
            AegisConfig config
    ) {
        this.regionManager = regionManager;
        this.selectionManager = selectionManager;
        this.nameManager = nameManager;
        this.aegisForms = new AegisForms(regionManager, selectionManager, nameManager, config);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!(event.getEntity() instanceof EntityPlayer entityPlayer)) {
            return;
        }

        Player player = entityPlayer.getController();
        if (player == null) {
            return;
        }

        if (selectionManager.isSelectionTool(entityPlayer)) {
            event.setCancelled(true);
            selectionManager.setPos1(entityPlayer, event.getBlock().getPosition());
            resendBlock(player, event.getBlock().getPosition());
            return;
        }

        if (isInfoTool(entityPlayer)) {
            event.setCancelled(true);
            resendBlock(player, event.getBlock().getPosition());
            return;
        }

        if (!canBuild(player, event.getBlock().getPosition())) {
            event.setCancelled(true);
            player.sendTip(Messages.get("protection.break"));
            resendBlock(player, event.getBlock().getPosition());
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (event.isCancelled() || event.getInteractInfo() == null) {
            return;
        }

        EntityPlayer entityPlayer = event.getInteractInfo().player();
        Player player = entityPlayer.getController();
        if (player == null) {
            return;
        }

        if (!canBuild(player, event.getBlock().getPosition())) {
            event.setCancelled(true);
            player.sendTip(Messages.get("protection.place"));
            resendBlock(player, event.getBlock().getPosition());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractBlockEvent event) {
        if (event.isCancelled()) {
            return;
        }

        PlayerInteractInfo info = event.getInteractInfo();
        if (info == null) {
            return;
        }

        EntityPlayer entityPlayer = event.getPlayer();
        Player player = entityPlayer.getController();
        if (player == null) {
            return;
        }

        if (isInfoTool(entityPlayer)) {
            event.setCancelled(true);
            List<Region> regions = regionManager.getRegionsAt(info.getClickedBlock().getPosition());
            if (regions.isEmpty()) {
                player.sendMessage(Messages.get("info.no_region"));
            } else {
                for (Region region : regions) {
                    player.sendMessage(Messages.get("info.found", region.getName()));
                }
            }
            aegisForms.openRegionInfo(player, regions);
            return;
        }

        if (selectionManager.isSelectionTool(entityPlayer)) {
            event.setCancelled(true);
            if (event.getAction() == PlayerInteractBlockEvent.Action.LEFT_CLICK) {
                selectionManager.setPos1(entityPlayer, info.getClickedBlock().getPosition());
            } else if (event.getAction() == PlayerInteractBlockEvent.Action.RIGHT_CLICK) {
                selectionManager.setPos2(entityPlayer, info.getClickedBlock().getPosition());
            }
            return;
        }

        if (!canUse(player, info.getClickedBlock().getPosition(), info.getClickedBlock().getBlockEntity())) {
            event.setCancelled(true);
            player.sendTip(Messages.get("protection.interact"));
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof EntityPlayer victim)) {
            return;
        }
        if (!(event.getDamageContainer().getAttacker() instanceof EntityPlayer)) {
            return;
        }

        Position3ic victimPos = toPosition(victim);
        if (victimPos == null) {
            return;
        }

        for (Region region : regionManager.getRegionsAt(victimPos)) {
            if (!region.getFlag(FlagRegistry.PVP)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if ((int) event.getFrom().x() == (int) event.getTo().x()
                && (int) event.getFrom().y() == (int) event.getTo().y()
                && (int) event.getFrom().z() == (int) event.getTo().z()) {
            return;
        }
        if (event.getTo().dimension() == null || event.getFrom().dimension() == null) {
            return;
        }

        Position3ic toPos = new Position3i((int) event.getTo().x(), (int) event.getTo().y(), (int) event.getTo().z(),
                event.getTo().dimension());
        Position3ic fromPos = new Position3i((int) event.getFrom().x(), (int) event.getFrom().y(), (int) event.getFrom().z(),
                event.getFrom().dimension());
        List<Region> toRegions = regionManager.getRegionsAt(toPos);
        List<Region> fromRegions = regionManager.getRegionsAt(fromPos);
        Player player = event.getPlayer().getController();
        if (player == null) {
            return;
        }

        List<Region> enteredRegions = toRegions.stream().filter(region -> !fromRegions.contains(region)).toList();
        for (Region region : enteredRegions) {
            if (isBypassed(player, region)) {
                continue;
            }
            if (!region.getFlag(FlagRegistry.ENTRY)) {
                event.setCancelled(true);
                player.sendTip(Messages.get("protection.entry"));
                return;
            }
        }

        List<Region> leftRegions = fromRegions.stream().filter(region -> !toRegions.contains(region)).toList();
        for (Region region : leftRegions) {
            if (isBypassed(player, region)) {
                continue;
            }
            if (!region.getFlag(FlagRegistry.EXIT)) {
                event.setCancelled(true);
                player.sendTip(Messages.get("protection.exit"));
                return;
            }
        }

        sendRegionMessage(player, enteredRegions, FlagRegistry.GREETING);
        sendRegionMessage(player, leftRegions, FlagRegistry.FAREWELL);
    }

    @EventHandler
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer().getController();
        Position3ic pos = toPosition(event.getPlayer());
        if (player == null || pos == null || isAdmin(player)) {
            return;
        }

        if (!isAllowed(player, pos, FlagRegistry.CHAT)) {
            event.setCancelled(true);
            player.sendTip(Messages.get("protection.chat"));
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer().getController();
        Position3ic pos = toPosition(event.getPlayer());
        if (player == null || pos == null || isAdmin(player)) {
            return;
        }

        if (!isAllowed(player, pos, FlagRegistry.DROP_ITEMS)) {
            event.setCancelled(true);
            player.sendTip(Messages.get("protection.drop_items"));
        }
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer().getController();
        Position3ic pos = toPosition(event.getItemEntity());
        if (player == null || pos == null || isAdmin(player)) {
            return;
        }

        if (!isAllowed(player, pos, FlagRegistry.PICKUP_ITEMS)) {
            event.setCancelled(true);
            player.sendTip(Messages.get("protection.pickup_items"));
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer().getController();
        Position3ic pos = toPosition(event.getEntity());
        if (player == null || pos == null || isAdmin(player)) {
            return;
        }

        if (!isAllowed(player, pos, FlagRegistry.INTERACT_ENTITIES)) {
            event.setCancelled(true);
            player.sendTip(Messages.get("protection.interact_entities"));
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer().getController();
        Position3ic pos = toPosition(event.getPlayer());
        if (player == null || pos == null || isAdmin(player)) {
            return;
        }

        if (!isAllowed(player, pos, FlagRegistry.COMMANDS)) {
            event.setCancelled(true);
            player.sendTip(Messages.get("protection.commands"));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        nameManager.addName(event.getPlayer().getLoginData().getUuid(), event.getPlayer().getOriginName());
    }

    private boolean isInfoTool(EntityPlayer player) {
        var extraTag = player.getItemInHand().saveExtraTag();
        return extraTag.containsKey("BlockEntityTag")
                && "info".equals(extraTag.getCompound("BlockEntityTag").getString("aegis_tool"));
    }

    private boolean canBuild(Player player, Position3ic pos) {
        for (Region region : regionManager.getRegionsAt(pos)) {
            if (!region.canBuild(player)) {
                return false;
            }
        }
        return true;
    }

    private boolean canUse(Player player, Position3ic pos, BlockEntity blockEntity) {
        for (Region region : regionManager.getRegionsAt(pos)) {
            if (isBypassed(player, region)) {
                continue;
            }
            if (!region.getFlag(FlagRegistry.USE)) {
                return false;
            }
            if (blockEntity instanceof BlockEntityContainerHolderComponent && !region.getFlag(FlagRegistry.CHEST_ACCESS)) {
                return false;
            }
        }
        return true;
    }

    private boolean isAllowed(Player player, Position3ic pos, StateFlag flag) {
        for (Region region : regionManager.getRegionsAt(pos)) {
            if (isBypassed(player, region)) {
                continue;
            }
            if (!region.getFlag(flag)) {
                return false;
            }
        }
        return true;
    }

    private boolean isBypassed(Player player, Region region) {
        return isAdmin(player) || region.isMember(player);
    }

    private boolean isAdmin(Player player) {
        return player.getControlledEntity().hasPermission("aegis.admin").asBoolean();
    }

    private void sendRegionMessage(Player player, List<Region> regions, StringFlag flag) {
        for (Region region : regions) {
            String message = region.getFlag(flag);
            if (message != null && !message.isBlank()) {
                player.sendMessage(TextFormat.colorize(message));
                return;
            }
        }
    }

    private Position3ic toPosition(org.allaymc.api.entity.Entity entity) {
        if (entity.getLocation().dimension() == null) {
            return null;
        }
        return new Position3i((int) entity.getLocation().x(), (int) entity.getLocation().y(), (int) entity.getLocation().z(),
                entity.getLocation().dimension());
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
}
