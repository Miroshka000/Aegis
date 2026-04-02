package miroshka.aegis;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import miroshka.aegis.command.AegisCommand;
import miroshka.aegis.config.AegisConfig;
import miroshka.aegis.listener.AegisListener;
import miroshka.aegis.manager.NameManager;
import miroshka.aegis.manager.RegionManager;
import miroshka.aegis.manager.SelectionManager;
import miroshka.aegis.storage.StorageManager;
import org.allaymc.api.plugin.Plugin;
import org.allaymc.api.registry.Registries;
import org.allaymc.api.server.Server;

import java.io.File;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Aegis extends Plugin {
    StorageManager storageManager;
    NameManager nameManager;
    RegionManager regionManager;
    SelectionManager selectionManager;
    AegisConfig config;

    @Override
    public void onEnable() {
        File dataFolder = getPluginContainer().dataFolder().toFile();
        regionManager = new RegionManager();
        config = loadConfig();
        selectionManager = new SelectionManager(config);
        storageManager = new StorageManager(regionManager, dataFolder);
        nameManager = new NameManager(dataFolder);

        storageManager.load();

        Server.getInstance().getEventBus()
                .registerListener(new AegisListener(regionManager, selectionManager, nameManager, config));
        Registries.COMMANDS.register(new AegisCommand(regionManager, selectionManager, nameManager, config));
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

    private AegisConfig loadConfig() {
        return (AegisConfig) ConfigManager.create(AegisConfig.class, initializer -> {
            initializer.withConfigurer(new YamlSnakeYamlConfigurer());
            initializer.withBindFile(getPluginContainer().dataFolder().resolve("config.yml"));
            initializer.saveDefaults();
            initializer.load(true);
        });
    }
}
