package miroshka.aegis.flags;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class FlagRegistry {
    private static final Map<String, Flag<?>> flags = new LinkedHashMap<>();

    public static final StateFlag BUILD = register(
            new StateFlag("build", "flag.build.name", false, "aegis.flag.build"));
    public static final StateFlag PVP = register(new StateFlag("pvp", "flag.pvp.name", false, "aegis.flag.pvp"));
    public static final StateFlag ENTRY = register(new StateFlag("entry", "flag.entry.name", true, "aegis.flag.entry"));
    public static final StateFlag EXIT = register(new StateFlag("exit", "flag.exit.name", true, "aegis.flag.exit"));
    public static final StateFlag CHEST_ACCESS = register(
            new StateFlag("chest-access", "flag.chest_access.name", false, "aegis.flag.chest_access"));
    public static final StateFlag USE = register(new StateFlag("use", "flag.use.name", false, "aegis.flag.use"));
    public static final StateFlag CHAT = register(new StateFlag("chat", "flag.chat.name", true, "aegis.flag.chat"));
    public static final StateFlag DROP_ITEMS = register(
            new StateFlag("drop-items", "flag.drop_items.name", true, "aegis.flag.drop_items"));
    public static final StateFlag PICKUP_ITEMS = register(
            new StateFlag("pickup-items", "flag.pickup_items.name", true, "aegis.flag.pickup_items"));
    public static final StateFlag INTERACT_ENTITIES = register(
            new StateFlag("interact-entities", "flag.interact_entities.name", false, "aegis.flag.interact_entities"));
    public static final StateFlag COMMANDS = register(
            new StateFlag("commands", "flag.commands.name", true, "aegis.flag.commands"));
    public static final StringFlag GREETING = register(
            new StringFlag("greeting", "flag.greeting.name", "", "aegis.flag.greeting"));
    public static final StringFlag FAREWELL = register(
            new StringFlag("farewell", "flag.farewell.name", "", "aegis.flag.farewell"));

    public static <T extends Flag<?>> T register(T flag) {
        flags.put(flag.getName(), flag);
        return flag;
    }

    public static Flag<?> get(String name) {
        return flags.get(name);
    }

    public static Collection<Flag<?>> getAll() {
        return flags.values();
    }
}
