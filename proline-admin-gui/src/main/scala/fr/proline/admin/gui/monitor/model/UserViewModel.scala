package fr.proline.admin.gui.monitor.model

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.application.Platform
import scalafx.beans.property.{ BooleanProperty, ObjectProperty }
import scalafx.stage.Window

import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.task.TaskRunner
import fr.proline.admin.gui.monitor.database.UsersDB
import fr.proline.admin.gui.monitor.model.AdapterModel._
import fr.proline.admin.gui.monitor.view.dialog._
import fr.profi.util.scala.ScalaUtils._

/**
 * The user view model. Defines UI actions and database actions via UsersDB.
 * @author aromdhani
 *
 */
class UserViewModel extends LazyLogging {

  var taskRunner: TaskRunner = _
  private val usersDB = UsersDB

  val parentWindow: ObjectProperty[Window] = ObjectProperty[Window](null.asInstanceOf[Window])

  val items: ObservableBuffer[User] = new ObservableBuffer[User]()

  // Read-only collection of rows selected in the table view
  var _selectedItems: ObservableBuffer[User] = _
  def selectedItems: ObservableBuffer[User] = _selectedItems
  def selectedItems_=(v: ObservableBuffer[User]): Unit = {
    _selectedItems = v
    _selectedItems.onChange {
      canRemoveRow.value = selectedItems.nonEmpty
    }
  }

  val canRemoveRow = BooleanProperty(false)

  /** Initialize the table view with users from UDS database */
  def onInitialize(): Unit = {
    items.clear()
    items ++= usersDB.initialize()
  }

  /** Add Proline user */
  def onAddUser(): Unit = {
    val result = AddUserDialog.showAndWait(Monitor.stage)
    result match {
      case Some(userCreator) =>
        taskRunner.run(
          caption = s"Creating user with login= #${userCreator.login}",
          op = {
            // Create user 
            logger.info(s"Creating user")
            usersDB.add(userCreator.login, userCreator.pswd, userCreator.user, userCreator.passwdEncrypted)

            // Return items from database
            val updatedItems = usersDB.queryUsersAsView()
            // Update items on FX thread
            Platform.runLater {
              updateItems(updatedItems)
            }
          })
      case _ =>
    }
  }

  /** Change user's password */
  def onResetPwd(): Unit = {
    val result = ResetPwdDialog.showAndWait(Monitor.stage)
    result match {
      case Some(resetPwd) =>
        taskRunner.run(
          caption = s"Reset user password",
          op = {
            // Reset user password
            logger.info(s"Reset user Password")
            usersDB.resetPassword(selectedItems.headOption.map(_.id.value).get, resetPwd.newPassword)
            // Return items from database
            val updatedItems = usersDB.queryUsersAsView()
            // Update items on FX thread
            Platform.runLater {
              updateItems(updatedItems)
            }
          })
      case _ =>
    }
  }

  /** Disable Proline user */
  def onDisable(): Unit = {
    taskRunner.run(
      caption = s"Disabling the user with id= #${selectedItems.headOption.map(_.id.value).getOrElse("no item was selected")}",
      op = {
        //disable user 
        logger.info(s"Disabling the user with id= #${selectedItems.headOption.map(_.id.value).getOrElse("no item was selected")}")
        usersDB.changeState(selectedItems.toSet, false)
        // Return items from database
        val updatedItems = usersDB.queryUsersAsView()
        // Update items on FX thread
        Platform.runLater {
          updateItems(updatedItems)
        }
      })
  }

  /** Activate Proline user */
  def onActivate(): Unit = {
    taskRunner.run(
      caption = s"Activating the user with id= #${selectedItems.headOption.map(_.id.value).getOrElse("no item was selected")}",
      op = {
        logger.info(s"Activating the user with id= #${selectedItems.headOption.map(_.id.value).getOrElse("no item was selected")}")
        usersDB.changeState(selectedItems.toSet, true)

        // Return items from database
        val updatedItems = usersDB.queryUsersAsView()
        // Update items on FX thread
        Platform.runLater {
          updateItems(updatedItems)
        }
      })
  }

  /** Add user to user group */
  def onAddToUserGrp(): Unit = {
    taskRunner.run(
      caption = s"Adding the user with id= #${selectedItems.headOption.map(_.id.value).getOrElse("no item was selected")} to user group",
      op = {
        logger.info(s"Adding the selected user with id= #${selectedItems.headOption.map(_.id.value).getOrElse("no item was selected")} to user group")
        usersDB.changeGroup(selectedItems.toSet, true)
        // Return items from database
        val updatedItems = usersDB.queryUsersAsView()
        // Update items on FX thread
        Platform.runLater {
          updateItems(updatedItems)
        }
      })
  }

  /** Add user to Admin group */
  def onAddToAdminGrp(): Unit = {
    taskRunner.run(
      caption = s"Adding the user with id= #${selectedItems.headOption.map(_.id.value).getOrElse("no item was selected")} to admin group",
      op = {
        logger.info(s"Adding the user with id= #${selectedItems.headOption.map(_.id.value).getOrElse("no item was selected")} to admin group")
        usersDB.changeGroup(selectedItems.toSet, false)
        // Return items from database
        val updatedItems = usersDB.queryUsersAsView()
        // Update items on FX thread
        Platform.runLater {
          updateItems(updatedItems)
        }
      })
  }

  /** Refresh the table view */
  def onRefresh(): Unit = {
    taskRunner.run(
      caption = "Refreshing the table view",
      op = {
        logger.info("Refreshing the table view")
        items.clear()
        items ++= usersDB.initialize()
      })
  }

  /** update items in table view */
  private def updateItems(updatedItems: Seq[User]): Unit = {
    val toAdd = updatedItems.diff(items)
    val toRemove = items.diff(updatedItems)
    items ++= toAdd
    items --= toRemove
  }

}