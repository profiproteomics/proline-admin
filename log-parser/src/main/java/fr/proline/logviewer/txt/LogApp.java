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
 * Create date : 7 nov. 2019
 */
package fr.proline.logviewer.txt;

import fr.proline.logviewer.model.LogLineReader;
import fr.proline.logviewer.model.LogLineReader.DATE_FORMAT;
import fr.proline.logviewer.model.ProlineException;
import fr.proline.logviewer.model.TimeFormat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.openide.util.Exceptions;

/**
 *
 * @author Karine XUE at CEA
 */
public class LogApp {

    LogLineReader m_reader;

    public LogApp(String fileName, LogLineReader.DATE_FORMAT dateFormat, String logTaskBeginEndFileName, String logTaskListFileName) throws IOException, ProlineException {
        TasksFlowWriter tasksFlowWriter = new TasksFlowWriter(logTaskBeginEndFileName, true);

        LogLineReader reader = new LogLineReader(tasksFlowWriter, dateFormat);
        m_reader = reader;
        long start = System.currentTimeMillis();
        InputStream inputStream = null;

        File file = new File(fileName);
        try {
            String abPath = file.getAbsolutePath();
            tasksFlowWriter.open(file.getName());
            System.out.println("absolute path is " + abPath);
            inputStream = new FileInputStream(abPath);
            Scanner fileScanner = new Scanner(inputStream, StandardCharsets.UTF_8.name());
            long index = 0;
            try {
                System.out.println("Analyse begin...");
                while (fileScanner.hasNextLine()) {
                    String line = fileScanner.nextLine();
                    index++;
//                    if (index == 502) {
//                        String s = "begin to debug";
//                    }

                    //m_logger.debug("{}, task register {}", index);
                    m_reader.registerTask(line, index);
                }
            } catch (ProlineException ex) {
                System.err.println(" task register stop at line " + index);
                throw ex;
            }
            String info = String.format("Analyse done. %d line in total, %d lines no treated. Total read time is %s", index, m_reader.getNoTreatLineCount(), TimeFormat.formatDeltaTime(System.currentTimeMillis() - start));
            System.out.println(info);
             m_reader.showNoTreatedLines();
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
           
            inputStream.close();
        }
        tasksFlowWriter.close();
        TasksJsonWriter taskListWriter = new TasksJsonWriter(logTaskListFileName);
        taskListWriter.setData(reader.getTasks(), file.getName());

    }

    /**
     * args[0] : log file to Analyse.<br> args[1] : text file which register log
     * Task Start-stop Flow.<br> args[2] : Json Text File which register all
     * info
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        int count = args.length;
        if (count == 0) {
            System.out.println("Please give the log file to Analyse");
            System.out.println("LogApp FileNameToAnalyse [dateformate(normal/short) [TaskFlowFileName] [TaskListJsonFileName]]");
            return;
        }

        String file2Anaylse = "";
        String logTaskBeginEndFileName = "";
        String logTaskListFileName = "";
        DATE_FORMAT dFormat = DATE_FORMAT.SHORT;
        String dateFormat = "";
        for (int i = 0; i < count; i++) {
            switch (i) {
                case 0:
                    file2Anaylse = args[0];
                    break;
                case 1:
                    dateFormat = args[1];
                    if (dateFormat.equalsIgnoreCase("normal")) {
                        dFormat = DATE_FORMAT.NORMAL;
                    }
                    break;
                case 2:
                    logTaskBeginEndFileName = args[2];
                    break;
                case 3:
                    logTaskListFileName = args[3];
                    break;
                default:
                    break;
            }
        }
        try {
            System.out.println(".File to Analyse: " + file2Anaylse);
            System.out.println(".. Task Start Stop registered in File : " + logTaskBeginEndFileName);
            System.out.println("... Task List json object registered in File : " + logTaskListFileName);

            LogApp mainApp = new LogApp(file2Anaylse, dFormat, logTaskBeginEndFileName, logTaskListFileName);

        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }

}
