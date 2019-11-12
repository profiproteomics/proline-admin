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
 * Create date : 6 nov. 2019
 */
package fr.proline.logviewer.gui;

import fr.proline.logviewer.model.LogLineReader;
import fr.proline.logviewer.model.LogLineReader.DATE_FORMAT;
import fr.proline.logviewer.model.ProlineException;
import fr.proline.logviewer.model.TimeFormat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;
import java.util.Scanner;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.SwingWorker;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Karine XUE at CEA
 */
public class LogReaderWorker extends SwingWorker<Long, String> {

    protected static final Logger m_logger = LoggerFactory.getLogger(LogReaderWorker.class);
    File m_file;
    private LogLineReader.DATE_FORMAT m_dateFormat;
    Scanner m_fileScanner;
    JTextPane m_taskFlowTextPane;
    LogLineReader m_reader;
    StringBuilder m_stringBuilder;

    LogReaderWorker(JTextPane taskFlowTextPane, File file, DATE_FORMAT dateFormat, LogLineReader reader) {
        super();
        FileInputStream inputStream = null;
        try {
            m_taskFlowTextPane = taskFlowTextPane;
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
                if (index == 19163) {
                    String s = "debug begin";
                }
                //m_logger.debug("{}, task register {}", index);
                m_reader.registerTask(line, index);
                if (m_reader.isHasNewTrace()) {
                    publish(m_reader.getNewTrace());
                }
            }

            m_logger.info("Analyse done. {} line, Total read time is {}", index, TimeFormat.formatDeltaTime(System.currentTimeMillis() - start));

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
        addTrace(trace);
    }

    public void addTraceBegin(String fileName) {
        m_stringBuilder = new StringBuilder("Analyse File: " + fileName + "\n");
        m_taskFlowTextPane.setText(m_stringBuilder.toString());
    }

    void addTrace(List<String> trace) {
        for (String line : trace) {
            m_stringBuilder.append(line);
        }
        m_taskFlowTextPane.setText(m_stringBuilder.toString());
    }
}
