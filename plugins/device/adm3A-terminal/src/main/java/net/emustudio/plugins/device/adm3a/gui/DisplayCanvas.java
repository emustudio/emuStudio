package net.emustudio.plugins.device.adm3a.gui;

import net.emustudio.plugins.device.adm3a.interaction.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.awt.RenderingHints.*;

public class DisplayCanvas extends Canvas implements AutoCloseable {
    private final static Logger LOGGER = LoggerFactory.getLogger(DisplayCanvas.class);

    private static final Color FOREGROUND = new Color(0, 255, 0);
    private static final Color BACKGROUND = Color.BLACK;
    private static final String TERMINAL_FONT_PATH = "/net/emustudio/plugins/device/adm3a/gui/adm-3a.ttf";
    private static final int FONT_SIZE = 12;

    private final Font terminalFont;
    private final Timer repaintTimer;
    private final Display display;

    private final AtomicBoolean painting = new AtomicBoolean(false);
    private volatile Dimension size = new Dimension(0, 0);

    public DisplayCanvas(Display display) {
        this.display = Objects.requireNonNull(display);
        this.terminalFont = loadFont();

        setForeground(FOREGROUND);
        setBackground(BACKGROUND);
        setFont(terminalFont);

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
        Map<TextAttribute, Object> attrs = new HashMap<>();
        attrs.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);

        try (InputStream fin = getClass().getResourceAsStream(TERMINAL_FONT_PATH)) {
            Font font = Font
                .createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(fin))
                .deriveFont(Font.PLAIN, FONT_SIZE)
                .deriveFont(attrs);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (Exception e) {
            LOGGER.error("Could not load custom font, using default monospaced font", e);
            return new Font(Font.MONOSPACED, Font.PLAIN, FONT_SIZE);
        }
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
            try {
                do {
                    do {
                        Graphics2D graphics = (Graphics2D) strategy.getDrawGraphics();
                        graphics.setColor(BACKGROUND);
                        graphics.fillRect(0, 0, dimension.width, dimension.height);

                        int lineHeight = graphics.getFontMetrics().getHeight() + 5;
                        graphics.setColor(FOREGROUND);
                        graphics.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
                        graphics.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON);
                        graphics.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
                        graphics.setRenderingHint(KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_QUALITY);
                        graphics.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);
                        graphics.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
                        graphics.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_NORMALIZE);
                        for (int y = 0; y < display.rows; y++) {
                            graphics.drawChars(
                                display.videoMemory,
                                y * display.columns,
                                display.columns,
                                1,
                                (y + 1) * lineHeight);
                        }
                        paintCursor(graphics, lineHeight);
                        graphics.dispose();
                    } while (strategy.contentsRestored());
                    strategy.show();
                } while (strategy.contentsLost());
            } catch (Exception ignored) {
            }
        }

        private void paintCursor(Graphics graphics, int lineHeight) {
            Point cursorPoint = display.getCursorPoint();

            graphics.setXORMode(BACKGROUND);
            graphics.setColor(FOREGROUND);

            Rectangle2D fontRectangle = terminalFont.getMaxCharBounds(graphics.getFontMetrics().getFontRenderContext());

            int x = 2 + (int) (cursorPoint.x * (fontRectangle.getWidth() + 0.3));
            int y = 3 + (cursorPoint.y * lineHeight);

            graphics.fillRect(x, y, (int) fontRectangle.getWidth(), (int) fontRectangle.getHeight() + 5);
            graphics.setPaintMode();
        }
    }
}
