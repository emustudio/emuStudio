package emustudio.gui.debugTable;

import org.junit.Test;

public class CallFlowTest {

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceNullDisassemblerThrows() throws Exception {
        new CallFlow(null);
    }


}
