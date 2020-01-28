/*
 * @cea 
 * @http://www.profiproteomics.fr
 * created date: 7 oct. 2019
 */
package fr.proline.logviewer.gui;

import fr.proline.logviewer.model.LogTask;
import fr.proline.logviewer.model.LogTask.STATUS;
import fr.proline.logviewer.model.Utility;
import java.awt.Component;
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
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Karine XUE at CEA
 */
public class TaskListView extends JScrollPane {

    protected static final Logger _logger = LoggerFactory.getLogger(TaskListView.class);
    private LogViewControlPanel m_ctrl;
    ArrayList<LogTask> m_taskList;//data model used by TableModel
    JTable m_taskTable;

    TaskListView(LogViewControlPanel control) {
        super();
        m_ctrl = control;
        this.setBorder(BorderFactory.createTitledBorder("List of Task"));
        this.setPreferredSize(new Dimension(800, 250));
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
        initColumnsize();
        this.repaint();
    }

    private void initColumnsize() {
        String[] example = {"198", "853bda4a-10d9-11e8-9a85-d9411af38406", "[pool-2-thread-25]", "proline/dps/msi/ImportValidateGenerateSM",
            "FINISHED_W", "104", "09:01:27.985 - 09 oct. 2019 ", "09:01:27.985 - 09 oct. 2019 ", "+10"};
        TableColumn column;
        int cellWidth;
        for (int i = 0; i < m_taskTable.getColumnCount(); i++) {
            column = m_taskTable.getColumnModel().getColumn(i);
            TableCellRenderer renderer = m_taskTable.getDefaultRenderer(column.getClass());
            Component comp = renderer.getTableCellRendererComponent(m_taskTable, example[i], false, false, 0, i);
            cellWidth = comp.getPreferredSize().width;
            column.setPreferredWidth(cellWidth);
        }
    }

    class TaskTable extends JTable {

        public TaskTable(TableModel dm) {
            super(dm);

            this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            ListSelectionModel selectionModel = this.getSelectionModel();
            selectionModel.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    long begin = System.currentTimeMillis();
                    if (e.getValueIsAdjusting()) {
                        return;
                    }
                    m_ctrl.setLoading(true);
                    ListSelectionModel lsm = (ListSelectionModel) e.getSource();
                    if (!lsm.isSelectionEmpty()) {
                        int sortedIndex = lsm.getMinSelectionIndex();
                        int selectedIndex = m_taskTable.convertRowIndexToModel(sortedIndex);
                        LogTask task = m_taskList.get(selectedIndex);
                        String taskOrder = (task == null) ? "" : "" + task.getTaskOrder();
                        _logger.debug("get task {} {} ms", taskOrder, System.currentTimeMillis() - begin);
                        m_ctrl.valueChanged(task);
                        _logger.debug("show task {} {} ms", taskOrder, System.currentTimeMillis() - begin);
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

        private String[] m_columnNames = {
            "Task N°",
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
            return this.m_columnNames.length;
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
                    return String.class;
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
            return this.m_columnNames[column]; //To change body of generated methods, choose Tools | Templates.
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
                    STATUS s = taskInfo.getStatus();
                    if (s == STATUS.FAILED) {
                        return "<html><font color=\"red\">" + s + "</font></html>";
                    } else if (s == STATUS.WARNING || s == STATUS.FINISHED_WARN) {
                        return "<html><font color=\"orange\">" + s + "</font></html>";
                    } else {
                        return s.toString();
                    }

                }
                case COLTYPE_START_TIME: {
                    return Utility.formatTime(taskInfo.getStartTime());
                }
                case COLTYPE_STOP_TIME: {
                    return Utility.formatTime(taskInfo.getStopTime());
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
                long delta = Utility.parseTime(s1) - Utility.parseTime(s2);
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
