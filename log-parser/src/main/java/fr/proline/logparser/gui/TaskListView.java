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
import fr.proline.logparser.model.Utility;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jdesktop.swingx.table.TableColumnExt;

/**
 *
 * @author Karine XUE at CEA
 */
public class TaskListView extends JScrollPane implements TaskListInterface {

    private LogViewControlPanel m_ctrl;
    ArrayList<LogTask> m_taskList;//data model used by TableModel
    TaskTable m_taskTable;
    TaskTableModel m_tableModel;

    TaskListView(LogViewControlPanel control) {
        super();
        m_ctrl = control;
        this.setBorder(BorderFactory.createTitledBorder("List of Task"));
        this.setPreferredSize(new Dimension(1400, 250));
        m_taskList = new ArrayList<>();
        m_tableModel = new TaskTableModel();
        m_taskTable = new TaskTable(m_tableModel);
        m_taskTable.init();
        this.getViewport().add(m_taskTable);
    }

    public void setData(ArrayList<LogTask> tasks, String fileName) {
        ((TitledBorder) this.getBorder()).setTitle("List of Tasks" + "     " + fileName);
        m_taskTable.removeAll();//must
        if (tasks == null || tasks.isEmpty()) {
            m_taskList = new ArrayList<>();
            m_tableModel.fireTableDataChanged();
        } else {
            m_taskList = tasks;
            m_tableModel.fireTableDataChanged();
            m_taskTable.getSelectionModel().setSelectionInterval(0, 0);
        }
        initColumnsize();
    }

    private void initColumnsize() {
        String[] example = {"198", "853bda4a-10d9-11e8-9a85-d9411af38406", "[pool-2-thread-25]", "proline/dps/msi/ImportValidateGenerateSM", " result_files :  mascot_data/20200113/F136424.dat ",
            "FINISHED_W", "104", "09:01:27.985 - 09 oct. 2019 ", "09:01:27.985 - 09 oct. 2019 ", "111:59:59.999", "+10                     "};
        TableColumn column;
        int cellWidth;
        for (int i = 0; i < m_taskTable.getColumnCount(); i++) {
            column = m_taskTable.getColumnModel().getColumn(i);
            int modelIndex = m_taskTable.convertColumnIndexToModel(i);
            TableCellRenderer renderer = m_taskTable.getDefaultRenderer(column.getClass());
            Component comp = renderer.getTableCellRendererComponent(m_taskTable, example[modelIndex], false, false, 0, i);
            cellWidth = comp.getPreferredSize().width;
            column.setPreferredWidth(cellWidth);
        }
    }

    class TaskTable extends JXTable {

        public TaskTable(TaskTableModel tModel) {
            super(tModel);
            this.setColumnControlVisible(true);
            this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            // highlight one line of two
            addHighlighter(HighlighterFactory.createSimpleStriping());
            setGridColor(Color.lightGray);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            ListSelectionModel lsm = (ListSelectionModel) e.getSource();
            if (!lsm.isSelectionEmpty()) {
                int sortedIndex = lsm.getMinSelectionIndex();
                int selectedIndex = m_taskTable.convertRowIndexToModel(sortedIndex);
                LogTask task = m_taskList.get(selectedIndex);
                String taskOrder = (task == null) ? "" : "" + task.getTaskOrder();
                m_ctrl.valueChanged(task);
            }
        }

        public void init() {
            this.setRowSorter(new TableRowSorter<>(this.getModel()));
            this.setTableHeader(new TooltipsTableHeader(m_taskTable.getColumnModel(), m_columnNames));
            this.setColumnsVisibility();
        }

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            int columnM = this.convertColumnIndexToModel(column);
            switch (columnM) {
                case TaskTableModel.COLTYPE_STATUS:
                    return new DefaultTableCellRenderer() {
                        @Override
                        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                            String toShow = ((LogTask.STATUS) value).getLabelTxt();
                            JLabel lb = (JLabel) super.getTableCellRendererComponent(table, toShow, isSelected, hasFocus, row, column);
                            return lb;
                        }
                    };
                case TaskTableModel.COLTYPE_NB_TASK_PARALELLE:
                    return new TaskNbCellRenderer();
                case TaskTableModel.COLTYPE_START_TIME:
                case TaskTableModel.COLTYPE_STOP_TIME:
                    return new DefaultTableCellRenderer() {
                        @Override
                        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                            String toShow = Utility.formatTime((Date) value);
                            JLabel lb = (JLabel) super.getTableCellRendererComponent(table, toShow, isSelected, hasFocus, row, column);
                            return lb;
                        }
                    };
                case TaskTableModel.COLTYPE_DURATION:
                    return new DefaultTableCellRenderer() {
                        @Override
                        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                            String toShow = Utility.formatDurationInHour((Long) value);
                            JLabel lb = (JLabel) super.getTableCellRendererComponent(table, toShow, isSelected, hasFocus, row, column);
                            return lb;
                        }
                    };
                default:
                    return super.getCellRenderer(row, columnM);
            }
        }

        private void setColumnsVisibility() {
            List<TableColumn> columns = getColumns(true);
            TableColumnExt columnExt = (TableColumnExt) columns.get(TaskTableModel.COLTYPE_MESSAGE_ID);
            if (columnExt != null) {
                columnExt.setVisible(false);
            }
        }

        class TooltipsTableHeader extends JTableHeader {

            String[] tooltips;

            TooltipsTableHeader(TableColumnModel columnModel, String[] columnTooltips) {
                super(columnModel);//do everything a normal JTableHeader does
                this.tooltips = columnTooltips;//plus extra data
            }

            public String getToolTipText(MouseEvent e) {
                java.awt.Point p = e.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                int realIndex = columnModel.getColumn(index).getModelIndex();
                return this.tooltips[realIndex];
            }
        }
    }

    private String[] m_columnNames = {
        "Task N°",
        "Message Id",
        "Thread Name",
        "Call Service",
        "Meta Info",
        "Status",
        "Project Id",
        "Start Time",
        "Stop Time",
        "Duration",
        "Parallel tasks Nb"};

    class TaskTableModel extends AbstractTableModel {

        static final int COLTYPE_ORDER_ID = 0;
        static final int COLTYPE_MESSAGE_ID = 1;
        static final int COLTYPE_THREAD_NAME = 2;
        static final int COLTYPE_CALL_SERVICE = 3;
        static final int COLTYPE_META_INFO = 4;
        static final int COLTYPE_STATUS = 5;
        static final int COLTYPE_PROJECT_ID = 6;
        static final int COLTYPE_START_TIME = 7;
        static final int COLTYPE_STOP_TIME = 8;
        static final int COLTYPE_DURATION = 9;
        static final int COLTYPE_NB_TASK_PARALELLE = 10;

        public TaskTableModel() {
            m_taskList = new ArrayList<>();
        }

        @Override
        public int getRowCount() {
            return m_taskList.size();
        }

        @Override
        public int getColumnCount() {
            return m_columnNames.length;
        }

        public LogTask getTask(int row) {
            return m_taskList.get(row);
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
                    return LogTask.STATUS.class;
                }
                case COLTYPE_START_TIME: {
                    return Date.class;
                }
                case COLTYPE_STOP_TIME: {
                    return Date.class;
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
                case COLTYPE_META_INFO: {
                    return String.class;
                }
                case COLTYPE_DURATION: {
                    return Long.class;
                }
                case COLTYPE_NB_TASK_PARALELLE: {
                    return Integer.class;
                }
                default:
                    return null;
            }
        }

        @Override
        public String getColumnName(int column) {
            return m_columnNames[column]; //To change body of generated methods, choose Tools | Templates.
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
                case COLTYPE_THREAD_NAME: {
                    return taskInfo.getThreadName();
                }
                case COLTYPE_MESSAGE_ID: {
                    return taskInfo.getMessageId();
                }
                case COLTYPE_CALL_SERVICE: {
                    return taskInfo.getCallService();
                }
                case COLTYPE_META_INFO: {
                    return taskInfo.getDataSet();
                }
                case COLTYPE_START_TIME: {
                    return taskInfo.getStartTime();
                }
                case COLTYPE_STOP_TIME: {
                    return taskInfo.getStopTime();
                }
                case COLTYPE_DURATION: {
                    return taskInfo.getDuration();
                }
                case COLTYPE_NB_TASK_PARALELLE: {
                    return taskInfo.getNbParallelTask();
                }
                default:
                    return null;
            }
        }

    }//end inner class TaskTableModel

    public class TaskNbCellRenderer extends DefaultTableCellRenderer {

        private TaskExecutionPanel m_valuePanel;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (m_valuePanel == null) {
                m_valuePanel = new TaskExecutionPanel(1, -1);
            }
            int modelIndex = table.convertRowIndexToModel(row);
            TaskTableModel model = (TaskTableModel) table.getModel();
            LogTask task = model.getTask(modelIndex);
            String toShow = ((int) value == 0) ? "" : "+" + value;
            m_valuePanel.setData(task.getTimeStamp(), task.getNbOtherTaskMoment(), toShow);
            return m_valuePanel;
        }

    }

}
