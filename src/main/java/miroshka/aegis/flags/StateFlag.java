package miroshka.aegis.flags;

public class StateFlag extends Flag<Boolean> {

    public StateFlag(String name, String displayName, boolean defaultValue, String permission) {
        super(name, displayName, defaultValue, permission);
    }

    @Override
    public Boolean parse(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return Boolean.parseBoolean(value.toString());
    }

    @Override
    public Object serialize(Boolean value) {
        return value;
    }
}
