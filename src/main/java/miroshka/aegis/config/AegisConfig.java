package miroshka.aegis.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Header("Aegis configuration")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AegisConfig extends OkaeriConfig {
    @Comment("Use WorldEdit selection engine when the plugin is available")
    boolean preferWorldEditSelection = true;

    @Comment("Show local selection box when WorldEdit is unavailable")
    boolean localSelectionVisualization = true;

    @Comment("Default priority for newly created regions")
    int defaultPriority = 0;
}
