/**
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
 * along with this program; If not, see
 * <http://www.cecill.info/licences/Licence_CeCILL_V2.1-en.html>.
 */
package fr.proline.logparser.gui;

import fr.proline.logparser.model.LogLineReader;
import fr.proline.logparser.model.ProlineException;
import fr.proline.logparser.model.Utility;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.SwingWorker;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SwingWorker<Long, String>: Long = number of line, Sting= a task begin/end
 * info
 *
 * @author Karine XUE at CEA
 */
public class LogReaderWorker extends SwingWorker<Long, String> {

    protected static final Logger m_logger = LoggerFactory.getLogger(LogReaderWorker.class);

    File m_file;
    private Utility.DATE_FORMAT m_dateFormat;
    Scanner m_fileScanner;
    JTextPane m_taskFlowPane;
    LogLineReader m_reader;
    StringBuilder m_stringBuilder;
    LogControlPanel m_ctrlLogPanel;
    private long m_fileSize;
    private int m_loadingPercent;
    private long m_loadingLength;

    public LogReaderWorker(LogControlPanel logPanel, JTextPane taskFlowTextPane, File file, Utility.DATE_FORMAT dateFormat, LogLineReader reader) {
        super();
        FileInputStream inputStream = null;
        try {
            m_taskFlowPane = taskFlowTextPane;
            m_ctrlLogPanel = logPanel;
            m_file = file;
            m_fileSize = m_file.length();
            m_loadingLength = 0;
            m_dateFormat = dateFormat;
            String abPath = file.getAbsolutePath();
            m_logger.debug("absolute path is {}", abPath);
            m_fileScanner = new Scanner(file, StandardCharsets.UTF_8.name());
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
            m_ctrlLogPanel.setProgress(0);
            m_ctrlLogPanel.setProgressBarVisible(true);
            while (m_fileScanner.hasNextLine()) {
                String line = m_fileScanner.nextLine();
                m_loadingLength += line.length();
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
                return -1L;
            } else {
                JOptionPane.showMessageDialog(m_taskFlowPane, ex + "\n" + ex.getStackTrace()[0], "Exception", JOptionPane.ERROR_MESSAGE);
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
        m_loadingPercent = (int) Math.floorDiv(m_loadingLength * 100, m_fileSize) + 1;
        m_taskFlowPane.setText(m_stringBuilder.toString());
        m_ctrlLogPanel.setProgress(m_loadingPercent);
    }

    public void addTraceBegin(String fileName) {
        m_stringBuilder = new StringBuilder("Analyse File: " + fileName + "\n");
        m_taskFlowPane.setText(m_stringBuilder.toString());
    }

    @Override
    protected void done() {
        try {
            if (get() == -1) {
                m_reader.close();
                m_ctrlLogPanel.redo();
            } else {
                m_ctrlLogPanel.setProgressBarVisible(false);
                m_logger.debug("done, {} tasks for {}", m_reader.getTasks(), m_file.getName());
                m_ctrlLogPanel.setData(m_reader.getTasks(), m_file.getName());
                m_reader.close();
            }
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
