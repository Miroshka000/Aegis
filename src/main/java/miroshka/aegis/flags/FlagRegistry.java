package miroshka.aegis.flags;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FlagRegistry {
    private static final Map<String, Flag<?>> flags = new HashMap<>();

    public static final StateFlag BUILD = register(
            new StateFlag("build", "flag.build.name", false, "aegis.flag.build"));
    public static final StateFlag PVP = register(new StateFlag("pvp", "flag.pvp.name", false, "aegis.flag.pvp"));
    public static final StateFlag ENTRY = register(new StateFlag("entry", "flag.entry.name", true, "aegis.flag.entry"));
    public static final StateFlag EXIT = register(new StateFlag("exit", "flag.exit.name", true, "aegis.flag.exit"));
    public static final StateFlag CHEST_ACCESS = register(
            new StateFlag("chest-access", "flag.chest_access.name", false, "aegis.flag.chest_access"));
    public static final StateFlag USE = register(new StateFlag("use", "flag.use.name", false, "aegis.flag.use"));
    public static final StateFlag CHAT = register(new StateFlag("chat", "flag.chat.name", true, "aegis.flag.chat"));

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
