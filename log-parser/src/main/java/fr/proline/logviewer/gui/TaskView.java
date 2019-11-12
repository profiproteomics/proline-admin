/*
 * @cea 
 * @http://www.profiproteomics.fr
 * created date: 7 oct. 2019
 */
package fr.proline.logviewer.gui;

import fr.proline.logviewer.model.LogTask;
import fr.proline.logviewer.model.TimeFormat;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;

/**
 *
 * @author Karine XUE at CEA //copy from TaskDescriptionPanel @todo? extends
 */
public class TaskView extends JPanel {

    //TextField for time
    private JTextField m_startTimeTextfield;
    private JTextField m_endTimeTextfield;
    private JTextField m_deltaEndTimeTextfield;
    private JTextField m_deltaEndTimeInHourTextfield;
    //TextField for main info
    private JTextField m_threadNameTextfield;
    private JTextField m_messageIdTextfield;
    private JTextField m_statusTextfield;
    private JTextField m_nbTaskParallelTextfield;
    private JTextField m_callServiceTextfield;
    private JTextField m_dataSetTextField;
    private JTextField m_startLineTextfield;
    private JTextField m_stopLineTextfield;
    private JTextField m_projectIdTextField;
    private JLabel m_startLineIndexLabel;
    private JLabel m_stopLineIndexLabel;
    //private JTextArea m_parameterTextArea;
    JScrollPane m_paramPanel;
    private LogViewControlPanel m_ctrl;
    private LogTask m_task;

    public TaskView(LogViewControlPanel control) {
        super();
        m_ctrl = control;
        this.setBorder(BorderFactory.createTitledBorder("Task Detail"));
        initComponents();
        this.setPreferredSize(new Dimension(600, 800));
    }

    void setData(LogTask selectedTask) {
        m_task = selectedTask;
        if (m_task == null) {
            reinit();
        } else {
            updateData();
        }
    }

    private void updateData() {
        reinit();
        m_threadNameTextfield.setText(m_task.getThreadName());
        m_messageIdTextfield.setText(m_task.getMessageId());
        LogTask.STATUS status = m_task.getStatus();
        m_statusTextfield.setText((status == null) ? null : status.toString());
        int nbTask = m_task.getNbParallelTask();
        if (nbTask > 0) {
            m_nbTaskParallelTextfield.setText("+" + nbTask);
        } else {
            m_nbTaskParallelTextfield.setText("");
        }
        m_callServiceTextfield.setText(m_task.getCallService());
        m_dataSetTextField.setText(m_task.getDataSet());
        m_projectIdTextField.setText(m_task.getProjectId());
        m_startLineTextfield.setText(m_task.getStartLine().line);
        m_startLineTextfield.setCaretPosition(0);
        m_startLineIndexLabel.setText("L." + m_task.getStartLine().index);
        m_stopLineTextfield.setText(m_task.getStopLine().line);
        m_stopLineTextfield.setCaretPosition(0);
        m_stopLineIndexLabel.setText("L." + m_task.getStopLine().index);
        m_startTimeTextfield.setText(TimeFormat.formatTime(m_task.getStartTime()));
        m_endTimeTextfield.setText(TimeFormat.formatTime(m_task.getStopTime()));

        m_deltaEndTimeTextfield.setText(TimeFormat.formatDeltaTime(m_task.getDuration()));
        m_deltaEndTimeInHourTextfield.setText(TimeFormat.formatDurationInHour(m_task.getDuration()));
        JTree tree = m_task.getParamTree();
        expandTreeAllNodes(tree, 0, tree.getRowCount());
        m_paramPanel.getViewport().add(tree);
        //_parameterTextArea.setText(m_task.getRequestParam());
    }

    private void expandTreeAllNodes(JTree tree, int startingIndex, int rowCount) {
        for (int i = startingIndex; i < rowCount; ++i) {
            tree.expandRow(i);
        }

        if (tree.getRowCount() != rowCount) {
            expandTreeAllNodes(tree, rowCount, tree.getRowCount());
        }
    }

    private void initComponents() {

        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new java.awt.Insets(5, 5, 5, 5);

        // --- create objects
        JPanel mainInfoPanel = createMainInfoPanel();
        JPanel timePanel = createTimePanel();
        m_paramPanel = createParameterPanel();
        // --- add objects
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.gridwidth = 3;
        add(mainInfoPanel, c);

        c.weightx = 1;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        add(timePanel, c);

        c.gridy++;
        c.weighty = 1;
        add(m_paramPanel, c);
    }

    public JPanel createTimePanel() {
        JPanel timePanel = new JPanel(new GridBagLayout());
        timePanel.setBorder(BorderFactory.createTitledBorder(" Timestamp "));

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new java.awt.Insets(5, 5, 5, 5);

        JLabel startTimeLabel = new JLabel("Start Time:");
        m_startTimeTextfield = new JTextField();
        m_startTimeTextfield.setEditable(false);
        m_startTimeTextfield.setBackground(Color.white);

        JLabel durationLabel = new JLabel("Duration:");

        JLabel endTimeLabel = new JLabel("End Time:");
        m_endTimeTextfield = new JTextField();
        m_endTimeTextfield.setEditable(false);
        m_endTimeTextfield.setBackground(Color.white);
        m_deltaEndTimeTextfield = new JTextField();
        m_deltaEndTimeTextfield.setEditable(false);
        m_deltaEndTimeTextfield.setBackground(Color.white);
        m_deltaEndTimeInHourTextfield = new JTextField();
        m_deltaEndTimeInHourTextfield.setToolTipText("duration in HHHH:mm:ss.SSS");
        m_deltaEndTimeInHourTextfield.setEditable(false);
        m_deltaEndTimeInHourTextfield.setBackground(Color.white);
        // --- add objects
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        timePanel.add(startTimeLabel, c);
        c.gridx++;
        c.weightx = 1;
        timePanel.add(m_startTimeTextfield, c);

        c.gridx++;
        c.weightx = 0;
        timePanel.add(durationLabel, c);
        c.gridx++;
        c.weightx = 1;
        timePanel.add(m_deltaEndTimeTextfield, c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        timePanel.add(endTimeLabel, c);
        c.gridx++;
        c.weightx = 1;
        timePanel.add(m_endTimeTextfield, c);

        c.gridx += 2;
        c.weightx = 0.3;
        timePanel.add(m_deltaEndTimeInHourTextfield, c);
        return timePanel;
    }

    private void reinit() {
        m_threadNameTextfield.setText("");
        m_messageIdTextfield.setText("");
        m_statusTextfield.setText("");
        m_nbTaskParallelTextfield.setText("");
        m_callServiceTextfield.setText("");
        m_dataSetTextField.setText("");
        m_startLineTextfield.setText("");
        m_stopLineTextfield.setText("");
        m_projectIdTextField.setText("");
        m_startTimeTextfield.setText("");
        m_endTimeTextfield.setText("");
        m_deltaEndTimeTextfield.setText("");
        m_deltaEndTimeInHourTextfield.setText("");
        m_startLineIndexLabel.setText("line index");
        m_stopLineIndexLabel.setText("line index");
        //TextField for main info
    }

    public JPanel createMainInfoPanel() {
        JPanel mainInfoPanel = new JPanel(new GridBagLayout());
        mainInfoPanel.setBorder(BorderFactory.createTitledBorder(" Main Info "));

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new java.awt.Insets(5, 5, 5, 5);

        // --- create objects
        JLabel threadNameLabel = new JLabel("Thread:");
        threadNameLabel.setToolTipText("run on the Thread");
        m_threadNameTextfield = new JTextField();
        m_threadNameTextfield.setEditable(false);
        m_threadNameTextfield.setBackground(Color.white);

        JLabel messageIdLabel = new JLabel("Message Id:");
        m_messageIdTextfield = new JTextField();
        m_messageIdTextfield.setEditable(false);
        m_messageIdTextfield.setBackground(Color.white);

        JLabel statusLabel = new JLabel("Stauts:");
        m_statusTextfield = new JTextField();
        m_statusTextfield.setEditable(false);
        m_statusTextfield.setBackground(Color.white);

        JLabel callServiceLabel = new JLabel("Call Service:");
        m_callServiceTextfield = new JTextField();
        m_callServiceTextfield.setEditable(false);
        m_callServiceTextfield.setBackground(Color.white);

        JLabel dataSetLabel = new JLabel("DataSet:");
        m_dataSetTextField = new JTextField();
        m_dataSetTextField.setEditable(false);
        m_dataSetTextField.setBackground(Color.white);

        JLabel projectIdLabel = new JLabel("Project Id:");
        m_projectIdTextField = new JTextField();
        m_projectIdTextField.setEditable(false);
        m_projectIdTextField.setBackground(Color.white);

        JLabel nbTaskLabel = new JLabel("Tasks:");
        nbTaskLabel.setToolTipText("Nombre Task in parallel:");
        m_nbTaskParallelTextfield = new JTextField();
        m_nbTaskParallelTextfield.setEditable(false);
        m_nbTaskParallelTextfield.setBackground(Color.white);

        JLabel startLineLabel = new JLabel("Start line:");
        m_startLineTextfield = new JTextField();
        m_startLineTextfield.setEditable(false);
        m_startLineTextfield.setBackground(Color.white);
        JLabel stopLineLabel = new JLabel("Stop line:");
        m_stopLineTextfield = new JTextField();
        m_stopLineTextfield.setEditable(false);
        m_stopLineTextfield.setBackground(Color.white);
        m_startLineIndexLabel = new JLabel("line index");
        m_stopLineIndexLabel = new JLabel("line index");

        // --- add objects, suppose grid bag is x=9
        // new Line
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        mainInfoPanel.add(threadNameLabel, c);
        c.gridx++;
        c.gridwidth = 4;
        c.weightx = 1;
        mainInfoPanel.add(m_threadNameTextfield, c);

        c.gridx += 4;
        c.gridwidth = 1;
        c.weightx = 0;
        mainInfoPanel.add(messageIdLabel, c);
        c.gridx++;
        c.gridwidth = 4;
        c.weightx = 0.5;
        mainInfoPanel.add(m_messageIdTextfield, c);
// new Line
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        mainInfoPanel.add(callServiceLabel, c);
        c.gridx++;
        c.weightx = 0.6;
        c.gridwidth = 4;
        mainInfoPanel.add(m_callServiceTextfield, c);

        c.gridx += 4;
        c.weightx = 0;
        c.gridwidth = 1;
        mainInfoPanel.add(statusLabel, c);
        c.gridx++;

        c.weightx = 0.3;
        c.gridwidth = 2;
        mainInfoPanel.add(m_statusTextfield, c);
        c.gridx += 2;
        c.weightx = 0;
        c.gridwidth = 1;
        mainInfoPanel.add(nbTaskLabel, c);
        c.gridx++;
        c.weightx = 0.1;
        c.gridwidth = 1;
        mainInfoPanel.add(m_nbTaskParallelTextfield, c);
// new Line
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        mainInfoPanel.add(dataSetLabel, c);
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = 7;
        mainInfoPanel.add(m_dataSetTextField, c);

        c.gridx += 7;
        c.weightx = 0;
        c.gridwidth = 1;
        mainInfoPanel.add(projectIdLabel, c);
        c.gridx++;
        c.weightx = 0.1;
        c.gridwidth = 1;
        mainInfoPanel.add(m_projectIdTextField, c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        mainInfoPanel.add(startLineLabel, c);
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = 8;
        mainInfoPanel.add(m_startLineTextfield, c);
        c.gridx += 8;
        c.weightx = 0;
        c.gridwidth = 1;
        mainInfoPanel.add(m_startLineIndexLabel, c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        mainInfoPanel.add(stopLineLabel, c);
        c.gridx++;
        c.gridwidth = 8;
        mainInfoPanel.add(m_stopLineTextfield, c);
        c.gridx += 8;
        c.weightx = 0;
        c.gridwidth = 1;
        mainInfoPanel.add(m_stopLineIndexLabel, c);
        return mainInfoPanel;
    }

    private JScrollPane createParameterPanel() {
        JScrollPane paramePane = new JScrollPane();
        paramePane.setBorder(BorderFactory.createTitledBorder("task parameters"));
        return paramePane;
    }

}
