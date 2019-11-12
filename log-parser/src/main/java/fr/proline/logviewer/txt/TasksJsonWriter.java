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
 * Create date : 24 oct. 2019
 */
package fr.proline.logviewer.txt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.proline.logviewer.model.LogTask;
import java.util.ArrayList;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author Karine XUE at CEA
 */
public class TasksJsonWriter {

    ArrayList<LogTask> m_taskList;
    String m_fileName;
    Gson m_gson;

    public TasksJsonWriter(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            m_fileName = "TaskList";
        } else {
            m_fileName = fileName;
        }
        m_taskList = new ArrayList();

    }

    /**
     * useful to print a list of object in Json format, unusful at the moment
     *
     * @param tasks
     * @param fileName: file name
     * @throws IOException
     */
    public void setData(ArrayList<LogTask> tasks, String fileName) throws IOException {
        FileWriter outputFile;
        outputFile = new FileWriter(m_fileName + "_" + fileName);
        try {
            m_gson = new GsonBuilder().setPrettyPrinting().create();
            for (LogTask task : tasks) {
                String jsonOutput = m_gson.toJson(task);
                outputFile.write(jsonOutput);
                //System.out.println(jsonOutput);
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            outputFile.close();
        }
    }

}
