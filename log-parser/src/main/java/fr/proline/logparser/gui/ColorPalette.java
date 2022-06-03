/*
 * Copyright (C) 2019
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the CeCILL FREE SOFTWARE LICENSE AGREEMENT
 * ; either version 2.1 
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * CeCILL License V2.1 for more details.
 * 
 * You should have received a copy of the CeCILL License 
 * along with this program; If not, see <http://www.cecill.info/licences/Licence_CeCILL_V2.1-en.html>.
 */
package fr.proline.logparser.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 *
 * @author Karine XUE at CEA
 */
public class ColorPalette extends JPanel {

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
    final static int colorSize = INTENSITY_PALETTE.length;
    static final int EDGE = 18;
    static final int GAP = 1;

    public ColorPalette() {
        super();
        setBorder(BorderFactory.createLineBorder(Color.darkGray, 1, true));
        //this.setLayout(new BorderLayout());
        //this.add(new ColorPalette(), BorderLayout.CENTER);
        MouseAdapter dragGestureAdapter = getMouseAdapter();
        addMouseMotionListener(dragGestureAdapter);
        addMouseListener(dragGestureAdapter);

        setVisible(false);
        this.setPreferredSize(new Dimension(getWeightX(), EDGE + GAP));
        this.setSize(this.getPreferredSize());
    }

    private int getWeightX() {
        return (EDGE+6) * INTENSITY_PALETTE.length + GAP * 2;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        String legend = "Color-Nb tasks : ";
        Rectangle2D bounds = g.getFontMetrics().getStringBounds(legend, g);
        int cx = Double.valueOf(bounds.getWidth()).intValue();
        Color color;
        g.setColor(Color.BLACK);
        g.drawString(legend, GAP*2, EDGE - 2);
        int start;
        int x0 = cx;
        int y0 = 16;
        for (int i = 0; i < colorSize; i++) {
            start = GAP*2 + x0 + i * EDGE;
            color = pickColor(i);
            g.setColor(color);
            g.fillRect(start, GAP, EDGE, EDGE-1);
            g.setColor(Color.BLACK);
            bounds = g.getFontMetrics().getStringBounds("" + i, g);
            cx = Double.valueOf(bounds.getCenterX()).intValue();
            g.drawString("" + i, start + (EDGE / 2 - cx), y0);
        }
    }

    public static Color pickColor(int nbTask) {
        int i = (nbTask >= colorSize) ? colorSize - 1 : nbTask;
        Color color = INTENSITY_PALETTE[i];
        return color;
    }

    private MouseAdapter getMouseAdapter() {
        MouseAdapter dragGestureAdapter = new MouseAdapter() {
            int dX, dY;

            @Override
            public void mouseDragged(MouseEvent e) {
                Component panel = e.getComponent();

                int newX = e.getLocationOnScreen().x - dX;
                int newY = e.getLocationOnScreen().y - dY;

                Component parentComponent = panel.getParent();
                int parentX = parentComponent.getX();
                if (newX < parentX) {
                    newX = parentX;
                }
                int parentY = parentComponent.getY();
                if (newY < parentY) {
                    newY = parentY;
                }
                int parentWidth = parentComponent.getWidth();
                if (newX + panel.getWidth() > parentWidth - parentX) {
                    newX = parentWidth - parentX - panel.getWidth();
                }
                int parentHeight = parentComponent.getHeight();
                if (newY + panel.getHeight() > parentHeight - parentY) {
                    newY = parentHeight - parentY - panel.getHeight();
                }

                panel.setLocation(newX, newY);

                dX = e.getLocationOnScreen().x - panel.getX();
                dY = e.getLocationOnScreen().y - panel.getY();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                JPanel panel = (JPanel) e.getComponent();
                panel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                dX = e.getLocationOnScreen().x - panel.getX();
                dY = e.getLocationOnScreen().y - panel.getY();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                JPanel panel = (JPanel) e.getComponent();
                panel.setCursor(null);
            }
        };
        return dragGestureAdapter;
    }
}
