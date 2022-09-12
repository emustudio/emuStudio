package net.emustudio.plugins.device.adm3a.gui;

import net.emustudio.plugins.device.adm3a.Utils;
import net.emustudio.plugins.device.adm3a.interaction.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class DisplayCanvas extends Canvas implements AutoCloseable {
    private final static Logger LOGGER = LoggerFactory.getLogger(DisplayCanvas.class);

    private static final Color FOREGROUND = new Color(0, 255, 0);
    private static final Color BACKGROUND = Color.BLACK;
    private static final String TERMINAL_FONT_PATH = "/net/emustudio/plugins/device/adm3a/gui/terminal.ttf";

    private final Font terminalFont;
    private final Timer repaintTimer;
    private final Display display;

    private final AtomicBoolean painting = new AtomicBoolean(false);
    private volatile Dimension size;

    public DisplayCanvas(Display display) {
        this.display = Objects.requireNonNull(display);
        this.terminalFont = loadFont();

        setForeground(FOREGROUND);
        setBackground(BACKGROUND);
        setFont(terminalFont);

        DisplayParameters displayParameters = measure();
        this.size = new Dimension(displayParameters.maxWidth, displayParameters.maxHeight);

        PaintCycle paintCycle = new PaintCycle();
        this.repaintTimer = new Timer(1000 / 60, e -> paintCycle.run()); // 60 HZ
        this.repaintTimer.setCoalesce(true);
    }

    public void start() {
        if (painting.compareAndSet(false, true)) {
            createBufferStrategy(2);
            this.repaintTimer.restart();
        }
    }


    @Override
    public Dimension getPreferredSize() {
        return this.size;
    }

    @Override
    public Dimension getMinimumSize() {
        return this.size;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        this.size = getSize();
    }

    @Override
    public void setBounds(Rectangle r) {
        super.setBounds(r);
        this.size = getSize();
    }

    private Font loadFont() {
        Font font;
        try (InputStream fin = getClass().getResourceAsStream(TERMINAL_FONT_PATH)) {
            font = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(fin)).deriveFont(Font.PLAIN, 15f);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
        } catch (Exception e) {
            LOGGER.error("Could not load custom font, using default monospaced font", e);
            font = new Font(Font.MONOSPACED, Font.PLAIN, 15);
        }
        return font;
    }

    private DisplayParameters measure() {
        Font font = getFont();
        Rectangle2D metrics = font.getStringBounds("W", Utils.getDefaultFrc());
        LineMetrics lineMetrics = font.getLineMetrics("W", Utils.getDefaultFrc());

        int charWidth = (int) metrics.getWidth();
        int charHeight = (int) lineMetrics.getHeight();

        int maxWidth = display.columns * charWidth;
        int maxHeight = display.rows * charHeight;

        return new DisplayParameters(maxWidth, maxHeight);
    }

    @Override
    public void close() {
        repaintTimer.stop();
        display.close();
        painting.set(false);
    }

    public class PaintCycle implements Runnable {
        private BufferStrategy strategy;

        @Override
        public void run() {
            strategy = getBufferStrategy();
            if (painting.get()) {
                paint();
            }
        }

        protected void paint() {
            Dimension dimension = size;
            do {
                do {
                    Graphics2D graphics = (Graphics2D) strategy.getDrawGraphics();
                    graphics.setColor(BACKGROUND);
                    graphics.fillRect(0, 0, dimension.width, dimension.height);

                    int lineHeight = graphics.getFontMetrics().getHeight();
                    graphics.setColor(FOREGROUND);
                    for (int y = 0; y < display.rows; y++) {
                        graphics.drawChars(
                            display.videoMemory,
                            y * display.columns,
                            display.columns,
                            1,
                            (y + 1) * lineHeight);
                    }
                    graphics.setColor(BACKGROUND);
                    graphics.fillRect(0, 0, 100, 20);
                    graphics.setColor(FOREGROUND);

                    paintCursor(graphics, lineHeight);
                    try {
                        graphics.dispose();
                    } catch (Exception ignored) {

                    }
                } while (strategy.contentsRestored());
                strategy.show();
            } while (strategy.contentsLost());
        }

        private void paintCursor(Graphics graphics, int lineHeight) {
            Point cursorPoint = display.getCursorPoint();

            graphics.setXORMode(BACKGROUND);
            graphics.setColor(FOREGROUND);

            Rectangle2D fontRectangle = terminalFont.getMaxCharBounds(graphics.getFontMetrics().getFontRenderContext());

            int x = 2 + (int) (cursorPoint.x * fontRectangle.getWidth());
            int y = 3 + (cursorPoint.y * lineHeight);

            graphics.fillRect(x, y, (int) fontRectangle.getWidth(), (int) fontRectangle.getHeight());
            graphics.setPaintMode();
        }
    }
}
