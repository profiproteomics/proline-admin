/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.proline.logparser.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import fr.proline.logparser.model.LogTask.LogLine;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Karine XUE at CEA
 */
public class TaskInJsonCtrl {

    protected static final Logger m_logger = LoggerFactory.getLogger(TaskInJsonCtrl.class);
    private static TaskInJsonCtrl m_instance;
    private Gson m_gson;
    LogTask m_currentTask; //in order to reuse memory space
    File m_currentTaskFile;

    public File logFileDirectory;

    public static TaskInJsonCtrl getInstance() {
        if (m_instance == null) {
            m_instance = new TaskInJsonCtrl();
        }
        return m_instance;
    }

    public TaskInJsonCtrl() {
        this.m_gson = new GsonBuilder().setPrettyPrinting().create();;
    }

    /**
     *
     * @param CortexLogFileName : String can't be null
     */
    public void initFileSystem(String CortexLogFileName) {
        logFileDirectory = new File(Utility.WORKING_DATA_DIRECTORY + File.separator + CortexLogFileName+".jsons");
        if (!logFileDirectory.isDirectory()) {
            boolean b;
            b = logFileDirectory.mkdir();
            m_logger.info("create folder {} successful ={}", logFileDirectory.getAbsolutePath(), b);
        }

    }

    private File getFile(int taskOrder) {
        return new File(logFileDirectory + File.separator + taskOrder + ".json");
    }

    /**
     * delete the old file
     *
     * @param task
     */
    public void initTaskFile(LogTask task) {
        //String taskId = task.getMessageId();
        int taskOrder = task.getTaskOrder();
        File taskFile = getFile(taskOrder);
        if (taskFile.isFile()) {//file already exist 
            taskFile.delete();
        }
    }

    public void writeTaskTrace(LogTask task, boolean isFirstTime, boolean isLastTime) {
        try {
            //String taskId = task.getMessageId();
            int taskOrder = task.getTaskOrder();
            File taskFile = getFile(taskOrder);
            FileWriter outputFile = null;
            try {
                outputFile = new FileWriter(taskFile, true);//append mode
                String jsonOutput = m_gson.toJson(task.getTrace());//regist only trace
                if (jsonOutput.equals("[]")) {//empty array
                    jsonOutput = "]";//end Array
                } else {
                    String j1 = (isFirstTime) ? jsonOutput : "," + jsonOutput.substring(1);
                    int i = j1.lastIndexOf("]");
                    jsonOutput = j1.substring(0, i);
                    if (isLastTime) {
                        jsonOutput += "]";//end Array
                    }
                }
                outputFile.write(jsonOutput);
            } catch (IOException iex) {

                throw iex;
            } finally {
                if (outputFile != null) {
                    outputFile.close();
                }
            }
        } catch (IOException i) {
            m_logger.error("IO Exception when WriteTask in File. {} {}", i.getCause(), i.getLocalizedMessage());
        }
    }

//    public LogTask getCurrentTask(int taskOrder) {
//        try {
//            m_currentTaskFile = getFile(taskOrder);
//            JsonReader reader = new JsonReader(new FileReader(m_currentTaskFile));
//            m_currentTask = m_gson.fromJson(reader, LogTask.class);
//            return m_currentTask;
//        } catch (FileNotFoundException ex) {
//            Exceptions.printStackTrace(ex);
//        }
//        return null;
//    }
    /**
     * load all traces, no used now
     * @param taskOrder
     * @return 
     */
    public ArrayList<LogLine> getCurrentTaskTrace(int taskOrder) {
        try {
            m_currentTaskFile = getFile(taskOrder);
            JsonReader reader = new JsonReader(new FileReader(m_currentTaskFile));
            ArrayList<LogLine> traceList = null;
            Type type = new TypeToken<ArrayList<LogLine>>() {
            }.getType();;
            traceList = m_gson.fromJson(reader, type);

            return traceList;
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    /**
     * load trace
     * @param taskOrder
     * @param nomber, maximum line to load
     * @return 
     */
    public ArrayList<LogLine> loadTrace(int taskOrder, int nomber) {
        try {
            int i = 0;
             m_currentTaskFile = getFile(taskOrder);
            JsonReader reader = new JsonReader(new FileReader(m_currentTaskFile));
            ArrayList<LogLine> traces = new ArrayList<LogLine>();
            reader.beginArray();
            while (reader.hasNext() && i < nomber) {
                LogLine message = m_gson.fromJson(reader, LogLine.class);
                traces.add(message);
                i++;
            }
            //reader.endArray();//has more OBJECT, endArray is at the end of the file
            reader.close();
            return traces;

        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    public File getCurrentFile() {
        return m_currentTaskFile;
    }

}
