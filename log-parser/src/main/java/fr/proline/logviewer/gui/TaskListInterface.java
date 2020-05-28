/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.proline.logviewer.gui;

import fr.proline.logviewer.model.LogTask;
import java.util.ArrayList;

/**
 *
 * @author KX257079
 */
public interface TaskListInterface {

    public void setData(ArrayList<LogTask> tasks, String fileName);
}
