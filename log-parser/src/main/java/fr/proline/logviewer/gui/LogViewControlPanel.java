/*
 * @cea 
 * @http://www.profiproteomics.fr
 * created date: 7 oct. 2019
 */
package fr.proline.logviewer.gui;

import fr.proline.logviewer.model.LogTask;
import fr.proline.logviewer.model.Utility;
import fr.proline.logviewer.model.TaskInJsonCtrl;
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

    private TaskConsolePane m_console;
    private TaskListView m_taskQueueView;
    private TaskView m_taskView;
    private JSplitPane m_bottomPanel;
    private LogGuiApp m_ctrl;
    private LogTask m_selectedTask;

    public LogViewControlPanel(LogGuiApp ctrl) {
        super(new BorderLayout());
        m_ctrl = ctrl;
        this.setBackground(Color.white);
        m_console = new TaskConsolePane(this);
        m_taskQueueView = new TaskListView(this);
        m_taskView = new TaskView(this);

        JSplitPane mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        m_bottomPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        m_bottomPanel.setLeftComponent(m_taskView);
        m_bottomPanel.setRightComponent(m_console);
        mainPanel.setTopComponent(m_taskQueueView);
        mainPanel.setBottomComponent(m_bottomPanel);
        this.add(mainPanel, BorderLayout.CENTER);
        this.setSize(1200, 800);

    }

    public TaskConsolePane getConsole() {
        return m_console;
    }

    public void valueChanged(LogTask selectedTask) {
        long begin = System.currentTimeMillis();
        String order = "";
        m_selectedTask = selectedTask;
        if (selectedTask != null) {
            order = "" + selectedTask.getTaskOrder();
        }
        System.out.println("task " + order + ": " + Utility.getMemory());

        m_taskView.setData(selectedTask);
        //System.out.println("task " + order + " view  show time " + (System.currentTimeMillis() - begin));
        begin = System.currentTimeMillis();
        if (selectedTask == null) {
            m_console.setData(null);

        } else {
            ArrayList<LogTask.LogLine> trace = selectedTask.getTrace();
            if (trace == null) {
                LogTask task = TaskInJsonCtrl.getInstance().getCurrentTask(selectedTask.getTaskOrder());

                m_console.setData(task.getTrace());
            } else {
                m_console.setData(trace);
            }
        }
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

    public void setLoading(boolean b) {
//        m_infoPane.setLoading(b);
//        repaint();
    }

    boolean isBigFile() {
        return m_ctrl.isBigFile();
    }

    public String getAnalysedTaskName() {
        return TaskInJsonCtrl.getInstance().getCurrentFile().getPath();
    }

}
