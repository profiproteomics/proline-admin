/*
 * @cea 
 * @http://www.profiproteomics.fr
 * created date: 7 oct. 2019
 */
package fr.proline.logviewer.gui;

import fr.proline.logviewer.model.LogTask;
import fr.proline.logviewer.model.TimeFormat;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author Karine XUE at CEA
 */
public class TaskListView extends JScrollPane {

    private LogViewControlPanel m_ctrl;
    ArrayList<LogTask> m_taskList;//data model used by TableModel
    JTable m_taskTable;

    TaskListView(LogViewControlPanel control) {
        super();
        m_ctrl = control;
        this.setBorder(BorderFactory.createTitledBorder("List of Task"));
        this.setPreferredSize(new Dimension(800, 150));
        TaskTableModel model = new TaskTableModel();

        m_taskTable = new TaskTable(model);
        m_taskTable.setRowSorter(createSorter(model));
        this.getViewport().add(m_taskTable);

    }

    void setData(ArrayList<LogTask> tasks, String fileName) {
        m_taskTable.setModel(new TaskTableModel());
        ((TitledBorder) this.getBorder()).setTitle("List of Tasks" + "     " + fileName);
        if (tasks == null) {
            m_taskList = new ArrayList<>();
        } else {
            m_taskList = tasks;
        }
        this.viewport.revalidate();
        this.repaint();
    }

    class TaskTable extends JTable {

        public TaskTable(TableModel dm) {
            super(dm);
            this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            ListSelectionModel selectionModel = this.getSelectionModel();
            selectionModel.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (e.getValueIsAdjusting()) {
                        return;
                    }

                    ListSelectionModel lsm = (ListSelectionModel) e.getSource();
                    if (!lsm.isSelectionEmpty()) {
                        int sortedIndex = lsm.getMinSelectionIndex();
                        int selectedIndex = m_taskTable.convertRowIndexToModel(sortedIndex);
                        m_ctrl.valueChanged(m_taskList.get(selectedIndex));
                    }
                }
            });
        }

    }

    class TaskTableModel extends AbstractTableModel {

        static final int COLTYPE_ORDER_ID = 0;
        static final int COLTYPE_MESSAGE_ID = 1;
        static final int COLTYPE_THREAD_NAME = 2;
        static final int COLTYPE_CALL_SERVICE = 3;
        static final int COLTYPE_STATUS = 4;
        static final int COLTYPE_PROJECT_ID = 5;
        static final int COLTYPE_START_TIME = 6;
        static final int COLTYPE_STOP_TIME = 7;

        static final int COLTYPE_NB_TASK_PARALELLE = 8;

        private String[] _columnNames = {
            "Task Number",
            "Message Id",
            "Thread Name",
            "Call Service",
            "Status",
            "Project Id",
            "Start Time",
            "Stop Time",
            "Nb Task Paralelle"};

        public TaskTableModel() {
            m_taskList = new ArrayList();

        }

        @Override
        public int getRowCount() {
            return m_taskList.size();
        }

        @Override
        public int getColumnCount() {
            return this._columnNames.length;
        }

        /**
         * useful for compare, sort
         *
         * @param columnIndex
         * @return
         */
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case COLTYPE_ORDER_ID: {
                    return Integer.class;
                }
                case COLTYPE_PROJECT_ID: {
                    return Integer.class;
                }
                case COLTYPE_STATUS: {
                    return Enum.class;
                }
                case COLTYPE_START_TIME: {
                    return String.class;
                }
                case COLTYPE_STOP_TIME: {
                    return String.class;
                }
                case COLTYPE_THREAD_NAME: {
                    return String.class;
                }
                case COLTYPE_MESSAGE_ID: {
                    return String.class;
                }
                case COLTYPE_CALL_SERVICE: {
                    return String.class;
                }
                case COLTYPE_NB_TASK_PARALELLE: {
                    return String.class;
                }
                default:
                    return null;
            }
        }

        @Override
        public String getColumnName(int column) {
            return this._columnNames[column]; //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= m_taskList.size()) {
                return null;
            }
            LogTask taskInfo = m_taskList.get(rowIndex);
            switch (columnIndex) {
                case COLTYPE_ORDER_ID: {
                    return rowIndex;
                }
                case COLTYPE_PROJECT_ID: {
                    return taskInfo.getProjectId();
                }
                case COLTYPE_STATUS: {
                    return taskInfo.getStatus();
                }
                case COLTYPE_START_TIME: {
                    return TimeFormat.formatTime(taskInfo.getStartTime());
                }
                case COLTYPE_STOP_TIME: {
                    return TimeFormat.formatTime(taskInfo.getStopTime());
                }
                case COLTYPE_THREAD_NAME: {
                    return taskInfo.getThreadName();
                }
                case COLTYPE_MESSAGE_ID: {
                    return taskInfo.getMessageId();
                }
                case COLTYPE_CALL_SERVICE: {
                    return taskInfo.getCallService();
                }
                case COLTYPE_NB_TASK_PARALELLE: {
                    int nbTask = taskInfo.getNbParallelTask();
                    if (nbTask > 0) {
                        return "+" + nbTask;
                    }
                }
                default:
                    return null;
            }
        }

    }

    private TableRowSorter<TableModel> createSorter(TaskTableModel model) {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        Comparator c1 = new Comparator<String>() {
            public int compare(String s1, String s2) {
                long delta = TimeFormat.parseTime(s1) - TimeFormat.parseTime(s2);
                if (delta == 0) {
                    return 0;
                } else {
                    return (delta) > 0 ? 1 : -1;
                }
            }

        };
        Comparator c2 = new Comparator<String>() {
            public int compare(String s1, String s2) {
                long delta = Integer.parseInt(s1) - Integer.parseInt(s2);
                if (delta == 0) {
                    return 0;
                } else {
                    return (delta) > 0 ? 1 : -1;
                }
            }

        };

        sorter.setComparator(TaskTableModel.COLTYPE_START_TIME, c1);
        sorter.setComparator(TaskTableModel.COLTYPE_STOP_TIME, c1);
        sorter.setComparator(TaskTableModel.COLTYPE_NB_TASK_PARALELLE, c2);
        return sorter;
    }
}
