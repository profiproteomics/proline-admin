package fr.proline.admin.gui.wizard.monitor.component

import scalafx.scene.control.{ TableColumn, TableView }
import scalafx.scene.control.TableColumn._
import scala.collection.immutable.List
import fr.proline.admin.gui.component.resource.implicits.UserView
import scalafx.scene.layout.VBox
import javafx.scene.{ control => jfxsc }
/**
 * builds users view
 * @aromdhani
 *
 */
object UsesrsPane extends VBox {

  //import data from database 

  //create the table user view 
  val usersTable = new TableView[UserView]() {
    columns ++= List(
      new TableColumn[UserView, Long] {
        text = "Id"
        prefWidth = 100
        cellValueFactory = { _.value.id }
      },
      new TableColumn[UserView, String] {

        text = "Login"
        cellValueFactory = { _.value.login }
        prefWidth = 100
      },
      new TableColumn[UserView, String] {
        text = "Password hash"
        cellValueFactory = { _.value.pwdHash }
        prefWidth = 100
      },
      new TableColumn[UserView, String] {
        text = "User group"
        cellValueFactory = { _.value.userGroup }
        prefWidth = 100
      },
      new TableColumn[UserView, String] {
        text = "State"
        cellValueFactory = { _.value.userIsActive }
        prefWidth = 100
      },
      new TableColumn[UserView, Unit] {
        text = "Action"
        prefWidth = 100
      })
  }
  usersTable.setColumnResizePolicy(jfxsc.TableView.CONSTRAINED_RESIZE_POLICY)
  children = Seq(usersTable)
}