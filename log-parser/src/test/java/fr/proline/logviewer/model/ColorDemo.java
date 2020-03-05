/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.proline.logviewer.model;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author Karine XUE at CEA
 */
public class ColorDemo extends JFrame {

    public ColorDemo() {
        super("Color Demo");
        this.setSize(680, 100);
        this.getContentPane().add(new JPanel() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                Color color;
                int start;
                for (int i = 0; i < colorSize; i++) {
                    start = 10 + i * 40;
                    g.setColor(Color.BLACK);
                    g.drawString(""+i, start+16, 18);
                    color = pickColor(i);
                    g.setColor(color);
                    g.fillRect(start, 20, 40, height);
                    
                }

            }

        });
        this.setLocation(230, 120);
       
        this.setVisible(true);
    }

    private Color pickColor(int nbTask) {
        int i = (nbTask >= colorSize) ? colorSize : nbTask;
        Color color = INTENSITY_PALETTE[i];
        return color;
    }

    final Color[] INTENSITY_PALETTE = {
        Color.getHSBColor(0, 0, 1),//while        
        Color.getHSBColor(0.55f, 0.1f, 1.0f),//bleu-white1
        Color.getHSBColor(0.55f, 0.2f, 1.0f),//bleu-white2
        Color.getHSBColor(0.55f, 0.3f, 1.0f),//bleu-white3
        Color.getHSBColor(0.3f, 0.4f, 1.0f),//green1
        Color.getHSBColor(0.3f, 0.5f, 1.0f),//green2
        Color.getHSBColor(0.18f, 0.6f, 1.0f),//yellow1
        Color.getHSBColor(0.16f, 0.7f, 1.0f),//yellow2
        Color.getHSBColor(0.11f, 0.8f, 1.0f),//yellow-orange1
        Color.getHSBColor(0.10f, 0.9f, 1.0f),//orange2
        Color.getHSBColor(0.08f, 1.0f, 1.0f),//orange3
        Color.getHSBColor(1, 1.0f, 1.0f),//red
        Color.getHSBColor(0.9f, 0.6f, 0.8f),//red->Dark2
        Color.getHSBColor(0.8f, 0.4f, 0.6f),//red->Dark3
        Color.getHSBColor(0.8f, 0.2f, 0.4f),//red->Dark3
        Color.getHSBColor(0, 1.0f, 0)//Dark
    };
    final int colorSize = INTENSITY_PALETTE.length;
    final int height = 20;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                new ColorDemo();
            }
        });
    }

}
