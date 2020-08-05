/*
 * @cea 
 * @http://www.profiproteomics.fr
 * created date: 7 oct. 2019
 */
package fr.proline.logparser.model;

import com.google.gson.internal.LinkedTreeMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Karine XUE at CEA
 */
public class LogTask {

    protected static final Logger m_logger = LoggerFactory.getLogger(LogTask.class.getName());
    private static final int MAX_LINE = 1000;
    final static String ERROR_LOG = "ERROR";
    final static String WARN_LOG = "WARN ";

    public enum STATUS {
        RUNNING(0, "RUNNING"),
        FINISHED(1, "FINISHED"),
        WARNING(2, "<html><font color=\"orange\">WARNING</font></html>"),
        FINISHED_WARN(3, "<html><font color=\"orange\">WARNING</font></html>"),
        FAILED(4, "<html><font color=\"red\">FAILED</font></html>");
        private String _labelTxt;
        private int _index;

        private STATUS(int index, String txt) {
            this._index = index;
            this._labelTxt = txt;
        }

        public String getLabelTxt() {
            return _labelTxt;
        }

        public int getIndex() {
            return _index;
        }

    }
    /**
     * unique id
     */
    private String m_messageId;
    private int m_taskOrder;
    private String m_projectId;
    private String m_threadName;
    private STATUS m_status;
    private Date m_startTime;
    private Date m_stopTime;
    private String m_callService;
    private LogLine m_startLine;
    private LogLine m_stopLine;
    private LinkedTreeMap m_paramObject;
    private ArrayList<LogLine> m_trace;
    private int m_otherTasksInRun;
    private String m_dataSet;
    private int m_nbLine;

    private ArrayList<Long> m_timeStamp;

    private ArrayList<Integer> m_nbOtherTaskMoment;

    public void updateNbTask(long instant, int nbTask) {
        long from = instant - m_startTime.getTime();
        int index = m_nbOtherTaskMoment.size() - 1;
        if (index >= 0 && nbTask == m_nbOtherTaskMoment.get(index)) {//compact same size 
            m_timeStamp.remove(index);
            m_nbOtherTaskMoment.remove(index);
        }
        m_timeStamp.add(from);
        m_nbOtherTaskMoment.add(nbTask);
        if (m_otherTasksInRun < nbTask) {
            m_otherTasksInRun = nbTask;
        }
    }

    public int getTaskOrder() {
        return m_taskOrder;
    }

    public LogTask(int fileIndex, String messageId) {
        this.m_messageId = messageId;
        m_trace = new ArrayList();
        m_otherTasksInRun = 0;
        m_dataSet = "";
        m_startLine = new LogLine(fileIndex, -1, "");
        m_stopLine = new LogLine(fileIndex, -1, "");
        m_taskOrder = -1;
        m_projectId = "";

        m_timeStamp = new ArrayList();
        m_nbOtherTaskMoment = new ArrayList();
        m_startTime = Calendar.getInstance().getTime();
        m_nbLine = 0;

    }

    public void setTaskOrder(int order) {
        m_taskOrder = order;
    }

    public String getDataSet() {
        return m_dataSet;
    }

    public void setDataSet(String dataSet) {
        this.m_dataSet = dataSet;
    }

    public String getProjectId() {
        return m_projectId;
    }

    public void setProjectId(String projectId) {
        this.m_projectId = projectId;
    }

    public void addLine(int fileIndex, long index, String line, Date date, STATUS status) {
        if (status == null) {
            if (line.contains(ERROR_LOG)) {
                setStatus(LogTask.STATUS.FAILED);
            } else if (line.contains(WARN_LOG)) {
                setStatus(LogTask.STATUS.WARNING);
            }
        } else {
            this.setStatus(status);
        }

        if (m_taskOrder > 0 && date != null && !line.contains(this.m_threadName) && !line.contains("Calling")) {
            m_logger.error("XXX thread name different {}, index = {}", this, index);
        }
        LogLine ll = new LogLine(fileIndex, index, line);
        m_trace.add(ll);
        m_nbLine++;
        if (m_trace.size() > MAX_LINE) {//more than 1000 line
            //write in Json
            TaskInJsonCtrl.getInstance().writeTaskTrace(this);
            m_trace = new ArrayList<>();//empty it
        }
        if (date != null) {
            m_stopTime = date;
            m_stopLine = ll;
        }//when date == null, don't add as stop line, because they are broken lines
    }

    /**
     * in order to gain memory
     */
    public void emptyTrace() {
        this.m_trace = null;
    }

    LogLine removeLastLine() {
        if (m_trace.size() > 0) {
            m_nbLine--;
            return m_trace.remove(m_trace.size() - 1);//@todo how restore last time
        } else {
            return null;
        }
    }

    public void setStopLine(int fileIndex, long index, String line) {
        LogLine ll = new LogLine(fileIndex, index, line);
        m_stopLine = ll;
    }

    public ArrayList<LogLine> getTrace() {
        return m_trace;
    }

    public void setTrace(ArrayList<LogLine> trace) {
        this.m_trace = trace;
    }

    
    public int getNbLine() {
        return m_nbLine;
    }

    public void setStatus(STATUS status) {
        if (m_status == null) {
            m_status = status;
        } else if (status.compareTo(m_status) > 0) {//status after m_status
            m_status = status;
        }
    }

    public void setStartLine(int fileIndex, long index, String startLine, Date date) {
        this.m_startLine = new LogLine(fileIndex, index, startLine);
        this.m_startTime = date;
    }

//    public void setStopLine(long index, String stopLine) {
//        this.m_stopLine = new LogLine(index, stopLine);
//    }
    public void setThreadName(String threadName) {
        this.m_threadName = threadName;
    }

    public void setCallService(String callService) {
        this.m_callService = callService;
    }

    public void setRequestParam(LinkedTreeMap paramMap) {
        this.m_paramObject = paramMap;
    }

    public String getMessageId() {
        return m_messageId;
    }

    public String getThreadName() {
        return m_threadName;
    }

    public STATUS getStatus() {
        return m_status;
    }

    public Date getStopTime() {
        return m_stopTime;
    }

    public Date getStartTime() {
        return m_startTime;
    }

    public long getDuration() {
        return m_stopTime.getTime() - m_startTime.getTime();
    }

    public String getCallService() {
        return m_callService;
    }

    public JTree getParamTree() {
        DefaultMutableTreeNode top
                = new DefaultMutableTreeNode(this.m_messageId);
        createParameterTree(this.m_paramObject, top, 0);
        JTree tree = new JTree(top);
        return tree;
    }

    private int createParameterTree(LinkedTreeMap params, DefaultMutableTreeNode parent, int childIndex) {
        final int PARMETER_STEP_LIMIT = 9000;//for recursion method
        if (childIndex > PARMETER_STEP_LIMIT) {
            return childIndex;
        }
        if (params == null) {
            return childIndex;
        }
        int index = childIndex;
        DefaultMutableTreeNode root = parent;
        DefaultMutableTreeNode child;
        for (Object key : params.keySet()) {
            child = new DefaultMutableTreeNode(key);

            Object value = params.get(key);
            if (value instanceof LinkedTreeMap) {
                root.add(child);
                index = createParameterTree((LinkedTreeMap) value, child, index++);
            } else {
                if (value instanceof ArrayList) {
                    ArrayList valueList = (ArrayList) value;
                    if (!valueList.isEmpty() && valueList.get(0) instanceof LinkedTreeMap) {
                        root.add(child);
                        for (Object item : valueList) {
                            index = createParameterTree((LinkedTreeMap) item, child, index++);
                        }
                    } else {
                        String node = (child + ":  " + value);
                        root.add(new DefaultMutableTreeNode(node));
                    }
                } else {
                    String node = (child + ":  " + value);
                    root.add(new DefaultMutableTreeNode(node));
                }
            }

        }
        return index;
    }

    public int getNbParallelTask() {
        return m_otherTasksInRun;
    }

    public void setNbOtherTasksInRun(int nb) {
        this.m_otherTasksInRun = nb;
    }

    public ArrayList<Long> getTimeStamp() {
        return m_timeStamp;
    }

    public ArrayList<Integer> getNbOtherTaskMoment() {
        return m_nbOtherTaskMoment;
    }

    public LogLine getStartLine() {
        return m_startLine;
    }

    public LogLine getStopLine() {
        return m_stopLine;
    }

    public String getStartInfo() {
        String time = Utility.formatTime(m_startTime);
        String projectId = (m_projectId.length() > 0) ? "project_id:" + m_projectId + ", " : "";
        String result = time + "[" + m_taskOrder + "]" + m_callService + " start (" + projectId + m_dataSet + " ID:" + m_messageId + ")";
        return result;
    }

    public String getStopInfo() {
        String time = Utility.formatTime(m_stopTime);
        String projectId = (m_projectId.length() > 1) ? "project_id:" + m_projectId + " " : "";
        String result = time + "[" + m_taskOrder + "]" + m_callService + " end (ID:" + m_messageId + ")";
        return result;
    }

    @Override
    public String toString() {
        return "LogTask{" + "m_messageId=" + m_messageId + ", m_threadName=" + m_threadName + ", m_status=" + m_status + ", m_taskOrder=" + m_taskOrder
                + ", m_nbLine=" + m_nbLine + '}';

    }

    public class LogLine {

        public long index;
        public String line;
        public int fileIndex;

        /**
         *
         * @param index
         * @param line
         * @param fIndex , index of the debug log file
         */
        public LogLine(int fIndex, long index, String line) {
            this.fileIndex = fIndex;
            this.index = index;
            this.line = line;
        }

        public String toString() {
            if (fileIndex == -1) {
                return "[" + this.index + "]" + this.line;
            } else {
                return "[" + fileIndex + "." + this.index + "]" + this.line;
            }

        }

    }
}
