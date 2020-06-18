/*
 * @cea 
 * @http://www.profiproteomics.fr
 * created date: 10 oct. 2019
 */
package fr.proline.logparser.model;

import static fr.proline.logparser.model.TaskInJsonCtrl.m_logger;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Karine XUE at CEA
 */
public class Utility {

    static SimpleDateFormat sf = new SimpleDateFormat("HH:mm:ss.SSS");
    static SimpleDateFormat m_timestampFormat = new SimpleDateFormat("HH:mm:ss.SSS - dd MMM yyyy ");
    static final DecimalFormat m_decimalFormat = new DecimalFormat("####0.000");
    public final static File WORKING_DIRECTORY = new File(".").getAbsoluteFile();
    public final static File WORKING_DATA_DIRECTORY = new File(WORKING_DIRECTORY + "/parseServerLogData");

    public static void init() {
        if (!WORKING_DATA_DIRECTORY.isDirectory()) {
            boolean b;
            b = WORKING_DATA_DIRECTORY.mkdir();
            m_logger.info("create folder {} successful ={}", WORKING_DATA_DIRECTORY.getAbsolutePath(), b);
        }
    }

    public static String formatTime(Date timestamp) {
        if (timestamp == null) {
            return "N/A";
        }
        return m_timestampFormat.format(timestamp);
    }

    /**
     * reverse of formatTime
     *
     * @param time
     * @return
     */
    public static Date parseTime(String time) {
        if (time.equals("N/A")) {
            return null;
        } else {
            try {
                return m_timestampFormat.parse(time);
            } catch (ParseException ex) {
                return null;
            }

        }
    }

    public static String formatDeltaTime(long delta) {
        if (delta == -1) {
            return "";
        }

        double deltaSeconds = ((double) delta) / 1000.0;

        return "+" + m_decimalFormat.format(deltaSeconds) + " s";
    }

    public static String formatDurationInHour(long duration) {
        int second = 1000, minute = 60000, hour = 3600000;

        String time = String.format("%d:%02d:%02d.%03d",
                duration / hour,
                (duration % hour) / minute,
                (duration % minute) / second,
                (duration % second));
        return time;
    }

    public enum DATE_FORMAT {
        NORMAL, SHORT
    };

    public static String getMemory() {
        long MEGABYTE = (1024 * 1024);
        MemoryUsage heapUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long maxMemory = heapUsage.getMax() / MEGABYTE;
        long usedMemory = heapUsage.getUsed() / MEGABYTE;
        return " : Memory Use :" + usedMemory + "M/" + maxMemory + "M";
    }

}
