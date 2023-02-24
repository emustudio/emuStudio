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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AbstractTapeContextImpl implements AbstractTapeContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTapeContextImpl.class);
    private final AtomicReference<Writer> symbolLog = new AtomicReference<>();
    private final Map<Integer, TapeSymbol> content = new HashMap<>();
    private final Set<TapeSymbol.Type> acceptedTypes = new HashSet<>(Set.of(
            TapeSymbol.Type.NUMBER, TapeSymbol.Type.STRING
    ));

    private final ReadWriteLock rwl = new ReentrantReadWriteLock();

    private final AbstractTape tape;

    private int position = 0;
    private boolean leftBounded;
    private boolean editable;
    private boolean highlightCurrentPosition;
    private boolean clearAtReset = true;
    private boolean displayRowNumbers = false;
    private TapeListener listener;

    AbstractTapeContextImpl(AbstractTape tape) {
        this.tape = Objects.requireNonNull(tape);
        leftBounded = false;
        editable = true;
        highlightCurrentPosition = true;
    }

    @Override
    public void setAcceptTypes(TapeSymbol.Type... types) {
        writeSynchronized(() -> {
            acceptedTypes.clear();
            acceptedTypes.addAll(Arrays.asList(types));
        });
    }

    @Override
    public Set<TapeSymbol.Type> getAcceptedTypes() {
        rwl.readLock().lock();
        try {
            return Collections.unmodifiableSet(acceptedTypes);
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public void setTitle(String title) {
        tape.setGUITitle(title);
    }

    @Override
    public boolean getShowPositions() {
        return displayRowNumbers;
    }

    @Override
    public void setShowPositions(boolean showPositions) {
        this.displayRowNumbers = showPositions;
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
        if (clearAtReset) {
            clear();
        } else {
            writeSynchronized(() -> position = 0);
            fireChange();
        }
    }

    @Override
    public boolean isLeftBounded() {
        return leftBounded;
    }

    @Override
    public void setLeftBounded(boolean bounded) {
        this.leftBounded = bounded;
    }

    @Override
    public boolean moveLeft() {
        AtomicBoolean moved = new AtomicBoolean(false);
        writeSynchronized(() -> {
            if (position > 0) {
                position--;
                moved.set(true);
            } else if (!leftBounded) {
                position = 0;
                moved.set(true);
            }
        });
        fireChange();
        return moved.get();
    }

    @Override
    public void moveRight() {
        writeSynchronized(() -> position++);
        fireChange();
    }

    /**
     * Adds symbol to the beginning of this tape.
     *
     * @param symbol tape symbol
     * @throws IllegalArgumentException if the symbol type is not among accepted ones
     */
    public void addFirst(TapeSymbol symbol) {
        if (leftBounded) {
            return;
        }
        writeSynchronized(() -> {
            if (!acceptedTypes.contains(symbol.type)) {
                throw new IllegalArgumentException("Tape symbol type is not accepted");
            }
            incrementContentPositions();
            content.put(0, symbol);
            logSymbol(0, symbol);
            position++;
        });
        fireChange();
    }

    /**
     * Adds symbol at the end of this tape.
     * The "end" is computed as the highest position + 1.
     *
     * @param symbol tape symbol
     * @throws IllegalArgumentException if the symbol type is not among accepted ones
     */
    public void addLast(TapeSymbol symbol) {
        writeSynchronized(() -> {
            if (!acceptedTypes.contains(symbol.type)) {
                throw new IllegalArgumentException("Tape symbol type is not accepted");
            }
            int index = position;
            if (!content.isEmpty()) {
                index = Collections.max(content.keySet()) + 1;
            }
            content.put(index, symbol);
            logSymbol(index, symbol);
        });
        fireChange();
    }

    @Override
    public void removeSymbolAt(int position) {
        writeSynchronized(() -> {
            content.remove(position);
            logSymbol(position, TapeSymbol.EMPTY);
            if (this.position >= position) {
                this.position = (position > 0) ? position - 1 : 0;
            }
        });
        fireChange();
    }

    public boolean getEditable() {
        return editable;
    }

    @Override
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    @Override
    public List<Integer> getNonEmptyPositions() {
        return List.copyOf(content.keySet());
    }


    @Override
    public Optional<TapeSymbol> getSymbolAt(int position) {
        rwl.readLock().lock();
        try {
            return Optional.ofNullable(content.get(position));
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public void setSymbolAt(int position, TapeSymbol symbol) {
        if (position < 0) {
            return;
        }
        writeSynchronized(() -> {
            if (!acceptedTypes.contains(symbol.type)) {
                throw new IllegalArgumentException("Tape symbol type is not accepted");
            }
            content.put(position, symbol);
            logSymbol(position, symbol);
        });
        fireChange();
    }

    @Override
    public void setHighlightHeadPosition(boolean highlight) {
        highlightCurrentPosition = highlight;
    }

    @Override
    public void setClearAtReset(boolean clear) {
        this.clearAtReset = clear;
    }

    public boolean highlightCurrentPosition() {
        return highlightCurrentPosition;
    }

    @Override
    public int getSize() {
        rwl.readLock().lock();
        try {
            return content.size();
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public int getHeadPosition() {
        rwl.readLock().lock();
        try {
            return position;
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        rwl.readLock().lock();
        try {
            return content.isEmpty();
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public TapeSymbol readData() {
        rwl.readLock().lock();
        try {
            return content.getOrDefault(position, TapeSymbol.EMPTY);
        } finally {
            rwl.readLock().unlock();
        }
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
     *
     * @param logSymbols whether to log symbols in a file
     */
    void setLogSymbols(boolean logSymbols) {
        // should be called in a synchronized context

        if (!logSymbols) {
            Writer w = symbolLog.getAndSet(null);
            if (w != null) {
                LOGGER.info("Stopping logging symbols changes");
                try {
                    w.close();
                } catch (IOException e) {
                    LOGGER.error("Could not close the symbol log", e);
                }
            }
        } else {
            String fileName = createValidFileName(tape.getTitle().trim()) + ".out";
            File file = new File(fileName + ".out");
            LOGGER.info("Starting logging symbols changes to a file:" + fileName);
            try {
                Writer w = new FileWriter(file);
                if (!symbolLog.compareAndSet(null, w)) {
                    w.close();
                }
            } catch (IOException e) {
                LOGGER.error("Could not create the symbol log file", e);
            }
        }
    }

    public void setListener(TapeListener listener) {
        this.listener = listener;
    }

    private void writeSynchronized(Runnable r) {
        rwl.writeLock().lock();
        try {
            r.run();
        } finally {
            rwl.writeLock().unlock();
        }
    }

    private void fireChange() {
        if (listener != null) {
            listener.tapeChanged();
        }
    }

    private void logSymbol(int position, TapeSymbol symbol) {
        Writer w = symbolLog.get();
        if (w != null) {
            try {
                w.write(position + " " + symbol + "\n");
                w.flush();
            } catch (IOException e) {
                LOGGER.error("Could not write a symbol to symbol log", e);
            }
        }
    }

    private String createValidFileName(String str) {
        return str.trim().toLowerCase().replaceAll("[*.#%&\\s+!~/?<>,|{}\\[\\]\\\\\"'`=]", "_");
    }

    // should be called in a synchronized context
    private void incrementContentPositions() {
        Map<Integer, TapeSymbol> newContent = new HashMap<>();
        for (int position : content.keySet()) {
            newContent.put(position + 1, content.get(position));
        }
        content.clear();
        content.putAll(newContent);
    }

    public interface TapeListener extends EventListener {

        void tapeChanged();
    }
}
