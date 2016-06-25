package net.sf.emustudio.intel8080.api;

import org.junit.Test;

import static org.easymock.EasyMock.anyFloat;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class FrequencyUpdaterTest {

    @Test
    public void testFrequencyNotChanged() throws Exception {
        CpuEngine cpuEngine = createMock(CpuEngine.class);
        expect(cpuEngine.getAndResetExecutedCycles()).andReturn(0L);
        replay(cpuEngine);

        new FrequencyUpdater(cpuEngine).run();

        verify(cpuEngine);
    }

    @Test
    public void testFrequencyChanged() throws Exception {
        CpuEngine cpuEngine = createMock(CpuEngine.class);
        expect(cpuEngine.getAndResetExecutedCycles()).andReturn(2000L);
        cpuEngine.fireFrequencyChanged(anyFloat());
        expectLastCall().once();
        replay(cpuEngine);

        new FrequencyUpdater(cpuEngine).run();

        verify(cpuEngine);
    }
}
