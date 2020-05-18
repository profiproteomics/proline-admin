package fr.proline.logviewer.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 *
 * @author KX257079
 */
public class ColorPalette extends JComponent {

    public static final Color[] INTENSITY_PALETTE = {
        Color.getHSBColor(0, 0, 1),//while        
        Color.getHSBColor(0.55f, 0.1f, 1.0f),//bleu-white1
        Color.getHSBColor(0.55f, 0.2f, 1.0f),//bleu-white2
        Color.getHSBColor(0.55f, 0.3f, 1.0f),//bleu-white3
        Color.getHSBColor(0.55f, 0.4f, 1.0f),//bleu-white2
        Color.getHSBColor(0.55f, 0.5f, 1.0f),//bleu-white3
        Color.getHSBColor(0.30f, 0.2f, 1.0f),//green1
        Color.getHSBColor(0.30f, 0.3f, 1.0f),//green2
        Color.getHSBColor(0.30f, 0.5f, 1.0f),//green3
        Color.getHSBColor(0.30f, 0.7f, 1.0f),//green4
        Color.getHSBColor(0.30f, 0.9f, 1.0f),//green5
        Color.getHSBColor(0.20f, 0.4f, 1.0f),//yellow1
        Color.getHSBColor(0.18f, 0.5f, 1.0f),//yellow1
        Color.getHSBColor(0.16f, 0.6f, 1.0f),//yellow2
        Color.getHSBColor(0.14f, 0.6f, 1.0f),//yellow-orange1
        Color.getHSBColor(0.12f, 0.7f, 1.0f),//yellow-orange1
        Color.getHSBColor(0.10f, 0.8f, 1.0f),//orange2
        Color.getHSBColor(0.07f, 0.7f, 1.0f),//orange3
        Color.getHSBColor(0.05f, 0.6f, 1.0f),//red1
        Color.getHSBColor(0.03f, 0.8f, 1.0f),//red2
        Color.getHSBColor(0, 1.0f, 1.0f)//red3
    };

    static final int EDGE = 18;
    private int x0, y0;

    public ColorPalette(int x, int y) {
        x0 = x;
        y0 = y;
        this.setPreferredSize(new Dimension(getWeightX(), EDGE));
    }

    private int getWeightX() {
        return EDGE * INTENSITY_PALETTE.length;
    }

    public ColorPalette(JPanel pane) {
        Rectangle bound = pane.getBounds();
        x0 = bound.x + bound.width - getWeightX();
        y0 = bound.y + 2;

    }

    @Override
    public void paintComponent(Graphics g) {
        Color color;
        int start;
        for (int i = 0; i < colorSize; i++) {
            start = x0 + i * EDGE;
            color = pickColor(i);
            g.setColor(color);
            g.fillRect(start, y0, EDGE, EDGE);
            g.setColor(Color.BLACK);
            Rectangle2D bounds = g.getFontMetrics().getStringBounds("" + i, g);
            int cx = new Double(bounds.getCenterX()).intValue();
            int cy = new Double(bounds.getCenterY()).intValue();
            g.drawString("" + i, start + (EDGE / 2 - cx), y0 + 16);
        }
    }

    public static Color pickColor(int nbTask) {
        int i = (nbTask >= colorSize) ? colorSize - 1 : nbTask;
        Color color = INTENSITY_PALETTE[i];
        return color;
    }

    final static int colorSize = INTENSITY_PALETTE.length;
}
