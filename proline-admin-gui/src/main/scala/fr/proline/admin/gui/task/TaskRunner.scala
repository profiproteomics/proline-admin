package fr.proline.admin.gui.task

import com.typesafe.scalalogging.LazyLogging
import javafx.{ concurrent => jfxc }
import scalafx.application.Platform
import scalafx.stage.Stage
import scalafx.scene.Node
import scalafx.scene.layout.VBox
import scalafx.scene.control.TextArea
import scalafx.scene.control.Label
import scalafx.scene.{ Scene, Cursor }
import scalafx.scene.control.ScrollPane
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.util.ShowPopupWindow
import fr.proline.admin.service.user.CheckForUpdates
import fr.proline.admin.service.db.migration.UpgradeAllDatabases
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle
import fr.profi.util.scalafx.ScalaFxUtils
import scala.collection.mutable.Set
import scala.collection.mutable.Map

/**
 * Runs a background task disabling the `mainView` and main visible `glassPane`.
 * Shows status using `statusLabel`.
 *
 */
class TaskRunner(
    mainView: Node,
    glassPane: Node,
    statusLabel: Label) extends LazyLogging {

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
        mainView.getScene().setCursor(Cursor.Default)

        if (showPopup)
          this.get match {
            // Upgrade all databases task
            case (upgradeDbs, failedDbs) if (upgradeDbs.isInstanceOf[UpgradeAllDatabases]) => {
              var title = new Label {
                text = caption + " - Finished successfully."
                style = TextStyle.GREEN_ITALIC
              }
              var failedDbNames = "All databases have been upgraded successfully!"
              statusLabel.setStyle(TextStyle.GREEN_ITALIC)
              statusLabel.text = caption + " - Finished successfully."
              if (!failedDbs.asInstanceOf[Set[String]].isEmpty) {
                statusLabel.setStyle(TextStyle.ORANGE_ITALIC)
                statusLabel.text = caption + " - Finished but some databases has failed to migrate."
                title = new Label {
                  text = "Warning: Some databases has failed to migrate."
                  graphic = ScalaFxUtils.newImageView(IconResource.WARNING)
                  style = TextStyle.ORANGE_ITALIC
                }
                failedDbNames = s"Warning: Some databases has failed to migrate:\n${failedDbs.asInstanceOf[Set[String]].mkString("\n")}"
              }
              val textArea = new TextArea {
                text = failedDbNames
                prefHeight = 150
              }
              ShowPopupWindow(
                node = new VBox {
                  spacing = 5
                  children = Seq(textArea)
                },
                caption,
                stage,
                true)
            }
            // Check for updates task : database name => list of scripts to apply: state
            case (checkDbs, scriptsToApply,dbObjectNeedUpgrade) if (checkDbs.isInstanceOf[CheckForUpdates]) => {
              var title = new Label {
                text = caption + " - Finished successfully."
                style = TextStyle.GREEN_ITALIC
              }
              var scriptsText: String = "All databases are up to date."
              statusLabel.text = caption + " - Finished successfully."
              statusLabel.setStyle(TextStyle.GREEN_ITALIC)

              if (!scriptsToApply.asInstanceOf[Map[String, Map[String, String]]].isEmpty) {
                val str = new StringBuilder()
                str.append("Some updates are available:\n")
                scriptsToApply.asInstanceOf[Map[String, Map[String, String]]].foreach {
                  case (k, v) => {
                    str.append("# ").append(k).append("\n ")
                    v.foreach { case (k, v) => str.append(k).append(" : ").append(v).append("\n ") }
                  }
                }
                title = new Label {
                  text = caption + " - Finished . Some updates are available."
                  graphic = ScalaFxUtils.newImageView(IconResource.WARNING)
                  style = TextStyle.ORANGE_ITALIC
                }
                scriptsText = str.toString
                statusLabel.setStyle(TextStyle.ORANGE_ITALIC)
                statusLabel.text = caption + " - Finished . Some updates are available."

              } else if(dbObjectNeedUpgrade.asInstanceOf[Boolean]) {
                val message = "Some Proline Datastore data need update."
                title = new Label {
                  text = caption + " - Finished . Datastore data need update."
                  graphic = ScalaFxUtils.newImageView(IconResource.WARNING)
                  style = TextStyle.ORANGE_ITALIC
                }
                scriptsText = message
                statusLabel.setStyle(TextStyle.ORANGE_ITALIC)
                statusLabel.text = caption + " - Finished . Some updates are available."
              }

              val textArea = new TextArea {
                text = scriptsText
                prefHeight = 150
              }
              ShowPopupWindow(
                node = new VBox {
                  spacing = 5
                  children = Seq(textArea)
                },
                caption,
                stage,
                true)
            }
            // Other tasks
            case _ => {
              statusLabel.setStyle(TextStyle.GREEN_ITALIC)
              statusLabel.text = caption + " - Finished successfully."
              ShowPopupWindow(
                node = new Label(caption + " - Finished successfully."),
                caption,
                stage,
                false)
            }
          }
      }

      // Task is running
      override def running(): Unit = {
        showProgress(true)
        mainView.getScene().setCursor(Cursor.Wait)
        statusLabel.setStyle(TextStyle.BLUE_ITALIC)
        statusLabel.text = caption + " - In progress, please wait... "
        //TODO callback
      }

      // Task failed
      override def failed(): Unit = {

        showProgress(false)
        mainView.getScene().setCursor(Cursor.Default)
        statusLabel.setStyle(TextStyle.RED_ITALIC)
        statusLabel.text = caption + " - Failed."
        val t = Option(getException)
        t.foreach(_.printStackTrace())

        // Show dialog 
        if (showPopup) {
          val errortextArea = new TextArea {
            text = s"Operation failed. ${t.map("Exception: " + _.getClass).getOrElse("")}\n ${t.map(_.getMessage).getOrElse("")}"
            prefHeight = 80
          }
          ShowPopupWindow(
            errortextArea,
            caption,
            stage,
            false)
        }
      }

      // Task cancelled
      override def cancelled(): Unit = {
        showProgress(false)
        mainView.getScene().setCursor(Cursor.Default)
        statusLabel.setStyle(TextStyle.RED_ITALIC)
        statusLabel.text = caption + " - Cancelled."
        // Show dialog 
        if (showPopup)
          ShowPopupWindow(new Label(caption + " - Cancelled."), caption, stage, false)
      }
    }

    // Run the task as a daemon
    val th = new Thread(task, caption)
    th.setDaemon(true)
    th.start()

  }
}