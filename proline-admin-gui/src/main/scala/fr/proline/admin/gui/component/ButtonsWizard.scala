package fr.proline.admin.gui.component

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.application.Platform
import scalafx.geometry.Insets
import scalafx.scene.control.Button
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.Scene
import fr.proline.admin.gui.process._
import fr.proline.admin.gui.util._
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scala.ScalaUtils._
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle._
import fr.proline.admin.gui.component.configuration.file.ProlineConfigFilesPanelQStart
import fr.proline.admin.gui.QuickStart
import fr.proline.admin.gui.component.configuration.form._
import fr.proline.admin.gui.process.config._

/**
 * Create  panel contains buttons : cancel ,previous and next .
 */
object ButtonsPanelQStart extends LazyLogging {

  /*initialize panels */
  val prolineConfigFilesPanel = new ProlineConfigFilesPanelQStart()
  private val Databaseconfig = new DatabaseConfig()
  private val monutfiles = new MountFilesContent()
  private val adminConfigFile = new AdminConfigFile(QuickStart.adminConfPath)
  private val adminConfigOpt = adminConfigFile.read()
  require(adminConfigOpt.isDefined, "admin config is undefined")
  private val adminConfig = adminConfigOpt.get
  private val driverTypeOpt = adminConfig.driverType

  private val serverConfigFileOpt =
    if (isEmpty(QuickStart.serverConfPath)) None
    else Option(new ServerConfigFile(QuickStart.serverConfPath))
  private val serverConfigOpt = serverConfigFileOpt.map(_.read()).flatten
  private var buttonValue: String = _

  /**
   * ******* *
   * BUTTONS *
   * ******* *
   */

  /* buttons to navigaute bteweens panel */

  val nextButton = new Button("Next") {
    onAction = handle {
      getButton()
      panelStateOnNext()
      activePreviousButton()
      changeNextToFinish()
    }
  }
  val previousButton = new Button("Previous") {
    onAction = handle {
      panelStateOnPrevious()
      activePreviousButton()
      changeNextToFinish()
    }
  }

  /*exit  application */

  val cancelButton = new Button("Cancel") {
    onAction = handle {
      closeStage()
    }
  }

  /**
   * ***** *
   * LAYOUT *
   * **** *
   */

  Seq(
    nextButton,
    previousButton,
    cancelButton).foreach { b =>
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
        nextButton)
    }
  }
  previousButton.setVisible(false)

  /* set panel in mainPAnel when next button applied */

  private def panelStateOnNext() {
    if (QuickStart.panelState.equals("panelConfig")) {

      QuickStart.mainPanel.getChildren().clear()
      QuickStart.mainPanel.getChildren().add(Databaseconfig)
      QuickStart.panelState = "Databaseconfig"
    } else {
      if (QuickStart.panelState.equals("Databaseconfig")) {

        QuickStart.mainPanel.getChildren().clear()
        QuickStart.mainPanel.getChildren().add(monutfiles)
        QuickStart.panelState = "mountfiles"
      }
    }
  }

  /* when previous button is called*/

  def panelStateOnPrevious() {
    if (QuickStart.panelState.equals("Databaseconfig")) {

      QuickStart.mainPanel.getChildren().clear()
      QuickStart.mainPanel.getChildren().add(prolineConfigFilesPanel)
      QuickStart.panelState = "panelConfig"
    } else {
      if (QuickStart.panelState.equals("mountfiles")) {
        QuickStart.mainPanel.getChildren().clear()
        QuickStart.mainPanel.getChildren().add(Databaseconfig)
        QuickStart.panelState = "Databaseconfig"
      }
    }
  }

  /*set visible or not the button in buuton panel */

  private def activePreviousButton() {
    if (QuickStart.panelState.equals("panelConfig")) {
      previousButton.setVisible(false)
    } else {
      previousButton.setVisible(true)
    }
  }

  /*change buuton next to finisj in the end */

  private def changeNextToFinish() {
    if (QuickStart.panelState.equals("mountfiles")) {
      nextButton.setText("Finish")
    } else {
      nextButton.setText("Next")
    }
  }

  /*save params and close*/

  private def getButton() {
    buttonValue = nextButton.getText()
    if (buttonValue.equals("Finish")) {
      saveParamsInFileConfig()
      closeStage()
    }
  }

  /* save params in file admin .conf */

  private def _parseToAdminConfig() = AdminConfig(
    filePath = QuickStart.adminConfPath,
    serverConfigFilePath = Option(QuickStart.serverConfPath),
    pwxConfigFilePath = None,
    pgsqlDataDir = None,
    seqRepoConfigFilePath = Option(QuickStart.seqRepoConfPath),
    driverType = driverTypeOpt,
    prolineDataDir = None,
    dbUserName = Option(QuickStart.userName),
    dbPassword = Option(QuickStart.passwordUser),
    dbHost = Option(QuickStart.hostNameUser),
    dbPort = Option(QuickStart.port))

  /*parse to serverconfig */

  private def _parseToServerConfig() = ServerConfig(
    QuickStart.rawFiles,
    QuickStart.mzdbFiles,
    QuickStart.resultFiles)

  /* save params in admin .conf and server .conf */

  private def saveParamsInFileConfig() {

    /*save proline admin params */

    val newAdminConfig = _parseToAdminConfig()
    adminConfigFile.write(newAdminConfig)

    /* Save proline server parameters */

    if (serverConfigOpt.isDefined) {
      val newServerConfig = _parseToServerConfig()
      serverConfigFileOpt.get.write(newServerConfig, newAdminConfig)
    }
  }
  /* close window */

  private def closeStage() {
    QuickStart.stage.close()
  }
}