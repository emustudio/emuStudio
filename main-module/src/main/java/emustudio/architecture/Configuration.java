package emustudio.architecture;

import emustudio.drawing.Schema;

public interface Configuration {
    public String get(String key);
    public String get(String key, String defaultValue);
    public void set(String key, String value);
    public void remove(String key);
    public boolean contains(String key);

    public void write() throws WriteConfigurationException;
    public Schema loadSchema();
}
