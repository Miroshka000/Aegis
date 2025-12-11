package miroshka.aegis;

import miroshka.aegis.command.AegisCommand;
import miroshka.aegis.listener.AegisListener;
import miroshka.aegis.manager.RegionManager;
import miroshka.aegis.manager.SelectionManager;
import miroshka.aegis.storage.StorageManager;
import org.allaymc.api.plugin.Plugin;
import org.allaymc.api.registry.Registries;
import org.allaymc.api.server.Server;

import java.io.File;

public class Aegis extends Plugin {
    private StorageManager storageManager;

    @Override
    public void onEnable() {
        RegionManager regionManager = new RegionManager();
        SelectionManager selectionManager = new SelectionManager();
        File dataFolder = getPluginContainer().dataFolder().toFile();
        storageManager = new StorageManager(regionManager, dataFolder);

        storageManager.load();

        Server.getInstance().getEventBus().registerListener(new AegisListener(regionManager, selectionManager));
        Registries.COMMANDS.register(new AegisCommand(regionManager, selectionManager));
    }

    @Override
    public void onDisable() {
        if (storageManager != null) {
            storageManager.save();
        }
    }
}
