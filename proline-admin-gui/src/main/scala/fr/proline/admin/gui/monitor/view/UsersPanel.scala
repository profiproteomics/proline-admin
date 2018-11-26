package fr.proline.admin.gui.monitor.view

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.control.{ TableColumn, TableView }
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.TitledPane
import scalafx.scene.control.Button
import scalafx.scene.layout.{ VBox, HBox }
import scalafx.collections.ObservableBuffer
import javafx.scene.{ control => jfxsc }

import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.monitor.model.UserViewModel
import fr.proline.admin.gui.monitor.model.AdapterModel._
import fr.proline.admin.gui.wizard.util.GetConfirmation
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.util.FxUtils
import fr.profi.util.scala.ScalaUtils._

/**
 * Create and display a table view of Proline users.
 * @author aromdhani
 *
 */

class UsersPanel(val model: UserViewModel) extends VBox with LazyLogging {
  //users table view 
  private val usersTable = new TableView[User](model.items) {
    columns ++= List(
      new TableColumn[User, Long] {
        text = "Id"
        cellValueFactory = { _.value.id }
      },
      new TableColumn[User, String] {
        text = "Login"
        cellValueFactory = { _.value.login }
      },
      new TableColumn[User, String] {
        text = "Password hash"
        cellValueFactory = { _.value.pwdHash }
      },
      new TableColumn[User, String] {
        text = "User group"
        cellValueFactory = { _.value.group }
      },
      new TableColumn[User, String]() {
        text = "State"
        cellValueFactory = { _.value.isActivated }
      })
  }

  //resize columns
  usersTable.setColumnResizePolicy(jfxsc.TableView.CONSTRAINED_RESIZE_POLICY)
  //selected items 
  model.selectedItems = usersTable.selectionModel.value.selectedItems

  //buttons panel 

  private val refreshButton = new Button {
    text = "Refresh"
    tooltip = "Refresh the table view of users."
    graphic = FxUtils.newImageView(IconResource.REFERESH)
    onAction = handle {
      model.onRefresh()
    }
  }
  private val addUserButton = new Button {
    text = " Add "
    tooltip = "Add a new Proline user."
    graphic = FxUtils.newImageView(IconResource.PLUS)
    onAction = handle {
      model.onAddUser()
    }
  }

  private val disableButton = new Button {
    text = "Disable"
    tooltip = "Disable the selected user."
    graphic = FxUtils.newImageView(IconResource.DISABLE)
    onAction = handle {
      if (!model.selectedItems.isEmpty) {
        val confirmed = GetConfirmation(s"Are you sure that you want to disable the user with id= #${model.selectedItems.headOption.map(_.id.value).getOrElse("no item was selected")}? ", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
        if (confirmed) model.onDisable()
      }
    }
  }

  private val activateButton = new Button {
    text = "Activate"
    tooltip = "Activate the selected user."
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction = handle {
      if (!model.selectedItems.isEmpty) {
        val confirmed = GetConfirmation(s"Are you sure that you want to activate the user with id= #${model.selectedItems.headOption.map(_.id.value).getOrElse("no item was selected")}? ", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
        if (confirmed) model.onActivate()
      }
    }
  }

  private val changePwdButton = new Button {
    text = "Reset Password"
    tooltip = "Reset the selected user's password."
    graphic = FxUtils.newImageView(IconResource.UNLOCK)
    onAction = handle {
      if (!model.selectedItems.isEmpty) {
        model.onResetPwd()
      }
    }
  }
  private val addToUserGrpButton = new Button {
    text = "Add to user group"
    tooltip = "Add the selected user to user group."
    graphic = FxUtils.newImageView(IconResource.USER)
    onAction = handle {
      if (!model.selectedItems.isEmpty) {
        val confirmed = GetConfirmation(s"Are you sure that you want to add the user with id= #${model.selectedItems.headOption.map(_.id.value).getOrElse("no item was selected")} to user group? ", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
        if (confirmed) model.onAddToUserGrp()
      }
    }
  }
  private val addToAdminGrpButton = new Button {
    text = "Add to admin group"
    tooltip = "Add the selected user to admin group."
    graphic = FxUtils.newImageView(IconResource.ADMIN)
    onAction = handle {
      if (!model.selectedItems.isEmpty) {
        val confirmed = GetConfirmation(s"Are you sure that you want to add the user with id= #${model.selectedItems.headOption.map(_.id.value).getOrElse("no item was selected")} to admin group? ", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
        if (confirmed) model.onAddToAdminGrp()
      }
    }
  }
  Seq(
    refreshButton,
    addUserButton,
    activateButton,
    changePwdButton,
    disableButton,
    addToUserGrpButton,
    addToAdminGrpButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 140
      b.styleClass += ("mainButtons")
    }
  val buttonsPanel = new HBox {
    spacing = 20
    alignment_=(Pos.BOTTOM_CENTER)
    children = Seq(
      refreshButton,
      addUserButton,
      activateButton,
      disableButton,
      addToUserGrpButton,
      addToAdminGrpButton,
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

}