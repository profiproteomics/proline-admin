/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.proline.logviewer.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author Karine XUE at CEA
 * 
 * For Gui user, we need to know at which size, the Cortex Log File is a big file.
 */
public class Config {

    private static Properties m_properties;
    private static String BIG_LOG_FILE_SIZE = "Cortex_Log_File_Big_Size";
    private static String ONE_TASK_TRACE_MAX_SHOW_LINE = "max_line_show_for_one_task";
    private static int DEFALT_FILE_SIZE = 4; //mega 
    private static int DEFALT_SHOW_LINE = 5000; //mega 

    private static void init() {
        if (m_properties == null) {
            try {
                m_properties = new Properties();
                File configFile = new File("log_parse.config");
                m_properties.load(new FileInputStream(configFile));
            } catch (IOException t) {
               t.printStackTrace();
            }
        }
    }

    public static long getBigFileSize() {
        init();
        int K = 1024;
        long M = 1024 * K;
        long memory = DEFALT_FILE_SIZE * M;;
        try {
            String requestedMemory = m_properties.getProperty(BIG_LOG_FILE_SIZE);
            int iMemory = Integer.parseInt(requestedMemory.trim().replaceAll("[kmgtKMGT]$", ""));
            String unit = requestedMemory.trim().replaceAll("\\d", "");
            if (unit.equals("K")) {
                memory = iMemory * K;
            } else if (unit.equals("M")) {
                memory = iMemory * M;
            } else if (unit.equals("")) {
                memory = iMemory;
            } else {
                memory = DEFALT_FILE_SIZE * M;//don't accept G, T byte
            }
        } catch (Exception e) {
            e.printStackTrace();
            memory = DEFALT_FILE_SIZE * M;
        }
        return memory;
    }

    public static int getMaxLine2Show() {
        init();
        try {
            String result = m_properties.getProperty(ONE_TASK_TRACE_MAX_SHOW_LINE);
            return Integer.parseInt(result);
        } catch (Exception e) {
            e.printStackTrace();
            return DEFALT_SHOW_LINE;
        }
    }
}
