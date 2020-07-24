/*
 * @cea 
 * @http://www.profiproteomics.fr
 * created date: 7 oct. 2019
 */
package fr.proline.logparser.model;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import fr.proline.logparser.model.LogTask.LogLine;
import fr.proline.logparser.model.Utility.DATE_FORMAT;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
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
import org.openide.util.Exceptions;
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
    /**
     * List of thread name (ex pool-2-thread-3)
     */
    private ArrayList<String> m_thread2Ignore;
    /**
     * All tasks, List to reused as data
     */
    private ArrayList<LogTask> m_taskInOrder;
    /**
     * only no finished task
     */
    private ArrayList<LogTask> m_taskInRun;

    private Matcher m_lastBeginMatch;
    private Stack<String> m_noTreatLine;
    private Stack<Long> m_noTreatLineIndex;
    private DATE_FORMAT m_dateFormat;
    public static final String regex_logLevel_9 = "(\\bDEBUG|\\bWARN |\\bERROR|\\bINFO )";

    final String regex_content_Task_ID = "ID:([\\w-]+)";
    final String regex_consumerTaskKey1 = "Consumer selector string:";
    final String regex_ignoreSource = "PeakelsDetector";
    //final String regex_consumerTaskKey2 = "Entering Consumer receive loop";
    final String regex_consumerTaskKey3 = "Entering ExpiredMessage Consumer receive loop";
    final String regex_consumerTaskKey4 = "Entering Notification Topic Publisher send loop";
    LogTasksFlow m_flow;
    private boolean m_hasNewTrace;
    private TasksFlowWriter m_flowWriter;
    private TaskInJsonCtrl m_taskInJsonCtrl;
    private boolean m_isBigFile;
    private String m_fileName;
    private int m_fileIndex;
    File m_nonTreatedLineFile;

    public LogLineReader(String fileName, DATE_FORMAT dateFormat, boolean isBigFile, boolean stdout) {
        Utility.init();//create data directory if necessary
        m_fileName = fileName.replace(".txt", "");//remove .txt
        m_isBigFile = isBigFile;
        m_flow = new LogTasksFlow();
        m_hasNewTrace = false;
        m_flowWriter = new TasksFlowWriter(stdout);
        m_flowWriter.open(m_fileName);
        m_dateFormat = dateFormat;
        m_msgId2TaskMap = new HashMap();
        m_thread2TaskMap = new HashMap<>();
        m_thread2Ignore = new ArrayList();
        m_taskInOrder = new ArrayList();
        m_taskInRun = new ArrayList<>();
        m_noTreatLine = new Stack<>();
        m_noTreatLineIndex = new Stack<>();
        if (m_isBigFile) {
            m_taskInJsonCtrl = TaskInJsonCtrl.getInstance().getInstance();
            m_taskInJsonCtrl.init(fileName);
        }
        m_nonTreatedLineFile = new File(Utility.WORKING_DATA_DIRECTORY + File.separator + m_fileName + "_NoTreatedLine.txt");
        if (m_nonTreatedLineFile.isFile()) {
            m_nonTreatedLineFile.delete();
        }

    }

    public String getNewTrace() {
        m_hasNewTrace = false;
        return m_flow.getNewTrace();
    }

    public boolean isHasNewTrace() {
        return m_hasNewTrace;
    }

    private void push(long i, String line) {
        if (m_noTreatLine.size() > 100) {
            showNoTreatedLines();
        }
        m_noTreatLine.push(line);
        m_noTreatLineIndex.push(i);
    }

    private void pop() {
        m_noTreatLine.pop();
        m_noTreatLineIndex.pop();
    }

    public int getNoTreatLineCount() {
        return m_noTreatLine.size();
    }

    /**
     * each call will clean m_noTreatLine
     */
    public void showNoTreatedLines() {
        if (m_noTreatLine.size() == 0) {
            return;
        }
        File nonTreatedLineFile = new File(Utility.WORKING_DATA_DIRECTORY + File.separator + m_fileName + "_NoTreatedLine.txt");
//        m_logger.debug("No treated lines  : size index stack= {}, lines stack ={}, {} ",
//                m_noTreatLine.size(), m_noTreatLineIndex.size(), nonTreatedLineFile.getName());
        try {
            BufferedWriter bWriter = new BufferedWriter(new FileWriter(nonTreatedLineFile, true));//mode append
            while (!m_noTreatLine.isEmpty()) {
                bWriter.write("[" + m_noTreatLineIndex.pop() + "]," + m_noTreatLine.pop());
                bWriter.newLine();
            }
            bWriter.close();
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    public ArrayList<LogTask> getTasks() {
        return m_taskInOrder;
    }

    /**
     * entry point of analyse log file
     *
     * @param line
     * @param index
     * @param fileInde
     * @throws ProlineException
     */
    public void registerTask(int fileIndex, String line, long index) throws ProlineException {
        m_fileIndex = fileIndex;
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
                    existTask.addLine(m_fileIndex, index, line, date, null);
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
                        //set status
                        isNotMatch = !matchTaskStart(index, line, string2Analyse, date, threadName);
                    }
                    if (isNotMatch) {//@todo here, BUG possible: when a thread has not finished by end key word, pool match, but task don't mache 
                        pop();

                        existTask.addLine(m_fileIndex, index, line, date, null);
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
                if (m_lastBeginMatch != null) {
                    String threadName = m_lastBeginMatch.group(8);
                    LogTask task = m_thread2TaskMap.get(threadName);
                    if (task != null) {
                        task.addLine(m_fileIndex, index, line, null, null);//don't set stop line, because they are break line                    
                    }
                } else {
                    push(index, line);
                    //throw new ProlineException("line can't link with a task. " + line);
                    //m_logger.info("line {} can't link with a task, {}", index, line);
                }
            }

        }

    }

    private void createOtherTask(long index, String line, String threadName, Date date) {
        LogTask newTask = new LogTask(m_fileIndex, threadName + "_" + Math.random());
        //don't add in this.m_taskInRun, because it has not end key word
        newTask.setThreadName(threadName);
        newTask.setStartLine(m_fileIndex, index, line, date);
        newTask.addLine(m_fileIndex, index, line, date, null);
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
                if (m_thread2Ignore.contains(threadName)) {
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
        //a new task will be printed after calling service, but not at the first JMS request
    }

    private void newTask(LogTask task) {
        String newLine = m_flow.taskStart(task);
        if (m_flowWriter != null) {
            m_flowWriter.addLine(newLine);
        }
        m_hasNewTrace = true;
    }

    /**
     * close & clean memory
     */
    public void close() {
        m_flow.close();
        m_flow = null;
        m_hasNewTrace = false;
        m_flowWriter.close();
        long start = 0;
        m_dateFormat = null;
        m_msgId2TaskMap = null;
        m_thread2TaskMap = null;
        m_thread2Ignore = null;
        m_taskInOrder = null;
        m_taskInRun = null;
        m_noTreatLine = null;
        m_noTreatLineIndex = null;
    }

    /**
     * register non end task in it's file. then force some data sturcture = null
     */
    public void memoryClean() {
        if (m_isBigFile) {
            for (LogTask task : m_taskInRun) {
                m_taskInJsonCtrl.WriteTask(task);
                task.emptyTrace();
            }
        }
        m_taskInRun = null;
        m_msgId2TaskMap = null;
        m_thread2TaskMap = null;
    }

    private void removeTask(long time, LogTask task) {
        for (LogTask t : m_taskInRun) {
            t.updateNbTask(time, m_taskInRun.size() - 1);
        }
        m_taskInRun.remove(task);
        m_msgId2TaskMap.remove(task.getMessageId());
        m_thread2TaskMap.remove(task.getThreadName());
        String newLine = m_flow.taskStop(task);
        if (m_flowWriter != null) {
            m_flowWriter.addLine(newLine);
        }
        this.m_hasNewTrace = true;
        if (m_isBigFile) {
            m_taskInJsonCtrl.WriteTask(task);
            task.emptyTrace();
        }
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
                task = new LogTask(m_fileIndex, messageId);
                m_msgId2TaskMap.put(messageId, task);
                String threadName = m_lastBeginMatch.group(8);
                addTask(task);
                m_taskInRun.add(task);
                task.setThreadName(threadName);
                task.setStartLine(m_fileIndex, index, line, date);

                LogTask existTaskEnThread = m_thread2TaskMap.get(threadName);
                if (existTaskEnThread != null) {
                    LogLine lastLine = existTaskEnThread.removeLastLine();
                    task.addLine(m_fileIndex, index - 1, m_lastBeginMatch.group(), date, LogTask.STATUS.FINISHED_WARN);
                    m_thread2TaskMap.remove(threadName);
                    m_taskInRun.remove(existTaskEnThread);
                } else {
                    task.addLine(m_fileIndex, index - 1, m_noTreatLine.pop(), date, LogTask.STATUS.RUNNING);//the head(time, thread) of break line which begin with calling service, suppose index is last index
                    m_noTreatLineIndex.pop();
                }
                updateTaskInRun(task, date.getTime());
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
            task.addLine(m_fileIndex, index, line, date, LogTask.STATUS.RUNNING);
            //m_logger.debug("OK \"{}\" now is used ", m_lastLine);
            matchMetaInfo(service, parameter, task);
            newTask(task);
            return true;
        }
        return false;
    }

    private void matchMetaInfo(String service, String parameter, LogTask task) {
        final String regex_fileName = "\"[\\w|\\d| |_|.|-]+\"";
        final String regex_filePath = "[\\w|\\d| |_|.|-|\\\\|\\/]+";

        Pattern pattern;
        Matcher matcher;
        String result;
        if (service.contains("UserAccount")) {
            final String regex_login = "(\"login\":\"[\\w\\d]+\")";//group 1
            pattern = Pattern.compile(regex_login);
            matcher = pattern.matcher(parameter);
            if (matcher.find()) {
                result = matcher.group(1);
                task.setDataSet(result);
            }
        } else if (service.contains("RegisterRawFile")) {

            final String regex_rowFile = "(\"raw_file_identifier\":" + regex_fileName + "),";
            pattern = Pattern.compile(regex_rowFile);
            matcher = pattern.matcher(parameter);
            if (matcher.find()) {
                result = matcher.group(1);
                task.setDataSet(result);
            }
        } else if (service.contains("CreateProject")) {
            pattern = Pattern.compile("(\"name\":" + regex_fileName + "),");
            matcher = pattern.matcher(parameter);
            if (matcher.find()) {
                result = matcher.group(1);
                task.setDataSet(result);
            }
        } else if (parameter.contains("ImportMaxQuantResults")) {//not sure, has not been tested
            pattern = Pattern.compile("(\"result_files_dir\":" + regex_filePath + "),");
            matcher = pattern.matcher(parameter);
            if (matcher.find()) {
                result = matcher.group(1);
                task.setDataSet(result);
            }
        } else {
            final String regex_projectId = "\"project_id\":([\\d]+)[,]?";
            final String regex_dataSet = "(\"result_summary_id\"|\"result_summary_ids\"|\"result_set_ids\"|\"result_set_id\"|\"dataset_id\"|\"resultset_ids\"):([\\[]?[\\d,]+[\\]]?)[,]?";//group 1+3
            //regex to reuse //("result_summary_id"|"result_summary_ids"|"result_set_ids"|"result_set_id"|"dataset_id"|"resultset_ids":[\[]?[\d,]+[\]]?)[,]?

            final String regex_resultFile = "(\"result_files\"):\\[\\{\"(path)\":\"(" + regex_filePath + ")\",";
            //regex to reuse //("result_files"):\[{"(path)":"([\w|\d|\\|\/|.|-|_| ]+)",
            pattern = Pattern.compile(regex_projectId);
            matcher = pattern.matcher(parameter);
            if (matcher.find()) {
                result = matcher.group(1);
                task.setProjectId(result);
                pattern = Pattern.compile(regex_dataSet);
                matcher = pattern.matcher(parameter);
                if (matcher.find()) {
                    result = matcher.group(1) + ":" + matcher.group(2);
                    task.setDataSet(result);
                } else {
                    pattern = Pattern.compile(regex_resultFile);
                    matcher = pattern.matcher(parameter);
                    if (matcher.find()) {
                        //File file = new File(matcher.group(3));
                        result = matcher.group(1) + ":" + matcher.group(3);
                        task.setDataSet(result);
                    } else {
                        final String regex_name = "\"project_id\":[\\d]+,(\"[\\w|_]+\":\\d+,)*(\"name\":" + regex_fileName + "),";//group 2
                        //regex_name to reuse "project_id":[\d]+,("[\w|_]+":\d+,)*("name":"[\w| |_|.|-]+"),
                        pattern = Pattern.compile(regex_name);
                        matcher = pattern.matcher(parameter);
                        if (matcher.find()) {
                            result = matcher.group(2);
                            task.setDataSet(result);
                        } else {
                            final String regex_quant_channel = "\"project_id\":[\\d]+,(\"master_quant_channel_id\":[\\d]+),";
                            pattern = Pattern.compile(regex_quant_channel);
                            matcher = pattern.matcher(parameter);
                            if (matcher.find()) {
                                result = matcher.group(1);
                                task.setDataSet(result);
                            } else {
                                final String regex_export_rsm = "\"project_id\":\\d+,\"ds_id\":\\d+,\"rsm_id\":(\\d+)";
                                pattern = Pattern.compile(regex_export_rsm);
                                matcher = pattern.matcher(parameter);
                                result = "\"rsm_id\": [";
                                while (matcher.find()) {
                                    result += matcher.group(1) + ",";
                                }
                                result += "]";
                                task.setDataSet(result);
                            }
                        }
                    }
                }
            }//has project_id
        }
    }

//
    private boolean matchTaskEnd(long index, String line2add, String line2Analyse, Date date, String threadName) throws ProlineException {
        //final String regex_content_task_end = "JMS response to [\\w]+ sent \\(##Message##_" + regex_content_Task_ID + "\\)";
        final String regex_content_task_end_2format = "^JMS [r|R]esponse to [\\w\\W]+ID:([\\w-]+)";//group 1 = message id
        final String regex_task_end_no_client = "Request JMS Message has no 'JMSReplyTo' destination: cannot send JSON response to the client";
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
                task = new LogTask(m_fileIndex, messageId);
                task.setThreadName(threadName);
                task.setStartLine(m_fileIndex, index, line2add, date);
                m_msgId2TaskMap.put(messageId, task);
                addTask(task);
            }

            task.addLine(m_fileIndex, index, line2add, date, LogTask.STATUS.FINISHED);
            task.setStopLine(m_fileIndex, index, line2Analyse);
            removeTask(date.getTime(), task);
            pop();
            return true;
        } else {
            if (line2Analyse.contains(regex_task_end_no_client)) {
                task = m_thread2TaskMap.get(threadName);
                if (task != null) {
                    task.addLine(m_fileIndex, index, line2add, date, LogTask.STATUS.FINISHED_WARN);
                    task.setStopLine(m_fileIndex, index, line2Analyse);
                    removeTask(date.getTime(), task);
                    pop();
                    return true;
                }
            }
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
            task = new LogTask(m_fileIndex, messageId);
            m_msgId2TaskMap.put(messageId, task);
            addTask(task);
            m_taskInRun.add(task);
            task.setStatus(LogTask.STATUS.RUNNING);
            task.setStartLine(m_fileIndex, index, line2Analyse, date);
            task.setThreadName(threadName);
            //verify(threadName, messageId);
            {
                LogTask existTask = this.m_thread2TaskMap.get(threadName);
                if (existTask == null) {//normal state
                    this.m_thread2TaskMap.put(threadName, task);
                } else {//msgId already exist, means one task not finished, but the thread is distribute on a new task
                    this.m_thread2TaskMap.replace(threadName, task);
                    //therminate task with msgId
                    existTask.setStatus(LogTask.STATUS.FINISHED_WARN);
                    m_taskInRun.remove(existTask);
                }
            }
            task.addLine(m_fileIndex, index, line2add, date, LogTask.STATUS.RUNNING);
            pop();
            updateTaskInRun(task, date.getTime());
            return true;
        }
        return false;

    }

    public void setDateFormat(DATE_FORMAT dateFormat) {
        m_dateFormat = dateFormat;
    }

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
            throw new ProlineException("DataFormat Exception, in log file date format is: " + dateString + ", dateFormat is " + m_dateFormat, ex);
        }
    }

    private void updateTaskInRun(LogTask task, long time) {
        int size = m_taskInRun.size() - 1;//don't count itself
        for (LogTask taskInRun : m_taskInRun) {
            taskInRun.updateNbTask(time, size);
        }
    }

    class LogTasksFlow {

        ArrayList<LogTask> m_taskList;
        ArrayList<String> m_newTrace;
        String m_lineEnd = "\n";

        public LogTasksFlow() {
            this.m_taskList = new ArrayList();
            m_newTrace = new ArrayList();
        }

        public void close() {
            m_taskList = null;
            m_newTrace = new ArrayList();
        }

        private String getNewTrace() {
            String result = "";
            for (String line : m_newTrace) {
                result += line;
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
                        || oneTask.getStatus().equals(LogTask.STATUS.FINISHED_WARN)
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
            String newLine = (startMark + "\\" + "    " + task.getDataSet() + m_lineEnd)
                    + (startMark + "|+" + task.getStartInfo() + m_lineEnd);

            m_newTrace.add(newLine);
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
            return (newLine);
        }
    }

    class TasksFlowWriter {

        FileWriter m_outputFile;
        boolean m_stdout;
        String FILE_NAME_END = "_TasksFlow.txt";

        public TasksFlowWriter(boolean stdout) {
            m_stdout = stdout;
        }

        public void close() {
            try {
                if (m_outputFile != null) {
                    m_outputFile.close();
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        public void open(String file2Anaylse) {
            try {
                m_outputFile = new FileWriter(Utility.WORKING_DATA_DIRECTORY + File.separator + file2Anaylse + FILE_NAME_END);
                String head = "Analyse " + file2Anaylse;
                m_outputFile.write(head);
                if (m_stdout) {
                    System.out.println(head);
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        public void addLine(String s) {
            try {
                m_outputFile.write(s);
                if (m_stdout) {
                    System.out.print(s);
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

    }
}
