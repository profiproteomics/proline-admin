package fr.proline.admin.gui.process //TODO: rename/re-organize package

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.future
import scala.util.Failure
import scala.util.Success

import com.typesafe.scalalogging.slf4j.Logging

import fr.proline.admin.gui.component.panel.ButtonsPanel

import scalafx.application.Platform
import scalafx.scene.control.Button
import scalafx.scene.control.ProgressIndicator
import scalafx.scene.image.ImageView

/**
 * Run button's action asynchronously
 */
object LaunchAction extends Logging {

  def apply(
    actionButton: Button,
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

      /** Print relative command line in console panel */
      println("\n" + actionString)
    }

    /** Lauch action asynchronously then update enabled/disabled buttons*/
    val f = future {
      action()
      ButtonsPanel.updateBooleans()
    }

    /** Future's callback : when action is finished */
    f onComplete {

      case Success(_) => {

        Platform.runLater {
          println(s"[ $actionString : <b>success</b> ]")
          actionButton.graphic = new ImageView()
          //actionButton.style = " -fx-background-color: SlateGrey;"
          //actionButton.styleClass -= ("activeButtons")
          //actionButton.styleClass += ("mainButtons")
        }
      }

      case Failure(e) => {
        
        e match {
          case fxThread: java.lang.IllegalStateException => System.err.println("MY FX THREAD? " + e)
          case _                                         => Platform.runLater {
            
            logger.error(s"Failed to run action [$actionString]",e)
            
            // TODO: user sys.err
            println(s"[ $actionString : finished with <b>error</b> ]")
            println("Got error : "+ e.getMessage)
            //e.printStackTrace() //TODO: remove me
            
            actionButton.graphic = new ImageView()
          }
        }
      }
    }
  }

}