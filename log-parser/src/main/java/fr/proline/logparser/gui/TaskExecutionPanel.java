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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import javax.swing.JPanel;

/**
 *
 * @author Karine XUE at CEA
 */
public class TaskExecutionPanel extends JPanel {

    /**
     * time stamp
     */
    private ArrayList<Long> m_xValues;
    /**
     * nomber of task
     */
    private ArrayList<Integer> m_yValues;

    /**
     * number of time stamps
     */
    private int m_size;
    /**
     * time lenght
     */
    private long m_length = 0;

    private String m_value = "";
    private int m_y0;
    private int m_height;

    /**
     *
     * @param y , the paint begin y position
     */
    public TaskExecutionPanel(int y, int height) {
        m_y0 = y;
        m_height = height;

    }

    public void setData(ArrayList<Long> xValues, ArrayList<Integer> yValues, String value) {
        this.m_xValues = xValues;
        this.m_yValues = yValues;
        this.m_size = xValues.size();
        if (m_size != 0) {
            this.m_length = m_xValues.get(m_size - 1);
        }
        if (m_length == 0) {
            m_length = 1;
        }
        m_value = value;
        //System.out.println(m_task.getTaskOrder() + " : duree " + m_length + "," + this.toString());
    }

    public void init() {
        m_xValues = null;
    }

    @Override
    public void paint(Graphics g) {
        if (m_xValues == null || m_size < 1) {
            super.paint(g);
            return;
        }
        setOpaque(true);
        super.paint(g);
        long maxLength = getWidth() - 10;
        int start, w;
        Color color;
        for (int i = 0; i < m_size; i++) {
            long predTimeStamp = (i == 0) ? 0 : m_xValues.get(i - 1);
            start = Math.round(5l + (predTimeStamp * maxLength / m_length));
            w = Math.round((m_xValues.get(i) - predTimeStamp) * maxLength / m_length);
            color = ColorPalette.pickColor((int) m_yValues.get(i));
            g.setColor(color);
            if (m_height == -1) {
                m_height = getHeight() - 2;
            }
            g.fillRect(start, m_y0, w, m_height);
        }

        FontMetrics fm = g.getFontMetrics(g.getFont());
        Rectangle2D fb = fm.getStringBounds(m_value, g);
        int px = (int) (maxLength - fb.getWidth()) / 2;
        int py = (int) fb.getHeight();
        //g.setColor(Color.white);
        //g.fillRect(px, 1, (int) fb.getWidth(), py);
        g.setColor(Color.BLACK);
        g.drawString(m_value, px, py);
        //System.out.println("(|" + start + "->" + w + " panel width=" + maxLength + " color=" + m_yValues.get(i));

    }

     public String toString() {
        String s = "" + m_xValues.size() + " ";
        for (int i = 0; i < m_xValues.size(); i++) {
            s += ",[" + m_xValues.get(i) + "," + m_yValues.get(i) + "]";
        }
        return s;
    }
    
}
