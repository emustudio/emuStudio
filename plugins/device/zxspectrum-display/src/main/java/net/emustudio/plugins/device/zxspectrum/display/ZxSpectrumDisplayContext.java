package net.emustudio.plugins.device.zxspectrum.display;


import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.device.zxspectrum.display.io.IOProvider;
import net.emustudio.plugins.device.zxspectrum.display.io.OutputProvider;
import net.jcip.annotations.ThreadSafe;

import java.io.IOException;
import java.util.Objects;

@ThreadSafe
class ZxSpectrumDisplayContext implements DeviceContext<Byte>, IOProvider {
    private volatile OutputProvider outputProvider = OutputProvider.DUMMY;

    void setOutputProvider(OutputProvider outputProvider) {
        this.outputProvider = Objects.requireNonNull(outputProvider);
    }

    @Override
    public Byte readData() {
        return 0;
    }

    @Override
    public void writeData(Byte data) {
        OutputProvider tmpOutputProvider = outputProvider;
        if (tmpOutputProvider != null) {
            tmpOutputProvider.write(data & 0xFF);
        }
    }

    @Override
    public Class<Byte> getDataType() {
        return Byte.class;
    }

    @Override
    public void reset() {
        OutputProvider tmpOutputProvider = outputProvider;
        if (tmpOutputProvider != null) {
            tmpOutputProvider.reset();
        }
    }

    @Override
    public void close() throws IOException {
        OutputProvider tmpOutputProvider = outputProvider;
        if (tmpOutputProvider != null) {
            tmpOutputProvider.close();
        }
    }

    void showGUI() {
        OutputProvider tmpOutputProvider = outputProvider;
        if (tmpOutputProvider != null) {
            tmpOutputProvider.showGUI();
        }
    }
}
