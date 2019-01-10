package fr.proline.admin.gui.process //TODO: rename/re-organize package

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import scalafx.application.Platform
import scalafx.scene.Cursor
import scalafx.scene.Cursor.sfxCursor2jfx
import scalafx.scene.Node
import scalafx.scene.control.Button
import scalafx.scene.control.ProgressIndicator
import scalafx.scene.image.ImageView

import fr.proline.admin.gui.Main

/**
 * Run button's action asynchronously
 */
object LaunchAction extends LazyLogging {

  def apply(
    actionButton: Button, //Array[Button] ?
    actionString: String,
    action: () => Unit,
    reloadConfig: Boolean = true,
    disableNode: Node = null) {

    synchronized {
      /** Add progress indicator to activated button */
      //    actionButton.style = "-fx-background-color: lightgray; "
      //    actionButton.styleClass -= ("mainButtons")
      //    actionButton.styleClass += ("activeButtons")
      actionButton.graphic = new ProgressIndicator {
        prefHeight = 20
        prefWidth = 20
      }

      if (disableNode != null) disableNode.disable = true

      Main.stage.scene().setCursor(Cursor.WAIT)
      //      ButtonsPanel.disableAll()

      /** Print relative command line in console panel */
      println("\n" + actionString)
    }

    /** Lauch action asynchronously then update enabled/disabled buttons*/
    val f = Future { synchronized { action() } }

    /** Future's callback : when action is finished */
    f onComplete {

      case Success(_) => {
        synchronized {
          logger.info(s"Action '$actionString' finished with success.")
          println(s"""[ $actionString : <b>success</b> ]<br><br>""")
          _initialize(reloadConfig)
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

      case Failure(t) => {

        t match {
          case fxThread: java.lang.IllegalStateException => {
            logger.warn(fxThread.getLocalizedMessage())
            _initialize(reloadConfig)
          }

          case _ => synchronized {
            logger.warn(s"Failed to run action [$actionString]", t)

            System.err.println("ERROR - " + t.getMessage)
            System.err.println(t.getMessage())

            println(s"[ $actionString : finished with <b>error</b> ]<br>")
            _initialize(reloadConfig)
          }
        }

      }
    }

    def _initialize(reloadConfig: Boolean = true) {
      Platform.runLater {
        actionButton.graphic = new ImageView()
        if (disableNode != null) disableNode.disable = false
        Main.stage.scene().setCursor(Cursor.DEFAULT)
      }
      //ButtonsPanel.computeButtonsAvailability()
      if (reloadConfig) ProlineAdminConnection.loadProlineConf(verbose = false, Option(Main.stage)) //workaround => correctly compute buttons' availability for SQLite (FIXME)

    }
  }
}