package miroshka.aegis.flags;

public class StringFlag extends Flag<String> {

    public StringFlag(String name, String displayName, String defaultValue, String permission) {
        super(name, displayName, defaultValue, permission);
    }

    @Override
    public String parse(Object value) {
        return value.toString();
    }

    @Override
    public Object serialize(String value) {
        return value;
    }
}
