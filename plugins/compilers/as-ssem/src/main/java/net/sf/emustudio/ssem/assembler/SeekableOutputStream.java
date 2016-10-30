package net.sf.emustudio.ssem.assembler;

import java.io.IOException;
import java.io.OutputStream;

public abstract class SeekableOutputStream extends OutputStream {

    public abstract void seek(int position) throws IOException;
    
}
