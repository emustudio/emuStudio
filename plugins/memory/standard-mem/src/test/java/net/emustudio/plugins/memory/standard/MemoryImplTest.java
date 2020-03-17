package net.emustudio.plugins.memory.standard;

import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.PluginSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNotEquals;

public class MemoryImplTest {
    private MemoryImpl memory;

    @Before
    public void setup() {
        ContextPool contextPool = createNiceMock(ContextPool.class);
        replay(contextPool);
        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        this.memory = new MemoryImpl(0, applicationApi, PluginSettings.UNAVAILABLE);
    }

    @After
    public void tearDown() {
        memory.destroy();
    }

    @Test
    public void testVersionIsKnown() {
        assertNotEquals("(unknown)", memory.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        assertNotEquals("(unknown)", memory.getCopyright());
    }
}
