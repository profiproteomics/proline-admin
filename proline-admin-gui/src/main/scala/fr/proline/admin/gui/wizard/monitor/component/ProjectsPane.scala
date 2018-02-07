package fr.proline.admin.gui.wizard.monitor.component

import scalafx.scene.control.{ TableColumn, TableView }
import scalafx.scene.control.TableColumn._
import scalafx.scene.layout.{ VBox, HBox }
import javafx.scene.{ control => jfxsc }
import scalafx.beans.property.{ BooleanProperty, StringProperty }
import scala.collection.immutable.List
import scalafx.collections.ObservableBuffer

import scalafx.beans.property.ObjectProperty
import scalafx.geometry.Pos
import scalafx.scene.control.Button
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.core.orm.uds.Project
import fr.profi.util.scalafx.TitledBorderPane
import fr.proline.admin.gui.component.resource.implicits.ProjectView
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scala.ScalaUtils._
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.util._
/**
 * builds projects view
 * @aromdhani
 *
 */
object ProjectPane extends VBox {

  //import data from  database 
  val projectViews = UdsRepository.getAllProjects().toBuffer[Project].sortBy(_.getId).map(new ProjectView(_))
  val tableLines = ObservableBuffer(projectViews)
  //create table view 
  val projectTable = new TableView[ProjectView](tableLines) {
    columns ++= List(
      new TableColumn[ProjectView, Long] {
        text = "Id"
        cellValueFactory = { _.value.id }
      },
      new TableColumn[ProjectView, String] {
        text = "Owner"
        cellValueFactory = { _.value.ownerLogin }
      },
      new TableColumn[ProjectView, String] {
        text = "Name"
        cellValueFactory = { _.value.name }
      },
      new TableColumn[ProjectView, String] {
        text = "Schema version (MSI-LCMS)"
        cellValueFactory = { _.value.version }
      },
      new TableColumn[ProjectView, String] {
        text = "Size (MSI-LCMS)"
        cellValueFactory = { _.value.size }
      })
  }
  projectTable.setColumnResizePolicy(jfxsc.TableView.CONSTRAINED_RESIZE_POLICY)

  //buttons panel  
  val newProjButton = new Button {
    text = "New project"
    graphic = FxUtils.newImageView(IconResource.PLUS)
  }

  val deleteProjButton = new Button {
    text = "Delete project"
    graphic = FxUtils.newImageView(IconResource.TRASH)
  }

  val saveProjButton = new Button {
    text = "Save project"
    graphic = FxUtils.newImageView(IconResource.SAVE)
  }

  val restoreProjButton = new Button {
    text = "Restore project"
    graphic = FxUtils.newImageView(IconResource.LOAD)
  }

  Seq(
    newProjButton,
    deleteProjButton,
    saveProjButton,
    restoreProjButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }
  val buttonsPanel = new HBox {
    spacing = 50
    alignment_=(Pos.BOTTOM_CENTER)
    children = Seq(newProjButton, deleteProjButton, saveProjButton, restoreProjButton)
  }
  val usersTitledPane = new TitledBorderPane(
    title = "Proline projects",
    titleTooltip = "Proline projects",
    contentNode = new VBox {
      spacing = 20
      children = Seq(projectTable, buttonsPanel)
    })

  spacing = 20
  children = Seq(usersTitledPane)

}