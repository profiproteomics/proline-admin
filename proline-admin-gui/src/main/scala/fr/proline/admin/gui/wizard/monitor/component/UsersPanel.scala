package fr.proline.admin.gui.wizard.monitor.component

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.control.{ TableColumn, TableView }
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.TableCell
import scalafx.scene.control.TitledPane
import scalafx.scene.control.Button
import scalafx.scene.control.{ Label, TextField }
import scalafx.scene.control.SelectionMode
import scalafx.scene.layout.{ VBox, HBox }
import javafx.scene.{ control => jfxsc }
import javafx.scene.input.MouseEvent
import javafx.event.EventHandler
import scalafx.scene.Cursor
import scalafx.collections.ObservableBuffer
import scalafx.application.Platform
import scala.collection.immutable.List
import scala.collection.mutable.TreeSet
import scala.collection.mutable.SortedSet

import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.component.resource.implicits.UserView
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.wizard.util.{ GetConfirmation, HelpPopup }
import fr.proline.admin.gui.wizard.service.{ AddUser, ChangeUdsUserState, DisableUser, ChangeUserGrp }
import fr.proline.core.orm.uds.UserAccount
import fr.profi.util.scala.ScalaUtils._

import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import fr.proline.admin.gui.component.resource.implicits.UserView
import fr.proline.admin.gui.wizard.util.MultiSelectTableView

/**
 * UsesrsPanel builds a table view with the list of users.
 * @author aromdhani
 *
 */
object UsesrsPanel extends VBox with LazyLogging {

  //load list of users from database 
  val users = Await.ready(getUserasViews(UdsRepository.getAllUserAccounts()), Duration.Inf).value.get
  require(users.isSuccess, "Error while trying to load users...")
  lazy val tableLines = ObservableBuffer(users.get)
  //create the table user view 
  val usersTable = new MultiSelectTableView[UserView](tableLines) {
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
        cellValueFactory = { _.value.group }
      },
      new TableColumn[UserView, String]() {
        text = "State"
        cellValueFactory = { _.value.isActivated }
      })
  }
  //buttons panel 
  val refreshButton = new Button {
    text = "Refresh"
    tooltip = "Refresh the table of users."
    graphic = FxUtils.newImageView(IconResource.REFERESH)
    onAction = handle {
      refreshTableView()
    }
  }
  val addUserButton = new Button {
    text = "New user"
    tooltip = "Create a new Proline user."
    graphic = FxUtils.newImageView(IconResource.PLUS)
    onAction = handle {
      NewUserDialog("New user", Some(Monitor.stage), true)
    }
  }

  val disableUserButton = new Button {
    text = "Disable user"
    tooltip = "Disable the selected users."
    graphic = FxUtils.newImageView(IconResource.DISABLE)
    onAction = handle {
      disableUser()
    }
  }
  val activateUserButton = new Button {
    text = "Activate user"
    tooltip = "Activate the selected users."
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction = handle {
      activateUser()
    }
  }
  val changePwdButton = new Button {
    text = "Change password"
    tooltip = "Change the password of the selected user."
    graphic = FxUtils.newImageView(IconResource.UNLOCK)
    onAction = handle {
      changeUserPwd()
    }
  }
  val setAsUserButton = new Button {
    text = "Add to user group"
    tooltip = "Add the selected users to user group."
    graphic = FxUtils.newImageView(IconResource.USER)
    onAction = handle {
      setInUserGrp()
    }
  }
  val setAsAdminButton = new Button {
    text = "Add to admin group"
    tooltip = "Add the selected users to admin group."
    graphic = FxUtils.newImageView(IconResource.ADMIN)
    onAction = handle {
      setInAdminGrp()
    }
  }
  Seq(
    refreshButton,
    addUserButton,
    activateUserButton,
    changePwdButton,
    disableUserButton,
    setAsUserButton,
    setAsAdminButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 140
      b.styleClass += ("mainButtons")
    }
  val buttonsPanel = new HBox {
    spacing = 20
    alignment_=(Pos.BOTTOM_CENTER)
    children = Seq(refreshButton,
      addUserButton,
      activateUserButton,
      disableUserButton,
      setAsUserButton,
      setAsAdminButton,
      changePwdButton)
  }
  val contentNode = new VBox {
    spacing = 10
    children = Seq(usersTable, buttonsPanel)
  }
  val usersTitledPane = new TitledPane {
    text = "Users Table"
    expanded_=(true)
    collapsible_=(false)
    content_=(contentNode)
  }

  children = Seq(usersTitledPane)

  /**
   *  get a list of existing users.
   *
   */
  def getAllUsers: Array[UserAccount] = {
    UdsRepository.getAllUserAccounts()
  }

  /**
   * get the list of users as a sequence of userView  from the database.
   *
   */
  def getUserasViews(array: => Array[UserAccount]): Future[Seq[UserView]] = Future {
    array.toBuffer[UserAccount].sortBy(_.getId).map(new UserView(_))
  }

  /**
   * disable the selected user(s)
   *
   */
  private def disableUser() {
    if (!usersTable.selectedItems.isEmpty) {
      val confirmed = GetConfirmation("Are you sure that you want to disable the selected users ? ", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
      if (confirmed) {
        val userIds = usersTable.selectedItems.map(_.id.value)
        DisableUser(userIds, false).restart()
      }
    } else {
      HelpPopup("Warning", "There is no user was selected ! ", Some(Monitor.stage), false)
    }
  }

  /**
   * Activate the selected user(s)
   */
  private def activateUser() {
    if (!usersTable.selectedItems.isEmpty) {
      val confirmed = GetConfirmation("Are you sure that you want to activate the selected users ? ", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
      if (confirmed) {
        val userIds = usersTable.selectedItems.map(_.id.value)
        ChangeUdsUserState(userIds, true).restart()
      }
    } else {
      HelpPopup("Warning", "There is no user(s) was selected ! ", Some(Monitor.stage), false)
    }
  }

  /**
   * Change password.
   */
  private def changeUserPwd() {
    ChangePwdDialog("Change password", Some(Monitor.stage), true)
  }

  /**
   * set the selected admin(s) in user group
   */
  private def setInUserGrp() {
    if (!usersTable.selectedItems.isEmpty) {
      val confirmed = GetConfirmation("Are you sure that you want to change the selected user group ? ", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
      if (confirmed) {
        if (usersTable.selectedItems.forall(_.group.value == "ADMIN")) {
          val userIds = usersTable.selectedItems.map(_.id.value)
          ChangeUserGrp(userIds, true).restart()
        }
      }
    } else {
      HelpPopup("Warning", "There is no user was selected ! ", Some(Monitor.stage), false)
    }
  }

  /**
   * set the selected user(s) in Admin group.
   *
   */
  private def setInAdminGrp() {
    if (!usersTable.selectedItems.isEmpty) {
      val confirmed = GetConfirmation("Are you sure that you want to change the selected users group ? ", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
      if (confirmed) {
        if (usersTable.selectedItems.forall(_.group.value == "USER")) {
          val userIds = usersTable.selectedItems.map(_.id.value)
          ChangeUserGrp(userIds, false).restart()
        }
      }
    } else {
      HelpPopup("Warning", "There is no user was selected ! ", Some(Monitor.stage), false)
    }
  }

  /**
   * refresh user table view
   */
  def refreshTableView() {
    getUserasViews(UdsRepository.getAllUserAccounts()).map { users =>
      tableLines.clear()
      tableLines.addAll(ObservableBuffer(users))
    }
  }
}