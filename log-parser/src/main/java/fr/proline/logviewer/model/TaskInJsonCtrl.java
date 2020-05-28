/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.proline.logviewer.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
    public void init(String CortexLogFileName) {
        logFileDirectory = new File(Utility.WORKING_DATA_DIRECTORY + "/" + CortexLogFileName);
        if (!logFileDirectory.isDirectory()) {
            boolean b;
            b = logFileDirectory.mkdir();
            m_logger.info("create folder {} successful ={}", logFileDirectory.getAbsolutePath(), b);
        }

    }

    private File getFile(int taskOrder) {
        return new File(logFileDirectory + "/" + taskOrder + ".json");
    }

    public void WriteTask(LogTask task) {
        try {
            //String taskId = task.getMessageId();
            int taskOrder = task.getTaskOrder();
            File taskFile = getFile(taskOrder);
            FileWriter outputFile = null;
            try {
                outputFile = new FileWriter(taskFile);
                String jsonOutput = m_gson.toJson(task);
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

    public LogTask getCurrentTask(int taskOrder) {
        try {
            m_currentTaskFile = getFile(taskOrder);
            JsonReader reader = new JsonReader(new FileReader(m_currentTaskFile));
            m_currentTask = m_gson.fromJson(reader, LogTask.class);
            return m_currentTask;
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    public File getCurrentFile() {
        return m_currentTaskFile;
    }

}
