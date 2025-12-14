package miroshka.aegis.form;

import miroshka.aegis.flags.*;
import miroshka.aegis.manager.NameManager;
import miroshka.aegis.manager.RegionManager;
import miroshka.aegis.manager.SelectionManager;
import miroshka.aegis.region.Region;
import miroshka.aegis.utils.Messages;
import org.allaymc.api.form.type.CustomForm;
import org.allaymc.api.form.type.SimpleForm;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.player.Player;

import java.util.stream.Collectors;

public class AegisForms {
    private final RegionManager regionManager;
    private final SelectionManager selectionManager;
    private final NameManager nameManager;

    public AegisForms(RegionManager regionManager, SelectionManager selectionManager, NameManager nameManager) {
        this.regionManager = regionManager;
        this.selectionManager = selectionManager;
        this.nameManager = nameManager;
    }

    public void openMainMenu(Player player) {
        SimpleForm form = new SimpleForm()
                .title(Messages.get("form.main_menu.title"))
                .content(Messages.get("form.main_menu.content"));

        form.buttonWithPathImage(Messages.get("form.button.create_region"), "textures/ui/color_plus")
                .onClick(button -> openCreateRegionForm(player));

        form.buttonWithPathImage(Messages.get("form.button.delete_region"), "textures/ui/trash")
                .onClick(button -> openDeleteRegionForm(player));

        form.buttonWithPathImage(Messages.get("form.button.list_regions"), "textures/items/book_writable")
                .onClick(button -> openListRegionsForm(player));

        form.buttonWithPathImage(Messages.get("form.button.get_wand"), "textures/items/wood_axe")
                .onClick(button -> {
                    player.getControlledEntity().tryAddItem(ItemTypes.WOODEN_AXE.createItemStack());
                    player.sendTip(Messages.get("command.wand_given"));
                });

        player.viewForm(form);
    }

    private void openCreateRegionForm(Player player) {
        CustomForm form = new CustomForm()
                .title(Messages.get("form.create_region.title"))
                .input(Messages.get("form.create_region.input.name"),
                        Messages.get("form.create_region.input.placeholder"));

        form.onResponse(responses -> {
            if (responses == null || responses.isEmpty())
                return;
            String name = responses.get(0);
            if (name == null || name.isEmpty())
                return;

            if (regionManager.getRegion(name) != null) {
                player.sendTip(Messages.get("command.region_exists", name));
                return;
            }

            SelectionManager.Selection selection = selectionManager
                    .getSelection(player.getControlledEntity().getUniqueId());
            if (selection == null || !selection.isComplete()) {
                player.sendTip(Messages.get("command.selection_missing"));
                return;
            }

            Region region = new Region(name, selection.pos1, selection.pos2,
                    player.getControlledEntity().getWorld().getName(), 0);
            region.addOwner(player.getControlledEntity().getUniqueId().toString());

            regionManager.addRegion(region);
            player.sendTip(Messages.get("command.region_created", name));
        });

        player.viewForm(form);
    }

    private void openDeleteRegionForm(Player player) {
        CustomForm form = new CustomForm()
                .title(Messages.get("form.delete_region.title"))
                .input(Messages.get("form.delete_region.input.name"),
                        Messages.get("form.delete_region.input.placeholder"));

        form.onResponse(responses -> {
            if (responses == null || responses.isEmpty())
                return;
            String name = responses.get(0);
            if (name == null || name.isEmpty())
                return;

            if (regionManager.getRegion(name) == null) {
                player.sendTip(Messages.get("command.region_not_found", name));
                return;
            }

            regionManager.removeRegion(name);
            player.sendTip(Messages.get("command.region_deleted", name));
        });

        player.viewForm(form);
    }

    private void openListRegionsForm(Player player) {
        SimpleForm form = new SimpleForm().title(Messages.get("form.list_regions.title"));
        for (Region region : regionManager.getAllRegions()) {
            form.buttonWithPathImage(region.getName(), "textures/ui/world_glyph")
                    .onClick(button -> openRegionSettings(player, region));
        }
        player.viewForm(form);
    }

    public void openRegionInfo(Player player, java.util.List<Region> regions) {
        StringBuilder content = new StringBuilder();

        if (regions.isEmpty()) {
            content.append(Messages.get("info.no_region"));
        } else {
            for (Region region : regions) {
                String owners = region.getOwners().stream().map(this::getPlayerName).collect(Collectors.joining(", "));
                String members = region.getMembers().stream().map(this::getPlayerName)
                        .collect(Collectors.joining(", "));

                content.append(Messages.get("form.info.region_name", region.getName())).append("\n");
                content.append("Min: ").append(region.getMin()).append("\n");
                content.append("Max: ").append(region.getMax()).append("\n");
                content.append(Messages.get("form.info.owners", owners)).append("\n");
                content.append(Messages.get("form.info.members", members))
                        .append("\n\n");
            }
        }

        SimpleForm form = new SimpleForm()
                .title(Messages.get("form.info.title"))
                .content(content.toString());
        form.buttonWithPathImage("OK", "textures/ui/check");

        player.viewForm(form);
    }

    private void openRegionSettings(Player player, Region region) {
        SimpleForm form = new SimpleForm()
                .title(Messages.get("form.settings.title", region.getName()))
                .content(Messages.get("form.info.region_name", region.getName()) + "\n" +
                        "Min: " + region.getMin() + "\n" +
                        "Max: " + region.getMax());

        boolean isOwner = region.getOwners().contains(player.getControlledEntity().getUniqueId().toString());
        boolean isAdmin = player.getControlledEntity().hasPermission("aegis.admin").asBoolean();

        if (isOwner || isAdmin) {
            form.buttonWithPathImage(Messages.get("form.settings.button.flags"), "textures/ui/op")
                    .onClick(b -> openRegionFlags(player, region));

            form.buttonWithPathImage(Messages.get("form.settings.button.members"), "textures/ui/multiplayer_glyph")
                    .onClick(b -> openRegionMembers(player, region));

            form.buttonWithPathImage(Messages.get("form.settings.button.delete"), "textures/ui/trash")
                    .onClick(b -> openConfirmDelete(player, region));
        }

        form.buttonWithPathImage(Messages.get("form.settings.button.back"), "textures/ui/arrow_left")
                .onClick(b -> openListRegionsForm(player));

        player.viewForm(form);
    }

    private void openRegionMembers(Player player, Region region) {
        SimpleForm form = new SimpleForm()
                .title(Messages.get("form.members.title", region.getName()));

        form.buttonWithPathImage(Messages.get("form.members.button.add"), "textures/ui/color_plus")
                .onClick(b -> openAddMember(player, region));

        for (String memberUuid : region.getMembers()) {
            String name = getPlayerName(memberUuid);
            form.buttonWithPathImage(name + "\n" + Messages.get("form.members.button.remove"), "textures/ui/trash")
                    .onClick(b -> {
                        region.removeMember(memberUuid);
                        player.sendTip(Messages.get("command.member.removed", name, region.getName()));
                        openRegionMembers(player, region);
                    });
        }

        form.buttonWithPathImage(Messages.get("form.settings.button.back"), "textures/ui/arrow_left")
                .onClick(b -> openRegionSettings(player, region));

        player.viewForm(form);
    }

    private void openAddMember(Player player, Region region) {
        CustomForm form = new CustomForm()
                .title(Messages.get("form.members.add.title"));

        form.input(Messages.get("form.members.add.input"), "Player Name");

        form.onResponse(r -> {
            if (r == null || r.isEmpty())
                return;
            String name = r.get(0);
            if (name == null || name.isEmpty())
                return;

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
                .title("Flags: " + region.getName());

        for (Flag<?> flag : FlagRegistry.getAll()) {
            Object val = region.getFlag(flag);
            String strVal = String.valueOf(val);

            if (val instanceof Boolean) {
                strVal = (Boolean) val ? "§aTrue" : "§cFalse";
            }

            form.buttonWithPathImage(Messages.get(flag.getDisplayName()) + "\n" + strVal, getFlagIcon(flag))
                    .onClick(b -> {
                        if (!player.getControlledEntity().hasPermission("aegis.admin").asBoolean() &&
                                flag.getPermission() != null &&
                                !player.getControlledEntity().hasPermission(flag.getPermission()).asBoolean()) {
                            player.sendTip(Messages.get("command.no_permission"));
                        } else {
                            openFlagEdit(player, region, flag);
                        }
                    });
        }
        form.buttonWithPathImage(Messages.get("form.settings.button.back"), "textures/ui/arrow_left")
                .onClick(b -> openRegionSettings(player, region));

        player.viewForm(form);
    }

    private <T> void openFlagEdit(Player player, Region region, Flag<T> flag) {
        CustomForm form = new CustomForm().title("Edit " + Messages.get(flag.getDisplayName()));

        if (flag instanceof StateFlag) {
            boolean current = (Boolean) region.getFlag(flag);
            form.toggle("Enabled", current);
            form.onResponse(r -> {
                if (r == null || r.isEmpty())
                    return;
                Object val = r.get(0);
                if (val instanceof Boolean) {
                    boolean newVal = (Boolean) val;
                    region.setFlag((StateFlag) flag, newVal);
                    openRegionFlags(player, region);
                }
            });
        } else if (flag instanceof IntegerFlag) {
            int current = (Integer) region.getFlag(flag);
            form.input("Value", String.valueOf(current));
            form.onResponse(r -> {
                if (r == null || r.isEmpty())
                    return;
                Object s = r.get(0);
                if (s == null)
                    return;
                try {
                    int val = Integer.parseInt(s.toString());
                    region.setFlag((IntegerFlag) flag, val);
                } catch (NumberFormatException e) {
                    player.sendTip("Invalid number");
                }
                openRegionFlags(player, region);
            });
        } else if (flag instanceof StringFlag) {
            String current = (String) region.getFlag(flag);
            form.input("Value", current == null ? "" : current);
            form.onResponse(r -> {
                if (r == null || r.isEmpty())
                    return;
                Object s = r.get(0);
                region.setFlag((StringFlag) flag, s == null ? "" : s.toString());
                openRegionFlags(player, region);
            });
        } else {
            openRegionFlags(player, region);
        }

        player.viewForm(form);
    }

    private void openConfirmDelete(Player player, Region region) {
        SimpleForm form = new SimpleForm()
                .title(Messages.get("form.confirm_delete.title"))
                .content(Messages.get("form.confirm_delete.content", region.getName()));

        form.buttonWithPathImage(Messages.get("form.button.yes"), "textures/ui/check")
                .onClick(b -> {
                    regionManager.removeRegion(region.getName());
                    player.sendTip(Messages.get("command.region_deleted", region.getName()));
                    openListRegionsForm(player);
                });

        form.buttonWithPathImage(Messages.get("form.button.no"), "textures/ui/cancel")
                .onClick(b -> openRegionSettings(player, region));

        player.viewForm(form);
    }

    private String getFlagIcon(Flag<?> flag) {
        String name = flag.getName().toLowerCase();
        if (name.contains("pvp"))
            return "textures/items/diamond_sword";
        if (name.contains("build"))
            return "textures/blocks/grass_side_carried";
        if (name.contains("chest"))
            return "textures/blocks/chest_front";
        if (name.contains("use"))
            return "textures/items/lever";
        if (name.contains("entry"))
            return "textures/items/door_wood";
        if (name.contains("exit"))
            return "textures/items/door_iron";
        if (name.contains("chat"))
            return "textures/items/paper";
        return "textures/ui/op";
    }

    private String getPlayerName(String uuidStr) {
        return nameManager.getName(uuidStr);
    }

    public NameManager getNameManager() {
        return nameManager;
    }
}
