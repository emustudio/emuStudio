package emustudio.architecture;

import emustudio.drawing.Schema;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.net.URISyntaxException;
import java.util.Properties;

import static emustudio.architecture.ComputerConfigTest.getBaseDirectory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConfigurationImplTest {

    @BeforeClass
    public static void setUpClass() throws URISyntaxException {
        ComputerConfig.setConfigBaseDir(getBaseDirectory().toFile().getAbsolutePath());
    }

    @AfterClass
    public static void tearDownClass() {
        ComputerConfig.setConfigBaseDir(System.getProperty("user.dir"));
    }

    @Test
    public void testLoadSchema() throws Exception {
        String configName = "tmp";
        File configFile = ComputerConfig.getConfigDir().resolve(configName + ".conf").toFile();

        Properties properties = new Properties();
        properties.load(new FileInputStream(configFile));

        ConfigurationImpl configuration = new ConfigurationImpl(configName, properties);

        Schema result = configuration.loadSchema();
        assertNotNull(result);
        assertEquals(configName, result.getConfigName());
        assertNotNull(result.getCompilerElement());
        assertNotNull(result.getCpuElement());
        assertNotNull(result.getMemoryElement());
        assertEquals(1, result.getDeviceElements().size());
        assertEquals("brainc-brainduck-0.15.1-SNAPSHOT", result.getCompilerElement().getPluginName());
        assertEquals("brainduck-cpu-0.14.1-SNAPSHOT", result.getCpuElement().getPluginName());
        assertEquals("brainduck-mem-0.12.1-SNAPSHOT", result.getMemoryElement().getPluginName());
        assertEquals("terminal-brainduck-0.13.1-SNAPSHOT", result.getDeviceElements().get(0).getPluginName());
        assertEquals(20, result.getGridGap());
        assertTrue(result.isGridUsed());
        assertEquals(3, result.getConnectionLines().size());
    }

}
