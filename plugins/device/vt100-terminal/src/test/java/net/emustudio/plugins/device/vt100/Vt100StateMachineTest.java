/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class Vt100StateMachineTest {
    private RecordingDispatcher dispatcher;
    private Vt100StateMachine sm;

    @Before
    public void setUp() {
        dispatcher = new RecordingDispatcher();
        sm = new Vt100StateMachine(dispatcher);
    }

    // ========== Ground state tests ==========

    @Test
    public void testPrintableCharactersCallPrint() {
        sm.accept('A');
        assertEquals(1, dispatcher.prints.size());
        assertEquals('A', (int) dispatcher.prints.get(0));
    }

    @Test
    public void testSpaceCallsPrint() {
        sm.accept(0x20);
        assertEquals(1, dispatcher.prints.size());
        assertEquals(0x20, (int) dispatcher.prints.get(0));
    }

    @Test
    public void testDeleteIsIgnoredInGround() {
        sm.accept(0x7F);
        assertEquals(0, dispatcher.prints.size());
        assertEquals(0, dispatcher.executes.size());
    }

    @Test
    public void testC0ControlCharactersCallExecute() {
        sm.accept(0x0D); // CR
        assertEquals(1, dispatcher.executes.size());
        assertEquals(0x0D, (int) dispatcher.executes.get(0));
    }

    @Test
    public void testBellCharacterCallsExecute() {
        sm.accept(0x07);
        assertEquals(1, dispatcher.executes.size());
        assertEquals(0x07, (int) dispatcher.executes.get(0));
    }

    @Test
    public void testBackspaceCallsExecute() {
        sm.accept(0x08);
        assertEquals(1, dispatcher.executes.size());
        assertEquals(0x08, (int) dispatcher.executes.get(0));
    }

    @Test
    public void testLineFeedCallsExecute() {
        sm.accept(0x0A);
        assertEquals(1, dispatcher.executes.size());
        assertEquals(0x0A, (int) dispatcher.executes.get(0));
    }

    // ========== ESC sequence tests ==========

    @Test
    public void testEscapeSequenceDispatchesEsc() {
        // ESC D = Index (IND)
        sm.accept(0x1B); // ESC
        sm.accept(0x44); // 'D'
        assertEquals(1, dispatcher.escDispatches.size());
        assertEquals(0x44, dispatcher.escDispatches.get(0).data);
        assertTrue(dispatcher.escDispatches.get(0).collected.isEmpty());
    }

    @Test
    public void testEscapeSequenceWithIntermediate() {
        // ESC ( B - Designate G0 Character Set
        sm.accept(0x1B); // ESC
        sm.accept(0x28); // '(' intermediate
        sm.accept(0x42); // 'B' final
        assertEquals(1, dispatcher.escDispatches.size());
        assertEquals(0x42, dispatcher.escDispatches.get(0).data);
        assertEquals(List.of(0x28), dispatcher.escDispatches.get(0).collected);
    }

    @Test
    public void testEscapeSaveCursor() {
        // ESC 7 = DECSC
        sm.accept(0x1B);
        sm.accept(0x37);
        assertEquals(1, dispatcher.escDispatches.size());
        assertEquals(0x37, dispatcher.escDispatches.get(0).data);
    }

    @Test
    public void testEscapeRestoreCursor() {
        // ESC 8 = DECRC
        sm.accept(0x1B);
        sm.accept(0x38);
        assertEquals(1, dispatcher.escDispatches.size());
        assertEquals(0x38, dispatcher.escDispatches.get(0).data);
    }

    @Test
    public void testEscapeSequenceReturnsToGround() {
        sm.accept(0x1B); // ESC
        sm.accept(0x44); // 'D' final
        sm.accept('X');  // printable in ground
        assertEquals(1, dispatcher.prints.size());
        assertEquals('X', (int) dispatcher.prints.get(0));
    }

    @Test
    public void testEscapeThenControlCharacterExecutes() {
        sm.accept(0x1B); // ESC
        sm.accept(0x0D); // CR during escape
        assertEquals(1, dispatcher.executes.size());
        assertEquals(0x0D, (int) dispatcher.executes.get(0));
    }

    // ========== CSI sequence tests ==========

    @Test
    public void testCsiSequenceWithoutParams() {
        // ESC [ H = Cursor Home
        sm.accept(0x1B);
        sm.accept(0x5B); // '['
        sm.accept(0x48); // 'H'
        assertEquals(1, dispatcher.csiDispatches.size());
        assertEquals(0x48, dispatcher.csiDispatches.get(0).data);
        assertTrue(dispatcher.csiDispatches.get(0).params.isEmpty());
    }

    @Test
    public void testCsiSequenceWithSingleParam() {
        // ESC [ 5 A = Cursor Up 5
        sm.accept(0x1B);
        sm.accept(0x5B);
        sm.accept('5');
        sm.accept(0x41); // 'A'
        assertEquals(1, dispatcher.csiDispatches.size());
        assertEquals(0x41, dispatcher.csiDispatches.get(0).data);
        assertEquals(List.of(5), dispatcher.csiDispatches.get(0).params);
    }

    @Test
    public void testCsiSequenceWithMultipleParams() {
        // ESC [ 10 ; 20 H = Cursor Position (10, 20)
        sm.accept(0x1B);
        sm.accept(0x5B);
        sm.accept('1');
        sm.accept('0');
        sm.accept(';');
        sm.accept('2');
        sm.accept('0');
        sm.accept(0x48); // 'H'
        assertEquals(1, dispatcher.csiDispatches.size());
        assertEquals(0x48, dispatcher.csiDispatches.get(0).data);
        assertEquals(List.of(10, 20), dispatcher.csiDispatches.get(0).params);
    }

    @Test
    public void testCsiSequenceReturnsToGround() {
        sm.accept(0x1B);
        sm.accept(0x5B);
        sm.accept(0x48); // final char
        sm.accept('Y');  // printable in ground
        assertEquals(1, dispatcher.prints.size());
        assertEquals('Y', (int) dispatcher.prints.get(0));
    }

    @Test
    public void testCsiSequenceWithPrivateMarker() {
        // ESC [ ? 1 h = DECCKM (set cursor keys to application)
        sm.accept(0x1B);
        sm.accept(0x5B);
        sm.accept('?');  // private marker (0x3F)
        sm.accept('1');
        sm.accept('h');  // 0x68
        assertEquals(1, dispatcher.csiDispatches.size());
        assertEquals(0x68, dispatcher.csiDispatches.get(0).data);
        assertEquals(List.of((int) '?'), dispatcher.csiDispatches.get(0).collected);
        assertEquals(List.of(1), dispatcher.csiDispatches.get(0).params);
    }

    @Test
    public void testCsiSequenceWithIntermediate() {
        sm.accept(0x1B);
        sm.accept(0x5B);
        sm.accept(0x20); // intermediate
        sm.accept(0x40); // final
        assertEquals(1, dispatcher.csiDispatches.size());
        assertEquals(List.of(0x20), dispatcher.csiDispatches.get(0).collected);
    }

    @Test
    public void testCsiIgnoreOnColon() {
        // A colon (0x3A) in CSI_ENTRY transitions to CSI_IGNORE
        sm.accept(0x1B);
        sm.accept(0x5B);
        sm.accept(0x3A); // colon -> CSI_IGNORE
        sm.accept(0x41); // final char -> back to GROUND, but no dispatch
        assertEquals(0, dispatcher.csiDispatches.size());
    }

    @Test
    public void testCsiParamIgnoreOnInvalidMarker() {
        // After entering CSI_PARAM, a 0x3C-0x3F transitions to CSI_IGNORE
        sm.accept(0x1B);
        sm.accept(0x5B);
        sm.accept('1');  // param digit -> CSI_PARAM
        sm.accept(0x3C); // '<' -> CSI_IGNORE
        sm.accept(0x41); // final -> back to GROUND
        assertEquals(0, dispatcher.csiDispatches.size());
    }

    // ========== 8-bit C1 control tests ==========

    @Test
    public void testC1CsiEntry() {
        // 0x9B is 8-bit CSI
        sm.accept(0x9B);
        sm.accept(0x48); // 'H'
        assertEquals(1, dispatcher.csiDispatches.size());
        assertEquals(0x48, dispatcher.csiDispatches.get(0).data);
    }

    @Test
    public void testC1ControlCharactersExecute() {
        // 0x80-0x8F should call execute and return to GROUND
        sm.accept(0x80);
        assertEquals(1, dispatcher.executes.size());
        assertEquals(0x80, (int) dispatcher.executes.get(0));
        assertEquals(0, dispatcher.prints.size());
    }

    @Test
    public void testC1OscString() {
        // 0x9D transitions to OSC_STRING
        sm.accept(0x9D);
        // Data in OSC string
        sm.accept('h'); // 0x68
        // 0x9C ends the string, returning to ground
        sm.accept(0x9C);
        sm.accept('A'); // should be printed in ground
        assertEquals(1, dispatcher.prints.size());
    }

    @Test
    public void testC1DcsEntry() {
        // 0x90 transitions to DCS_ENTRY
        sm.accept(0x90);
        sm.accept(0x40); // final char -> transitions to DCS_PASSTHROUGH; hook() called on entry
        assertEquals(1, dispatcher.hooks.size());
    }

    // ========== Cancel / substitute tests ==========

    @Test
    public void testCancelDuringEscapeReturnsToGround() {
        sm.accept(0x1B); // ESC
        sm.accept(0x18); // CAN
        // Should be back in ground
        sm.accept('Z');
        assertEquals(1, dispatcher.prints.size());
        assertEquals('Z', (int) dispatcher.prints.get(0));
    }

    @Test
    public void testSubstituteDuringEscapeReturnsToGround() {
        sm.accept(0x1B); // ESC
        sm.accept(0x1A); // SUB
        sm.accept('A');
        assertEquals(1, dispatcher.prints.size());
    }

    @Test
    public void testCancelDuringCsiReturnsToGround() {
        sm.accept(0x1B);
        sm.accept(0x5B);
        sm.accept('3');
        sm.accept(0x18); // CAN
        sm.accept('B');
        assertEquals(1, dispatcher.prints.size());
        assertEquals('B', (int) dispatcher.prints.get(0));
        assertEquals(0, dispatcher.csiDispatches.size());
    }

    // ========== SOS / PM / APC string tests ==========

    @Test
    public void testSosPmApmStringFromEsc() {
        // ESC X enters SOS_PM_APM_STRING state
        sm.accept(0x1B);
        sm.accept(0x58); // 'X'
        // Content in string is ignored
        sm.accept('H');
        sm.accept('i');
        // Terminate with 0x9C
        sm.accept(0x9C);
        // Back to ground
        sm.accept('Q');
        assertEquals(1, dispatcher.prints.size());
        assertEquals('Q', (int) dispatcher.prints.get(0));
    }

    @Test
    public void testSosPmApmStringFrom8bit() {
        // 0x98 enters SOS_PM_APM_STRING
        sm.accept(0x98);
        sm.accept('X');
        sm.accept(0x9C); // terminate
        sm.accept('A');
        assertEquals(1, dispatcher.prints.size());
    }

    // ========== Erroneous sequences ==========

    @Test
    public void testErroneousSequenceCsiInterrupted() {
        // CSI 3 ; 1 CSI 2 J - the second CSI should clear and restart
        sm.accept(0x1B);
        sm.accept(0x5B);
        sm.accept('3');
        sm.accept(';');
        sm.accept('1');
        // Now another CSI (via 0x9B)
        sm.accept(0x9B);
        sm.accept('2');
        sm.accept('J'); // 0x4A
        assertEquals(1, dispatcher.csiDispatches.size());
        assertEquals(0x4A, dispatcher.csiDispatches.get(0).data);
        assertEquals(List.of(2), dispatcher.csiDispatches.get(0).params);
    }

    // ========== Multiple sequences ==========

    @Test
    public void testMultiplePrintCharacters() {
        sm.accept('H');
        sm.accept('e');
        sm.accept('l');
        sm.accept('l');
        sm.accept('o');
        assertEquals(5, dispatcher.prints.size());
        assertEquals('H', (int) dispatcher.prints.get(0));
        assertEquals('o', (int) dispatcher.prints.get(4));
    }

    @Test
    public void testMixedPrintAndControl() {
        sm.accept('A');
        sm.accept(0x0D); // CR
        sm.accept('B');
        assertEquals(2, dispatcher.prints.size());
        assertEquals(1, dispatcher.executes.size());
    }

    @Test
    public void testMultipleCsiSequences() {
        // CSI 5 A (cursor up 5)
        sm.accept(0x1B);
        sm.accept(0x5B);
        sm.accept('5');
        sm.accept('A');
        // CSI 3 B (cursor down 3)
        sm.accept(0x1B);
        sm.accept(0x5B);
        sm.accept('3');
        sm.accept('B');
        assertEquals(2, dispatcher.csiDispatches.size());
        assertEquals(0x41, dispatcher.csiDispatches.get(0).data);
        assertEquals(0x42, dispatcher.csiDispatches.get(1).data);
    }

    // ========== DCS tests ==========

    @Test
    public void testDcsEntryFromEsc() {
        // ESC P enters DCS_ENTRY
        sm.accept(0x1B);
        sm.accept(0x50); // 'P' -> DCS_ENTRY
        sm.accept(0x40); // final char -> transitions to DCS_PASSTHROUGH; hook() called on entry
        assertEquals(1, dispatcher.hooks.size());
    }

    @Test
    public void testDcsWithParams() {
        sm.accept(0x1B);
        sm.accept(0x50);
        sm.accept('1');
        sm.accept(';');
        sm.accept('2');
        sm.accept(0x40); // final -> transitions to DCS_PASSTHROUGH; hook() called on entry
        assertEquals(1, dispatcher.hooks.size());
    }

    @Test
    public void testDcsIgnoreOnColon() {
        sm.accept(0x1B);
        sm.accept(0x50);
        sm.accept(0x3A); // colon -> DCS_IGNORE
        sm.accept(0x40); // should not trigger hook
        assertEquals(0, dispatcher.hooks.size());
    }

    @Test
    public void testDcsIntermediate() {
        sm.accept(0x1B);
        sm.accept(0x50);
        sm.accept(0x20); // intermediate
        sm.accept(0x40); // final -> transitions to DCS_PASSTHROUGH; hook() called on entry
        assertEquals(1, dispatcher.hooks.size());
    }

    @Test
    public void testDcsParamToIgnoreOnInvalid() {
        sm.accept(0x1B);
        sm.accept(0x50);
        sm.accept('1');  // param -> DCS_PARAM
        sm.accept(0x3C); // invalid -> DCS_IGNORE
        sm.accept(0x40); // should not trigger hook
        assertEquals(0, dispatcher.hooks.size());
    }

    // ========== OSC tests ==========

    @Test
    public void testOscStringFromEsc() {
        // ESC ] enters OSC_STRING; oscStart() called once on entry
        sm.accept(0x1B);
        sm.accept(0x5D); // ']' -> OSC_STRING; oscStart called here
        assertEquals(1, dispatcher.oscStarts.size());
        // Data in OSC_STRING is passed to the stored handler (no additional oscStart calls)
        sm.accept('2');
        assertEquals(1, dispatcher.oscStarts.size());
        // 0x9C terminates and calls oscEnd
        sm.accept(0x9C);
        assertEquals(1, dispatcher.oscEnds.size());
        // Back in ground
        sm.accept('A');
        assertEquals(1, dispatcher.prints.size());
    }

    // ========== cancel() method test ==========

    @Test
    public void testCancelResetsToGround() {
        sm.accept(0x1B); // enter escape
        sm.cancel();
        sm.accept('A');
        assertEquals(1, dispatcher.prints.size());
        assertEquals('A', (int) dispatcher.prints.get(0));
    }

    // ========== NullPointerException for null dispatcher ==========

    @Test(expected = NullPointerException.class)
    public void testNullDispatcherThrows() {
        new Vt100StateMachine(null);
    }

    // ========== CSI intermediate to ignore transition ==========

    @Test
    public void testCsiIntermediateToIgnore() {
        // If 0x30-0x3F received in CSI_INTERMEDIATE, transition to CSI_IGNORE
        sm.accept(0x1B);
        sm.accept(0x5B);
        sm.accept(0x20); // intermediate -> CSI_INTERMEDIATE
        sm.accept(0x30); // 0x30-0x3F -> CSI_IGNORE
        sm.accept(0x41); // final -> GROUND (no dispatch)
        assertEquals(0, dispatcher.csiDispatches.size());
    }

    @Test
    public void testCsiIntermediateWithFinal() {
        sm.accept(0x1B);
        sm.accept(0x5B);
        sm.accept(0x20); // intermediate
        sm.accept(0x41); // final -> dispatch
        assertEquals(1, dispatcher.csiDispatches.size());
        assertEquals(List.of(0x20), dispatcher.csiDispatches.get(0).collected);
    }

    // ========== Edge cases ==========

    @Test
    public void testExecuteInCsiParam() {
        // C0 controls in CSI_PARAM should be executed
        sm.accept(0x1B);
        sm.accept(0x5B);
        sm.accept('1');
        sm.accept(0x0D); // CR - execute
        sm.accept('A');  // final
        assertEquals(1, dispatcher.executes.size());
        assertEquals(1, dispatcher.csiDispatches.size());
    }

    @Test
    public void testExecuteInCsiIgnore() {
        sm.accept(0x1B);
        sm.accept(0x5B);
        sm.accept(0x3A); // -> CSI_IGNORE
        sm.accept(0x07); // bell - should execute
        assertEquals(1, dispatcher.executes.size());
        assertEquals(0x07, (int) dispatcher.executes.get(0));
    }

    @Test
    public void testDcsIntermediateMultiple() {
        sm.accept(0x1B);
        sm.accept(0x50);
        sm.accept(0x20); // intermediate
        sm.accept(0x21); // another intermediate
        sm.accept(0x40); // final -> transitions to DCS_PASSTHROUGH; hook() called on entry
        assertEquals(1, dispatcher.hooks.size());
    }

    @Test
    public void testDcsIntermediateToIgnore() {
        sm.accept(0x1B);
        sm.accept(0x50);
        sm.accept(0x20); // intermediate -> DCS_INTERMEDIATE
        sm.accept(0x30); // 0x30-0x3F -> DCS_IGNORE
        sm.accept(0x9C); // ST -> GROUND
        assertEquals(0, dispatcher.hooks.size());
    }

    // ========== Recording dispatcher ==========

    // ========== DCS lifecycle tests ==========

    @Test
    public void testDcsHookCalledOnceOnEntry() {
        sm.accept(0x1B);
        sm.accept(0x50);
        sm.accept(0x40); // -> DCS_PASSTHROUGH, hook called
        sm.accept(0x30); // data in passthrough (uses stored handler, no extra hook call)
        sm.accept(0x31); // more data
        assertEquals(1, dispatcher.hooks.size()); // hook called exactly once
    }

    @Test
    public void testDcsUnhookCalledOnExit() {
        sm.accept(0x1B);
        sm.accept(0x50);
        sm.accept(0x40); // -> DCS_PASSTHROUGH
        sm.accept(0x9C); // ST -> GROUND (exit from DCS_PASSTHROUGH)
        assertEquals(1, dispatcher.unhooks.size());
    }

    @Test
    public void testDcsUnhookCalledOnCancelExit() {
        sm.accept(0x1B);
        sm.accept(0x50);
        sm.accept(0x40); // -> DCS_PASSTHROUGH
        sm.accept(0x18); // CAN -> GROUND
        assertEquals(1, dispatcher.unhooks.size());
    }

    @Test
    public void testDcsUnhookCalledOnEscExit() {
        sm.accept(0x1B);
        sm.accept(0x50);
        sm.accept(0x40); // -> DCS_PASSTHROUGH
        sm.accept(0x1B); // ESC -> ESCAPE state (exits DCS_PASSTHROUGH)
        assertEquals(1, dispatcher.unhooks.size());
    }

    // ========== OSC lifecycle tests ==========

    @Test
    public void testOscStartCalledOnceOnEntry() {
        sm.accept(0x1B);
        sm.accept(0x5D); // -> OSC_STRING, oscStart called
        sm.accept('0');   // data (uses stored handler)
        sm.accept(';');   // more data
        assertEquals(1, dispatcher.oscStarts.size()); // oscStart called exactly once
    }

    @Test
    public void testOscEndCalledOnExit() {
        sm.accept(0x1B);
        sm.accept(0x5D); // -> OSC_STRING
        sm.accept(0x9C); // ST -> GROUND
        assertEquals(1, dispatcher.oscEnds.size());
    }

    @Test
    public void testOscEndCalledOnCancelExit() {
        sm.accept(0x1B);
        sm.accept(0x5D); // -> OSC_STRING
        sm.accept(0x18); // CAN -> GROUND
        assertEquals(1, dispatcher.oscEnds.size());
    }

    @Test
    public void testCancelInDcsPassthroughCallsUnhook() {
        sm.accept(0x1B);
        sm.accept(0x50);
        sm.accept(0x40); // -> DCS_PASSTHROUGH
        sm.cancel();
        assertEquals(1, dispatcher.unhooks.size());
    }

    @Test
    public void testCancelInOscStringCallsOscEnd() {
        sm.accept(0x1B);
        sm.accept(0x5D); // -> OSC_STRING
        sm.cancel();
        assertEquals(1, dispatcher.oscEnds.size());
    }

    // ========== parseParams edge cases ==========

    @Test
    public void testCsiWithLeadingSemicolon() {
        // CSI ; 5 H → params should be [0, 5]
        sm.accept(0x1B);
        sm.accept(0x5B);
        sm.accept(';');
        sm.accept('5');
        sm.accept('H');
        assertEquals(1, dispatcher.csiDispatches.size());
        assertEquals(List.of(0, 5), dispatcher.csiDispatches.get(0).params);
    }

    @Test
    public void testCsiWithConsecutiveSemicolons() {
        // CSI 1 ;; 3 H → params should be [1, 0, 3]
        sm.accept(0x1B);
        sm.accept(0x5B);
        sm.accept('1');
        sm.accept(';');
        sm.accept(';');
        sm.accept('3');
        sm.accept('H');
        assertEquals(1, dispatcher.csiDispatches.size());
        assertEquals(List.of(1, 0, 3), dispatcher.csiDispatches.get(0).params);
    }

    // ========== C1 controls call execute ==========

    @Test
    public void testC1_0x84_CallsExecute() {
        sm.accept(0x84);
        assertEquals(1, dispatcher.executes.size());
        assertEquals(0x84, (int) dispatcher.executes.get(0));
    }

    @Test
    public void testC1_0x8D_CallsExecute() {
        sm.accept(0x8D);
        assertEquals(1, dispatcher.executes.size());
        assertEquals(0x8D, (int) dispatcher.executes.get(0));
    }

    @Test
    public void testC1_0x91_0x97_CallExecute() {
        sm.accept(0x91);
        sm.accept(0x97);
        assertEquals(2, dispatcher.executes.size());
        assertEquals(0x91, (int) dispatcher.executes.get(0));
        assertEquals(0x97, (int) dispatcher.executes.get(1));
    }

    @Test
    public void testC1_0x99_CallsExecute() {
        sm.accept(0x99);
        assertEquals(1, dispatcher.executes.size());
        assertEquals(0x99, (int) dispatcher.executes.get(0));
    }

    @Test
    public void testC1_0x9A_CallsExecute() {
        sm.accept(0x9A);
        assertEquals(1, dispatcher.executes.size());
        assertEquals(0x9A, (int) dispatcher.executes.get(0));
    }

    private static class RecordingDispatcher implements Vt100StateMachine.Vt100Dispatcher {
        final List<Integer> executes = new ArrayList<>();
        final List<Integer> prints = new ArrayList<>();
        final List<EscDispatch> escDispatches = new ArrayList<>();
        final List<CsiDispatch> csiDispatches = new ArrayList<>();
        final List<HookDispatch> hooks = new ArrayList<>();
        final List<Integer> unhooks = new ArrayList<>();
        final List<Integer> oscStarts = new ArrayList<>();
        final List<Integer> oscEnds = new ArrayList<>();

        @Override
        public void execute(int data) {
            executes.add(data);
        }

        @Override
        public void print(int data) {
            prints.add(data);
        }

        @Override
        public void escDispatch(int data, List<Integer> collected) {
            escDispatches.add(new EscDispatch(data, collected));
        }

        @Override
        public void csiDispatch(int data, List<Integer> collected, List<Integer> params) {
            csiDispatches.add(new CsiDispatch(data, collected, params));
        }

        @Override
        public Consumer<Integer> hook(int data, List<Integer> collected, List<Integer> params) {
            hooks.add(new HookDispatch(data, collected, params));
            return (d) -> {};
        }

        @Override
        public void unhook(int data) {
            unhooks.add(data);
        }

        @Override
        public Consumer<Integer> oscStart(int data) {
            oscStarts.add(data);
            return (d) -> {};
        }

        @Override
        public void oscEnd(int data) {
            oscEnds.add(data);
        }
    }

    private static class EscDispatch {
        final int data;
        final List<Integer> collected;

        EscDispatch(int data, List<Integer> collected) {
            this.data = data;
            this.collected = collected;
        }
    }

    private static class CsiDispatch {
        final int data;
        final List<Integer> collected;
        final List<Integer> params;

        CsiDispatch(int data, List<Integer> collected, List<Integer> params) {
            this.data = data;
            this.collected = collected;
            this.params = params;
        }
    }

    private static class HookDispatch {
        final int data;
        final List<Integer> collected;
        final List<Integer> params;

        HookDispatch(int data, List<Integer> collected, List<Integer> params) {
            this.data = data;
            this.collected = collected;
            this.params = params;
        }
    }
}

