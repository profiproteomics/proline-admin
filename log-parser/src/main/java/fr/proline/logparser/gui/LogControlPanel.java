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
 */
package fr.proline.logparser.gui;

import fr.proline.logparser.model.LogTask;
import fr.proline.logparser.model.TaskInJsonCtrl;
import java.awt.BorderLayout;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 *
 * @author Karine XUE at CEA
 */
public abstract class LogControlPanel extends JPanel {

    protected TaskView m_taskView;
    protected TaskConsolePane m_taskConsole;
    protected TaskListInterface m_taskQueueView;
    protected JProgressBar m_progressBar;

    public LogControlPanel() {
        super(new BorderLayout());

    }

    public abstract void setProgressBarVisible(boolean b);

    public abstract void setProgress(int percent);

    public abstract boolean isBigFile();

    public abstract int getMaxLine2Show();

    public abstract void redo();

    public void setData(ArrayList<LogTask> tasks, String fileName) {
        m_taskQueueView.setData(tasks, fileName);
        if (tasks == null || tasks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No task to show");
            m_taskView.setData(null);
            m_taskConsole.setData("");
        }
        this.repaint();//force title change
        this.requestFocus();
    }

    public TaskConsolePane getConsole() {
        return m_taskConsole;
    }

    public void valueChanged(LogTask selectedTask) {
        String order = "";
        if (selectedTask != null) {
            order = "" + selectedTask.getTaskOrder();
        }

        m_taskView.setData(selectedTask);
        if (selectedTask == null) {
            m_taskConsole.setData("");

        } else {
            m_taskConsole.setData("In loading...");
            ArrayList<LogTask.LogLine> trace = selectedTask.getTrace();
            if (trace == null) {
                trace = TaskInJsonCtrl.getInstance().loadTrace(selectedTask.getTaskOrder(), Config.getMaxLine2Show());
            }
            //in order to remain minimun memory space, we don't set trace in the task
            TaskLoaderWorker taskLoader = new TaskLoaderWorker(trace, selectedTask.getNbLine(), this);
            taskLoader.execute();
        }
    }

    /**
     * Memory management
     */
    public void clear() {
        m_taskQueueView.setData(null, null);
        m_taskView.setData(null);
        m_taskConsole.setData("");
    }

}
