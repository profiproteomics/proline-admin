package fr.proline.logviewer.gui;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

/**
 *
 * @author KX257079
 */
public class TaskFlowPane extends JPanel {
    
    JProgressBar m_progressBar;
    JTextPane m_textPane;
    
    public TaskFlowPane() {
        super(new BorderLayout());
        m_progressBar = new JProgressBar(0, 100);
        m_progressBar.setValue(0);
        m_progressBar.setStringPainted(true);
        this.add(m_progressBar, BorderLayout.PAGE_START);
        m_textPane = new JTextPane();
        this.add(new JScrollPane(m_textPane), BorderLayout.CENTER);
    }
    
    public void setProgress(int percent) {
        m_progressBar.setValue(percent);
    }

    public void setFlow(String buffer) {
        m_textPane.setText(buffer);
    }
}
