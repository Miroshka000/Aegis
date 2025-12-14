package miroshka.aegis.flags;

import lombok.Getter;

@Getter
public abstract class Flag<T> {
    private final String name;
    private final String displayName;
    private final T defaultValue;
    private final String permission;

    public Flag(String name, String displayName, T defaultValue, String permission) {
        this.name = name;
        this.displayName = displayName;
        this.defaultValue = defaultValue;
        this.permission = permission;
    }

    public abstract T parse(Object value);

    public abstract Object serialize(T value);
}
