package net.sf.emustudio.devices.adm3a.impl;

import java.awt.GraphicsEnvironment;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;

public class Utils {
    private static FontRenderContext DEFAULT_FRC;

    public static FontRenderContext getDefaultFrc() {
        if (DEFAULT_FRC == null) {
            AffineTransform tx;
            if (GraphicsEnvironment.isHeadless()) {
                tx = new AffineTransform();
            } else {
                tx = GraphicsEnvironment
                        .getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice()
                        .getDefaultConfiguration()
                        .getDefaultTransform();
            }
            DEFAULT_FRC = new FontRenderContext(tx, false, false);
        }
        return DEFAULT_FRC;
    }
}
