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

import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

/**
 *
 * @author Karine XUE at CEA
 */
public class TaskConsolePane extends JScrollPane {

    JTextPane m_logTextPane;
    StringBuffer m_stringBuffer;
    LogViewControlPanel m_ctrl;

    public TaskConsolePane(LogViewControlPanel ctrl) {
        m_ctrl = ctrl;
        this.setBorder(BorderFactory.createTitledBorder("Console"));
        this.setPreferredSize(new Dimension(700, 700));
        m_stringBuffer = new StringBuffer("Log File Begin Here");
        m_logTextPane = new JTextPane();
        m_logTextPane.setContentType("text/html");
        m_logTextPane.setText(m_stringBuffer.toString());
        m_logTextPane.getParagraphAttributes();
        this.getViewport().add(m_logTextPane);
    }

    void setData(String trace) {
        m_logTextPane.setText(trace);
        m_logTextPane.setCaretPosition(0);
    }
}
