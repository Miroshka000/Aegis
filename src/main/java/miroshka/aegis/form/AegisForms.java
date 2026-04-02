package miroshka.aegis.form;

import miroshka.aegis.config.AegisConfig;
import miroshka.aegis.flags.Flag;
import miroshka.aegis.flags.FlagRegistry;
import miroshka.aegis.flags.IntegerFlag;
import miroshka.aegis.flags.StateFlag;
import miroshka.aegis.flags.StringFlag;
import miroshka.aegis.manager.NameManager;
import miroshka.aegis.manager.RegionManager;
import miroshka.aegis.manager.SelectionManager;
import miroshka.aegis.region.Region;
import miroshka.aegis.utils.Messages;
import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.form.type.CustomForm;
import org.allaymc.api.form.type.SimpleForm;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.player.Player;
import org.cloudburstmc.nbt.NbtMap;
import org.joml.Vector3i;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AegisForms {
    private final RegionManager regionManager;
    private final SelectionManager selectionManager;
    private final NameManager nameManager;
    private final AegisConfig config;

    public AegisForms(
            RegionManager regionManager,
            SelectionManager selectionManager,
            NameManager nameManager,
            AegisConfig config
    ) {
        this.regionManager = regionManager;
        this.selectionManager = selectionManager;
        this.nameManager = nameManager;
        this.config = config;
    }

    public void openMainMenu(Player player) {
        SimpleForm form = new SimpleForm()
                .title(Messages.get("form.main_menu.title"))
                .content(buildMainMenuContent(player));

        form.buttonWithPathImage(Messages.get("form.button.create_region"), "textures/ui/color_plus")
                .onClick(button -> openCreateRegionForm(player));

        form.buttonWithPathImage(Messages.get("form.button.list_regions"), "textures/items/book_writable")
                .onClick(button -> openListRegionsForm(player));

        form.buttonWithPathImage(Messages.get("form.button.get_wand"), "textures/items/wood_axe")
                .onClick(button -> {
                    if (selectionManager.giveWand(player.getControlledEntity())) {
                        player.sendTip(Messages.get("command.wand_given", selectionManager.getSelectionSourceName()));
                    } else {
                        player.sendTip(Messages.get("command.inventory_full"));
                    }
                });

        form.buttonWithPathImage(Messages.get("form.button.get_info_tool"), "textures/items/stick")
                .onClick(button -> {
                    if (giveInfoTool(player)) {
                        player.sendTip(Messages.get("command.info_tool_given"));
                    } else {
                        player.sendTip(Messages.get("command.inventory_full"));
                    }
                });

        form.buttonWithPathImage(Messages.get("form.button.delete_region"), "textures/ui/trash")
                .onClick(button -> openDeleteRegionForm(player));

        player.viewForm(form);
    }

    public boolean giveInfoTool(Player player) {
        if (player == null || player.getControlledEntity() == null) {
            return false;
        }

        ItemStack item = ItemTypes.STICK.createItemStack();
        item.setCustomName(Messages.get("tool.info.name"));
        item.setLore(List.of(Messages.get("tool.info.lore")));
        item.loadExtraTag(NbtMap.builder()
                .putCompound("BlockEntityTag", NbtMap.builder().putString("aegis_tool", "info").build())
                .build());
        return player.getControlledEntity().getContainer(ContainerTypes.INVENTORY).tryAddItem(item) != -1;
    }

    public void openRegionInfo(Player player, List<Region> regions) {
        String content = regions.isEmpty()
                ? Messages.get("info.no_region")
                : regions.stream()
                .sorted(Comparator.comparingInt(Region::getPriority).reversed().thenComparing(Region::getName))
                .map(this::buildRegionInfo)
                .collect(Collectors.joining("\n\n"));

        SimpleForm form = new SimpleForm()
                .title(Messages.get("form.info.title"))
                .content(content);
        form.buttonWithPathImage("OK", "textures/ui/check");
        player.viewForm(form);
    }

    public NameManager getNameManager() {
        return nameManager;
    }

    private void openCreateRegionForm(Player player) {
        SelectionManager.Selection selection = selectionManager.getSelection(player.getControlledEntity());
        if (selection == null || !selection.isComplete()) {
            player.sendTip(Messages.get("command.selection_missing"));
            return;
        }

        CustomForm form = new CustomForm()
                .title(Messages.get("form.create_region.title"))
                .input(Messages.get("form.create_region.input.name"), Messages.get("form.create_region.input.placeholder"))
                .input(Messages.get("form.create_region.input.priority"), String.valueOf(Math.max(config.getDefaultPriority(), 0)),
                        String.valueOf(Math.max(config.getDefaultPriority(), 0)));

        form.onResponse(responses -> {
            if (responses == null || responses.size() < 2) {
                return;
            }

            String name = responses.get(0);
            if (name == null || name.isBlank()) {
                return;
            }
            if (regionManager.getRegion(name) != null) {
                player.sendTip(Messages.get("command.region_exists", name));
                return;
            }

            SelectionManager.Selection currentSelection = selectionManager.getSelection(player.getControlledEntity());
            if (currentSelection == null || !currentSelection.isComplete()) {
                player.sendTip(Messages.get("command.selection_missing"));
                return;
            }

            int priority = parseInteger(responses.get(1), Math.max(config.getDefaultPriority(), 0));
            Region region = new Region(name, currentSelection.pos1(), currentSelection.pos2(),
                    player.getControlledEntity().getWorld().getName(), Math.max(priority, 0));
            region.addOwner(player.getControlledEntity().getUniqueId().toString());
            regionManager.addRegion(region);
            player.sendTip(Messages.get("command.region_created", name));
            openRegionSettings(player, region);
        });

        player.viewForm(form);
    }

    private void openDeleteRegionForm(Player player) {
        List<Region> regions = sortedRegions();
        SimpleForm form = new SimpleForm()
                .title(Messages.get("form.delete_region.title"))
                .content(regions.isEmpty() ? Messages.get("form.list_regions.empty") : Messages.get("form.delete_region.content"));

        for (Region region : regions) {
            form.buttonWithPathImage(region.getName(), "textures/ui/trash")
                    .onClick(button -> openConfirmDelete(player, region));
        }

        form.buttonWithPathImage(Messages.get("form.settings.button.back"), "textures/ui/arrow_left")
                .onClick(button -> openMainMenu(player));

        player.viewForm(form);
    }

    private void openListRegionsForm(Player player) {
        List<Region> regions = sortedRegions();
        SimpleForm form = new SimpleForm()
                .title(Messages.get("form.list_regions.title"))
                .content(regions.isEmpty()
                        ? Messages.get("form.list_regions.empty")
                        : Messages.get("form.region.count", regions.size()));

        for (Region region : regions) {
            form.buttonWithPathImage(region.getName() + "\n" + region.getWorldName() + " | " + region.getPriority(),
                    "textures/ui/world_glyph")
                    .onClick(button -> openRegionSettings(player, region));
        }

        form.buttonWithPathImage(Messages.get("form.settings.button.back"), "textures/ui/arrow_left")
                .onClick(button -> openMainMenu(player));

        player.viewForm(form);
    }

    private void openRegionSettings(Player player, Region region) {
        SimpleForm form = new SimpleForm()
                .title(Messages.get("form.settings.title", region.getName()))
                .content(buildRegionSummary(region));

        boolean isOwner = region.getOwners().contains(player.getControlledEntity().getUniqueId().toString());
        boolean isAdmin = player.getControlledEntity().hasPermission("aegis.admin").asBoolean();
        if (isOwner || isAdmin) {
            form.buttonWithPathImage(Messages.get("form.settings.button.flags"), "textures/ui/op")
                    .onClick(button -> openRegionFlags(player, region));

            form.buttonWithPathImage(Messages.get("form.settings.button.members"), "textures/ui/multiplayer_glyph")
                    .onClick(button -> openRegionMembers(player, region));

            form.buttonWithPathImage(Messages.get("form.settings.button.delete"), "textures/ui/trash")
                    .onClick(button -> openConfirmDelete(player, region));
        }

        form.buttonWithPathImage(Messages.get("form.settings.button.back"), "textures/ui/arrow_left")
                .onClick(button -> openListRegionsForm(player));

        player.viewForm(form);
    }

    private void openRegionMembers(Player player, Region region) {
        SimpleForm form = new SimpleForm()
                .title(Messages.get("form.members.title", region.getName()))
                .content(Messages.get("form.members.owners", formatPlayerList(region.getOwners())));

        form.buttonWithPathImage(Messages.get("form.members.button.add"), "textures/ui/color_plus")
                .onClick(button -> openAddMember(player, region));

        if (region.getMembers().isEmpty()) {
            form.buttonWithPathImage(Messages.get("form.members.empty"), "textures/ui/cancel");
        } else {
            for (String memberUuid : region.getMembers()) {
                String name = getPlayerName(memberUuid);
                form.buttonWithPathImage(name + "\n" + Messages.get("form.members.button.remove"), "textures/ui/trash")
                        .onClick(button -> {
                            region.removeMember(memberUuid);
                            player.sendTip(Messages.get("command.member.removed", name, region.getName()));
                            openRegionMembers(player, region);
                        });
            }
        }

        form.buttonWithPathImage(Messages.get("form.settings.button.back"), "textures/ui/arrow_left")
                .onClick(button -> openRegionSettings(player, region));

        player.viewForm(form);
    }

    private void openAddMember(Player player, Region region) {
        CustomForm form = new CustomForm()
                .title(Messages.get("form.members.add.title"))
                .input(Messages.get("form.members.add.input"), Messages.get("form.members.add.placeholder"));

        form.onResponse(responses -> {
            if (responses == null || responses.isEmpty()) {
                return;
            }

            String name = responses.get(0);
            if (name == null || name.isBlank()) {
                return;
            }

            java.util.UUID uuid = nameManager.getUUID(name);
            if (uuid == null) {
                player.sendTip(Messages.get("command.member.player_not_found", name));
                openRegionMembers(player, region);
                return;
            }
            if (region.getMembers().contains(uuid.toString()) || region.getOwners().contains(uuid.toString())) {
                player.sendTip(Messages.get("command.member.already_member", name));
                openRegionMembers(player, region);
                return;
            }

            region.addMember(uuid.toString());
            player.sendTip(Messages.get("command.member.added", name, region.getName()));
            openRegionMembers(player, region);
        });

        player.viewForm(form);
    }

    private void openRegionFlags(Player player, Region region) {
        SimpleForm form = new SimpleForm()
                .title(Messages.get("form.flags.title", region.getName()))
                .content(buildRegionSummary(region));

        for (Flag<?> flag : FlagRegistry.getAll()) {
            Object value = region.getFlag(flag);
            form.buttonWithPathImage(Messages.get(flag.getDisplayName()) + "\n" + formatFlagValue(flag, value), getFlagIcon(flag))
                    .onClick(button -> {
                        if (!player.getControlledEntity().hasPermission("aegis.admin").asBoolean()
                                && flag.getPermission() != null
                                && !player.getControlledEntity().hasPermission(flag.getPermission()).asBoolean()) {
                            player.sendTip(Messages.get("command.no_permission"));
                            return;
                        }
                        openFlagEdit(player, region, flag);
                    });
        }

        form.buttonWithPathImage(Messages.get("form.settings.button.back"), "textures/ui/arrow_left")
                .onClick(button -> openRegionSettings(player, region));

        player.viewForm(form);
    }

    private <T> void openFlagEdit(Player player, Region region, Flag<T> flag) {
        if (flag instanceof StateFlag stateFlag) {
            CustomForm form = new CustomForm()
                    .title(Messages.get("form.flag_edit.title", Messages.get(flag.getDisplayName())))
                    .toggle(Messages.get("form.flag_edit.toggle"), region.getFlag(stateFlag));

            form.onResponse(responses -> {
                if (responses == null || responses.isEmpty()) {
                    return;
                }
                region.setFlag(stateFlag, Boolean.parseBoolean(responses.get(0)));
                openRegionFlags(player, region);
            });
            player.viewForm(form);
            return;
        }

        if (flag instanceof IntegerFlag integerFlag) {
            CustomForm form = new CustomForm()
                    .title(Messages.get("form.flag_edit.title", Messages.get(flag.getDisplayName())))
                    .input(Messages.get("form.flag_edit.value"), Messages.get("form.flag_edit.input.hint"),
                            String.valueOf(region.getFlag(integerFlag)));

            form.onResponse(responses -> {
                if (responses == null || responses.isEmpty()) {
                    return;
                }
                try {
                    region.setFlag(integerFlag, Integer.parseInt(responses.get(0)));
                    openRegionFlags(player, region);
                } catch (NumberFormatException exception) {
                    player.sendTip(Messages.get("form.flag_edit.invalid_number"));
                }
            });
            player.viewForm(form);
            return;
        }

        if (flag instanceof StringFlag stringFlag) {
            String currentValue = region.getFlag(stringFlag);
            CustomForm form = new CustomForm()
                    .title(Messages.get("form.flag_edit.title", Messages.get(flag.getDisplayName())))
                    .input(Messages.get("form.flag_edit.value"), Messages.get("form.flag_edit.clear_hint"),
                            currentValue == null ? "" : currentValue);

            form.onResponse(responses -> {
                if (responses == null || responses.isEmpty()) {
                    return;
                }
                String raw = responses.get(0);
                region.setFlag(stringFlag, raw == null ? "" : raw.trim());
                openRegionFlags(player, region);
            });
            player.viewForm(form);
            return;
        }

        openRegionFlags(player, region);
    }

    private void openConfirmDelete(Player player, Region region) {
        SimpleForm form = new SimpleForm()
                .title(Messages.get("form.confirm_delete.title"))
                .content(Messages.get("form.confirm_delete.content", region.getName()));

        form.buttonWithPathImage(Messages.get("form.button.yes"), "textures/ui/check")
                .onClick(button -> {
                    regionManager.removeRegion(region.getName());
                    player.sendTip(Messages.get("command.region_deleted", region.getName()));
                    openDeleteRegionForm(player);
                });

        form.buttonWithPathImage(Messages.get("form.button.no"), "textures/ui/cancel")
                .onClick(button -> openRegionSettings(player, region));

        player.viewForm(form);
    }

    private String buildMainMenuContent(Player player) {
        SelectionManager.Selection selection = selectionManager.getSelection(player.getControlledEntity());
        return Messages.get("form.selection.source", selectionManager.getSelectionSourceName()) + "\n"
                + Messages.get("form.selection.pos1", selection == null ? Messages.get("common.none") : formatPosition(selection.pos1())) + "\n"
                + Messages.get("form.selection.pos2", selection == null ? Messages.get("common.none") : formatPosition(selection.pos2())) + "\n"
                + Messages.get(selection != null && selection.isComplete() ? "form.selection.state_ready" : "form.selection.state_incomplete") + "\n"
                + Messages.get("form.region.count", regionManager.getAllRegions().size());
    }

    private String buildRegionInfo(Region region) {
        return buildRegionSummary(region) + "\n"
                + Messages.get("form.info.owners", formatPlayerList(region.getOwners())) + "\n"
                + Messages.get("form.info.members", formatPlayerList(region.getMembers()));
    }

    private String buildRegionSummary(Region region) {
        return Messages.get("form.info.region_name", region.getName()) + "\n"
                + Messages.get("form.info.world", region.getWorldName()) + "\n"
                + Messages.get("form.info.priority", region.getPriority()) + "\n"
                + Messages.get("form.info.min", formatPosition(region.getMin())) + "\n"
                + Messages.get("form.info.max", formatPosition(region.getMax())) + "\n"
                + Messages.get("form.info.active_flags", region.getFlags().size());
    }

    private List<Region> sortedRegions() {
        return regionManager.getAllRegions().stream()
                .sorted(Comparator.comparing(Region::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private String formatFlagValue(Flag<?> flag, Object value) {
        if (flag instanceof StateFlag stateFlag) {
            return Messages.get(regionFlagState(stateFlag, value) ? "form.flags.allow" : "form.flags.deny");
        }
        if (flag instanceof IntegerFlag integerFlag) {
            return String.valueOf(regionInteger(integerFlag, value));
        }
        if (flag instanceof StringFlag stringFlag) {
            String resolved = regionString(stringFlag, value);
            return resolved.isBlank() ? Messages.get("form.flags.empty_string") : resolved;
        }
        return String.valueOf(value);
    }

    private boolean regionFlagState(StateFlag flag, Object value) {
        return value == null ? flag.getDefaultValue() : flag.parse(value);
    }

    private int regionInteger(IntegerFlag flag, Object value) {
        return value == null ? flag.getDefaultValue() : flag.parse(value);
    }

    private String regionString(StringFlag flag, Object value) {
        return value == null ? flag.getDefaultValue() : flag.parse(value);
    }

    private int parseInteger(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String formatPosition(Vector3i pos) {
        if (pos == null) {
            return Messages.get("common.none");
        }
        return pos.x + ", " + pos.y + ", " + pos.z;
    }

    private String formatPlayerList(Set<String> players) {
        if (players.isEmpty()) {
            return Messages.get("common.none");
        }
        return players.stream().map(this::getPlayerName).collect(Collectors.joining(", "));
    }

    private String getPlayerName(String uuid) {
        return nameManager.getName(uuid);
    }

    private String getFlagIcon(Flag<?> flag) {
        String name = flag.getName().toLowerCase();
        if (name.contains("pvp")) {
            return "textures/items/diamond_sword";
        }
        if (name.contains("build")) {
            return "textures/blocks/grass_side_carried";
        }
        if (name.contains("chest")) {
            return "textures/blocks/chest_front";
        }
        if (name.contains("use")) {
            return "textures/items/lever";
        }
        if (name.contains("entry")) {
            return "textures/items/door_wood";
        }
        if (name.contains("exit")) {
            return "textures/items/door_iron";
        }
        if (name.contains("chat")) {
            return "textures/items/paper";
        }
        if (name.contains("drop")) {
            return "textures/ui/trash";
        }
        if (name.contains("pickup")) {
            return "textures/ui/color_plus";
        }
        if (name.contains("interact")) {
            return "textures/ui/op";
        }
        if (name.contains("command")) {
            return "textures/items/book_writable";
        }
        if (name.contains("greeting")) {
            return "textures/items/paper";
        }
        if (name.contains("farewell")) {
            return "textures/ui/cancel";
        }
        return "textures/ui/op";
    }
}
