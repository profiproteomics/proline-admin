package fr.proline.admin.gui.wizard.monitor.component

import fr.proline.admin.gui.component.resource.implicits.ProjectView
import scalafx.scene.control.{ TableColumn, TableView }
import scalafx.scene.control.TableColumn._
import scalafx.scene.layout.VBox
import javafx.scene.{ control => jfxsc }
import scala.collection.immutable.List
/**
 * builds projects view
 * @aromdhani
 */
object ProjectPane extends VBox {

  //data from  database 

  //create table view 
  val projectTable = new TableView[ProjectView]() {
    columns ++= List(
      new TableColumn[ProjectView, Long] {
        text = "Id"
        prefWidth = 100
      },
      new TableColumn[ProjectView, String] {
        text = "Owner"
        prefWidth = 100
      },
      new TableColumn[ProjectView, String] {
        text = "Name"
        prefWidth = 100
      },
      new TableColumn[ProjectView, String] {
        text = "Schema version (MSI-LCMS)"
        prefWidth = 100
      },
      new TableColumn[ProjectView, String] {
        text = "Size (MSI-LCMS)"
        prefWidth = 100
      },
      new TableColumn[ProjectView, Boolean] {
        text = "Action"
        prefWidth = 100
      })
  }
  children = Seq(projectTable)
  projectTable.setColumnResizePolicy(jfxsc.TableView.CONSTRAINED_RESIZE_POLICY)
}