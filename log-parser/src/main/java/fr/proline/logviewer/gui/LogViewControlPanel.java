/*
 * @cea 
 * @http://www.profiproteomics.fr
 * created date: 7 oct. 2019
 */
package fr.proline.logviewer.gui;

import fr.proline.logviewer.model.LogTask;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 *
 * @author Karine XUE at CEA
 */
public class LogViewControlPanel extends JPanel {

    private LogConsolePane m_console;
    private TaskListView m_taskQueueView;
    private TaskView m_taskView;

    public LogViewControlPanel() {
        super(new BorderLayout());
        this.setBackground(Color.white);
        JSplitPane mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        m_console = new LogConsolePane();
        m_taskQueueView = new TaskListView(this);
        m_taskView = new TaskView(this);
        JSplitPane leftPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        leftPanel.setTopComponent(m_taskQueueView);
        leftPanel.setBottomComponent(m_console);
        mainPanel.setLeftComponent(leftPanel);
        mainPanel.setRightComponent(m_taskView);
        this.add(mainPanel, BorderLayout.CENTER);
        this.setSize(1200, 800);
    }

    public LogConsolePane getConsole() {
        return m_console;
    }

    public void valueChanged(LogTask selectedTask) {
        m_taskView.setData(selectedTask);
        m_console.setData(selectedTask.getTrace());

    }

    public synchronized void setData(ArrayList<LogTask> tasks, String fileName) {
        m_taskQueueView.setData(tasks, fileName);
        if (tasks == null || tasks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No task to show");
            m_taskView.setData(null);
            m_console.setData(null);
        }
    }

    void clear() {
        m_taskQueueView.setData(null, null);
        m_taskView.setData(null);
        m_console.setData(null);
    }

}
