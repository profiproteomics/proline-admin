package fr.proline.admin.gui.process //TODO: rename/re-organize package

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.future
import scala.util.Failure
import scala.util.Success
import com.typesafe.scalalogging.slf4j.Logging
import fr.proline.admin.gui.Main
import fr.proline.admin.gui.component.panel.ButtonsPanel
import scalafx.application.Platform
import scalafx.scene.Cursor
import scalafx.scene.Cursor.sfxCursor2jfx
import scalafx.scene.control.Button
import scalafx.scene.control.ProgressIndicator
import scalafx.scene.image.ImageView
import scalafx.beans.property.BooleanProperty

/**
 * Run button's action asynchronously
 */
object LaunchAction extends Logging {

  def apply(
    actionButton: Button, //Array[Button] ?
    actionString: String,
    action: () => Unit) {

    synchronized {
      /** Add progress indicator to activated button */
      //    actionButton.style = "-fx-background-color: lightgray; "
      //    actionButton.styleClass -= ("mainButtons")
      //    actionButton.styleClass += ("activeButtons")
      actionButton.graphic = new ProgressIndicator {
        prefHeight = 20
        prefWidth = 20
      }
      Main.stage.scene().setCursor(Cursor.WAIT)
      //      ButtonsPanel.disableAll()

      /** Print relative command line in console panel */
      println("\n" + actionString)
    }

    /** Lauch action asynchronously then update enabled/disabled buttons*/
    val f = future { synchronized { action() } }

    /** Future's callback : when action is finished */
    f onComplete {

      case Success(_) => {
        synchronized {
          logger.info(s"Action '$actionString' finished with success.")
          println(s"""[ $actionString : <b>success</b> ]<br><br>""")
          _initialize()
          //          Platform.runLater {
          //            Main.stage.scene().setCursor(Cursor.DEFAULT)
          //            actionButton.graphic = new ImageView()
          //          }
          //          ButtonsPanel.computeButtonsAvailability()

          /*actionButton.style = " -fx-background-color: SlateGrey;"
          actionButton.styleClass -= ("activeButtons")
          actionButton.styleClass += ("mainButtons")*/
        }
      }

      case Failure(e) => {

        e match {
          case fxThread: java.lang.IllegalStateException => {
            logger.warn(fxThread.getLocalizedMessage())
            _initialize()
          }

          case _ => synchronized {
            logger.warn(s"Failed to run action [$actionString]", e)
            println("ERROR - " + e.getMessage)
            println(s"[ $actionString : finished with <b>error</b> ]<br>")
            _initialize()
          }
        }

      }
    }

    def _initialize() {
      Platform.runLater {
        actionButton.graphic = new ImageView()
        Main.stage.scene().setCursor(Cursor.DEFAULT)
      }
      //      ButtonsPanel.computeButtonsAvailability()
      ProlineAdminConnection.loadProlineConf(verbose = false) //workaround => correctly compute buttons' availability for SQLite (FIXME)

    }
  }
}