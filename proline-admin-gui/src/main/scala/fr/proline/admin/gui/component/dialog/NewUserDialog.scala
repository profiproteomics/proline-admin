package fr.proline.admin.gui.component.dialog

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.application.Platform
import scalafx.geometry.HPos
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Hyperlink
import scalafx.scene.control.Label
import scalafx.scene.control.PasswordField
import scalafx.scene.control.TextField
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.ColumnConstraints
import scalafx.scene.layout.ColumnConstraints.sfxColumnConstraints2jfx
import scalafx.scene.layout.GridPane
import scalafx.scene.layout.Priority
import scalafx.stage.Modality
import scalafx.stage.Stage

import fr.profi.util.scalafx.ScalaFxUtils
import fr.proline.admin.gui.Main
import fr.proline.admin.gui.component.panel.ButtonsPanel
import fr.proline.admin.gui.process.LaunchAction
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.util._
import fr.proline.admin.service.user.CreateUser

/**
 *  Create and display a modal dialog to create a new user in database with optional password.
 */
class NewUserDialog extends LazyLogging {

  /** Define modal window */
  val _stage = new Stage {

    val newUserDialog = this

    title = "Create a new user"
    resizable = false
    initModality(Modality.WINDOW_MODAL)
    initOwner(Main.stage)
    this.x = FxUtils.getStartX()
    this.y = FxUtils.getStartY()

    //    newUserDialog.onShowing = handle { ButtonsPanel.disableAll() }
    //    newUserDialog.onHiding = handle { ButtonsPanel.someActionRunning.set(false) }

    scene = new Scene {

      onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(newUserDialog, ke)}

      /**
       * ********** *
       * COMPONENTS *
       * ********** *
       */

      /** Login */

      val loginLabel = new Label("Login* : ")
      val loginField = new TextField
      //      {
      //        styleClass += ("textFields")
      //      }

      val loginWarningLabel = new Label {
        //        styleClass += ("warningLabels")
        style = "-fx-text-fill: red;  -fx-font-style: italic;"
        minHeight = 15
      }

      /** See all users */
      val seeAllUsers = new Hyperlink("See all users...") {
        style = "-fx-color:#66CCFF;"
        alignmentInParent = Pos.BaselineRight

        //TODO: ne dotted border when clicked

        onAction = handle {
          val users = UdsRepository.getAllUserAccounts()
          Platform.runLater(println(s"INFO - Loaded ${users.length} user(s) from UDSdb."))

          val text = if (users.isEmpty) "No user found." else users.map(_.getLogin()).sorted.mkString("\n")
          ShowPopupWindow(
            wTitle = "All users",
            wText = text,
            wParent = Option(Main.stage),
            isResizable = true
          )
        }
      }

      /** Password */

      val pwLabel = new Label("Password : ")
      val pwField = new PasswordField // { styleClass += ("textFields") }

      val pwConfirmLabel = new Label("Confirm password : ")
      val pwConfirmField = new PasswordField // { styleClass += ("textFields") }

      val pwWarningLabel = new Label {
        // styleClass += ("warningLabels")
        style = "-fx-text-fill: red;  -fx-font-style: italic;"
        minHeight = 15
      }

      /** Action button */
      val okButton = new Button("Register") {
        // styleClass += ("minorButtons")
        alignment = Pos.BaselineCenter
        hgrow = Priority.Always
      }

      /**
       * ****** *
       * LAYOUT *
       * ****** *
       */
      //stylesheets = List(Main.CSS)
      root = new GridPane {
        hgrow = Priority.Always
        vgap = 5
        hgap = 10
        padding = Insets(10)
        prefWidth = 500

        columnConstraints ++= Seq(
          new ColumnConstraints { percentWidth = 25 },
          new ColumnConstraints { percentWidth = 75 }
        )

        content = ScalaFxUtils.getFormattedGridContent5(Seq(
          //col, row, colSpan, rowSpan
          (loginLabel, 0, 0, 1, 1),
          (loginField, 1, 0, 1, 1),
          (loginWarningLabel, 1, 1, 1, 1),
          (seeAllUsers, 1, 1, 1, 1),
          (pwLabel, 0, 3, 1, 1),
          (pwField, 1, 3, 1, 1),
          (pwConfirmLabel, 0, 4, 1, 1),
          (pwConfirmField, 1, 4, 1, 1),
          (pwWarningLabel, 1, 5, 2, 1),
          (ScalaFxUtils.newVSpacer(), 0, 6, 2, 1),
          (okButton, 1, 7, 1, 1)
        ))
      }

      Seq(
        loginLabel,
        pwLabel,
        pwConfirmLabel
      ).foreach(GridPane.setHalignment(_, HPos.Right))

      GridPane.setHalignment(okButton, HPos.Center)

      /**
       * ****** *
       * ACTION *
       * ****** *
       */

      okButton.onAction = handle {

        /** Empty warning labels */
        Seq(
          loginWarningLabel,
          pwWarningLabel
        ).foreach(_.text.value = "")

        /** Check form */
        val _login = loginField.text()
        val isLoginDefined = _login.isEmpty == false

        val _pw = pwField.text()
        val _pwConfirm = pwConfirmField.text()
        val isPwdConfirmed = _pw == _pwConfirm

        /** Check if login is unique in database */
        val isLoginAvailable =
          //          if (isLoginDefined) (UdsRepository.getAllUserAccounts().contains(_login) == false)
          if (isLoginDefined) (UdsRepository.getAllUserAccounts().exists(_.getLogin() == _login) == false)
          else false

        /** If all is ok, run action */
        if (isLoginDefined && isLoginAvailable && isPwdConfirmed) {

          val (pswdOpt, cmd) =
            if (_pw.isEmpty())
              (None, s"create_user --login ${_login}")
            else
              (Some(_pw), s"create_user --login ${_login} --password ${"*" * _pw.length()}")

          /** CREATE USER and close dialog */
          logger.debug("Create user")
          LaunchAction(
            actionButton = ButtonsPanel.createUserButton,
            actionString = Utils.mkCmd(cmd),
            action = () => {

              val udsDbContext = UdsRepository.getUdsDbContext()

              try {
                // Create user
                val pswd = if (pswdOpt.isDefined) pswdOpt.get else "proline"
                val userCreator = new CreateUser(udsDbContext, _login, pswd)
                userCreator.run()

              } finally {
                // Close udsDbContext
                logger.debug("Closing current UDS Db Context")
                try {
                  udsDbContext.close()
                } catch {
                  case exClose: Exception => logger.error("Error closing UDS Db Context", exClose)
                }
              }
            }
          )

          newUserDialog.close()

          /** Otherwise fill warning labels */
        } else {

          if (isLoginDefined == false) loginWarningLabel.text = "Please enter a login"
          else if (isLoginAvailable == false) loginWarningLabel.text = "This login is not available."

          if (isPwdConfirmed == false) pwWarningLabel.text = "Password and confirm password do not match."
        }
      }
    }
  }

  /** Display stage */
  _stage.showAndWait()

}