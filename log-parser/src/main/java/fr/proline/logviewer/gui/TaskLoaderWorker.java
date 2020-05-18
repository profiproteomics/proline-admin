package fr.proline.logviewer.gui;

import fr.proline.logviewer.model.LogTask;
import fr.proline.logviewer.model.Utility;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;

/**
 *
 * @author KX257079
 */
public class TaskLoaderWorker extends SwingWorker<String, Void> {

    ArrayList<LogTask.LogLine> m_traces;
    StringBuffer m_stringBuffer;
    ControlInterface m_ctrl;

    public TaskLoaderWorker(ArrayList<LogTask.LogLine> traces, ControlInterface ctrl) {
        m_ctrl = ctrl;
        m_traces = traces;
        m_stringBuffer = new StringBuffer();
    }

    @Override
    protected String doInBackground() throws Exception {
        m_stringBuffer.setLength(0);//reuse cretaed StringBuffer
        int traceSize = m_traces.size();
        int diffrent = 0;

        if (m_ctrl.isBigFile()) {
            int maxLine = Config.getMaxLine2Show();
            diffrent = traceSize - maxLine;
            if (diffrent > 0) {
                traceSize = maxLine;
            }
        }
        try {
            LogTask.LogLine item;
            for (int i = 0; i < traceSize; i++) {
                item = m_traces.get(i);
                String markerLine;
                String lindeIndex = "<font color=\"Gray\">[" + item.index + "]</font>:";
                String markerLine1 = item.line.replaceAll("<", "&lt;");
                String markerLine2 = markerLine1.replaceAll(">", "&gt;");
                String markerLine3 = replaceLogLevelInColor(markerLine2);
                if (markerLine3.contains("Calling service") || markerLine3.contains("Calling BytesMessage Service")) {
                    markerLine = "<font color=\"Blue\">" + markerLine3 + "</font>";
                } else {
                    markerLine = markerLine3;
                }

                m_stringBuffer.append("<div>" + lindeIndex + markerLine + "</div>");
                m_stringBuffer.length();
            }
            m_stringBuffer.append("<div> </div><div>Au total " + traceSize + " lines shown. </div>");
            if (diffrent > 0) {
                String fileName = m_ctrl.getAnalysedTaskName();
                m_stringBuffer.append("<div> ... </div><div> " + diffrent + " more lines, please refer to the file " + fileName);
            }
            return m_stringBuffer.toString();

        } catch (OutOfMemoryError e) {
            System.out.println(Utility.getMemory());
            System.out.println(e.getMessage() + "\n" + e.getLocalizedMessage() + "\n" + e.getCause());
            System.exit(1);
            return "";
        }

    }

    private String replaceLogLevelInColor(String srcString) {
        //public static final String regex_logLevel_9 = "(\\bDEBUG|\\bWARN |\\bERROR|\\bINFO )";
        String ds = srcString.replaceAll("DEBUG", "<font color=\"Green\">DEBUG</font>");
        String ws = ds.replaceAll("WARN", "<font color=\"orange\">WARN</font>");
        String es = ws.replaceAll("ERROR", "<font color=\"red\">ERROR</font>");
        String is = es.replaceAll("INFO", "<font color=\"Blue\">INFO</font>");
        return is;
    }

    @Override
    protected void done() {
        String trace;
        try {
            trace = this.get();
            m_ctrl.getConsole().setData(trace);
        } catch (InterruptedException ex) {
            m_ctrl.getConsole().setData(ex.getMessage());
        } catch (ExecutionException ex) {
            m_ctrl.getConsole().setData(ex.getMessage());
        }
    }

}
