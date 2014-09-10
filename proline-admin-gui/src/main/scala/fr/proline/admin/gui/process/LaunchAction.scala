package fr.proline.admin.gui.process

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.future
import scala.util.Failure
import scala.util.Success

import fr.proline.admin.gui.component.panel.ButtonsPanel

import scalafx.application.Platform
import scalafx.scene.control.Button
import scalafx.scene.control.ProgressIndicator
import scalafx.scene.image.ImageView

/**
 * Run button's action asynchronously
 */
object LaunchAction {

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
      //    Platform.runLater(println("\n\n" + actionString))
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
        Platform.runLater {
          println("JavaFX FAILURE : failed to run action : " + e)
          e.printStackTrace() //TODO: remove me
          println(s"[ $actionString : finished with <b>error</b> ]")
          actionButton.graphic = new ImageView()
        }
      }
    }
  }

}