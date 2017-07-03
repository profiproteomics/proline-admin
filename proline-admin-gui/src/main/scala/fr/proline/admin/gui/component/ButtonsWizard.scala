package fr.proline.admin.gui.component

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.application.Platform
import scalafx.geometry.Insets
import scalafx.scene.control.Button
import scalafx.scene.control.Button._
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.Scene
import fr.proline.admin.gui.process._
import fr.proline.admin.gui.util._
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scala.ScalaUtils._
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle._
import fr.proline.admin.gui.component.wizard._
import fr.proline.admin.gui.QuickStart
import fr.proline.admin.gui.component.configuration.form._
import fr.proline.admin.gui.process.config._
import fr.proline.admin.gui.component.wizard.MountFilesContent
import scalafx.scene.control.Label
import fr.proline.admin.gui.IconResource
import java.io.File

/**
 * Create  panel contains buttons : cancel ,previous and next .
 */
object ButtonsPanelQStart extends LazyLogging {
  var Databaseconfig: DatabaseConfig = null
  //initialize panels 
  val prolineConfigFilesPanel = new ProlineConfigFilesPanelQStart()
  try {
    Databaseconfig = new DatabaseConfig()
  } catch {
    case e: Exception => logger.info("the file application.conf is corrupted and can not be opened.")
  }
  // private val Databaseconfig = new DatabaseConfig()
  private var buttonValue: String = _
  var mountFiles: MountFilesContent = null

  /**
   * ******* *
   * BUTTONS *
   * ******* *
   */

  val nextButton = new Button("Next >") {
    onAction = handle {
      getButton()
      panelStateOnNext()
      activePreviousButton()
      changeNextToFinish()
    }
  }
  val previousButton = new Button("< Previous") {
    onAction = handle {
      panelStateOnPrevious()
      activePreviousButton()
      changeNextToFinish()
    }
  }

  // button to skip this step 

  val skipButton = new Button("Skip") {
    onAction = handle {
      skipStep()
      activePreviousButton()
      changeNextToFinish()
    }
  }
  // exit  application 

  val cancelButton = new Button("Exit") {
    onAction = handle {
      confirmDialog()
    }
  }
  // Warning 
  val warningAboutExitText = "Are you sure you want to exit Proline Setup? "
  val warningAboutExitLabel = new Label {
    graphic = ScalaFxUtils.newImageView(IconResource.WARNING)
    text = "Exit Proline Setup"
  }

  /**
   * ***** *
   * LAYOUT *
   * **** *
   */

  Seq(
    nextButton,
    previousButton,
    cancelButton,
    skipButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }

  /**
   * ****** *
   * APPLY() *
   * ***** *
   */

  /** Display these buttons in a HBox **/
  def apply(): HBox = {
    new HBox {
      padding = Insets(10)
      spacing = 10
      content = Seq(
        cancelButton,
        ScalaFxUtils.newHSpacer(),
        ScalaFxUtils.newHSpacer(),
        ScalaFxUtils.newHSpacer(),
        previousButton,
        nextButton,
        skipButton)
    }
  }
  previousButton.setVisible(false)

  // change panel when next button is applied 

  private def panelStateOnNext() {
    if (QuickStart.panelState.equals("panelConfig")) {
      if ((QuickStart.adminConfPath != null) && (!QuickStart.adminConfPath.isEmpty) && (QuickStart.serverConfPath != null) && (!QuickStart.serverConfPath.isEmpty)) {
        prolineConfigFilesPanel.validStep()
        prolineConfigFilesPanel.setAdminfield(QuickStart.adminConfPath)
        prolineConfigFilesPanel.setServerfield(QuickStart.serverConfPath)
        prolineConfigFilesPanel.setSeqfield(QuickStart.seqRepoConfPath)
        QuickStart.mainPanel.getChildren().clear()
        QuickStart.mainPanel.getChildren().add(Databaseconfig)
        QuickStart.panelState = "Databaseconfig"
      }
    } else {
      if (QuickStart.panelState.equals("Databaseconfig")) {
        QuickStart.mainPanel.getChildren().clear()
        mountFiles = new MountFilesContent()
        QuickStart.mainPanel.getChildren().add(mountFiles)
        QuickStart.panelState = "mountfiles"
        QuickStart.buttonsPanel.getChildren().remove(skipButton)

      }
    }
  }

  // change panel when previous button is applied 

  def panelStateOnPrevious() {
    if (QuickStart.panelState.equals("Databaseconfig")) {
      prolineConfigFilesPanel.setAdminfield(QuickStart.adminConfPath)
      prolineConfigFilesPanel.setServerfield(QuickStart.serverConfPath)
      prolineConfigFilesPanel.setSeqfield(QuickStart.seqRepoConfPath)
      QuickStart.mainPanel.getChildren().clear()
      QuickStart.mainPanel.getChildren().add(prolineConfigFilesPanel)
      QuickStart.panelState = "panelConfig"
    } else {
      if (QuickStart.panelState.equals("mountfiles")) {
        QuickStart.mainPanel.getChildren().clear()
        QuickStart.mainPanel.getChildren().add(Databaseconfig)
        QuickStart.panelState = "Databaseconfig"
        QuickStart.buttonsPanel.getChildren().add(skipButton)
      }
    }
  }

  // set visible or not the button in button panel 

  private def activePreviousButton() {
    if (QuickStart.panelState.equals("panelConfig")) {
      previousButton.setVisible(false)
    } else {
      previousButton.setVisible(true)
    }
  }

  private def changeNextToFinish() {
    if (QuickStart.panelState.equals("mountfiles")) {
      nextButton.setText("Finish")
    } else {
      nextButton.setText("Next >")
    }
  }

  // save parameters and close 

  private def getButton() {
    buttonValue = nextButton.getText()
    if (buttonValue.equals("Finish")) {
      mountFiles.saveForm()
    }
  }
  // confirm dialog 
  private def confirmDialog() {

    ExitConfirmWindow(
      wTitle = "WARNING",
      wText = warningAboutExitText,
      wParent = Option(QuickStart.stage))
  }
  // skip this step 
  def skipStep() {
    if (QuickStart.panelState.equals("panelConfig")) {
      if ((QuickStart.adminConfPath != null) && (!QuickStart.adminConfPath.isEmpty)) {
        prolineConfigFilesPanel.validStep()
        prolineConfigFilesPanel.setAdminfield(QuickStart.adminConfPath)
        prolineConfigFilesPanel.setServerfield(QuickStart.serverConfPath)
        prolineConfigFilesPanel.setSeqfield(QuickStart.seqRepoConfPath)
        QuickStart.mainPanel.getChildren().clear()
        QuickStart.mainPanel.getChildren().add(Databaseconfig)
        QuickStart.panelState = "Databaseconfig"
      }
    } else {
      if (QuickStart.panelState.equals("Databaseconfig")) {
        QuickStart.mainPanel.getChildren().clear()
        mountFiles = new MountFilesContent()
        QuickStart.mainPanel.getChildren().add(mountFiles)
        QuickStart.panelState = "mountfiles"
        QuickStart.buttonsPanel.getChildren().remove(skipButton)
      }
    }

  }
  // close window 

  private def closeStage() {

    QuickStart.stage.close()
  }
}