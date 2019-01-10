package fr.proline.admin.gui.task

import javafx.{ concurrent => jfxc }
import scalafx.application.Platform
import scalafx.stage.Stage
import scalafx.scene.Node
import scalafx.scene.control.Label
import scalafx.scene.{ Scene, Cursor }
import fr.proline.admin.gui.util.ShowPopupWindow
import fr.proline.admin.gui.Monitor
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle

/**
 * Runs a background task disabling the `mainView` and main visible `glassPane`.
 * Shows status using `statusLabel`.
 *
 */
class TaskRunner(
  mainView: Node,
  glassPane: Node,
  statusLabel: Label) {

  /**
   * Run an operation on a separate thread. Return and wait for its completion,
   * then return result of running that operation.
   *
   * A progress indicator is displayed while running the operation.
   *
   * @param caption name for the thread (useful in debugging) and status displayed
   *                when running the task.
   * @param op operation to run.
   * @param R type of result returned by the operation.
   * @param showPopup show popup window.
   * @param stage the parent stage.
   * @return result returned by operation `op`.
   */
  def run[R](
    caption: String,
    op: => R,
    showPopup: Boolean = true,
    stage: Option[Stage] = Option(Monitor.stage)): Unit = {

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

      // Task succeeded
      override def succeeded(): Unit = {
        showProgress(false)
        mainView.getScene().setCursor(Cursor.DEFAULT)
        statusLabel.setStyle(TextStyle.GREEN_ITALIC)
        statusLabel.text = caption + " - Finished successfully."
        if (showPopup)
          ShowPopupWindow(caption + " - Finished successfully.", caption, stage, false)
        //TODO callback
      }

      // Task is running
      override def running(): Unit = {
        showProgress(true)
        mainView.getScene().setCursor(Cursor.WAIT)
        statusLabel.setStyle(TextStyle.BLUE_ITALIC)
        statusLabel.text = caption + " - In progress, please wait... "
        //TODO callback
      }

      // Task failed
      override def failed(): Unit = {

        showProgress(false)
        mainView.getScene().setCursor(Cursor.DEFAULT)
        statusLabel.setStyle(TextStyle.RED_ITALIC)
        statusLabel.text = caption + " - Failed."
        val t = Option(getException)
        t.foreach(_.printStackTrace())

        // Show popup
        if (showPopup)
          ShowPopupWindow(
            s"Operation failed. ${t.map("Exception: " + _.getClass).getOrElse("")}\n"
              + s"${t.map(_.getMessage).getOrElse("")}", caption,
            stage, false)
      }

      // Task cancelled
      override def cancelled(): Unit = {
        showProgress(false)
        mainView.getScene().setCursor(Cursor.DEFAULT)
        statusLabel.setStyle(TextStyle.RED_ITALIC)
        statusLabel.text = caption + " - Cancelled."
        // Show popup
        if (showPopup)
          ShowPopupWindow(caption + " - Cancelled.", caption, stage, false)
      }
    }

    // Run the task as a daemon
    val th = new Thread(task, caption)
    th.setDaemon(true)
    th.start()

  }
}