package net.emustudio.plugins.device.adm3a;

import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertNotEquals;

public class DeviceImplTest {
    private DeviceImpl device;

    @Before
    public void setup() {
        ContextPool contextPool = createNiceMock(ContextPool.class);
        replay(contextPool);
        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        expect(applicationApi.getDialogs()).andReturn(createNiceMock(Dialogs.class)).anyTimes();
        replay(applicationApi);

        this.device = new DeviceImpl(0, applicationApi, PluginSettings.UNAVAILABLE);
    }

    @After
    public void tearDown() {
        device.destroy();
    }

    @Test
    public void testVersionIsKnown() {
        assertNotEquals("(unknown)", device.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        assertNotEquals("(unknown)", device.getCopyright());
    }
}
