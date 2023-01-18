package net.emustudio.plugins.device.vt100.api;

import net.emustudio.emulib.plugins.device.DeviceContext;
import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Consumer;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class ContextVt100Test {

    private ContextVt100 context;
    private final Capture<Consumer<Byte>> captureOnKey = Capture.newInstance();

    @Before
    public void setUp() {
        Keyboard keyboard = mock(Keyboard.class);
        this.captureOnKey.reset();
        keyboard.addOnKeyHandler(capture(captureOnKey));
        expectLastCall().once();
        replay(keyboard);
        this.context = new ContextVt100(keyboard);
    }

    @Test
    public void testNoExceptionThrownOnResetWithoutSettingDisplay() {
        context.reset();
    }

    @Test(expected = NullPointerException.class)
    public void testSetNullDisplayThrows() {
        context.setDisplay(null);
    }

    @Test
    public void testResetCallsDisplayReset() {
        Display display = mock(Display.class);
        display.reset();
        expectLastCall().once();
        replay(display);

        context.setDisplay(display);
        context.reset();
        verify(display);
    }

    @Test
    public void testWriteDataCallsDisplayWrite() {
        Display display = mock(Display.class);
        display.write((byte) 0xFF);
        expectLastCall().once();
        replay(display);

        context.setDisplay(display);
        context.writeData((byte) 0xFF);
        verify(display);
    }

    @Test
    public void testGetDataTypeIsByte() {
        assertEquals(Byte.class, context.getDataType());
    }

    @Test
    public void testReadDataWhenDeviceIsNull() {
        Keyboard keyboard = mock(Keyboard.class);
        this.captureOnKey.reset();
        keyboard.addOnKeyHandler(capture(captureOnKey));
        expectLastCall().once();
        keyboard.inputRequested(false);
        expectLastCall().once();
        replay(keyboard);

        this.context = new ContextVt100(keyboard);

        captureOnKey.getValue().accept((byte) 0xFF);
        assertEquals((byte) 0xFF, context.readData().byteValue());
        verify(keyboard);
    }

    @Test
    public void testOnKeyFromKeyboardSendsDataToDevice() {
        DeviceContext<Byte> device = mock(DeviceContext.class);
        device.writeData((byte) 0xFF);
        replay(device);

        context.setExternalDevice(device);
        captureOnKey.getValue().accept((byte) 0xFF);
        verify(device);
    }

}
