package fr.proline.admin.gui.component.dialog

import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.HPos
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.ComboBox
import scalafx.scene.control.Hyperlink
import scalafx.scene.control.Label
import scalafx.scene.control.TextArea
import scalafx.scene.control.TextField
import scalafx.scene.input.KeyCode
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.ColumnConstraints
import scalafx.scene.layout.ColumnConstraints.sfxColumnConstraints2jfx
import scalafx.scene.layout.GridPane
import scalafx.scene.layout.Priority
import scalafx.stage.Modality
import scalafx.stage.Stage

import com.typesafe.scalalogging.slf4j.Logging

import fr.proline.admin.gui.Main
import fr.proline.admin.gui.Util
import fr.proline.admin.gui.component.panel.ButtonsPanel
import fr.proline.admin.gui.process.LaunchAction
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.service.db.CreateProjectDBs
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.service.user.CreateProject
import fr.proline.core.orm.uds.Project
import fr.proline.core.orm.uds.UserAccount

/**
 *  Create and display a modal dialog to create a new project in database
 *  with registered owner and optional description.
 */

class NewProjectDialog extends Logging {

  /** Stage */
  val _stage = new Stage {

    val newProjectDialog = this

    title = "Create a new project"

    resizable = false
    initModality(Modality.WINDOW_MODAL)
    initOwner(Main.stage)
    this.x = Util.getStartX()
    this.y = Util.getStartY()

    //    newProjectDialog.onShowing = handle { ButtonsPanel.someActionRunning.set(true) }
    //    newProjectDialog.onHiding = handle { ButtonsPanel.someActionRunning.set(false) }

    //        newProjectDialog.onShowing = handle { ButtonsPanel.disableAll() }

    scene = new Scene {

      onKeyPressed = (ke: KeyEvent) => { if (ke.code == KeyCode.ESCAPE) newProjectDialog.close() }

      /**
       * ********** *
       * COMPONENTS *
       * ********** *
       */

      /** Project owner */

      val userAccounts = UdsRepository.getAllUserAccounts()

      val ownerLabel = new Label("Owner* : ")

      case class PrintableUserAccount(wrappedUser: UserAccount) {
        override def toString() = wrappedUser.getLogin()

        // Define proxy methods
        def getId() = wrappedUser.getId()
        def getLogin() = wrappedUser.getLogin()
      }

      val ownerComboBox = new ComboBox[PrintableUserAccount] {

        val obsBuf = new ObservableBuffer[PrintableUserAccount]
        userAccounts.map { userAccount => new PrintableUserAccount(userAccount) }.sortBy(ua => ua.getLogin()).copyToBuffer(obsBuf)

        items = obsBuf

        selectionModel().select(-1)
        prefWidth = Double.MaxValue
        //promptText = "<Select user>"
        //styleClass += ("comboBoxes")
      }

      val ownerWarningLabel = new Label {
        //    styleClass += ("warningLabels")
        style = "-fx-text-fill: red;  -fx-font-style: italic;"
        minHeight = 15
      }

      /** See user's projects */
      val seeUserProjects = new Hyperlink("See user's projects...") {
        style = "-fx-color:#66CCFF;"
        //        alignmentInParent = Pos.BASELINE_RIGHT

        //      }
        //        new Hyperlink("See user's projects...") {
        //        style = "-fx-color:#66CCFF;"
        //        alignmentInParent = Pos.BASELINE_RIGHT
        //
        onAction = handle {

          val userAccount = ownerComboBox.selectionModel().getSelectedItem()
          if (userAccount == null) {
            new PopupWindow("No owner selected", "You must select an owner first.", Option(Main.stage))
            //TODO: print all 

          } else {
            val projects = UdsRepository.findProjectsByOwnerId(userAccount.getId)

            val text: String = {
              if (projects.isEmpty) "No projects"
              else {
                projects.map { p =>
                  if (p.getDescription().isEmpty()) s"${p.getName()}"
                  else s"${p.getName()} [${p.getDescription()}]"
                }.mkString("\n")
              }
            }

            new PopupWindow(
              wTitle = s"Projects belonging to user '${userAccount.getLogin}'",
              wText = text,
              wParent = Option(Main.stage),
              isResizable = true
            )
          }
        }
      }

      /** Project name */

      val nameLabel = new Label("Project name* : ")

      val nameField = new TextField
      //      {
      //        styleClass += ("textFields")
      //      }

      val nameWarningLabel = new Label {
        //        styleClass += ("warningLabels")
        style = "-fx-text-fill: red;  -fx-font-style: italic;"
        minHeight = 15
      }

      /** Project description */

      val descLabel = new Label("Description : ")

      val descField = new TextArea {
        //        styleClass += ("textFields")
        prefHeight = 100
        wrapText = true
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
        vgap = 10
        hgap = 10
        padding = Insets(20)
        prefWidth = 500

        columnConstraints ++= Seq(
          new ColumnConstraints { percentWidth = 20 },
          new ColumnConstraints { percentWidth = 80 }
        )

        val s = Seq(
          (ownerLabel, 0, 0, 1, 1),
          (ownerComboBox, 1, 0, 1, 1),
          (ownerWarningLabel, 1, 1, 1, 1),
          (seeUserProjects, 1, 1, 1, 1),
          (nameLabel, 0, 2, 1, 1),
          (nameField, 1, 2, 1, 1),
          (nameWarningLabel, 1, 3, 2, 1),
          (descLabel, 0, 4, 1, 1),
          (descField, 1, 4, 1, 1),
          (Util.newVSpacer, 0, 5, 2, 1),
          (okButton, 0, 6, 2, 1)
        )
        Util.setGridContent5(s, this)
      }

      Seq(
        ownerLabel,
        seeUserProjects,
        nameLabel,
        descLabel
      ).foreach(GridPane.setHalignment(_, HPos.RIGHT))

      GridPane.setHalignment(okButton, HPos.CENTER)

      /**
       * ****** *
       * ACTION *
       * ****** *
       */

      okButton.onAction = handle {

        /** Empty warning labels */

        Seq(ownerWarningLabel,
          nameWarningLabel
        ).foreach(_.text = "")

        /** Check form */
        logger.debug("Checking project's form..."
        )
        val ownerIdx = ownerComboBox.selectionModel().getSelectedIndex()
        val isOwnerDefined: Boolean = ownerIdx >= 0
        val ownerOpt: Option[PrintableUserAccount] =
          if (isOwnerDefined) Option(ownerComboBox.selectionModel().getSelectedItem())
          else None

        val newProjectName = nameField.text()
        val isNameDefined = newProjectName.isEmpty == false

        logger.debug("Looking for user's projects")
        val userProjects: Array[Project] =
          if (ownerOpt.isEmpty) Array()
          else UdsRepository.findProjectsByOwnerId(ownerOpt.get.getId)

        logger.debug("Check if (user, project name) is available")
        val isNameAvailable = (userProjects.exists(_.getName() == newProjectName) == false) //for this user

        /** If all is ok, run action */

        if (isOwnerDefined && isNameDefined && isNameAvailable) {

          val ownerID: Long = ownerOpt.get.getId
          val newProjectDesc = descField.text()

          val cmd = s"""create_project --owner_id ${ownerID} --name "${newProjectName}"  --description "${newProjectDesc}""""

          /** Create project */
          logger.debug("Create project")
          LaunchAction(
            actionButton = ButtonsPanel.createProjectButton,
            actionString = Util.mkCmd(cmd),
            action = () => {

              val udsDbContext = UdsRepository.getUdsDbContext()
              val prolineConf = SetupProline.getUpdatedConfig

              try {
                // Create project
                val projectCreator = new CreateProject(udsDbContext, newProjectName, newProjectDesc, ownerID)
                projectCreator.doWork()
                val projectId = projectCreator.projectId

                // Create project databases
                if (projectId > 0L) {
                  new CreateProjectDBs(udsDbContext, prolineConf, projectId).doWork()
                } else {
                  logger.error("Invalid Project Id: " + projectId)
                }

              } finally {
                logger.debug("Closing current UDS Db Context")

                try {
                  udsDbContext.close()
                } catch {
                  case exClose: Exception => logger.error("Error closing UDS Db Context", exClose)
                }
              }
            }
          )

          newProjectDialog.close()

          /** Otherwise fill warning labels */
        } else {
          logger.debug("Invalid project form")
          if (isOwnerDefined == false) ownerWarningLabel.text = "Please select the project's owner."
          if (isNameDefined == false) nameWarningLabel.text = "Please enter a name for the project."
          else if (isNameAvailable == false) nameWarningLabel.text = "This project's name is not available."
        }
      }

    }
  }

  /** Display stage */
  _stage.showAndWait()

}

