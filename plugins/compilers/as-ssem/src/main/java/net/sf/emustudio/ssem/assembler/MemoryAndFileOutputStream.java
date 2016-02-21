package net.sf.emustudio.ssem.assembler;

import emulib.plugins.memory.MemoryContext;
import net.jcip.annotations.NotThreadSafe;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

@NotThreadSafe
public class MemoryAndFileOutputStream extends OutputStream {
    private final FileOutputStream fileOutputStream;
    private final MemoryContext<Short> memoryContext;
    int position = 0;

    public MemoryAndFileOutputStream(String filename, MemoryContext<Short> memoryContext) throws FileNotFoundException {
        this.fileOutputStream = new FileOutputStream(filename);
        this.memoryContext = Objects.requireNonNull(memoryContext);
    }

    @Override
    public void write(int b) throws IOException {
        fileOutputStream.write(b);
        memoryContext.write(position++, (short)b);
    }

    @Override
    public void close() throws IOException {
        fileOutputStream.close();
    }
}
