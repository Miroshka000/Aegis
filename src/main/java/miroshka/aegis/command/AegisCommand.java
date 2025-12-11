package miroshka.aegis.command;

import miroshka.aegis.form.AegisForms;
import miroshka.aegis.manager.RegionManager;
import miroshka.aegis.manager.SelectionManager;
import miroshka.aegis.region.Region;
import miroshka.aegis.utils.Messages;
import org.allaymc.api.command.Command;
import org.allaymc.api.command.SenderType;
import org.allaymc.api.command.tree.CommandTree;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.player.Player;

public class AegisCommand extends Command {
    private final RegionManager regionManager;
    private final SelectionManager selectionManager;
    private final AegisForms aegisForms;

    public AegisCommand(RegionManager regionManager, SelectionManager selectionManager) {
        super("aegis", "Aegis region management", null);
        this.regionManager = regionManager;
        this.selectionManager = selectionManager;
        this.aegisForms = new AegisForms(regionManager, selectionManager);
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
                }, SenderType.ACTUAL_PLAYER);
    }
}
