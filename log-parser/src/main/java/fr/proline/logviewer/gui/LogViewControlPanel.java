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
 */
package fr.proline.logviewer.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

/**
 *
 * @author Karine XUE at CEA
 */
public class LogViewControlPanel extends LogControlPanel {

    private LogGuiApp m_ctrl;

    public LogViewControlPanel(LogGuiApp ctrl) {
        super();
        m_ctrl = ctrl;
        this.setBackground(Color.white);
        m_taskConsole = new TaskConsolePane();
        m_taskQueueView = new TaskListView(this);
        m_taskView = new TaskView();

        JSplitPane mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainPanel.setDividerLocation(300);
        JSplitPane contentBottomPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, m_taskView, m_taskConsole);
        contentBottomPanel.setDividerLocation(680);
        mainPanel.setTopComponent((JScrollPane) m_taskQueueView);
        mainPanel.setBottomComponent(contentBottomPanel);

        this.add(mainPanel, BorderLayout.CENTER);
        this.setSize(1400, 800);
    }

    @Override
    public boolean isBigFile() {
        return m_ctrl.isBigFile();
    }

    @Override
    public int getMaxLine2Show() {
        return Config.getMaxLine2Show();
    }

    @Override
    public void redo() {
        m_ctrl.redo();
    }

    @Override
    public void setProgress(int percent) {
        m_ctrl.setProgress(percent);
    }

    @Override
    public void setProgressBarVisible(boolean b) {
        m_ctrl.setProgressBarVisible(b);
    }

}
