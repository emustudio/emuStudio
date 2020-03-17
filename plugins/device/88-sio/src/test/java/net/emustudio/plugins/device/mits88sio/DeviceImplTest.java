package net.emustudio.plugins.device.mits88sio;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.ExtendedContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNotEquals;

public class DeviceImplTest {
    private DeviceImpl device;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws PluginInitializationException {
        ContextPool contextPool = createNiceMock(ContextPool.class);
        expect(contextPool.getCPUContext(0, ExtendedContext.class))
            .andReturn(createNiceMock(ExtendedContext.class)).anyTimes();
        expect(contextPool.getDeviceContext(0, DeviceContext.class))
            .andThrow(new ContextNotFoundException("")).anyTimes();
        replay(contextPool);
        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        this.device = new DeviceImpl(0, applicationApi, PluginSettings.UNAVAILABLE);
        device.initialize();
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
