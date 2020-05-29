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
 * Create date : 21 oct. 2019
 */
package fr.proline.logparser.gui;

import fr.proline.logparser.model.Utility.DATE_FORMAT;
import fr.proline.logparser.model.LogLineReader;
import fr.proline.logparser.model.Utility;
import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.openide.util.ImageUtilities;
import org.openide.util.NbPreferences;
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
    LogLineReader m_logReader;
    LogReaderWorker m_readWorker;
    private String m_path;
    private File m_file;
    private JFrame m_taskFlowFrame;
    private JTextPane m_taskFlowPane;
    private boolean m_isBigFile = false;
    private ColorPalette m_colorPanel;
    private JProgressBar m_progressBar;

    public LogGuiApp() {
        super("Log Analyser");
        m_fileChooser = new JFileChooser();
        m_logPanel = new LogViewControlPanel(this);
        m_dateFormat = DATE_FORMAT.SHORT;
        m_path = initParameters();//load path
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

        m_taskFlowPane = new JTextPane();
        m_taskFlowFrame = new JFrame("Log Task Flow");
        m_taskFlowFrame.getContentPane().add(new JScrollPane(m_taskFlowPane));
        m_taskFlowFrame.setSize(700, 750);
        m_taskFlowFrame.setLocation(950, 250);
        m_taskFlowFrame.setVisible(true);
        m_taskFlowFrame.setIconImage(icon);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JMenuBar jMenuBar = initMenuBar();
        setJMenuBar(jMenuBar);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(m_logPanel, BorderLayout.CENTER);
        m_colorPanel = new ColorPalette();
        m_colorPanel.setSize(m_colorPanel.getPreferredSize());
        m_colorPanel.setLocation(750, 1);
        this.getLayeredPane().add(m_colorPanel, JLayeredPane.DRAG_LAYER);
        //
        m_progressBar = new JProgressBar(0, 100);
        m_progressBar.setValue(0);
        m_progressBar.setStringPainted(true);
        m_progressBar.setSize(400, 50);
        m_progressBar.setVisible(false);
        m_progressBar.setLocation(200, 200);
        this.getLayeredPane().add(m_progressBar, JLayeredPane.PALETTE_LAYER);
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
                    m_colorPanel.setVisible(showNBTaskItem.isSelected());
                }

            });
            taskMenu.add(showTaskFlowItem);
            taskMenu.add(showNBTaskItem);
        }
        //main menuBar
        JMenuBar jMenuBar = new JMenuBar();
        jMenuBar.add(fileMenu);
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
                saveParameters(m_file.getPath());
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
                parseFile();
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

    private void parseFile() {
        m_logReader = new LogLineReader(m_file.getName(), m_dateFormat, m_isBigFile, false);
        ArrayList<File> fileList = new ArrayList();
        fileList.add(m_file);
        m_readWorker = new LogReaderWorker(m_logPanel, m_taskFlowPane, fileList, m_dateFormat, m_logReader);
        m_taskFlowFrame.setVisible(true);
        m_taskFlowFrame.requestFocus();
        m_readWorker.execute();
    }

    static String KEY_LOG_FILE_PATH = "Server_log_file_path";

    public void saveParameters(String path) {
        Preferences preferences = NbPreferences.root();

        preferences.put(KEY_LOG_FILE_PATH, path);
    }

    public String initParameters() {
        Preferences preferences = NbPreferences.root();
        return preferences.get(KEY_LOG_FILE_PATH, "");
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

    void redo() {
        if (m_dateFormat.equals(Utility.DATE_FORMAT.NORMAL)) {
            m_dateFormat = Utility.DATE_FORMAT.SHORT;
        } else {
            m_dateFormat = Utility.DATE_FORMAT.NORMAL;
        }
        parseFile();
    }

    public void setProgress(int percent) {
        m_progressBar.setValue(percent);
    }

    public void setProgressBarVisible(boolean b) {
        m_progressBar.setVisible(b);
    }

}
