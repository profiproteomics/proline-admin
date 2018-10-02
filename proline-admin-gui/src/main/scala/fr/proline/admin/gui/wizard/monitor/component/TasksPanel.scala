package fr.proline.admin.gui.wizard.monitor.component

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.control.{ TableColumn, TableView }
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.Button
import scalafx.scene.control.TitledPane
import scalafx.scene.layout.{ VBox, HBox }
import javafx.scene.{ control => jfxsc }

import scalafx.collections.ObservableBuffer
import scala.collection.immutable.List

import fr.proline.admin.gui.component.resource.implicits.TaskView
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.util.FxUtils
import fr.profi.util.scala.ScalaUtils._
import scalafx.application.Platform

/**
 * TasksPane Build a table with a list of running tasks
 * @author aromdhani
 */

object TasksPanel extends VBox {

  //create the table user view 
  val tasksTable = new TableView[TaskView]() {
    columns ++= List(
      new TableColumn[TaskView, String] {
        text = "Id"
        cellValueFactory = { _.value.id }
      },
      new TableColumn[TaskView, String] {
        text = "state"
        cellValueFactory = { _.value.state }
      })
  }
  tasksTable.setColumnResizePolicy(jfxsc.TableView.CONSTRAINED_RESIZE_POLICY)

  //buttons panel  
  val pauseTaskButton = new Button {
    text = "Pause"
    graphic = FxUtils.newImageView(IconResource.PAUSE)
    prefHeight = 20
    prefWidth = 120
    styleClass += ("mainButtons")
    onAction = handle {
    }
  }
  val resumeTaskButton = new Button {
    text = "Resume"
    graphic = FxUtils.newImageView(IconResource.EXECUTE)
    prefHeight = 20
    prefWidth = 120
    styleClass += ("mainButtons")
    onAction = handle {
    }
  }
  val cancelTaskButton = new Button {
    text = "Cancel"
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    prefHeight = 20
    prefWidth = 120
    styleClass += ("mainButtons")
    onAction = handle {
    }
  }
  val buttonsPanel = new HBox {
    spacing = 50
    alignment_=(Pos.BOTTOM_CENTER)
    children = Seq(pauseTaskButton, resumeTaskButton, cancelTaskButton)
  }
  val contentNode = new VBox {
    spacing = 10
    children = Seq(tasksTable, buttonsPanel)
  }
  val tasksPane = new TitledPane {
    text = "Proline Tasks Table"
    expanded_=(true)
    collapsible_=(false)
    content_=(contentNode)
  }
  Platform.runLater(children = Seq(tasksPane))

}