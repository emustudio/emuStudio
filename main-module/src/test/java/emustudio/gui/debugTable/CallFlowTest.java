package emustudio.gui.debugTable;

import emulib.plugins.cpu.Disassembler;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CallFlowTest {

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceNullDisassemblerThrows() throws Exception {
        new CallFlow(null);
    }

    @Test
    public void testKnownFromButEmptyUntilTo() throws Exception {
        CallFlow callFlow = makeCallFlow(0);
        List<Integer> locations = callFlow.getLocationsInterval(0, 9);

        assertEquals(Arrays.asList(0,1,2,3,4,5,6,7,8,9), locations);
    }

    @Test
    public void testEmptyFromButKnownTo() throws Exception {
        CallFlow callFlow = makeCallFlow(9);
        List<Integer> locations = callFlow.getLocationsInterval(0, 9);

        assertEquals(Arrays.asList(9), locations);
    }

    @Test
    public void testPreviousFromEmptyUntilTo() throws Exception {
        CallFlow callFlow = makeCallFlow(0);
        List<Integer> locations = callFlow.getLocationsInterval(5,9);

        assertEquals(Arrays.asList(5,6,7,8,9), locations);
    }

    @Test
    public void testHigherFromEmptyUntilTo() throws Exception {
        CallFlow callFlow = makeCallFlow(3);
        List<Integer> locations = callFlow.getLocationsInterval(0, 5);

        assertEquals(Arrays.asList(3,4,5), locations);
    }

    @Test
    public void testKnownFromPreviousUntilTo() throws Exception {
        CallFlow callFlow = makeCallFlow(3, 5);
        List<Integer> locations = callFlow.getLocationsInterval(3, 9);

        assertEquals(Arrays.asList(3,4,5,6,7,8,9), locations);
    }

    @Test
    public void testEmptyFromPreviousUntilTo() throws Exception {
        CallFlow callFlow = makeCallFlow(5);
        List<Integer> locations = callFlow.getLocationsInterval(3, 9);

        assertEquals(Arrays.asList(5,6,7,8,9), locations);
    }

    @Test
    public void testKnownFromUnfitTo() throws Exception {
        CallFlow callFlow = makeCallFlowStep(0, 2);
        List<Integer> locations = callFlow.getLocationsInterval(0, 3);

        assertEquals(Arrays.asList(0,2), locations);
    }

    @Test
    public void testUnfitBeforeKnownFromKnownTo() throws Exception {
        CallFlow callFlow = makeCallFlowStep(2, 6);
        List<Integer> locations = callFlow.getLocationsInterval(1, 6);

        assertEquals(Arrays.asList(2,4,6), locations);
    }

    @Test
    public void testUnfitAfterKnownFromKnownTo() throws Exception {
        CallFlow callFlow = makeCallFlowStep(2, 6);
        List<Integer> locations = callFlow.getLocationsInterval(3, 6);

        assertEquals(Arrays.asList(2,4,6), locations);
    }

    @Test
    public void testUnfitPreviousKnownFromUnfitPreviousKnownTo() throws Exception {
        CallFlow callFlow = makeCallFlowStep(2, 6);
        List<Integer> locations = callFlow.getLocationsInterval(1, 5);

        assertEquals(Arrays.asList(2, 4), locations);
    }

    @Test
    public void testUnfitAfterKnownFromUnfitPreviousKnownTo() throws Exception {
        CallFlow callFlow = makeCallFlowStep(0, 6);
        List<Integer> locations = callFlow.getLocationsInterval(1, 5);

        assertEquals(Arrays.asList(0, 2, 4), locations);
    }

    @Test
    public void testUnfitPreviousKnownFromUnfitAfterKnownTo() throws Exception {
        CallFlow callFlow = makeCallFlowStep(2, 6);
        List<Integer> locations = callFlow.getLocationsInterval(1, 7);

        assertEquals(Arrays.asList(2,4,6), locations);
    }

    @Test
    public void testUnfitAfterKnownFromUnfitAfterKnownTo() throws Exception {
        CallFlow callFlow = makeCallFlowStep(0, 6);
        List<Integer> locations = callFlow.getLocationsInterval(1, 7);

        assertEquals(Arrays.asList(0,2,4,6), locations);
    }

    @Test
    public void testUnfitEmptyKnownUnfitEmptyTo() throws Exception {
        CallFlow callFlow = makeCallFlowStep();
        List<Integer> locations = callFlow.getLocationsInterval(0, 5);

        assertEquals(Arrays.asList(0,2,4), locations);
    }

    @Test
    public void testSelfModificationKnownFromKnownUntilTo() throws Exception {
        DisassemblerStub disasm = makeDisassembler();
        CallFlow callFlow = makeCallFlow(disasm, 2, 4);
        modify(callFlow, disasm, 2, 6);

        List<Integer> locations = callFlow.getLocationsInterval(2, 8);

        assertEquals(Arrays.asList(2,6,7,8), locations);
    }

    @Test
    public void testSelfModificationPreviousKnownFromPreviousKnownUntilTo() throws Exception {
        DisassemblerStub disasm = makeDisassembler();
        CallFlow callFlow = makeCallFlow(disasm, 2, 4);
        modify(callFlow, disasm, 2, 6);

        List<Integer> locations = callFlow.getLocationsInterval(3, 8);

        assertEquals(Arrays.asList(2,6,7,8), locations);
    }

    @Test
    public void testSelfModificationAfterKnownFromPreviousKnownUntilTo() throws Exception {
        DisassemblerStub disasm = makeDisassembler();
        CallFlow callFlow = makeCallFlow(disasm, 2, 4);
        modify(callFlow, disasm, 2, 6);

        List<Integer> locations = callFlow.getLocationsInterval(1, 8);

        assertEquals(Arrays.asList(2,6,7,8), locations);
    }

    @Test
    public void testSelfModificationKnownFromEmptyUntilTo() throws Exception {
        DisassemblerStub disasm = makeDisassembler();
        CallFlow callFlow = makeCallFlow(disasm, 2);
        modify(callFlow, disasm, 2, 6 );

        List<Integer> locations = callFlow.getLocationsInterval(2, 8);

        assertEquals(Arrays.asList(2,6,7,8), locations);
    }

    @Test
    public void testSelfModificationKnownFromKnownUntilToLonger() throws Exception {
        DisassemblerStub disasm = makeDisassembler();
        CallFlow callFlow = makeCallFlow(disasm, 2, 4, 5, 8);
        modify(callFlow, disasm, 2, 6);

        List<Integer> locations = callFlow.getLocationsInterval(2, 8);

        assertEquals(Arrays.asList(2,6,7,8), locations);
    }

    @Test
    public void testInvalidLocationBetweenFromAndTo() throws Exception {
        DisassemblerStub disasm = makeDisassembler();
        disasm.set(1,10);
        CallFlow callFlow = makeCallFlow(disasm, 0, 6);

        List<Integer> locations = callFlow.getLocationsInterval(0, 8);

        assertEquals(Arrays.asList(0,1), locations);
    }

    private CallFlow makeCallFlow(Disassembler disassembler, int... updateLocations) {
        CallFlow callFlow = new CallFlow(disassembler);
        for (int knownLocation : updateLocations) {
            callFlow.updateCache(knownLocation);
        }
        return callFlow;
    }

    private CallFlow makeCallFlow(int... updateLocations) {
        return makeCallFlow(makeDisassembler(), updateLocations);
    }

    private CallFlow makeCallFlowStep(int... updateLocations) {
        return makeCallFlow(makeDisassemblerStep(), updateLocations);
    }

    private DisassemblerStub makeDisassembler() {
        return new DisassemblerStub(10,1,2,3,4,5,6,7,8,9,10);
    }

    private DisassemblerStub makeDisassemblerStep() {
        return new DisassemblerStub(10, 2, -1, 4, -1, 6, -1, 8, -1, 10, -1);
    }

    private void modify(CallFlow callFlow, DisassemblerStub disasm, int location, int value) {
        disasm.set(location, value);
        callFlow.updateCache(location);
    }
}
