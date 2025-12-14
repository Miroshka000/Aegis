package miroshka.aegis.command;

import miroshka.aegis.flags.*;
import miroshka.aegis.form.AegisForms;
import miroshka.aegis.manager.NameManager;
import miroshka.aegis.manager.RegionManager;
import miroshka.aegis.manager.SelectionManager;
import miroshka.aegis.region.Region;
import miroshka.aegis.utils.Messages;
import org.allaymc.api.command.Command;
import org.allaymc.api.command.SenderType;
import org.allaymc.api.command.tree.CommandTree;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.player.Player;

import org.allaymc.api.item.ItemStack;
import org.cloudburstmc.nbt.NbtMap;

import java.util.List;

public class AegisCommand extends Command {
    private final RegionManager regionManager;
    private final SelectionManager selectionManager;
    private final AegisForms aegisForms;

    public AegisCommand(RegionManager regionManager, SelectionManager selectionManager, NameManager nameManager) {
        super("aegis", "Aegis region management", null);
        this.regionManager = regionManager;
        this.selectionManager = selectionManager;
        this.aegisForms = new AegisForms(regionManager, selectionManager, nameManager);
        this.aliases.add("rg");
    }

    @Override
    public void prepareCommandTree(CommandTree tree) {
        tree.getRoot()
                .key("menu")
                .exec((context, entity) -> {
                    Player player = entity.getController();
                    if (player != null) {
                        aegisForms.openMainMenu(player);
                    }
                    return context.success();
                }, SenderType.ACTUAL_PLAYER)
                .root()
                .key("create")
                .str("name")
                .exec((context, entity) -> {
                    String name = context.getResult(1);

                    if (regionManager.getRegion(name) != null) {
                        context.addError(Messages.get("command.region_exists", name));
                        return context.fail();
                    }

                    SelectionManager.Selection selection = selectionManager.getSelection(entity.getUniqueId());
                    if (selection == null || !selection.isComplete()) {
                        context.addError(Messages.get("command.selection_missing"));
                        return context.fail();
                    }

                    Region region = new Region(name, selection.pos1, selection.pos2, entity.getWorld().getName(), 0);
                    region.addOwner(entity.getUniqueId().toString());

                    regionManager.addRegion(region);
                    context.addOutput(Messages.get("command.region_created", name));
                    return context.success();
                }, SenderType.ACTUAL_PLAYER)
                .root()
                .key("delete")
                .str("name")
                .exec((context, entity) -> {
                    String name = context.getResult(1);

                    if (regionManager.getRegion(name) == null) {
                        context.addError(Messages.get("command.region_not_found", name));
                        return context.fail();
                    }

                    regionManager.removeRegion(name);
                    context.addOutput(Messages.get("command.region_deleted", name));
                    return context.success();
                }, SenderType.ACTUAL_PLAYER)
                .root()
                .key("wand")
                .exec((context, entity) -> {
                    entity.tryAddItem(ItemTypes.WOODEN_AXE.createItemStack());
                    context.addOutput(Messages.get("command.wand_given"));
                    return context.success();
                }, SenderType.ACTUAL_PLAYER)
                .root()
                .key("info")
                .exec((context, entity) -> {
                    ItemStack item = ItemTypes.STICK.createItemStack();
                    item.setCustomName(Messages.get("tool.info.name"));
                    item.setLore(List.of(Messages.get("tool.info.lore")));
                    item.loadExtraTag(NbtMap.builder()
                            .putCompound("BlockEntityTag", NbtMap.builder().putString("aegis_tool", "info").build())
                            .build());
                    entity.tryAddItem(item);
                    context.addOutput(Messages.get("command.info_tool_given"));
                    return context.success();
                }, SenderType.ACTUAL_PLAYER)
                .root()
                .key("flag")
                .str("region")
                .str("flag")
                .str("value")
                .exec((context, entity) -> {
                    String regionName = context.getResult(1);
                    String flagName = context.getResult(2);
                    String valueStr = context.getResult(3);

                    Region region = regionManager.getRegion(regionName);
                    if (region == null) {
                        context.addError(Messages.get("command.region_not_found", regionName));
                        return context.fail();
                    }

                    if (!region.getOwners().contains(entity.getUniqueId().toString())
                            && !entity.hasPermission("aegis.admin").asBoolean()) {
                        context.addError(Messages.get("command.no_permission"));
                        return context.fail();
                    }

                    Flag<?> flag = FlagRegistry.get(flagName);
                    if (flag == null) {
                        context.addError(Messages.get("command.flag.unknown", flagName));
                        return context.fail();
                    }

                    if (!entity.hasPermission("aegis.admin").asBoolean() &&
                            flag.getPermission() != null &&
                            !entity.hasPermission(flag.getPermission()).asBoolean()) {
                        context.addError(Messages.get("command.no_permission"));
                        return context.fail();
                    }

                    try {
                        if (flag instanceof StateFlag) {
                            boolean val = Boolean.parseBoolean(valueStr);
                            if (!valueStr.equalsIgnoreCase("true") && !valueStr.equalsIgnoreCase("false")
                                    && !valueStr.equalsIgnoreCase("allow") && !valueStr.equalsIgnoreCase("deny")) {
                                context.addError(Messages.get("command.flag.invalid_value", "true/false/allow/deny"));
                                return context.fail();
                            }
                            if (valueStr.equalsIgnoreCase("allow"))
                                val = true;
                            if (valueStr.equalsIgnoreCase("deny"))
                                val = false;

                            region.setFlag((StateFlag) flag, val);
                        } else if (flag instanceof IntegerFlag) {
                            int val = Integer.parseInt(valueStr);
                            region.setFlag((IntegerFlag) flag, val);
                        } else if (flag instanceof StringFlag) {
                            region.setFlag((StringFlag) flag, valueStr);
                        }
                        context.addOutput(Messages.get("command.flag.set", flag.getName(), valueStr));
                        return context.success();
                    } catch (Exception e) {
                        context.addError(Messages.get("command.flag.invalid_value", valueStr));
                        return context.fail();
                    }
                }, SenderType.ACTUAL_PLAYER)
                .root()
                .key("addmember")
                .str("region")
                .str("player")
                .exec((context, entity) -> {
                    String regionName = context.getResult(1);
                    String targetName = context.getResult(2);

                    Region region = regionManager.getRegion(regionName);
                    if (region == null) {
                        context.addError(Messages.get("command.region_not_found", regionName));
                        return context.fail();
                    }

                    if (!region.getOwners().contains(entity.getUniqueId().toString())
                            && !entity.hasPermission("aegis.admin").asBoolean()) {
                        context.addError(Messages.get("command.no_permission"));
                        return context.fail();
                    }

                    java.util.UUID uuid = aegisForms.getNameManager().getUUID(targetName);
                    if (uuid == null) {
                        context.addError(Messages.get("command.member.player_not_found", targetName));
                        return context.fail();
                    }

                    if (region.getMembers().contains(uuid.toString()) || region.getOwners().contains(uuid.toString())) {
                        context.addError(Messages.get("command.member.already_member", targetName));
                        return context.fail();
                    }

                    region.addMember(uuid.toString());
                    context.addOutput(Messages.get("command.member.added", targetName, regionName));
                    return context.success();
                }, SenderType.ACTUAL_PLAYER)
                .root()
                .key("removemember")
                .str("region")
                .str("player")
                .exec((context, entity) -> {
                    String regionName = context.getResult(1);
                    String targetName = context.getResult(2);

                    Region region = regionManager.getRegion(regionName);
                    if (region == null) {
                        context.addError(Messages.get("command.region_not_found", regionName));
                        return context.fail();
                    }

                    if (!region.getOwners().contains(entity.getUniqueId().toString())
                            && !entity.hasPermission("aegis.admin").asBoolean()) {
                        context.addError(Messages.get("command.no_permission"));
                        return context.fail();
                    }

                    java.util.UUID uuid = aegisForms.getNameManager().getUUID(targetName);
                    if (uuid == null) {
                        context.addError(Messages.get("command.member.player_not_found", targetName));
                        return context.fail();
                    }

                    if (!region.getMembers().contains(uuid.toString())) {
                        context.addError(Messages.get("command.member.not_member", targetName));
                        return context.fail();
                    }

                    region.removeMember(uuid.toString());
                    context.addOutput(Messages.get("command.member.removed", targetName, regionName));
                    return context.success();
                }, SenderType.ACTUAL_PLAYER);
    }
}
