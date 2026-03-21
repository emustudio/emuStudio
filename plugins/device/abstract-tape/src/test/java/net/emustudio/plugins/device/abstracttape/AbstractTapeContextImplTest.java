/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.abstracttape;

import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class AbstractTapeContextImplTest {
    private AbstractTapeContextImpl context;
    private final AtomicReference<String> lastTitle = new AtomicReference<>();

    @Before
    public void setUp() {
        lastTitle.set(null);
        context = new AbstractTapeContextImpl(lastTitle::set);
    }

    // --- Initial state ---

    @Test
    public void testInitiallyEmpty() {
        assertTrue(context.isEmpty());
        assertEquals(0, context.getSize());
        assertEquals(0, context.getHeadPosition());
    }

    @Test
    public void testInitialReadDataReturnsEmpty() {
        assertEquals(TapeSymbol.EMPTY, context.readData());
    }

    @Test
    public void testGetDataType() {
        assertEquals(TapeSymbol.class, context.getDataType());
    }

    @Test
    public void testDefaultAcceptedTypes() {
        assertTrue(context.getAcceptedTypes().contains(TapeSymbol.Type.NUMBER));
        assertTrue(context.getAcceptedTypes().contains(TapeSymbol.Type.STRING));
    }

    @Test
    public void testDefaultEditable() {
        assertTrue(context.getEditable());
    }

    @Test
    public void testDefaultHighlightCurrentPosition() {
        assertTrue(context.highlightCurrentPosition());
    }

    @Test
    public void testDefaultLeftBounded() {
        assertFalse(context.isLeftBounded());
    }

    @Test
    public void testDefaultShowPositions() {
        assertFalse(context.getShowPositions());
    }

    // --- Accept types ---

    @Test
    public void testSetAcceptTypes() {
        context.setAcceptTypes(TapeSymbol.Type.NUMBER);
        assertTrue(context.getAcceptedTypes().contains(TapeSymbol.Type.NUMBER));
        assertFalse(context.getAcceptedTypes().contains(TapeSymbol.Type.STRING));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteUnacceptedTypeThrows() {
        context.setAcceptTypes(TapeSymbol.Type.NUMBER);
        context.writeData(new TapeSymbol("text"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetSymbolAtUnacceptedTypeThrows() {
        context.setAcceptTypes(TapeSymbol.Type.STRING);
        context.setSymbolAt(0, new TapeSymbol(42));
    }

    // --- Title ---

    @Test
    public void testSetTitle() {
        context.setTitle("My Tape");
        assertEquals("My Tape", lastTitle.get());
    }

    // --- Left bounded ---

    @Test
    public void testSetLeftBounded() {
        context.setLeftBounded(true);
        assertTrue(context.isLeftBounded());
    }

    // --- Editable ---

    @Test
    public void testSetEditable() {
        context.setEditable(false);
        assertFalse(context.getEditable());
    }

    // --- Highlight ---

    @Test
    public void testSetHighlightHeadPosition() {
        context.setHighlightHeadPosition(false);
        assertFalse(context.highlightCurrentPosition());
    }

    // --- Show positions ---

    @Test
    public void testSetShowPositions() {
        context.setShowPositions(true);
        assertTrue(context.getShowPositions());
    }

    // --- Move right ---

    @Test
    public void testMoveRight() {
        context.moveRight();
        assertEquals(1, context.getHeadPosition());
    }

    @Test
    public void testMoveRightMultipleTimes() {
        context.moveRight();
        context.moveRight();
        context.moveRight();
        assertEquals(3, context.getHeadPosition());
    }

    // --- Move left ---

    @Test
    public void testMoveLeftUnboundedFromZeroKeepsAtZero() {
        assertTrue(context.moveLeft());
        assertEquals(0, context.getHeadPosition());
    }

    @Test
    public void testMoveLeftFromPositivePosition() {
        context.moveRight();
        context.moveRight();
        assertTrue(context.moveLeft());
        assertEquals(1, context.getHeadPosition());
    }

    @Test
    public void testMoveLeftLeftBoundedFromZeroDoesNotMove() {
        context.setLeftBounded(true);
        assertFalse(context.moveLeft());
        assertEquals(0, context.getHeadPosition());
    }

    @Test
    public void testMoveLeftLeftBoundedFromPositivePosition() {
        context.setLeftBounded(true);
        context.moveRight();
        assertTrue(context.moveLeft());
        assertEquals(0, context.getHeadPosition());
    }

    @Test
    public void testMoveLeftUnboundedShiftsContent() {
        // place symbol at position 0
        context.setSymbolAt(0, new TapeSymbol(10));
        // move left from position 0 (unbounded) shifts content right
        context.moveLeft();
        // symbol formerly at 0 should now be at 1
        assertEquals(Optional.of(new TapeSymbol(10)), context.getSymbolAt(1));
        assertEquals(0, context.getHeadPosition());
    }

    // --- writeData / readData ---

    @Test
    public void testWriteAndReadData() {
        TapeSymbol symbol = new TapeSymbol(42);
        context.writeData(symbol);
        assertEquals(symbol, context.readData());
    }

    @Test
    public void testWriteDataAtDifferentPositions() {
        context.writeData(new TapeSymbol(1));
        context.moveRight();
        context.writeData(new TapeSymbol(2));

        assertEquals(new TapeSymbol(2), context.readData());

        context.moveLeft();
        assertEquals(new TapeSymbol(1), context.readData());
    }

    // --- setSymbolAt / getSymbolAt ---

    @Test
    public void testSetAndGetSymbolAt() {
        TapeSymbol symbol = new TapeSymbol("hello");
        context.setSymbolAt(0, symbol);
        assertEquals(Optional.of(symbol), context.getSymbolAt(0));
    }

    @Test
    public void testGetSymbolAtEmptyPosition() {
        assertEquals(Optional.empty(), context.getSymbolAt(5));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetSymbolAtNegativePositionThrows() {
        context.setSymbolAt(-1, new TapeSymbol(1));
    }

    @Test
    public void testSetSymbolAtOverwritesPrevious() {
        context.setSymbolAt(0, new TapeSymbol(1));
        context.setSymbolAt(0, new TapeSymbol(2));
        assertEquals(Optional.of(new TapeSymbol(2)), context.getSymbolAt(0));
        assertEquals(1, context.getSize());
    }

    // --- removeSymbolAt ---

    @Test
    public void testRemoveSymbolAt() {
        context.setSymbolAt(0, new TapeSymbol(1));
        context.removeSymbolAt(0);
        assertEquals(Optional.empty(), context.getSymbolAt(0));
        assertTrue(context.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveSymbolAtNegativePositionThrows() {
        context.removeSymbolAt(-1);
    }

    @Test
    public void testRemoveSymbolAtNonexistentPositionDoesNotThrow() {
        context.removeSymbolAt(10); // no-op, should not throw
        assertTrue(context.isEmpty());
    }

    // --- getSymbolAtIndex ---

    @Test
    public void testGetSymbolAtIndex() {
        context.setSymbolAt(0, new TapeSymbol(10));
        context.setSymbolAt(5, new TapeSymbol(50));
        context.setSymbolAt(2, new TapeSymbol(20));

        // Index 0 -> position 0 (sorted)
        Map.Entry<Integer, TapeSymbol> entry0 = context.getSymbolAtIndex(0);
        assertEquals(Integer.valueOf(0), entry0.getKey());
        assertEquals(new TapeSymbol(10), entry0.getValue());

        // Index 1 -> position 2 (sorted)
        Map.Entry<Integer, TapeSymbol> entry1 = context.getSymbolAtIndex(1);
        assertEquals(Integer.valueOf(2), entry1.getKey());
        assertEquals(new TapeSymbol(20), entry1.getValue());

        // Index 2 -> position 5 (sorted)
        Map.Entry<Integer, TapeSymbol> entry2 = context.getSymbolAtIndex(2);
        assertEquals(Integer.valueOf(5), entry2.getKey());
        assertEquals(new TapeSymbol(50), entry2.getValue());
    }

    @Test
    public void testGetSymbolAtIndexOnEmptyTapeReturnsEmptyEntry() {
        Map.Entry<Integer, TapeSymbol> entry = context.getSymbolAtIndex(0);
        assertEquals(Integer.valueOf(0), entry.getKey());
        assertEquals(TapeSymbol.EMPTY, entry.getValue());
    }

    // --- addFirst ---

    @Test
    public void testAddFirstOnUnboundedTape() {
        context.setSymbolAt(0, new TapeSymbol(1));
        context.addFirst(new TapeSymbol(0));
        // original symbol at 0 should be shifted to 1
        assertEquals(Optional.of(new TapeSymbol(0)), context.getSymbolAt(0));
        assertEquals(Optional.of(new TapeSymbol(1)), context.getSymbolAt(1));
        // head should move to 1 (was at 0, shifted)
        assertEquals(1, context.getHeadPosition());
    }

    @Test
    public void testAddFirstOnLeftBoundedTapeDoesNothing() {
        context.setLeftBounded(true);
        context.setSymbolAt(0, new TapeSymbol(1));
        context.addFirst(new TapeSymbol(0));
        // should not change since left-bounded
        assertEquals(Optional.of(new TapeSymbol(1)), context.getSymbolAt(0));
        assertEquals(1, context.getSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddFirstUnacceptedTypeThrows() {
        context.setAcceptTypes(TapeSymbol.Type.NUMBER);
        context.addFirst(new TapeSymbol("text"));
    }

    // --- addLast ---

    @Test
    public void testAddLastOnEmptyTape() {
        context.addLast(new TapeSymbol(99));
        // adds at position = current position (0)
        assertEquals(Optional.of(new TapeSymbol(99)), context.getSymbolAt(0));
        assertEquals(1, context.getSize());
    }

    @Test
    public void testAddLastOnNonEmptyTape() {
        context.setSymbolAt(0, new TapeSymbol(1));
        context.setSymbolAt(3, new TapeSymbol(3));
        context.addLast(new TapeSymbol(4));
        // should add at position 4 (lastKey + 1 = 3 + 1)
        assertEquals(Optional.of(new TapeSymbol(4)), context.getSymbolAt(4));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddLastUnacceptedTypeThrows() {
        context.setAcceptTypes(TapeSymbol.Type.STRING);
        context.addLast(new TapeSymbol(42));
    }

    // --- clear ---

    @Test
    public void testClearResetsContentAndPosition() {
        context.setSymbolAt(0, new TapeSymbol(1));
        context.setSymbolAt(1, new TapeSymbol(2));
        context.moveRight();
        context.clear();

        assertTrue(context.isEmpty());
        assertEquals(0, context.getSize());
        assertEquals(0, context.getHeadPosition());
    }

    // --- reset ---

    @Test
    public void testResetWithClearAtResetTrue() {
        context.setClearAtReset(true);
        context.setSymbolAt(0, new TapeSymbol(1));
        context.moveRight();
        context.reset();

        assertTrue(context.isEmpty());
        assertEquals(0, context.getHeadPosition());
    }

    @Test
    public void testResetWithClearAtResetFalsePreservesContent() {
        context.setClearAtReset(false);
        context.setSymbolAt(0, new TapeSymbol(1));
        context.moveRight();
        context.reset();

        assertFalse(context.isEmpty());
        assertEquals(Optional.of(new TapeSymbol(1)), context.getSymbolAt(0));
        assertEquals(0, context.getHeadPosition());
    }

    // --- getSize ---

    @Test
    public void testGetSizeAfterAddingSymbols() {
        context.setSymbolAt(0, new TapeSymbol(1));
        context.setSymbolAt(1, new TapeSymbol(2));
        assertEquals(2, context.getSize());
    }

    @Test
    public void testGetSizeAfterRemovingSymbol() {
        context.setSymbolAt(0, new TapeSymbol(1));
        context.setSymbolAt(1, new TapeSymbol(2));
        context.removeSymbolAt(0);
        assertEquals(1, context.getSize());
    }

    // --- Listener ---

    @Test
    public void testListenerNotifiedOnWriteData() {
        AtomicBoolean changed = new AtomicBoolean(false);
        context.setListener((() -> changed.set(true)));
        context.writeData(new TapeSymbol(1));
        assertTrue(changed.get());
    }

    @Test
    public void testListenerNotifiedOnMoveRight() {
        AtomicBoolean changed = new AtomicBoolean(false);
        context.setListener((() -> changed.set(true)));
        context.moveRight();
        assertTrue(changed.get());
    }

    @Test
    public void testListenerNotifiedOnMoveLeft() {
        AtomicBoolean changed = new AtomicBoolean(false);
        context.moveRight();
        context.setListener((() -> changed.set(true)));
        context.moveLeft();
        assertTrue(changed.get());
    }

    @Test
    public void testListenerNotifiedOnClear() {
        AtomicBoolean changed = new AtomicBoolean(false);
        context.setListener((() -> changed.set(true)));
        context.clear();
        assertTrue(changed.get());
    }

    @Test
    public void testListenerNotifiedOnSetSymbolAt() {
        AtomicBoolean changed = new AtomicBoolean(false);
        context.setListener((() -> changed.set(true)));
        context.setSymbolAt(0, new TapeSymbol(1));
        assertTrue(changed.get());
    }

    @Test
    public void testListenerNotifiedOnRemoveSymbolAt() {
        AtomicBoolean changed = new AtomicBoolean(false);
        context.setSymbolAt(0, new TapeSymbol(1));
        context.setListener((() -> changed.set(true)));
        context.removeSymbolAt(0);
        assertTrue(changed.get());
    }

    @Test
    public void testListenerNotifiedOnSetShowPositions() {
        AtomicBoolean changed = new AtomicBoolean(false);
        context.setListener((() -> changed.set(true)));
        context.setShowPositions(true);
        assertTrue(changed.get());
    }

    @Test
    public void testListenerNotifiedOnReset() {
        AtomicBoolean changed = new AtomicBoolean(false);
        context.setListener((() -> changed.set(true)));
        context.reset();
        assertTrue(changed.get());
    }

    @Test
    public void testListenerNotifiedOnAddFirst() {
        AtomicBoolean changed = new AtomicBoolean(false);
        context.setListener((() -> changed.set(true)));
        context.addFirst(new TapeSymbol(1));
        assertTrue(changed.get());
    }

    @Test
    public void testListenerNotifiedOnAddLast() {
        AtomicBoolean changed = new AtomicBoolean(false);
        context.setListener((() -> changed.set(true)));
        context.addLast(new TapeSymbol(1));
        assertTrue(changed.get());
    }

    @Test
    public void testNullListenerDoesNotThrow() {
        context.setListener(null);
        context.writeData(new TapeSymbol(1)); // should not throw
    }

    // --- Complex scenarios ---

    @Test
    public void testMultipleMovesAndWrites() {
        context.writeData(new TapeSymbol(0));
        context.moveRight();
        context.writeData(new TapeSymbol(1));
        context.moveRight();
        context.writeData(new TapeSymbol(2));

        assertEquals(2, context.getHeadPosition());
        assertEquals(new TapeSymbol(2), context.readData());

        context.moveLeft();
        assertEquals(new TapeSymbol(1), context.readData());

        context.moveLeft();
        assertEquals(new TapeSymbol(0), context.readData());
    }

    @Test
    public void testMoveLeftUnboundedAtZeroWithContentShiftsPositions() {
        context.setSymbolAt(0, new TapeSymbol("a"));
        context.setSymbolAt(1, new TapeSymbol("b"));
        context.moveLeft(); // at position 0, unbounded: shift content right

        assertEquals(0, context.getHeadPosition());
        assertEquals(Optional.of(new TapeSymbol("a")), context.getSymbolAt(1));
        assertEquals(Optional.of(new TapeSymbol("b")), context.getSymbolAt(2));
        assertEquals(Optional.empty(), context.getSymbolAt(0));
    }

    @Test
    public void testClearAfterMultipleOperations() {
        context.writeData(new TapeSymbol(1));
        context.moveRight();
        context.writeData(new TapeSymbol(2));
        context.moveRight();
        context.moveRight();

        context.clear();
        assertTrue(context.isEmpty());
        assertEquals(0, context.getHeadPosition());
        assertEquals(TapeSymbol.EMPTY, context.readData());
    }

    @Test
    public void testSetSymbolAtSparsePositions() {
        context.setSymbolAt(0, new TapeSymbol(0));
        context.setSymbolAt(100, new TapeSymbol(100));
        assertEquals(2, context.getSize());
        assertEquals(Optional.of(new TapeSymbol(0)), context.getSymbolAt(0));
        assertEquals(Optional.of(new TapeSymbol(100)), context.getSymbolAt(100));
        assertEquals(Optional.empty(), context.getSymbolAt(50));
    }

    @Test
    public void testWriteDataUsesCurrentPosition() {
        context.moveRight();
        context.moveRight();
        context.writeData(new TapeSymbol(5));
        assertEquals(Optional.of(new TapeSymbol(5)), context.getSymbolAt(2));
    }

    @Test
    public void testReadDataAtPositionWithNoSymbolReturnsEmpty() {
        context.moveRight();
        context.moveRight();
        assertEquals(TapeSymbol.EMPTY, context.readData());
    }

    @Test
    public void testAddFirstMultipleTimes() {
        context.addFirst(new TapeSymbol(1));
        context.addFirst(new TapeSymbol(2));
        context.addFirst(new TapeSymbol(3));
        // each addFirst shifts content right and adds at 0
        assertEquals(Optional.of(new TapeSymbol(3)), context.getSymbolAt(0));
        assertEquals(Optional.of(new TapeSymbol(2)), context.getSymbolAt(1));
        assertEquals(Optional.of(new TapeSymbol(1)), context.getSymbolAt(2));
    }

    @Test
    public void testSetClearAtReset() {
        context.setClearAtReset(false);
        context.setSymbolAt(0, new TapeSymbol(42));
        context.moveRight();
        context.moveRight();
        context.reset();
        // content preserved, position reset to 0
        assertEquals(0, context.getHeadPosition());
        assertEquals(Optional.of(new TapeSymbol(42)), context.getSymbolAt(0));
    }

    @Test
    public void testSymbolTypeStringOnly() {
        context.setAcceptTypes(TapeSymbol.Type.STRING);
        context.writeData(new TapeSymbol("ok"));
        assertEquals(new TapeSymbol("ok"), context.readData());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddFirstRejectsUnacceptedType() {
        context.setAcceptTypes(TapeSymbol.Type.NUMBER);
        context.addFirst(new TapeSymbol("nope"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddLastRejectsUnacceptedType() {
        context.setAcceptTypes(TapeSymbol.Type.NUMBER);
        context.addLast(new TapeSymbol("nope"));
    }
}

