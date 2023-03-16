/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.device.abstracttape;

import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ThreadSafe
public class AbstractTapeContextImpl implements AbstractTapeContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTapeContextImpl.class);

    private final AtomicReference<Set<TapeSymbol.Type>> acceptedTypes = new AtomicReference<>(Set.of(
            TapeSymbol.Type.NUMBER, TapeSymbol.Type.STRING
    ));
    private final Consumer<String> titleSetter;
    private final AtomicReference<String> title = new AtomicReference<>("abstract-tape");

    private final AtomicBoolean leftBounded = new AtomicBoolean();
    private final AtomicBoolean editable = new AtomicBoolean(true);
    private final AtomicBoolean highlightCurrentPosition = new AtomicBoolean(true);
    private final AtomicBoolean clearAtReset = new AtomicBoolean(true);
    private final AtomicBoolean displayRowNumbers = new AtomicBoolean();
    private final AtomicReference<TapeListener> listener = new AtomicReference<>();
    private final AtomicReference<Writer> symbolWriter = new AtomicReference<>();

    private final ReadWriteLock rwl = new ReentrantReadWriteLock();
    @GuardedBy("rwl")
    private final SortedMap<Integer, TapeSymbol> content = new TreeMap<>();
    @GuardedBy("rwl")
    private int position = 0;

    @FunctionalInterface
    public interface TapeListener {

        void tapeChanged();
    }

    AbstractTapeContextImpl(Consumer<String> titleSetter) {
        this.titleSetter = Objects.requireNonNull(titleSetter);
    }

    @Override
    public void setAcceptTypes(TapeSymbol.Type... types) {
        acceptedTypes.set(Set.of(types));
    }

    @Override
    public Set<TapeSymbol.Type> getAcceptedTypes() {
        return acceptedTypes.get();
    }

    @Override
    public void setTitle(String title) {
        titleSetter.accept(title);
        this.title.set(title);
    }

    @Override
    public boolean getShowPositions() {
        return displayRowNumbers.get();
    }

    @Override
    public void setShowPositions(boolean showPositions) {
        displayRowNumbers.set(showPositions);
        fireChange();
    }

    /**
     * Clears tape and set head position to 0
     */
    @Override
    public void clear() {
        writeSynchronized(() -> {
            content.clear();
            position = 0;
        });
        fireChange();
    }

    void reset() {
        if (clearAtReset.get()) {
            clear();
        } else {
            writeSynchronized(() -> position = 0);
            fireChange();
        }
    }

    @Override
    public boolean isLeftBounded() {
        return leftBounded.get();
    }

    @Override
    public void setLeftBounded(boolean bounded) {
        this.leftBounded.set(bounded);
    }

    @Override
    public boolean moveLeft() {
        AtomicBoolean moved = new AtomicBoolean(false);
        writeSynchronized(() -> {
            boolean tmpLeftBounded = leftBounded.get();
            if (position > 0) {
                position--;
                moved.set(true);
            } else if (!tmpLeftBounded) {
                Map<Integer, TapeSymbol> newContent = incrementContentPositions();
                content.clear();
                content.putAll(newContent);
                position = 0;
                moved.set(true);
            }
        });
        if (moved.get()) {
            fireChange();
            return true;
        }
        return false;
    }

    @Override
    public void moveRight() {
        writeSynchronized(() -> position++);
        fireChange();
    }

    /**
     * Adds symbol to the beginning of this tape.
     * Head position is preserved on the symbol before adding.
     *
     * @param symbol tape symbol
     * @throws IllegalArgumentException if the symbol type is not among accepted ones
     */
    public void addFirst(TapeSymbol symbol) {
        if (!acceptedTypes.get().contains(symbol.type)) {
            throw new IllegalArgumentException("Tape symbol type is not accepted");
        }
        writeSynchronized(() -> {
            if (!leftBounded.get()) {
                Map<Integer, TapeSymbol> newContent = incrementContentPositions();
                content.clear();
                content.putAll(newContent);
                content.put(0, symbol);
                position++;
                logSymbol(0, symbol);
            }
        });
        fireChange();
    }

    /**
     * Adds symbol at the end of this tape.
     * The "end" is computed as the highest position + 1.
     * Head position is preserved on the symbol before adding.
     *
     * @param symbol tape symbol
     * @throws IllegalArgumentException if the symbol type is not among accepted ones
     */
    public void addLast(TapeSymbol symbol) {
        if (!acceptedTypes.get().contains(symbol.type)) {
            throw new IllegalArgumentException("Tape symbol type is not accepted");
        }
        writeSynchronized(() -> {
            int index = position;
            if (!content.isEmpty()) {
                index = content.lastKey() + 1;
            }
            content.put(index, symbol);
            logSymbol(index, symbol);
        });
        fireChange();
    }

    @Override
    public void removeSymbolAt(int position) {
        if (position < 0) {
            throw new IllegalArgumentException("Position must be >= 0. Was: " + position);
        }
        writeSynchronized(() -> {
            content.remove(position);
            logSymbol(position, TapeSymbol.EMPTY);
        });
        fireChange();
    }

    public boolean getEditable() {
        return editable.get();
    }

    @Override
    public void setEditable(boolean editable) {
        this.editable.set(editable);
    }

    public Map.Entry<Integer, TapeSymbol> getSymbolAtIndex(int index) {
        return readSynchronized(() -> {
            Iterator<Map.Entry<Integer, TapeSymbol>> it = content.entrySet().iterator();
            int i = 0;
            Map.Entry<Integer, TapeSymbol> symbol = Map.entry(0, TapeSymbol.EMPTY);
            while (i++ <= index && it.hasNext()) {
                symbol = it.next();
            }
            return symbol;
        });
    }

    @Override
    public Optional<TapeSymbol> getSymbolAt(int position) {
        return readSynchronized(() -> Optional.ofNullable(content.get(position)));
    }

    @Override
    public void setSymbolAt(int position, TapeSymbol symbol) {
        if (position < 0) {
            throw new IllegalArgumentException("Position must be >= 0. Was: " + position);
        }
        if (!acceptedTypes.get().contains(symbol.type)) {
            throw new IllegalArgumentException("Tape symbol type is not accepted");
        }
        writeSynchronized(() -> {
            content.put(position, symbol);
            logSymbol(position, symbol);
        });
        fireChange();
    }

    @Override
    public void setHighlightHeadPosition(boolean highlight) {
        highlightCurrentPosition.set(highlight);
    }

    @Override
    public void setClearAtReset(boolean clear) {
        this.clearAtReset.set(clear);
    }

    public boolean highlightCurrentPosition() {
        return highlightCurrentPosition.get();
    }

    @Override
    public int getSize() {
        return readSynchronized(content::size);
    }

    @Override
    public int getHeadPosition() {
        return readSynchronized(() -> position);
    }

    @Override
    public boolean isEmpty() {
        return readSynchronized(content::isEmpty);
    }

    @Override
    public TapeSymbol readData() {
        return readSynchronized(() -> content.getOrDefault(position, TapeSymbol.EMPTY));
    }

    @Override
    public void writeData(TapeSymbol symbol) {
        setSymbolAt(position, symbol);
    }

    @Override
    public Class<TapeSymbol> getDataType() {
        return TapeSymbol.class;
    }

    /**
     * Set if changed symbols should be logged in a file.
     * <p>
     * The log file name will be derived from a tape title: [tape-title].out
     */
    public void startLoggingSymbols() {
        // should be called in a synchronized context
        String tmpTitle = title.get();
        String fileName = createValidFileName(tmpTitle.trim()) + ".out";
        File file = new File(fileName);
        LOGGER.info("Starting logging symbols changes to a file:" + fileName);
        try {
            Writer w = new FileWriter(file);
            Writer old = symbolWriter.getAndSet(w);
            if (old != null) {
                old.close();
            }
        } catch (IOException e) {
            LOGGER.error("Could not create the symbol log file", e);
        }
    }

    public void stopLoggingSymbols() {
        Writer w = symbolWriter.getAndSet(null);
        if (w != null) {
            LOGGER.info("Stopping logging symbols changes");
            try {
                w.close();
            } catch (IOException e) {
                LOGGER.error("Could not close the symbol log", e);
            }
        }
    }

    public void setListener(TapeListener listener) {
        this.listener.set(listener);
    }


    private void fireChange() {
        TapeListener tmp = listener.get();
        if (tmp != null) {
            tmp.tapeChanged();
        }
    }

    private void logSymbol(int position, TapeSymbol symbol) {
        Writer tmp = symbolWriter.get();
        if (tmp != null) {
            try {
                tmp.write(position + " " + symbol + "\n");
                tmp.flush();
            } catch (IOException e) {
                LOGGER.error("Could not write a symbol to symbol log", e);
            }
        }
    }

    private String createValidFileName(String str) {
        return str.trim().toLowerCase().replaceAll("[*.#%&\\s+!~/?<>,|{}\\[\\]\\\\\"'`=]", "_");
    }

    private Map<Integer, TapeSymbol> incrementContentPositions() {
        SortedMap<Integer, TapeSymbol> newContent = new TreeMap<>();
        for (int position : content.keySet()) {
            newContent.put(position + 1, content.get(position));
        }
        return newContent;
    }

    private <T> T readSynchronized(Supplier<T> s) {
        rwl.writeLock().lock();
        try {
            return s.get();
        } finally {
            rwl.writeLock().unlock();
        }
    }

    private void writeSynchronized(Runnable r) {
        rwl.writeLock().lock();
        try {
            r.run();
        } finally {
            rwl.writeLock().unlock();
        }
    }
}
