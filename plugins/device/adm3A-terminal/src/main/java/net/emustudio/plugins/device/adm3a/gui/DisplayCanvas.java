package net.emustudio.plugins.device.adm3a.gui;

import net.emustudio.plugins.device.adm3a.interaction.Display;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.awt.RenderingHints.*;
import static net.emustudio.plugins.device.adm3a.Utils.loadFont;

public class DisplayCanvas extends Canvas implements AutoCloseable {
    private static final Color FOREGROUND = new Color(0, 255, 0);
    private static final Color BACKGROUND = Color.BLACK;

    private volatile DisplayFont displayFont;
    private final Timer repaintTimer;
    private final Display display;

    private final AtomicBoolean painting = new AtomicBoolean(false);
    private volatile Dimension size = new Dimension(0, 0);

    public DisplayCanvas(DisplayFont displayFont, Display display) {
        this.displayFont = Objects.requireNonNull(displayFont);
        this.display = Objects.requireNonNull(display);

        setForeground(FOREGROUND);
        setBackground(BACKGROUND);
        setFont(loadFont(displayFont));

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

    public synchronized void setDisplayFont(DisplayFont font) {
        // setting font must be atomic
        setFont(loadFont(font));
        this.displayFont = Objects.requireNonNull(font);
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

                        int lineHeight = graphics.getFontMetrics().getHeight() + displayFont.yLineHeightMultiplierOffset;
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

            Rectangle2D fontRectangle = getFont().getMaxCharBounds(graphics.getFontMetrics().getFontRenderContext());

            int x = displayFont.xCursorOffset
                + (int) (cursorPoint.x * (fontRectangle.getWidth() + displayFont.xCursorMultiplierOffset));
            int y = displayFont.yCursorOffset + (cursorPoint.y * lineHeight);

            graphics.fillRect(x, y, (int) fontRectangle.getWidth(), (int) fontRectangle.getHeight() + displayFont.yCursorExtend);
            graphics.setPaintMode();
        }
    }
}
