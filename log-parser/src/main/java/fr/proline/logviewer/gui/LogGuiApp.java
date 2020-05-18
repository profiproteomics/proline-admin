/*
 * Copyright (C) 2019 VD225637
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
 * Create date : 21 oct. 2019
 */
package fr.proline.logviewer.gui;

import fr.proline.logviewer.model.Utility.DATE_FORMAT;
import fr.proline.logviewer.model.LogLineReader;
import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.openide.util.ImageUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Karine XUE at CEA
 */
public class LogGuiApp extends JFrame {

    protected static final Logger m_logger = LoggerFactory.getLogger(LogGuiApp.class);
    JFileChooser m_fileChooser;
    DATE_FORMAT m_dateFormat;
    LogViewControlPanel m_logPanel;

    private String m_path;
    private File m_file;
    private JFrame m_taskFlowFrame;
    private JTextPane m_taskFlowTextPane;
    private boolean m_isBigFile = false;
    private ColorPalette m_colorPalette;

    public LogGuiApp() {
        super("Log Analyser");
        m_fileChooser = new JFileChooser();
        m_logPanel = new LogViewControlPanel(this);
        m_dateFormat = DATE_FORMAT.SHORT;
        m_path = ("D:\\programs\\Proline-Zero-2.1.0-SNAPSHOT\\Proline-Cortex-2.1.0-SNAPSHOT\\logs\\");
        m_path = "D:\\prolineBak\\cortexLog\\";
        //m_path = "D:\\programs\\Proline-Zero-2.1.0-SNAPSHOT-PTM-02072019\\Proline-Cortex-2.1.0-SNAPSHOT\\logs\\proline_cortex_log.2020-02-05.txt";
        File defaultFile = new File(m_path);
        m_fileChooser.setSelectedFile(defaultFile);

        initComponents();
        this.setLocation(230, 2);
        pack();
        this.setVisible(true);
    }

    private void initComponents() {
        String path = "prolineLogo32x32.png";
        Image icon = ImageUtilities.loadImage(path);
        this.setIconImage(icon);

        m_taskFlowTextPane = new JTextPane();
        m_taskFlowFrame = new JFrame("Log Task Flow");
        m_taskFlowFrame.add(new JScrollPane(m_taskFlowTextPane));
        m_taskFlowFrame.setSize(700, 750);
        m_taskFlowFrame.setLocation(950, 250);
        m_taskFlowFrame.setVisible(true);
        m_taskFlowFrame.setIconImage(icon);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        m_colorPalette = new ColorPalette(m_logPanel);
        this.setGlassPane(m_colorPalette);
        JMenuBar jMenuBar = initMenuBar();
        setJMenuBar(jMenuBar);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(m_logPanel, BorderLayout.CENTER);

    }

    private JMenuBar initMenuBar() {
        JMenu fileMenu, dataFormatMenu, taskMenu;

        {//fileMenu
            fileMenu = new JMenu("File");
            JMenuItem analyseFileMenuItem = new JMenuItem("Analyse File");
            analyseFileMenuItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    analyseFileActionPerformed(evt);
                }
            });
            fileMenu.add(analyseFileMenuItem);
            JMenuItem exitMunuItem = new JMenuItem("Exit"); // NOI18N
            exitMunuItem.addActionListener((java.awt.event.ActionEvent evt) -> {
                dispose();
                System.exit(0);
            });
            fileMenu.add(exitMunuItem);
        }
        {//dataFormatMenu
            ButtonGroup group = new ButtonGroup();//one choice one time
            dataFormatMenu = new JMenu("Date Format");
            JRadioButtonMenuItem shortMonthMenuItem = new JRadioButtonMenuItem("Short Month: like (18 Sep 2019) ");
            JRadioButtonMenuItem normalMonthMenuItem = new JRadioButtonMenuItem("Normal Month: like (18 sep. 2019)");
            shortMonthMenuItem.setSelected(true);
            shortMonthMenuItem.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt
                ) {
                    m_dateFormat = DATE_FORMAT.SHORT;
                    m_logger.debug("m_dataFormat is {}", m_dateFormat);
                }
            });

            normalMonthMenuItem.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt
                ) {
                    m_dateFormat = DATE_FORMAT.NORMAL;
                    m_logger.debug("m_dataFormat is {}", m_dateFormat);
                }
            });
            dataFormatMenu.add(shortMonthMenuItem);
            dataFormatMenu.add(normalMonthMenuItem);
            group.add(shortMonthMenuItem);
            group.add(normalMonthMenuItem);
        }
        {//Task menu
            taskMenu = new JMenu("Tasks");
            JMenuItem showTaskFlowItem = new JMenuItem("Show Flow of the tasks");

            showTaskFlowItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    m_taskFlowFrame.setVisible(true);
                }

            }
            );
            JRadioButtonMenuItem showNBTaskItem = new JRadioButtonMenuItem("Show Color Palette of Parallel Tasks Numbers");
            showNBTaskItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    m_colorPalette.setVisible(showNBTaskItem.isSelected());
                }

            });
            taskMenu.add(showTaskFlowItem);
            taskMenu.add(showNBTaskItem);
        }
        //main menuBar
        JMenuBar jMenuBar = new JMenuBar();
        jMenuBar.add(fileMenu);
        jMenuBar.add(dataFormatMenu);
        jMenuBar.add(taskMenu);
        return jMenuBar;
    }

    private void analyseFileActionPerformed(ActionEvent evt) {
        LogLineReader logReader;
        int returnVal = m_fileChooser.showOpenDialog(LogGuiApp.this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                m_logPanel.clear();
                m_file = m_fileChooser.getSelectedFile();
                m_fileChooser.setSelectedFile(m_file);
                m_isBigFile = false;
                //This is where a real application would open the file.
                m_logger.debug("");//empty line
                m_logger.info("File to analyse: " + m_file.getName() + ".");
                long fileLength = m_file.length();
                long bigFileSize = Config.getBigFileSize();

                if (fileLength > bigFileSize) {
                    m_isBigFile = true;
                }
                String fileName = m_file.getName();
                logReader = new LogLineReader(m_file.getName(), m_dateFormat, m_isBigFile, false);
                LogReaderWorker readWorker = new LogReaderWorker(m_logPanel, m_taskFlowTextPane, m_file, m_dateFormat, logReader);
                m_taskFlowFrame.setVisible(true);
                m_taskFlowFrame.requestFocus();
                readWorker.execute();
                repaint();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex + "\n" + ex.getStackTrace()[0], "Exception", JOptionPane.ERROR_MESSAGE);
                StackTraceElement[] trace = ex.getStackTrace();
                String result = "";
                for (StackTraceElement el : trace) {
                    result += el.toString() + "\n";
                }
                m_logger.error(ex + "\n" + result);
            }
        } else {
            m_logger.debug("Open command cancelled by user.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                new LogGuiApp();
            }
        });
    }

    boolean isBigFile() {
        return m_isBigFile;

    }
}
