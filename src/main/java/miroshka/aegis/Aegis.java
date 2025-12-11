package miroshka.aegis;

import miroshka.aegis.command.AegisCommand;
import miroshka.aegis.listener.AegisListener;
import miroshka.aegis.manager.NameManager;
import miroshka.aegis.manager.RegionManager;
import miroshka.aegis.manager.SelectionManager;
import miroshka.aegis.storage.StorageManager;
import org.allaymc.api.plugin.Plugin;
import org.allaymc.api.registry.Registries;
import org.allaymc.api.server.Server;

import java.io.File;

public class Aegis extends Plugin {
    private StorageManager storageManager;
    private NameManager nameManager;

    @Override
    public void onEnable() {
        RegionManager regionManager = new RegionManager();
        SelectionManager selectionManager = new SelectionManager();
        File dataFolder = getPluginContainer().dataFolder().toFile();
        storageManager = new StorageManager(regionManager, dataFolder);
        nameManager = new NameManager(dataFolder);

        storageManager.load();

        Server.getInstance().getEventBus()
                .registerListener(new AegisListener(regionManager, selectionManager, nameManager));
        Registries.COMMANDS.register(new AegisCommand(regionManager, selectionManager, nameManager));
    }

    @Override
    public void onDisable() {
        if (storageManager != null) {
            storageManager.save();
        }
        if (nameManager != null) {
            nameManager.save();
        }
    }
}
