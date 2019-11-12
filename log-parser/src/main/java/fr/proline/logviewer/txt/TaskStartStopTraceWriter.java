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
 * Create date : 4 nov. 2019
 */
package fr.proline.logviewer.txt;

import java.io.FileWriter;
import java.io.IOException;
import org.openide.util.Exceptions;

/**
 *
 * @author Karine XUE at CEA
 */
public class TaskStartStopTraceWriter {

    String m_fileName;
    FileWriter m_outputFile;
    boolean m_stdout;

    public TaskStartStopTraceWriter(String fileName, boolean stdout) {
        if (fileName == null || fileName.isEmpty() ) {
            m_fileName = "LogTrace";
        } else {
            this.m_fileName = fileName;
        }
        m_stdout = stdout;
    }

    public void close() {
        try {
            if (m_outputFile != null) {
                m_outputFile.close();
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public void open(String file2Anaylse) {
        try {
            m_outputFile = new FileWriter(m_fileName + "_" + file2Anaylse);
            String head = "Analyse " + file2Anaylse;
            m_outputFile.write(head);
            if (m_stdout) {
                System.out.println(head);
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public void addLine(String s) {
        try {
            m_outputFile.write(s);
            if (m_stdout) {
                System.out.print(s);
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

}
