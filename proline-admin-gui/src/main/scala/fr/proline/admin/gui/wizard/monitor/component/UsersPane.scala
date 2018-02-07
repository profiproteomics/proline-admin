package fr.proline.admin.gui.wizard.monitor.component
import scalafx.Includes._
import scalafx.scene.control.{ TableColumn, TableView }
import scalafx.scene.control.TableColumn._
import scala.collection.immutable.List
import scalafx.scene.layout.{ VBox, HBox }
import scalafx.scene.control.Button
import scalafx.geometry.Pos
import javafx.scene.{ control => jfxsc }
import scalafx.collections.ObservableBuffer

import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.core.orm.uds.UserAccount
import fr.proline.admin.gui.component.resource.implicits.UserView
import fr.profi.util.scalafx.TitledBorderPane
import fr.profi.util.scala.ScalaUtils._
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.util._


/**
 * builds users view
 * @aromdhani
 *
 */
object UsesrsPane extends VBox {

  //import data from database 
  val userViews = UdsRepository.getAllUserAccounts().toBuffer[UserAccount].sortBy(_.getId).map(new UserView(_))
  val tableLines = ObservableBuffer(userViews)
  //create the table user view 
  val usersTable = new TableView[UserView](tableLines) {
    columns ++= List(
      new TableColumn[UserView, Long] {
        text = "Id"
        cellValueFactory = { _.value.id }
      },
      new TableColumn[UserView, String] {
        text = "Login"
        cellValueFactory = { _.value.login }
      },
      new TableColumn[UserView, String] {
        text = "Password hash"
        cellValueFactory = { _.value.pwdHash }
      },
      new TableColumn[UserView, String] {
        text = "User group"
        cellValueFactory = { _.value.userGroup }
      },
      new TableColumn[UserView, String] {
        text = "State"
        cellValueFactory = { _.value.userIsActive }
      })
  }
  usersTable.setColumnResizePolicy(jfxsc.TableView.CONSTRAINED_RESIZE_POLICY)

  //buttons panel  
  val addUserButton = new Button {
    text = "Add user"
    graphic = FxUtils.newImageView(IconResource.PLUS)
    onAction = handle {
      NewUserPane("New User", Some(Monitor.stage), true)
    }
  }

  val disableUserButton = new Button {
    text = "Disable user"
    graphic = FxUtils.newImageView(IconResource.TRASH)

  }
  Seq(
    addUserButton,
    disableUserButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }
  val buttonsPanel = new HBox {
    spacing = 50
    alignment_=(Pos.BOTTOM_CENTER)
    children = Seq(addUserButton, disableUserButton)
  }
  val usersTitledPane = new TitledBorderPane(
    title = "Proline users",
    titleTooltip = "Proline users",
    contentNode = new VBox {
      spacing = 20
      children = Seq(usersTable, buttonsPanel)
    })

  spacing = 20
  children = Seq(usersTitledPane)
}