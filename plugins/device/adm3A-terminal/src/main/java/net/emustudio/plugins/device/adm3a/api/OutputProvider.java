package net.emustudio.plugins.device.adm3a.api;

public interface OutputProvider extends AutoCloseable {
    OutputProvider DUMMY = new OutputProvider() {

        @Override
        public void write(byte data) {
        }

        @Override
        public void reset() {
        }

        @Override
        public void close() {
        }
    };

    void reset();

    void write(byte data);
}
