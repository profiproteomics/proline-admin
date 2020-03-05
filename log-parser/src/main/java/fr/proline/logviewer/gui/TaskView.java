/*
 * @cea 
 * @http://www.profiproteomics.fr
 * created date: 7 oct. 2019
 */
package fr.proline.logviewer.gui;

import fr.proline.logviewer.model.LogTask;
import fr.proline.logviewer.model.Utility;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;

/**
 *
 * @author Karine XUE at CEA
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
    TaskExecutionPanel m_executionPanel;
    private LogViewControlPanel m_ctrl;
    private LogTask m_task;

    public TaskView(LogViewControlPanel control) {
        super();
        m_ctrl = control;
        this.setBorder(BorderFactory.createTitledBorder("Task Detail"));
        initComponents();
        this.setPreferredSize(new Dimension(700, 700));
    }

    void setData(LogTask selectedTask) {
        m_task = selectedTask;
        if (m_task == null) {
            reinit();
        } else {
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
            m_startTimeTextfield.setText(Utility.formatTime(m_task.getStartTime()));
            m_endTimeTextfield.setText(Utility.formatTime(m_task.getStopTime()));

            m_deltaEndTimeTextfield.setText(Utility.formatDeltaTime(m_task.getDuration()));
            m_deltaEndTimeInHourTextfield.setText(Utility.formatDurationInHour(m_task.getDuration()));
            JTree tree = m_task.getParamTree();
            expandTreeAllNodes(tree, 0, tree.getRowCount());
            m_paramPanel.getViewport().add(tree);
            //_parameterTextArea.setText(m_task.getRequestParam());
            m_executionPanel.setData(m_task.getTimeStamp(), m_task.getNbOtherTaskMoment());
            repaint();
        }
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
        m_executionPanel = createExecutionPanel();
        // --- add objects
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        //c.gridwidth = 3;
        add(mainInfoPanel, c);

        c.weightx = 1;
        c.gridy++;
        //c.gridwidth = 3;
        add(timePanel, c);

        c.gridx = 0;
        c.gridy++;
        add(m_executionPanel, c);

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
        m_paramPanel.getViewport().removeAll();
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

        JLabel dataSetLabel = new JLabel("Meta info:");
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
        paramePane.setBorder(BorderFactory.createTitledBorder("Task parameters"));
        return paramePane;
    }

    private TaskExecutionPanel createExecutionPanel() {
        TaskExecutionPanel pane = new TaskExecutionPanel();
        pane.setSize(this.getWidth(), 40);
        pane.setBorder(BorderFactory.createTitledBorder("Exexution situation & Parallel task"));
        return pane;
    }

    class TaskExecutionPanel extends JPanel {

        /**
         * time stamp
         */
        private ArrayList<Long> m_xValues;
        /**
         * nomber of task
         */
        private ArrayList<Integer> m_yValues;

        /**
         * number of time stamps
         */
        private int m_size;
        /**
         * time lenght
         */
        private long m_length;

        private int m_maxNbOtherTask;

        void setData(ArrayList<Long> xValues, ArrayList<Integer> yValues) {
            this.m_xValues = xValues;
            this.m_yValues = yValues;
            this.m_size = xValues.size();
            this.m_length = m_xValues.get(m_size - 1);
            if (m_length == 0) {
                m_length = 1;
            }
            //System.out.println(m_task.getTaskOrder() + " : duree " + m_length + "," + this.toString());
        }

        public void paint(Graphics g) {
            if (m_xValues == null || m_size < 1) {
                super.paint(g);
                return;
            }
            setOpaque(true);
            super.paint(g);
            long maxLength = getWidth() - 10;
            int start, w;
            Color color;
            for (int i = 0; i < m_size; i++) {
                long predTimeStamp = (i == 0) ? 0 : m_xValues.get(i - 1);
                start = Math.round(5l + (predTimeStamp * maxLength / m_length));
                w = Math.round((m_xValues.get(i) - predTimeStamp) * maxLength / m_length);
                color = pickColor((int) m_yValues.get(i));
                g.setColor(color);
                g.fillRect(start, 20, w, height - 12);
                //System.out.println("(|" + start + "->" + w + " panel width=" + maxLength + " color=" + m_yValues.get(i));
            }
        }

        private Color pickColor(int nbTask) {
            int i = (nbTask >= colorSize) ? colorSize - 1 : nbTask;
            Color color = INTENSITY_PALETTE[i];
            return color;
        }

        public String toString() {
            String s = "" + m_xValues.size() + " ";
            for (int i = 0; i < m_xValues.size(); i++) {
                s += ",[" + m_xValues.get(i) + "," + m_yValues.get(i) + "]";
            }
            return s;
        }

        final Color[] INTENSITY_PALETTE = {
            Color.getHSBColor(0, 0, 1),//while        
            Color.getHSBColor(0.55f, 0.1f, 1.0f),//bleu-white1
            Color.getHSBColor(0.55f, 0.2f, 1.0f),//bleu-white2
            Color.getHSBColor(0.55f, 0.3f, 1.0f),//bleu-white3
            Color.getHSBColor(0.3f, 0.4f, 1.0f),//green1
            Color.getHSBColor(0.3f, 0.5f, 1.0f),//green2
            Color.getHSBColor(0.18f, 0.6f, 1.0f),//yellow1
            Color.getHSBColor(0.16f, 0.7f, 1.0f),//yellow2
            Color.getHSBColor(0.11f, 0.8f, 1.0f),//yellow-orange1
            Color.getHSBColor(0.10f, 0.9f, 1.0f),//orange2
            Color.getHSBColor(0.08f, 1.0f, 1.0f),//orange3
            Color.getHSBColor(1, 1.0f, 1.0f),//red
            Color.getHSBColor(0.9f, 0.6f, 0.8f),//bordeau
            Color.getHSBColor(0.8f, 0.4f, 0.6f),//purple
            Color.getHSBColor(0.8f, 0.2f, 0.4f),//Grey->Dark3
            Color.getHSBColor(0, 1.0f, 0)//Dark
        };
        final int colorSize = INTENSITY_PALETTE.length;
        final int height = 20;
    }
}
