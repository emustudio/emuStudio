package dependencies;

import dependencies.hidden.BdependsOnC;
import net.emustudio.emulib.plugins.Plugin;
import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.settings.PluginSettings;

import javax.swing.*;

@PluginRoot(
        type = PLUGIN_TYPE.COMPILER,
        title = "A"
)
public class APluginDependsOnB implements Plugin {
    private final BdependsOnC b = new BdependsOnC();

    public APluginDependsOnB(long pid, ApplicationApi applicationApi, PluginSettings settings) {

    }

    public void hi() {
        b.hi();
    }

    @Override
    public void reset() {

    }

    @Override
    public void initialize() throws PluginInitializationException {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void showSettings(JFrame parent) {

    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public String getCopyright() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }
}
