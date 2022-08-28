package net.emustudio.plugins.device.zxspectrum.display.io;

public interface OutputProvider extends IOProvider {
    OutputProvider DUMMY = new OutputProvider() {

        @Override
        public void write(int character) {
        }

        @Override
        public void reset() {
        }

        @Override
        public void close() {
        }

        @Override
        public void showGUI() {
        }
    };

    void write(int character);

    void showGUI();

}
