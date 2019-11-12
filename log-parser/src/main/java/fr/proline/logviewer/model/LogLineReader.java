/*
 * @cea 
 * @http://www.profiproteomics.fr
 * created date: 7 oct. 2019
 */
package fr.proline.logviewer.model;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import fr.proline.logviewer.model.LogTask.LogLine;
import fr.proline.logviewer.txt.TaskStartStopTraceWriter;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ignore: thread name is [main] or [Timer-PurgeTempDir] or [ForkJoinPoolXXXXX]
 * ignore: source is java class PeakelsDetector.class
 *
 * @author Karine XUE at CEA
 */
public class LogLineReader {

    protected static final Logger m_logger = LoggerFactory.getLogger(LogLineReader.class);
    private HashMap<String, LogTask> m_msgId2TaskMap;
    private HashMap<String, LogTask> m_thread2TaskMap;
    private ArrayList<String> m_thread2Ignore;
    private ArrayList<LogTask> m_taskInOrder;
    private ArrayList<LogTask> m_taskInRun;
    private Matcher m_lastBeginMatch;
    private Stack<String> m_noTreatLine;
    private Stack<Long> m_noTreatLineIndex;
    private DATE_FORMAT m_dateFormat;
    public static final String regex_logLevel_9 = "(\\bDEBUG|\\bWARN |\\bERROR|\\bINFO )";
    final static String ERROR_LOG = "ERROR";
    final String regex_content_Task_ID = "ID:([\\w-]+)";
    final String regex_consumerTaskKey1 = "Consumer selector string:";
    final String regex_ignoreSource = "PeakelsDetector";
    //final String regex_consumerTaskKey2 = "Entering Consumer receive loop";
    final String regex_consumerTaskKey3 = "Entering ExpiredMessage Consumer receive loop";
    final String regex_consumerTaskKey4 = "Entering Notification Topic Publisher send loop";
    LogTaskTrace m_trace;
    private boolean m_hasNewTrace;
    private TaskStartStopTraceWriter m_traceWriter;

    public LogLineReader(TaskStartStopTraceWriter traceWriter, DATE_FORMAT dateFormat) {
        m_trace = new LogTaskTrace();
        m_hasNewTrace = false;
        m_traceWriter = traceWriter;
        long start = System.currentTimeMillis();
        m_dateFormat = dateFormat;
        m_msgId2TaskMap = new HashMap();
        m_thread2TaskMap = new HashMap<>();
        m_thread2Ignore = new ArrayList();
        m_taskInOrder = new ArrayList();
        m_taskInRun = new ArrayList<>();
        m_noTreatLine = new Stack();
        m_noTreatLineIndex = new Stack<>();
    }

    public String getNewTraceHtmlFormat() {
        m_hasNewTrace = false;
        return m_trace.getNewTraceHtmlFormat();
    }

    public ArrayList<String> getRestTraceHtmlFormat() {
        return m_trace.getRestTraceHtmlFormat();
    }

    public String getAllTraceHtmlFormat() {
        return m_trace.getAllTraceHtmlFormat();
    }

    public boolean isHasNewTrace() {
        return m_hasNewTrace;
    }

    private void push(Long i, String line) {
        m_noTreatLine.push(line);
        m_noTreatLineIndex.push(i);
    }

    private void pop() {
        m_noTreatLine.pop();
        m_noTreatLineIndex.pop();
    }

    public void showNoTreatedLines() {
        m_logger.debug("No treated lines List: size index stack= {}, lines stack ={} ", m_noTreatLine.size(), m_noTreatLineIndex.size());
        while (!m_noTreatLine.isEmpty()) {
            m_logger.debug("[" + m_noTreatLineIndex.pop() + "]," + m_noTreatLine.pop());
        }
    }

    public ArrayList<LogTask> getTasks() {
        return m_taskInOrder;
    }

    public void registerTask(String line, long index) throws ProlineException {
        final String regex_date_123 = "^(0[1-9]|[12]\\d|3[01])[ ]([\\w|.]{3,5})[ ](20\\d\\d)";
        final String regex_time_4567 = "([01]\\d|2[0-3]):([0-5]\\d):([0-5]\\d).(\\d\\d\\d)";
        //final String regex_threadPool_8 = "(\\[pool-\\d+-thread-\\d+\\])";
        final String regex_threadPool_8 = "(\\[[\\w|-]+\\])";

        final String regex_logSource_10 = "(f[\\w.$]+)";

        final String regex_content_Begin = "  - ";

        if (line.length() == 0) {
            return;
        }

        boolean isFound = false;
        final String regex_LogLine_pool_head = regex_date_123 + " " + regex_time_4567 + " " + regex_threadPool_8 + " " + regex_logLevel_9 + " " + regex_logSource_10 + regex_content_Begin;
        Pattern pattern = Pattern.compile(regex_LogLine_pool_head);
        Matcher matcher = pattern.matcher(line);
        boolean isNotMatch = false;

        if (matcher.find()) {//normal head begin with date
            m_lastBeginMatch = matcher;//remain it for matchCallingService
            push(index, line);
            Date date = getDate(matcher);
            String threadName = matcher.group(8);
            int cutPosition = matcher.end();
            String string2Analyse = line.substring(cutPosition);
            LogTask existTask;
            if (threadName.equals("[main]") || threadName.equals("[Timer-PurgeTempDir]")) {//special task to ignore
                existTask = m_thread2TaskMap.get(threadName);
                if (existTask != null) {
                    existTask.addLine(index, line, date);
                    pop();
                } else {
                    createOtherTask(index, line, threadName, date);
                }
            } else if (threadName.contains("ForkJoinPool")) {//special task to ignore
                pop();
                return; //ignore, 
            } else {//normal task begin here
                existTask = m_thread2TaskMap.get(threadName);//a thread has not finished by marker
                if (existTask != null) {//task already created by "handling"
                    isNotMatch = !(matchTaskEnd(index, line, string2Analyse, date, threadName));
                    if (isNotMatch) {//task alreday exist but thread replaced by a new task begin
                        isNotMatch = !matchTaskStart(index, line, string2Analyse, date, threadName);
                    }
                    if (isNotMatch) {//@todo here, BUG possible: when a thread has not finished by end key word, pool match, but task don't mache, not match, 
                        pop();
                        if (line.contains(ERROR_LOG)) {
                            existTask.setStatus(LogTask.STATUS.WARNING);
                        }
                        existTask.addLine(index, line, date);
                    }
                } else {//task has not created/existed}
                    if (string2Analyse.contains(this.regex_consumerTaskKey1)
                            || string2Analyse.contains(this.regex_consumerTaskKey3) || string2Analyse.contains(this.regex_consumerTaskKey4)) {
                        createOtherTask(index, line, threadName, date);
                    } else {
                        isNotMatch = !matchTaskStart(index, line, string2Analyse, date, threadName);
                        if (isNotMatch) {
                            isNotMatch = !matchTaskEnd(index, line, string2Analyse, date, threadName);//a task may be launched in a privous log file
                            if (isNotMatch) {
//                                LogTask task = m_thread2TaskMap.get(threadName);
//                                if (task != null) {
//                                    task.addLine(index, line, date);
//                                    pop();
//                                } else
                                {
                                    String source = matcher.group(10);
                                    if (!source.contains(regex_ignoreSource)) {
                                        isNotMatch = !matchSubTaskPeakel(string2Analyse, threadName);
                                        //if (isNotMatch) {//line can't be linked to a task is in stack without pop
                                    } else {//contains regex_ignoreSource
                                        pop();
                                    }
                                }
                            }
                        }
                    }
                }//end other match
            }
        } else {//line does not begin with a date & pool thread
            //group 1 = service,2 = parameter,3=message id
            final String regex_content_with_parame = "Calling service \\[([\\w./]+)\\] with JSON Request \\[(\\{[\\w\\W]+\\})\\] \\(##Message##_ID:([\\w-]+)\\)";
            //group 1= service,2=nothing, 3 = message id;
            final String regex_content_no_parame = "Calling BytesMessage Service \\[([\\w.\\/]+)\\] ()\\(##Message##_ID:([\\w-]+)\\)";
            isNotMatch = !matchCallingService(regex_content_with_parame, index, line);
            if (isNotMatch) {
                isNotMatch = !matchCallingService(regex_content_no_parame, index, line);
            }
            if (isNotMatch) {
                String threadName = m_lastBeginMatch.group(8);
                LogTask task = m_thread2TaskMap.get(threadName);
                if (task != null) {
                    task.addLine(index, line, null);//don't set stop line, because they are break line
                } else {
                    push(index, line);
                    //throw new ProlineException("line can't link with a task. " + line);
                    //m_logger.info("line {} can't link with a task, {}", index, line);
                }
            }

        }

    }

    private void createOtherTask(long index, String line, String threadName, Date date) {
        LogTask newTask = new LogTask(threadName + "_" + Math.random());
        //don't add in this.m_taskInRun, because it has not end key word
        newTask.setThreadName(threadName);
        newTask.setStartLine(index, line, date);
        newTask.addLine(index, line, date);
        pop();
        m_thread2TaskMap.put(threadName, newTask);
        // addTask(newTask);
    }

    private boolean matchSubTaskPeakel(String line, String threadName) {
        final String lcmsBegin = "Detecting LC-MS maps...";
        final String lcmsEnd = "Peakel stream successfully published !";
        Pattern pattern = Pattern.compile(lcmsBegin);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            m_thread2Ignore.add(threadName);
            pop();
            return true;
        } else {
            matcher = Pattern.compile(lcmsEnd).matcher(line);
            if (matcher.find()) {
                m_thread2Ignore.remove(threadName);
                pop();
                return true;
            } else {
                if (m_thread2Ignore.contains(line)) {
                    pop();
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    private void addTask(LogTask task) {
        task.setTaskOrder(m_taskInOrder.size());
        m_taskInOrder.add(task);
        //a new task will to print after calling service, but not at the first JMS request
    }

    private void newTask(LogTask task) {
        String newLine = m_trace.taskStart(task);
        if (m_traceWriter != null) {
            m_traceWriter.addLine(newLine);
        }
        m_hasNewTrace = true;
    }

    private void removeTask(LogTask task) {
        m_taskInRun.remove(task);
        m_msgId2TaskMap.remove(task.getMessageId());
        m_thread2TaskMap.remove(task.getThreadName());
        String newLine = m_trace.taskStop(task);
        if (m_traceWriter != null) {
            m_traceWriter.addLine(newLine);
        }
        this.m_hasNewTrace = true;

    }

    /**
     *
     * @param regex_calling
     * @param index
     * @param line
     * @return
     * @throws ProlineException
     */
    private boolean matchCallingService(String regex_calling, long index, String line) throws ProlineException {
        //group 1 = service,2 = parameter,3=message id
        //final String regex_content_parame = "Calling service \\[([\\w./]+)\\] with JSON Request \\[(\\{[\\w\\W]+\\})\\] \\(##Message##_ID:([\\w-]+)\\)";
        //group 1= service,2=nothing, 3 = message id;
        //final String regex_content_parame2 = "Calling BytesMessage Service \\[([\\w.\\/]+)\\] ()\\(##Message##_ID:([\\w-]+)\\)";
        Pattern pattern = Pattern.compile(regex_calling);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String service = matcher.group(1);
            String parameter = matcher.group(2);
            String messageId = matcher.group(3);

            LogTask task = m_msgId2TaskMap.get(messageId);
            Date date = getDate(m_lastBeginMatch);//can verify if  pool name in this match is the same in m_msgId2TaskMap
            if (task == null) {
                task = new LogTask(messageId);
                m_msgId2TaskMap.put(messageId, task);
                String threadName = m_lastBeginMatch.group(8);
                addTask(task);
                m_taskInRun.add(task);
                task.setThreadName(threadName);
                task.setStartLine(index, line, date);
                updateTaskInRun(task);
                LogTask wrongTask = m_thread2TaskMap.get(threadName);
                if (wrongTask != null) {
                    LogLine lastLine = wrongTask.removeLastLine();
                    task.addLine(index - 1, m_lastBeginMatch.group(), date);
                    wrongTask.setStatus(LogTask.STATUS.FAILED);
                    m_thread2TaskMap.remove(threadName);
                } else {
                    task.addLine(index - 1, m_noTreatLine.pop(), date);//the head(time, thread) of break line which begin with calling service, suppose index is last index
                    m_noTreatLineIndex.pop();
                }
                m_thread2TaskMap.put(threadName, task);
            }
            task.setCallService(service);
            //retrive parameter by Gson ;
            if (!parameter.isEmpty()) {
                Gson gson = new Gson();
                LinkedTreeMap paramMap = new LinkedTreeMap();
                paramMap = gson.fromJson(parameter, paramMap.getClass());
                //end retrive
                task.setRequestParam(paramMap);
            }
            task.setStatus(LogTask.STATUS.RUNNING);
            task.addLine(index, line, date);
            //m_logger.debug("OK \"{}\" now is used ", m_lastLine);
            matchProjectId(parameter, task);
            newTask(task);
            return true;
        }
        return false;
    }

    private void matchProjectId(String line, LogTask task) {
        final String regex_projectId = "\"project_id\":([\\d]+)[,]?";
        final String regex_dataSet = "(\"result_summary_id\"|\"result_summary_ids\"|\"result_set_ids\"|\"result_set_id\"|\"dataset_id\")(:[\\[]?[\\d,]+[\\]]?)[,]?";
        //regex to reuse //("result_summary_id"|"result_summary_ids"|"result_set_ids"|"result_set_id"|"dataset_id")(:)([\[]?[\d,]+[\]]?)[,]?
        Pattern pattern = Pattern.compile(regex_projectId);
        Matcher matcher = pattern.matcher(line);
        String result;
        if (matcher.find()) {
            result = matcher.group(1);
            task.setProjectId(result);
            pattern = Pattern.compile(regex_dataSet);
            matcher = pattern.matcher(line);
            if (matcher.find()) {
                result = matcher.group(1) + matcher.group(2);
                task.setDataSet(result);
            }
        }

    }

    private boolean matchTaskEnd(long index, String line2add, String line2Analyse, Date date, String threadName) throws ProlineException {
        //final String regex_content_task_end = "JMS response to [\\w]+ sent \\(##Message##_" + regex_content_Task_ID + "\\)";
        final String regex_content_task_end_2format = "^JMS [r|R]esponse to [\\w\\W]+ID:([\\w-]+)";//group 1 = message id
        //final String regex_peakel_end = "BULK insert of";
        Pattern pattern = Pattern.compile(regex_content_task_end_2format, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(line2Analyse);
        LogTask task;
        if (matcher.find()) {
            String messageId = matcher.group(1);//
            //m_thread2TaskMap|m_taskInRun has non sens, so don't treat
            task = m_msgId2TaskMap.get(messageId);
            //m_logger.debug("GGGGGGGGGGGGGGGGGG getTask {} for msgId={}", task, messageId);
            if (task == null) {
                task = new LogTask(messageId);
                task.setThreadName(m_lastBeginMatch.group(8));
                task.setStartLine(index, line2add, date);
                m_msgId2TaskMap.put(messageId, task);
                addTask(task);
            }

            task.setStatus(LogTask.STATUS.FINISHED);
            task.addLine(index, line2add, date);
            task.setStopLine(index, line2Analyse);
            removeTask(task);
            pop();
            return true;
        } else {
            return false;
        }
    }

    private boolean matchTaskStart(long index, String line2add, String line2Analyse, Date date, String threadName) throws ProlineException {
        String test = "Handling Request JMS Message [ID:7ce3cec3-f3e7-11e9-8dbc-7175f01018fd]";
        final String regex_content_JMS_BEGIN = "Handling [\\w]+ JMS Message \\[" + regex_content_Task_ID + "\\]";//has a group 1 in regex_content_Task_ID
        Pattern pattern = Pattern.compile(regex_content_JMS_BEGIN, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(line2Analyse);
        LogTask task;
        if (matcher.find()) {
            String messageId = matcher.group(1);
            task = new LogTask(messageId);
            m_msgId2TaskMap.put(messageId, task);
            addTask(task);
            m_taskInRun.add(task);
            task.setStatus(LogTask.STATUS.RUNNING);
            task.setStartLine(index, line2Analyse, date);
            task.setThreadName(threadName);
            //verify(threadName, messageId);
            {
                LogTask existTask = this.m_thread2TaskMap.get(threadName);
                if (existTask == null) {//normal state
                    this.m_thread2TaskMap.put(threadName, task);
                } else {//msgId already exist, means one task not finished, but the thread is distribute on a new task
                    this.m_thread2TaskMap.replace(threadName, task);
                    //therminate task with msgId
                    existTask.setStatus(LogTask.STATUS.FAILED);
                    m_taskInRun.remove(existTask);
                }
            }
            task.addLine(index, line2add, date);
            pop();
            updateTaskInRun(task);
            return true;
        }
        return false;

    }

    public void setDateFormat(DATE_FORMAT dateFormat) {
        m_dateFormat = dateFormat;
    }

    public enum DATE_FORMAT {
        NORMAL, SHORT
    };

    private Date getDate(Matcher matcher) throws ProlineException {
        final String LOG_DATE_FORMAT = "dd MMM yyyy HH:mm:ss.SSS";

        String dateString = matcher.group(1) + " " + matcher.group(2) + " " + matcher.group(3) + " "
                + matcher.group(4) + ":" + matcher.group(5) + ":" + matcher.group(6) + "." + matcher.group(7);
        SimpleDateFormat dateF;
        switch (m_dateFormat) {
            case NORMAL:
                dateF = new SimpleDateFormat(LOG_DATE_FORMAT);
                break;
            case SHORT:
                DateFormatSymbols symbols = new DateFormatSymbols(new Locale("en", "US"));
                dateF = new SimpleDateFormat(LOG_DATE_FORMAT, symbols);
                break;
            default:
                throw new ProlineException(m_dateFormat.name());
        }
        //
        try {
            Date date = dateF.parse(dateString);
            //m_logger.debug("date {}, Date is {}, time is {}", dateString, date, date.getTime());
            return date;
        } catch (ParseException ex) {
            throw new ProlineException(" in log file date format is: " + dateString + ", dateFormat is " + m_dateFormat, ex);
        }
    }

    private void updateTaskInRun(LogTask task) {
        int size = m_taskInRun.size() - 1;//don't count itself
        for (LogTask taskInRun : m_taskInRun) {
            if (taskInRun.getNbParallelTask() < size) {
                taskInRun.setNbOtherTasksInRun(size);
            }
        }
    }

    class LogTaskTrace {

        ArrayList<LogTask> m_taskList;
        ArrayList<String> m_newTrace;
        String m_lineEnd = "\n";
        ArrayList<String> m_allTrace;

        public LogTaskTrace() {
            this.m_taskList = new ArrayList();
            m_newTrace = new ArrayList();
            m_allTrace = new ArrayList();
        }

        public void close() {
            m_taskList = null;
            m_newTrace = new ArrayList();
            m_allTrace = new ArrayList();
        }

        public synchronized String getNewTraceHtmlFormat() {
            return getTraceHtmlFormat(m_newTrace);
        }

        public String getAllTraceHtmlFormat() {
            return getTraceHtmlFormat(m_allTrace);
        }

        private ArrayList<String> getRestTraceHtmlFormat() {
            ArrayList<String> result = new ArrayList<>();
            String lineHtml;
            for (String line : m_newTrace) {
                lineHtml = line.replaceAll("\n", "<br>");
                result.add(lineHtml);
            }
            m_newTrace = new ArrayList<>();
            return result;
        }

        private String getTraceHtmlFormat(ArrayList<String> traceList) {
            String result = "";
            String lineHtml;
            for (String line : traceList) {
                lineHtml = line.replaceAll("\n", "<br>");
                result += lineHtml;
            }
            m_newTrace = new ArrayList<>();
            return result;
        }

        public String taskStart(LogTask task) {
            String startMark = "|";
            String startAddMark = "|";
            ArrayList<LogTask> tasks2Remove = new ArrayList<>();
            LogTask oneTask;
            for (int i = 0; i < m_taskList.size(); i++) {
                oneTask = m_taskList.get(i);
                if (oneTask.getStatus().equals(LogTask.STATUS.FINISHED)
                        || oneTask.getStatus().equals(LogTask.STATUS.FINSHED_WARN)
                        || oneTask.getStatus().equals(LogTask.STATUS.FAILED)) {
                    startMark += "/" + oneTask.getTaskOrder();
                    tasks2Remove.add(oneTask);
                } else {
                    startMark += "|";
                    startAddMark += "|";
                }
            }

            for (LogTask t : tasks2Remove) {
                m_taskList.remove(t);
            }
            m_taskList.add(task);
            String newLine = (startMark + "\\" + m_lineEnd)
                    + (startMark + "|+" + task.getStartInfo() + m_lineEnd);
            m_newTrace.add(newLine);
            m_allTrace.add(newLine);
            return newLine;
        }

        public String taskStop(LogTask task) {
            String startMark = "|";
            String removeMark = "|";
            LogTask oneTask;
            for (int i = 0; i < m_taskList.size(); i++) {
                oneTask = m_taskList.get(i);
                startMark += "|";
                if (oneTask.equals(task)) {
                    String number = (i == m_taskList.size() - 1) ? "" : "" + oneTask.getTaskOrder();
                    removeMark += "/" + number;
                } else {

                    removeMark += "|";

                }
            }
            m_taskList.remove(task);
            String newLine = (startMark + "-" + task.getStopInfo() + m_lineEnd)
                    + (removeMark + m_lineEnd);
            m_newTrace.add(newLine);
            m_allTrace.add(newLine);
            return (newLine);
        }
    }

}
