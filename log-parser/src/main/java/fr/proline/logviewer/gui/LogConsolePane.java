/*
 * Copyright (C) 2019 VD225637
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
 * created date: 7 oct. 2019
 */
package fr.proline.logviewer.gui;

import fr.proline.logviewer.model.LogTask.LogLine;
import java.awt.Dimension;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

/**
 * created date: 7 oct. 2019
 *
 * @author Karine XUE at CEA
 */
public class LogConsolePane extends JScrollPane {

    JTextPane m_logTextPane;
    StringBuffer m_stringBuiffer;

    public LogConsolePane() {
        this.setBorder(BorderFactory.createTitledBorder("Console"));
        this.setPreferredSize(new Dimension(600, 700));
        m_stringBuiffer = new StringBuffer("Log File Begin Here");
        m_logTextPane = new JTextPane();
        m_logTextPane.setContentType("text/html");
        m_logTextPane.setText(m_stringBuiffer.toString());
        m_logTextPane.getParagraphAttributes();
        this.getViewport().add(m_logTextPane);
    }

    void setData(ArrayList<LogLine> trace) {
        //void setData(ArrayList<String> trace) {
        if (trace == null) {
            m_logTextPane.setText("");
            return;
        }
        m_stringBuiffer.setLength(0);//reuse cretaed StringBuffer
        m_stringBuiffer.append("<html><body>");
        for (LogLine item : trace) {
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

            m_stringBuiffer.append("<div>" + lindeIndex + markerLine + "</div>");
        }
        m_stringBuiffer.append("</body></html>");
        m_logTextPane.setText(m_stringBuiffer.toString());
        m_logTextPane.setCaretPosition(0);
    }

    private String replaceLogLevelInColor(String srcString) {
        //public static final String regex_logLevel_9 = "(\\bDEBUG|\\bWARN |\\bERROR|\\bINFO )";
        String ds = srcString.replaceAll("DEBUG", "<font color=\"Green\">DEBUG</font>");
        String ws = ds.replaceAll("WARN", "<font color=\"orange\">WARN</font>");
        String es = ws.replaceAll("ERROR", "<font color=\"red\">ERROR</font>");
        String is = es.replaceAll("INFO", "<font color=\"Blue\">INFO</font>");
        return is;
    }

}
