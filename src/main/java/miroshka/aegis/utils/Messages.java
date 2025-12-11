package miroshka.aegis.utils;

import org.allaymc.api.message.I18n;
import org.allaymc.api.utils.TextFormat;

public class Messages {

    public static String get(String key, Object... args) {
        return TextFormat.colorize(I18n.get().tr("aegis:" + key, args));
    }
}
