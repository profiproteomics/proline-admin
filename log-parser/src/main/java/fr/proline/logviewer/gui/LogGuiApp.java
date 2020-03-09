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
import fr.proline.logviewer.model.ProlineException;
import fr.proline.logviewer.model.Utility;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;
import java.util.Scanner;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
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
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import org.openide.util.Exceptions;
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
        m_colorPalette = new ColorPalette();
        this.setGlassPane(m_colorPalette);
        JMenuBar jMenuBar = initMenuBar();
        setJMenuBar(jMenuBar);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(m_logPanel, BorderLayout.CENTER);

    }//

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

    class ColorPalette extends JComponent {

        private int x0, y0;

        public ColorPalette() {
            Rectangle bound = m_logPanel.getBounds();
            x0 = bound.x + bound.width / 2;
            y0 = bound.y + 2;

        }

        @Override
        public void paintComponent(Graphics g) {
            Color color;
            int start;
            for (int i = 0; i < colorSize; i++) {
                start = x0 + i * 30;
                color = pickColor(i);
                g.setColor(color);
                g.fillRect(start, y0, 30, 18);
                g.setColor(Color.BLACK);
                g.drawString("" + i, start + 12, y0 + 16);
            }
        }

        private Color pickColor(int nbTask) {
            int i = (nbTask >= colorSize) ? colorSize : nbTask;
            Color color = TaskExecutionPanel.INTENSITY_PALETTE[i];
            return color;
        }
        final int colorSize = TaskExecutionPanel.INTENSITY_PALETTE.length;
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

    class LogReaderWorker extends SwingWorker<Long, String> {

        //SwingWorker<Long, String>: Long = number of line, Sting= a task begin/end info
        File m_file;
        private DATE_FORMAT m_dateFormat;
        Scanner m_fileScanner;
        JTextPane m_taskFlowTextPane;
        LogLineReader m_reader;
        StringBuilder m_stringBuilder;
        LogViewControlPanel m_logPanel;

        LogReaderWorker(LogViewControlPanel logPanel, JTextPane taskFlowTextPane, File file, DATE_FORMAT dateFormat, LogLineReader reader) {
            super();
            FileInputStream inputStream = null;
            try {
                m_taskFlowTextPane = taskFlowTextPane;
                m_logPanel = logPanel;
                m_file = file;
                m_dateFormat = dateFormat;
                String abPath = file.getAbsolutePath();
                m_logger.debug("absolute path is {}", abPath);
                inputStream = new FileInputStream(abPath);
                m_fileScanner = new Scanner(inputStream, StandardCharsets.UTF_8.name());
                m_reader = reader;
                m_stringBuilder = new StringBuilder();
            } catch (FileNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        @Override
        protected Long doInBackground() {
            long start = System.currentTimeMillis();
            long index = 0;
            try {
                addTraceBegin(m_file.getName());
                m_logger.debug("Analyse begin...");
                while (m_fileScanner.hasNextLine()) {
                    String line = m_fileScanner.nextLine();
                    index++;
//                if (index == 19163) {
//                    String s = "debug begin";
//                }
                    //m_logger.debug("{}, task register {}", index);
                    m_reader.registerTask(line, index);
                    if (m_reader.isHasNewTrace()) {
                        publish(m_reader.getNewTrace());
                    }
                }
                m_reader.memoryClean();
                m_logger.info("Analyse done. {} line in total, {} lines no treated. Total read time is {}", index, m_reader.getNoTreatLineCount(), Utility.formatDeltaTime(System.currentTimeMillis() - start));
                m_reader.showNoTreatedLines();
            } catch (ProlineException ex) {
                if (ex.getCause() instanceof ParseException) {
                    JOptionPane.showMessageDialog(m_taskFlowTextPane, "Please veriry Data format, configuration is " + m_dateFormat + "\n" + ex.getMessage());
                    m_logger.error("Stop by date ParseException" + "Please veriry Data format, configuration is " + m_dateFormat + "\n" + ex.getMessage());
                } else {
                    JOptionPane.showMessageDialog(m_taskFlowTextPane, ex + "\n" + ex.getStackTrace()[0], "Exception", JOptionPane.ERROR_MESSAGE);
                    StackTraceElement[] trace = ex.getStackTrace();
                    String result = "";
                    for (StackTraceElement el : trace) {
                        result += el.toString() + "\n";
                    }
                    m_logger.error(ex + "\n" + result);
                }

                m_logger.error(" ProlineException, task register stop at line {}", index);
            } catch (Exception ex) {
                m_logger.error(" Exception task register stop at line {}", index);
                ex.printStackTrace();
            }
            return index;
        }

        @Override
        protected void process(List<String> trace) {
            //treat publish data
            for (String line : trace) {
                m_stringBuilder.append(line);
            }
            m_taskFlowTextPane.setText(m_stringBuilder.toString());
        }

        public void addTraceBegin(String fileName) {
            m_stringBuilder = new StringBuilder("Analyse File: " + fileName + "\n");
            m_taskFlowTextPane.setText(m_stringBuilder.toString());
        }

        @Override
        protected void done() {
            m_logPanel.requestFocus();
            m_logPanel.setData(m_reader.getTasks(), m_file.getName());
            m_reader.close();
        }
    }
}
