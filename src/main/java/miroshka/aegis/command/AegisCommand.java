package miroshka.aegis.command;

import miroshka.aegis.config.AegisConfig;
import miroshka.aegis.flags.Flag;
import miroshka.aegis.flags.FlagRegistry;
import miroshka.aegis.flags.IntegerFlag;
import miroshka.aegis.flags.StateFlag;
import miroshka.aegis.flags.StringFlag;
import miroshka.aegis.form.AegisForms;
import miroshka.aegis.manager.NameManager;
import miroshka.aegis.manager.RegionManager;
import miroshka.aegis.manager.SelectionManager;
import miroshka.aegis.region.Region;
import miroshka.aegis.utils.Messages;
import org.allaymc.api.command.Command;
import org.allaymc.api.command.CommandResult;
import org.allaymc.api.command.SenderType;
import org.allaymc.api.command.tree.CommandContext;
import org.allaymc.api.command.tree.CommandTree;

public class AegisCommand extends Command {
    private final RegionManager regionManager;
    private final SelectionManager selectionManager;
    private final AegisForms aegisForms;
    private final AegisConfig config;

    public AegisCommand(
            RegionManager regionManager,
            SelectionManager selectionManager,
            NameManager nameManager,
            AegisConfig config
    ) {
        super("aegis", "Aegis region management", null);
        this.regionManager = regionManager;
        this.selectionManager = selectionManager;
        this.config = config;
        this.aegisForms = new AegisForms(regionManager, selectionManager, nameManager, config);
        this.aliases.add("rg");
    }

    @Override
    public void prepareCommandTree(CommandTree tree) {
        var root = tree.getRoot();
        root.exec(this::openMainMenu, SenderType.ACTUAL_PLAYER);
        root.key(getName()).optional().exec(this::openMainMenu, SenderType.ACTUAL_PLAYER);
        for (String alias : getAliases()) {
            root.root().key(alias).optional().exec(this::openMainMenu, SenderType.ACTUAL_PLAYER);
        }

        root.key("create")
                .str("name")
                .exec((context, entity) -> {
                    String name = context.getResult(0);
                    if (regionManager.getRegion(name) != null) {
                        context.addError(Messages.get("command.region_exists", name));
                        return context.fail();
                    }

                    SelectionManager.Selection selection = selectionManager.getSelection(entity);
                    if (selection == null || !selection.isComplete()) {
                        context.addError(Messages.get("command.selection_missing"));
                        return context.fail();
                    }

                    Region region = new Region(name, selection.pos1(), selection.pos2(), entity.getWorld().getName(),
                            Math.max(config.getDefaultPriority(), 0));
                    region.addOwner(entity.getUniqueId().toString());
                    regionManager.addRegion(region);
                    context.addOutput(Messages.get("command.region_created", name));
                    return context.success();
                }, SenderType.ACTUAL_PLAYER);

        root.key("delete")
                .str("name")
                .exec((context, entity) -> {
                    String name = context.getResult(0);
                    if (regionManager.getRegion(name) == null) {
                        context.addError(Messages.get("command.region_not_found", name));
                        return context.fail();
                    }

                    regionManager.removeRegion(name);
                    context.addOutput(Messages.get("command.region_deleted", name));
                    return context.success();
                }, SenderType.ACTUAL_PLAYER);

        root.key("wand")
                .exec((context, entity) -> {
                    if (!selectionManager.giveWand(entity)) {
                        context.addError(Messages.get("command.inventory_full"));
                        return context.fail();
                    }
                    context.addOutput(Messages.get("command.wand_given", selectionManager.getSelectionSourceName()));
                    return context.success();
                }, SenderType.ACTUAL_PLAYER);

        root.key("info")
                .exec((context, entity) -> {
                    if (!aegisForms.giveInfoTool(entity.getController())) {
                        context.addError(Messages.get("command.inventory_full"));
                        return context.fail();
                    }
                    context.addOutput(Messages.get("command.info_tool_given"));
                    return context.success();
                }, SenderType.ACTUAL_PLAYER);

        root.key("flag")
                .str("region")
                .str("flag")
                .str("value")
                .exec((context, entity) -> {
                    String regionName = context.getResult(0);
                    String flagName = context.getResult(1);
                    String valueRaw = context.getResult(2);

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
                    if (!entity.hasPermission("aegis.admin").asBoolean()
                            && flag.getPermission() != null
                            && !entity.hasPermission(flag.getPermission()).asBoolean()) {
                        context.addError(Messages.get("command.no_permission"));
                        return context.fail();
                    }

                    try {
                        if (flag instanceof StateFlag stateFlag) {
                            Boolean value = parseStateValue(valueRaw);
                            if (value == null) {
                                context.addError(Messages.get("command.flag.invalid_value", "true/false/allow/deny"));
                                return context.fail();
                            }
                            region.setFlag(stateFlag, value);
                        } else if (flag instanceof IntegerFlag integerFlag) {
                            region.setFlag(integerFlag, Integer.parseInt(valueRaw));
                        } else if (flag instanceof StringFlag stringFlag) {
                            region.setFlag(stringFlag, valueRaw);
                        }
                    } catch (RuntimeException exception) {
                        context.addError(Messages.get("command.flag.invalid_value", valueRaw));
                        return context.fail();
                    }

                    context.addOutput(Messages.get("command.flag.set", flag.getName(), valueRaw));
                    return context.success();
                }, SenderType.ACTUAL_PLAYER);

        root.key("addmember")
                .str("region")
                .str("player")
                .exec((context, entity) -> handleMemberUpdate(context, entity, true), SenderType.ACTUAL_PLAYER);

        root.key("removemember")
                .str("region")
                .str("player")
                .exec((context, entity) -> handleMemberUpdate(context, entity, false), SenderType.ACTUAL_PLAYER);
    }

    private CommandResult openMainMenu(CommandContext context, org.allaymc.api.entity.interfaces.EntityPlayer entity) {
        aegisForms.openMainMenu(entity.getController());
        return context.success();
    }

    private CommandResult handleMemberUpdate(CommandContext context, org.allaymc.api.entity.interfaces.EntityPlayer entity, boolean add) {
        String regionName = context.getResult(0);
        String targetName = context.getResult(1);

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

        if (add) {
            if (region.getMembers().contains(uuid.toString()) || region.getOwners().contains(uuid.toString())) {
                context.addError(Messages.get("command.member.already_member", targetName));
                return context.fail();
            }
            region.addMember(uuid.toString());
            context.addOutput(Messages.get("command.member.added", targetName, regionName));
            return context.success();
        }

        if (!region.getMembers().contains(uuid.toString())) {
            context.addError(Messages.get("command.member.not_member", targetName));
            return context.fail();
        }
        region.removeMember(uuid.toString());
        context.addOutput(Messages.get("command.member.removed", targetName, regionName));
        return context.success();
    }

    private Boolean parseStateValue(String raw) {
        if (raw == null) {
            return null;
        }
        if (raw.equalsIgnoreCase("true") || raw.equalsIgnoreCase("allow")) {
            return true;
        }
        if (raw.equalsIgnoreCase("false") || raw.equalsIgnoreCase("deny")) {
            return false;
        }
        return null;
    }
}
