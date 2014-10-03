package fr.proline.admin.gui.component.dialog

import com.typesafe.scalalogging.slf4j.Logging

import fr.proline.admin.gui.Main
import fr.proline.admin.gui.Util
import fr.proline.admin.gui.component.panel.ButtonsPanel
import fr.proline.admin.gui.process.LaunchAction
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.service.user.CreateUser

import scalafx.Includes.eventClosureWrapperWithParam
import scalafx.Includes.handle
import scalafx.Includes.jfxKeyEvent2sfx
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.application.Platform
import scalafx.beans.property.BooleanProperty.sfxBooleanProperty2jfx
import scalafx.geometry.HPos
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Hyperlink
import scalafx.scene.control.Label
import scalafx.scene.control.PasswordField
import scalafx.scene.control.TextField
import scalafx.scene.input.KeyCode
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.ColumnConstraints
import scalafx.scene.layout.ColumnConstraints.sfxColumnConstraints2jfx
import scalafx.scene.layout.GridPane
import scalafx.scene.layout.Priority
import scalafx.stage.Modality
import scalafx.stage.Stage
import scalafx.stage.StageStyle

/**
 *  Create and display a modal dialog to create a new user in database with optional password.
 */
class NewUserDialog extends Logging {

  /** Define modal window */
  val _stage = new Stage {

    val newUserDialog = this

    title = "Create a new user"
    //    initStyle(StageStyle.UTILITY)
    resizable = false
    initModality(Modality.WINDOW_MODAL)
    initOwner(Main.stage)
    this.x = Util.getStartX()
    this.y = Util.getStartY()

//    newUserDialog.onShowing = handle { ButtonsPanel.disableAll() }
    //    newUserDialog.onHiding = handle { ButtonsPanel.someActionRunning.set(false) }

    scene = new Scene {

      onKeyPressed = (ke: KeyEvent) => { if (ke.code == KeyCode.ESCAPE) newUserDialog.close() }

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
        alignmentInParent = Pos.BASELINE_RIGHT

        //TODO: ne dotted border when clicked

        onAction = handle {
          val users = UdsRepository.getAllUserAccounts()
          Platform.runLater(println(s"INFO - Loaded ${users.length} user(s) from UDSdb."))

          val text = if (users.isEmpty) "No user found." else users.map(_.getLogin()).sorted.mkString("\n")
          new PopupWindow(
            "All users",
            text,
            Option(Main.stage)
          )
        }
      }

      /** Password */

      val pwLabel = new Label("Password : ")
      val pwField = new PasswordField
      //      {
      //        styleClass += ("textFields")
      //      }

      val pwConfirmLabel = new Label("Confirm password : ")
      val pwConfirmField = new PasswordField
      //      {
      //        styleClass += ("textFields")
      //      }

      val pwWarningLabel = new Label {
        //        styleClass += ("warningLabels")
        style = "-fx-text-fill: red;  -fx-font-style: italic;"
        minHeight = 15
      }

      /** Action button */
      val okButton = new Button("Register") {
        //        styleClass += ("minorButtons")
        alignment = Pos.BASELINE_CENTER
        hgrow = Priority.ALWAYS
      }

      /**
       * ****** *
       * LAYOUT *
       * ****** *
       */
      //      stylesheets = List(Main.CSS)

      root = new GridPane {
        hgrow = Priority.ALWAYS
        vgap = 5
        hgap = 10
        padding = Insets(10)
        prefWidth = 500

        val s = Seq(
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
          (Util.newVSpacer, 0, 6, 2, 1),
          (okButton, 1, 7, 1, 1)
        )
        Util.setGridContent5(s, this)

        columnConstraints ++= Seq(
          new ColumnConstraints { percentWidth = 25 },
          new ColumnConstraints { percentWidth = 75 }
        )
      }

      Seq(
        loginLabel,
        pwLabel,
        pwConfirmLabel
      ).foreach(GridPane.setHalignment(_, HPos.RIGHT))

      GridPane.setHalignment(okButton, HPos.CENTER)

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

          LaunchAction(
            actionButton = ButtonsPanel.createUserButton,
            actionString = Util.mkCmd(cmd),
            action = () => {
              val udsDbContext = UdsRepository.getUdsDbContext()
              val pswd = if (pswdOpt.isDefined) pswdOpt.get else "proline"

              val userCreator = new CreateUser(udsDbContext, _login, pswd)
              userCreator.run()
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