/*
 * @cea 
 * @http://www.profiproteomics.fr
 */
package fr.proline.logviewer.gui;

import fr.proline.logviewer.model.LogTask;
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
public class LogViewControlPanel extends JPanel implements ControlInterface{

    private TaskConsolePane m_taskConsole;
    private TaskListView m_taskQueueView;
    private TaskView m_taskView;
    private LogGuiApp m_ctrl;

    public LogViewControlPanel(LogGuiApp ctrl) {
        super(new BorderLayout());
        m_ctrl = ctrl;
        this.setBackground(Color.white);
        m_taskConsole = new TaskConsolePane();
        m_taskQueueView = new TaskListView(this);
        m_taskView = new TaskView();
        JSplitPane mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        JSplitPane contentBottomPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, m_taskView, m_taskConsole);
        mainPanel.setTopComponent(m_taskQueueView);
        mainPanel.setBottomComponent(contentBottomPanel);
        this.add(mainPanel, BorderLayout.CENTER);
        this.setSize(1400, 800);
    }

    public TaskConsolePane getConsole() {
        return m_taskConsole;
    }

    public void valueChanged(LogTask selectedTask) {
        long begin = System.currentTimeMillis();
        String order = "";
        if (selectedTask != null) {
            order = "" + selectedTask.getTaskOrder();
        }
        //System.out.println("task " + order + ": " + Utility.getMemory());

        m_taskView.setData(selectedTask);
        //System.out.println("task " + order + " view  show time " + (System.currentTimeMillis() - begin));
        begin = System.currentTimeMillis();
        if (selectedTask == null) {
            m_taskConsole.setData("");

        } else {
            m_taskConsole.setData("In loading...");
            ArrayList<LogTask.LogLine> trace = selectedTask.getTrace();
            if (trace == null) {
                trace = TaskInJsonCtrl.getInstance().getCurrentTask(selectedTask.getTaskOrder()).getTrace();
            }
            TaskLoaderWorker taskLoader = new TaskLoaderWorker(trace, this);
            taskLoader.execute();
        }
    }

    public synchronized void setData(ArrayList<LogTask> tasks, String fileName) {
        m_taskQueueView.setData(tasks, fileName);
        if (tasks == null || tasks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No task to show");
            m_taskView.setData(null);
            m_taskConsole.setData("");
        }
        super.requestFocus();
    }

    /**
     * Memory management
     */
    public void clear() {
        m_taskQueueView.setData(null, null);
        m_taskView.setData(null);
        m_taskConsole.setData("");
    }

    @Override
    public boolean isBigFile() {
        return m_ctrl.isBigFile();
    }

    public String getAnalysedTaskName() {
        return TaskInJsonCtrl.getInstance().getCurrentFile().getPath();
    }

}
