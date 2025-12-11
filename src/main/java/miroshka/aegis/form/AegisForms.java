package miroshka.aegis.form;

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

        form.button(Messages.get("form.button.create_region"))
                .onClick(button -> openCreateRegionForm(player));

        form.button(Messages.get("form.button.delete_region"))
                .onClick(button -> openDeleteRegionForm(player));

        form.button(Messages.get("form.button.list_regions"))
                .onClick(button -> openListRegionsForm(player));

        form.button(Messages.get("form.button.get_wand"))
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
            form.button(region.getName()).onClick(button -> openRegionSettings(player, region));
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
                content.append(Messages.get("form.info.owners", owners)).append("\n");
                content.append(Messages.get("form.info.members", members))
                        .append("\n\n");
            }
        }

        SimpleForm form = new SimpleForm()
                .title(Messages.get("form.info.title"))
                .content(content.toString());
        form.button("OK");

        player.viewForm(form);
    }

    private void openRegionSettings(Player player, Region region) {
        SimpleForm form = new SimpleForm()
                .title(Messages.get("form.settings.title", region.getName()))
                .content(Messages.get("form.info.region_name", region.getName()));

        boolean isOwner = region.getOwners().contains(player.getControlledEntity().getUniqueId().toString());
        boolean isAdmin = player.getControlledEntity().hasPermission("aegis.admin").asBoolean();

        if (isOwner || isAdmin) {
            form.button(Messages.get("form.settings.button.delete"))
                    .onClick(b -> openConfirmDelete(player, region));
        }

        form.button(Messages.get("form.settings.button.back"))
                .onClick(b -> openListRegionsForm(player));

        player.viewForm(form);
    }

    private void openConfirmDelete(Player player, Region region) {
        SimpleForm form = new SimpleForm()
                .title(Messages.get("form.confirm_delete.title"))
                .content(Messages.get("form.confirm_delete.content", region.getName()));

        form.button(Messages.get("form.button.yes")).onClick(b -> {
            regionManager.removeRegion(region.getName());
            player.sendTip(Messages.get("command.region_deleted", region.getName()));
            openListRegionsForm(player);
        });

        form.button(Messages.get("form.button.no"))
                .onClick(b -> openRegionSettings(player, region));

        player.viewForm(form);
    }

    private String getPlayerName(String uuidStr) {
        return nameManager.getName(uuidStr);
    }
}
