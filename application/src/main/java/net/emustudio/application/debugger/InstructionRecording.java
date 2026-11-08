/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.debug.DebugCPU;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Versioned execution recordings. Buffered writes apply backpressure; readers retain only one page. */
public final class InstructionRecording {
    private static final int MAGIC = 0x454D5554, VERSION = 1;
    private static final int EVENT = 1, END = 0, MAX_REGISTERS = 256, MAX_ACCESSES = 65536;

    private InstructionRecording() {}

    public static final class Header {
        public final String processor;
        public final List<DebugCPU.Register> registers;

        public Header(String processor, List<DebugCPU.Register> registers) {
            this.processor = Objects.requireNonNull(processor);
            this.registers = List.copyOf(registers);
            if (processor.isBlank() || registers.size() > MAX_REGISTERS)
                throw new IllegalArgumentException("Invalid recording processor/register definitions");
            Set<String> names = new HashSet<>();
            for (DebugCPU.Register r : registers)
                if (r.name.isBlank() || !names.add(r.name) || r.bits < 1 || r.bits > 64)
                    throw new IllegalArgumentException("Invalid recording register definition");
        }
    }

    public static final class Writer implements AutoCloseable {
        private final DataOutputStream out;
        private long count, lastSequence;
        private boolean closed, failed;

        public Writer(Path path, Header header) throws IOException {
            this(Files.newOutputStream(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE), header);
        }

        // Stream seam lets tests exercise failures without depending on a particular filesystem.
        Writer(OutputStream stream, Header header) throws IOException {
            out = new DataOutputStream(new BufferedOutputStream(stream, 65536));
            try {
                out.writeInt(MAGIC);
                out.writeInt(VERSION);
                out.writeUTF(header.processor);
                out.writeInt(header.registers.size());
                for (DebugCPU.Register r : header.registers) {
                    out.writeUTF(r.name);
                    out.writeInt(r.bits);
                }
                out.flush();
            } catch (IOException | RuntimeException e) {
                try { out.close(); } catch (IOException closing) { e.addSuppressed(closing); }
                throw e;
            }
        }

        public synchronized void append(DebugCPU.Instruction event) throws IOException {
            if (closed || failed) throw new IOException("Recording is closed or failed");
            try {
                if (event.sequence <= lastSequence) throw new IOException("Recording sequence is not increasing");
                out.writeByte(EVENT);
                out.writeLong(event.sequence);
                out.writeInt(event.address);
                out.writeInt(event.bank);
                out.writeUTF(event.mnemonic);
                out.writeUTF(event.opcodes);
                out.writeBoolean(event.completed);
                writeRegisters(out, event.before);
                writeRegisters(out, event.after);
                if (event.accesses.size() > MAX_ACCESSES) throw new IOException("Too many accesses in instruction");
                out.writeInt(event.accesses.size());
                for (DebugCPU.Access a : event.accesses) {
                    out.writeInt(a.address);
                    out.writeInt(a.bank);
                    out.writeInt(a.value);
                    out.writeBoolean(a.write);
                }
                count++;
                lastSequence = event.sequence;
            } catch (IOException e) {
                failed = true;
                throw e;
            }
        }

        @Override
        public synchronized void close() throws IOException {
            if (closed) return;
            closed = true;
            IOException failure = null;
            try {
                if (!failed) {
                    out.writeByte(END);
                    out.writeLong(count);
                    out.writeLong(lastSequence);
                }
                out.flush();
            } catch (IOException e) { failure = e; }
            try { out.close(); }
            catch (IOException e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
            if (failure != null) throw failure;
        }
    }

    public static final class Reader implements AutoCloseable {
        private final RandomAccessFile in;
        private final Path path;
        private final long modified, length, firstEvent;
        private final Header header;
        private long count, lastSequence;
        private boolean complete, finished;
        private String status = "Recording completeness unverified until its end is read";

        public Reader(Path path) throws IOException {
            this.path = path.toAbsolutePath().normalize();
            modified = Files.getLastModifiedTime(path).toMillis();
            in = new RandomAccessFile(path.toFile(), "r");
            length = in.length();
            try {
                if (in.readInt() != MAGIC || in.readInt() != VERSION)
                    throw new IOException("Unsupported instruction recording format/version");
                String processor = in.readUTF();
                int size = readCount(in, MAX_REGISTERS, "register definitions");
                List<DebugCPU.Register> registers = new ArrayList<>();
                for (int i = 0; i < size; i++) registers.add(new DebugCPU.Register(in.readUTF(), in.readInt()));
                header = new Header(processor, registers);
                firstEvent = in.getFilePointer();
            } catch (IOException | IllegalArgumentException e) {
                try { in.close(); } catch (IOException closing) { e.addSuppressed(closing); }
                throw new IOException("Invalid or incomplete recording header", e);
            }
        }

        public static final class Cursor {
            private final Path path;
            private final long position, count, sequence, modified, length;
            private Cursor(Reader reader) throws IOException {
                path = reader.path;
                position = reader.in.getFilePointer();
                count = reader.count;
                sequence = reader.lastSequence;
                modified = reader.modified;
                length = reader.length;
            }
        }

        public Cursor cursor() throws IOException { return new Cursor(this); }

        public void seek(Cursor cursor) throws IOException {
            if (!path.equals(cursor.path) || length != cursor.length || modified != cursor.modified
                    || cursor.position < firstEvent || cursor.position > length)
                throw new IOException("Recording changed; reopen it from the first page");
            in.seek(cursor.position);
            count = cursor.count;
            lastSequence = cursor.sequence;
            finished = complete = false;
            status = "Recording completeness unverified until its end is read";
        }

        public Header header() { return header; }
        public boolean complete() { return complete; }
        public boolean finished() { return finished; }
        public String status() { return status; }

        public List<DebugCPU.Instruction> nextPage(int limit) throws IOException {
            if (limit < 1 || limit > 10000) throw new IllegalArgumentException("Page size must be 1..10000");
            List<DebugCPU.Instruction> page = new ArrayList<>();
            while (!finished) {
                try {
                    long position = in.getFilePointer();
                    int marker = in.readUnsignedByte();
                    if (marker == EVENT && page.size() == limit) {
                        in.seek(position);
                        break;
                    }
                    if (marker == END) {
                        if (in.readLong() != count || in.readLong() != lastSequence || in.read() != -1)
                            throw new IOException("Recording footer does not match its events");
                        complete = true;
                        finished = true;
                        status = "Complete recording: " + count + " instructions";
                    } else if (marker == EVENT) {
                        long sequence = in.readLong();
                        int address = in.readInt(), bank = in.readInt();
                        if (sequence <= lastSequence || bank < 0)
                            throw new IOException("Invalid recording sequence or address");
                        String mnemonic = in.readUTF(), opcodes = in.readUTF();
                        boolean completed = in.readBoolean();
                        Map<String, Long> before = readRegisters(in), after = readRegisters(in);
                        int size = readCount(in, MAX_ACCESSES, "memory accesses");
                        List<DebugCPU.Access> accesses = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            int at = in.readInt(), accessBank = in.readInt(), value = in.readInt();
                            boolean write = in.readBoolean();
                            if (accessBank < 0)
                                throw new IOException("Invalid recorded memory access");
                            accesses.add(new DebugCPU.Access(at, accessBank, value, write));
                        }
                        page.add(new DebugCPU.Instruction(sequence, address, bank, mnemonic, opcodes,
                                before, after, accesses, completed));
                        count++;
                        lastSequence = sequence;
                    } else throw new IOException("Invalid recording event marker");
                } catch (EOFException e) {
                    finished = true;
                    status = "Incomplete recording: missing footer or truncated instruction after " + count + " instructions";
                } catch (IOException e) {
                    finished = true;
                    status = "Invalid recording: " + e.getMessage();
                    throw e;
                }
            }
            return List.copyOf(page);
        }

        @Override public void close() throws IOException { in.close(); }
    }

    public static void write(Path path, Header header, List<DebugCPU.Instruction> events) throws IOException {
        try (Writer writer = new Writer(path, header)) {
            for (DebugCPU.Instruction event : events) writer.append(event);
        }
    }

    public static void exportText(Path path, Header header, List<DebugCPU.Instruction> events) throws IOException {
        try (BufferedWriter out = newTextFile(path)) {
            writeTextHeader(out, header);
            writeTextEvents(out, events);
        }
    }

    /** Export an entire saved recording using bounded pages, including its completeness status. */
    public static void exportText(Path destination, Path source) throws IOException {
        try (Reader reader = new Reader(source); BufferedWriter out = newTextFile(destination)) {
            writeTextHeader(out, reader.header());
            while (!reader.finished()) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Recording export cancelled");
                writeTextEvents(out, reader.nextPage(1024));
            }
            out.write(reader.status());
            out.newLine();
        }
    }

    private static BufferedWriter newTextFile(Path path) throws IOException {
        return Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    private static void writeTextHeader(BufferedWriter out, Header header) throws IOException {
        out.write("emuStudio instruction recording v" + VERSION + " | " + header.processor + "\n");
        for (DebugCPU.Register r : header.registers) out.write(r.name + ":" + r.bits + " ");
        out.newLine();
    }

    private static void writeTextEvents(BufferedWriter out, List<DebugCPU.Instruction> events) throws IOException {
        for (DebugCPU.Instruction event : events) {
            out.write(String.format(Locale.ROOT, "#%d | bank=%d PC=%04X | %s | %s | %s%n",
                    event.sequence, event.bank, event.address, event.opcodes, event.mnemonic,
                    event.completed ? "completed" : "failed"));
            writeTextRegisters(out, "before", event.before);
            writeTextRegisters(out, "after", event.after);
            for (DebugCPU.Access a : event.accesses)
                out.write(String.format(Locale.ROOT, "  %s bank=%d:%04X value=%02X%n",
                        a.write ? "WRITE" : "READ", a.bank, a.address, a.value));
        }
    }

    private static void writeTextRegisters(BufferedWriter out, String title, Map<String, Long> values) throws IOException {
        out.write("  " + title + ":");
        for (String name : new TreeSet<>(values.keySet()))
            out.write(" " + name + "=" + Long.toHexString(values.get(name)).toUpperCase(Locale.ROOT));
        out.newLine();
    }

    private static void writeRegisters(DataOutputStream out, Map<String, Long> values) throws IOException {
        if (values.size() > MAX_REGISTERS) throw new IOException("Too many recorded registers");
        out.writeInt(values.size());
        for (String name : new TreeSet<>(values.keySet())) {
            out.writeUTF(name);
            out.writeLong(values.get(name));
        }
    }

    private static Map<String, Long> readRegisters(DataInput in) throws IOException {
        int count = readCount(in, MAX_REGISTERS, "register values");
        Map<String, Long> values = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String name = in.readUTF();
            if (name.isBlank() || values.put(name, in.readLong()) != null)
                throw new IOException("Invalid or duplicate recorded register");
        }
        return values;
    }

    private static int readCount(DataInput in, int maximum, String what) throws IOException {
        int count = in.readInt();
        if (count < 0 || count > maximum) throw new IOException("Invalid count of " + what);
        return count;
    }
}
