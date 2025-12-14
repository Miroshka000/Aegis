package miroshka.aegis.flags;

public class IntegerFlag extends Flag<Integer> {

    public IntegerFlag(String name, String displayName, Integer defaultValue, String permission) {
        super(name, displayName, defaultValue, permission);
    }

    @Override
    public Integer parse(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return getDefaultValue();
        }
    }

    @Override
    public Object serialize(Integer value) {
        return value;
    }
}
