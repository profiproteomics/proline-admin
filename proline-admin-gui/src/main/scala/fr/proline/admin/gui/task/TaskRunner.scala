package fr.proline.admin.gui.task

import javafx.{ concurrent => jfxc }
import scalafx.application.Platform
import scalafx.scene.Node
import scalafx.stage.Stage

import scalafx.scene.control.Label
import scalafx.scene.{ Scene, Cursor }
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle
import fr.proline.admin.gui.wizard.util.HelpPopup
import fr.proline.admin.gui.Monitor
/**
 * Runs a background task disabling the `mainView` and main visible `glassPane`.
 * Shows status using `statusLabel`.
 */
class TaskRunner(
    mainView: Node,
    glassPane: Node,
    statusLabel: Label)(implicit stage: Stage) {

  /**
   * Run an operation on a separate thread. Return and wait for its completion,
   * then return result of running that operation.
   *
   * A progress indicator is displayed while running the operation.
   *
   * @param caption name for the thread (useful in debugging) and status displayed
   *                when running the task.
   * @param op      operation to run.
   * @param R type of result returned by the operation.
   * @return result returned by operation `op`.
   */
  def run[R](
    caption: String,
    op: => R): Unit = {

    def showProgress(progressEnabled: Boolean): Unit = {
      mainView.disable = progressEnabled
      glassPane.visible = progressEnabled
    }

    // Indicate task in progress
    Platform.runLater {
      showProgress(true)
      statusLabel.text = caption
    }

    val task: jfxc.Task[R] = new jfxc.Task[R] {
      override def call(): R = {
        op
      }
      
      //task succeeded
      override def succeeded(): Unit = {
        showProgress(false)
        mainView.getScene().setCursor(Cursor.DEFAULT)
        statusLabel.setStyle(TextStyle.GREEN_ITALIC)
        statusLabel.text = caption + " - Finished successfully."
        HelpPopup(caption, caption + " - Finished successfully.", Some(Monitor.stage), false)
        // Do callback 
      }
      
      //task is running
      override def running(): Unit = {
        showProgress(true)
        mainView.getScene().setCursor(Cursor.WAIT)
        statusLabel.setStyle(TextStyle.BLUE_ITALIC)
        statusLabel.text = caption + " - In progress, please wait... "
        // Do callback,
      }
      
      //task failed 
      override def failed(): Unit = {

        showProgress(false)
        mainView.getScene().setCursor(Cursor.DEFAULT)
        statusLabel.setStyle(TextStyle.RED_ITALIC)
        statusLabel.text = caption + " - Failed."
        val t = Option(getException)
        t.foreach(_.printStackTrace())
        
       //show popup 
        HelpPopup(caption,
          s"Operation failed.  ${t.map("Exception: " + _.getClass).getOrElse("")}\n"
            + s"${t.map(_.getMessage).getOrElse("")}",
          Some(Monitor.stage), false)
      }
      
      // task canceled
      override def cancelled(): Unit = {
        showProgress(false)
         mainView.getScene().setCursor(Cursor.DEFAULT)
        statusLabel.setStyle(TextStyle.RED_ITALIC)
        statusLabel.text = caption + " - Cancelled."
      }

    }
    
    //run the task as daemon 
    val th = new Thread(task, caption)
    th.setDaemon(true)
    th.start()
  }
}