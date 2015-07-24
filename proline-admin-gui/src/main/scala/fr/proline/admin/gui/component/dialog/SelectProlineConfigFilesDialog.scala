package fr.proline.admin.gui.component.dialog

import com.typesafe.scalalogging.slf4j.Logging

import java.io.File

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Cursor
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.TextField
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.VBox
import scalafx.stage.Modality
import scalafx.stage.Stage

import fr.profi.util.scala.ScalaUtils
import fr.profi.util.scala.ScalaUtils.isEmpty
import fr.profi.util.scalafx.BoldLabel
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.proline.admin.gui.Main
import fr.proline.admin.gui.process.config.AdminConfigFile

object SelectProlineConfigFilesDialog extends Stage with Logging {

  val dialog = this

  /* Stage's properties */
  title = s"Select configuration files"
  initModality(Modality.WINDOW_MODAL)
  initOwner(Main.stage)

  private var serverConfigPathInAdminConfig: String = _
  private var adminConfigFile: AdminConfigFile = _
  if (isEmpty(Main.adminConfPath) == false ) adminConfigFile = new AdminConfigFile(Main.adminConfPath)
  

  /**
   * ********** *
   * COMPONENTS *
   * ********** *
   */

  /* Proline Admin configuration file */
  val adminConfigLabel = new HBox {
    content = List(
      new Label("Full path to "),
      new BoldLabel("ProlineAdmin", upperCase = false),
      new Label(" configuration file :")
    )
  }
  val adminConfigField = new TextField {
    text = Main.adminConfPath
    text.onChange { (_, oldText, newText) =>
      _onAdminConfigTextChange(newText)
    }
  }
  val adminConfigBrowse = new Button("Browse...") {
    onAction = handle { _browseAdminConfigFile() }
  }

  /* Proline Server configuration file */
  val serverConfigLabel = new HBox {
    content = List(
      new Label("Full path to "),
      new BoldLabel("Proline server", upperCase = false),
      new Label(" configuration file :")
    )
  }

  val serverConfigField = new TextField() {
    text.onChange { (_, oldText, newText) =>
      _onServerConfigTextChange(newText)
    }
  }
  val serverConfigBrowse = new Button("Browse...") {
    onAction = handle { _browseServerConfigFile }
  }
  val serverConfigNbLabel = new Label {
    text = "NB: this information was taken from ProlinAdmin configuration."
    alignmentInParent = Pos.BottomLeft
    style = "-fx-font-style: italic;-fx-text-fill:grey"
    visible = false
  }
  val serverConfigWarningLabel = new Label {
    text = "Warning: This does not correspond to the path specified in ProlineAdmin configuration."
    alignmentInParent = Pos.BottomLeft
    style = "-fx-font-style: italic;"
    visible = false
  }
  // TODO: Button("select file specified in admin")

  /* Buttons */
  val saveButton = new Button("Save") { onAction = handle { _onApplyPressed() } }
  val cancelButton = new Button("Cancel") { onAction = handle { dialog.close() } }

  /**
   * ****** *
   * LAYOUT *
   * ****** *
   */

  Seq(
    adminConfigField,
    serverConfigField
  ).foreach { f =>
      f.hgrow = Priority.Always
      //f.onKeyReleased = (ke: KeyEvent) => ScalaFxUtils.fireIfEnterPressed(saveButton, ke)
    }

  val buttons = new HBox {
    hgrow = Priority.Always
    alignment = Pos.BaselineCenter
    content = Seq(
      saveButton,
      ScalaFxUtils.newHSpacer(maxW = 10),
      cancelButton
    )
  }

  /* Scene */
  scene = new Scene {
    
    onKeyPressed = (ke: KeyEvent) => {
      closeIfEscapePressed(dialog, ke)
      fireIfEnterPressed(saveButton, ke)
    }
    
    root = new VBox {
      padding = Insets(20)
      minWidth = 464
      prefWidth = 592
      spacing = 5

      content = Seq(

        adminConfigLabel,
        new HBox {
          spacing = 5
          content = Seq(adminConfigField, adminConfigBrowse)
        },
        ScalaFxUtils.newVSpacer(minH = 10),

        serverConfigLabel,
        new HBox {
          spacing = 5
          content = Seq(serverConfigField, serverConfigBrowse)
        },
        new StackPane {
          content = List(serverConfigNbLabel, serverConfigWarningLabel)
          alignmentInParent = Pos.BaselineLeft
        },
        ScalaFxUtils.newVSpacer(minH = 10),

        buttons
      )
    }
  }

  /**
   * ******** *
   * FEATURES *
   * ******** *
   */

  /** Init and show this stage **/
  def apply() {
    
    adminConfigField.text = Main.adminConfPath
    //adminConfigFile = new AdminConfigFile(Main.adminConfPath)
    //serverConfigPathInAdminConfig = adminConfigFile.getServerConfigPath().getOrElse("")

    if (Main.serverConfPath != null) serverConfigField.text = Main.serverConfPath

    this.showAndWait()
  }
  
  /** Actions run when the admin config path changes **/
  private def _onAdminConfigTextChange(newText: String) {

    if (isEmpty(newText) == false) {

      /* Update adminConfigFile with new path */
      adminConfigFile = new AdminConfigFile(newText)

      /* Try to get server config path */
      val pathOptInAdminConfig = adminConfigFile.getServerConfigPath()
      val pathInField = serverConfigField.text()

      /* If there is some path in ProlineAdmin config */
      if (pathOptInAdminConfig.isDefined) {

        val pathInAdminConfig = pathOptInAdminConfig.get

        serverConfigPathInAdminConfig = pathInAdminConfig

        /* If field was empty, set retrieved path */
        if (pathInField.isEmpty()) {
          serverConfigField.text = pathInAdminConfig
          serverConfigNbLabel.visible = true
          serverConfigWarningLabel.visible = false
        }
        
        /* If not, test if it's the same */ 
        else {
          serverConfigPathInAdminConfig = pathInAdminConfig
          // let the previously referred one ???

          serverConfigNbLabel.visible = false
          if (pathInField == pathInAdminConfig) {
            serverConfigWarningLabel.visible = false
          } else {
            serverConfigWarningLabel.visible = true
          }
        }
      }
      
      /* If there is no path in ProlineAdmin config, just hide the putative NB / warning */
      else {
        serverConfigNbLabel.visible = false
        serverConfigWarningLabel.visible = false
      }
    }
  }

  /** Actions run when the server config path changes **/
  private def _onServerConfigTextChange(newText: String) {
    
    /* Update labels visibility */
    if (
      newText.isEmpty() == false &&
      adminConfigField.text().isEmpty() == false
    ) {

      if (
        newText == serverConfigPathInAdminConfig ||
        ScalaUtils.doubleBackSlashes(newText) == serverConfigPathInAdminConfig
      ) {
        serverConfigNbLabel.visible = true
        serverConfigWarningLabel.visible = false
      } else {
        serverConfigNbLabel.visible = false
        serverConfigWarningLabel.visible = true
      }

    } else {
      serverConfigNbLabel.visible = false
      serverConfigWarningLabel.visible = false
    }
    //set serverConfigPathInAdminConfig = pathInAdminConfig if needed
  }

  /** Browse ProlineAdmin configuration file: set global variable and update field **/
  private def _browseAdminConfigFile() {
    val filePath = BrowseProlineAdminConfigFile(adminConfigField.text(), dialog)
    if (filePath != null) adminConfigField.text = filePath
    //BrowseProlineAdminConfigFile(dialog)
    //adminConfigField.text = Main.adminConfPath
  }

  /** Browse Proline server configuration file: set global variable and update field **/
  private def _browseServerConfigFile() {
    val filePath = BrowseProlineServerConfigFile(serverConfigField.text(), dialog)
    if (filePath != null) serverConfigField.text = filePath
    //val newFilePath = BrowseProlineServerConfigFile(dialog)
    //serverConfigField.text = Main.serverConfPath
  }

  /** Action run when "Apply" in pressed **/
  private def _onApplyPressed() {

    Main.stage.scene().setCursor(Cursor.WAIT)

    val selectedAdminConfigPath = adminConfigField.text()
    val selectedServerConfigPath = serverConfigField.text()

    /* Make sure Admin config file is provided */
    if (isEmpty(selectedAdminConfigPath)) {
      ShowPopupWindow(
        "ProlineAdmin configuration file is missing",
        "Path to ProlineAdmin configuration file must be provided to use this application."
      )
    } else {

      /* Make sure extension is CONF */
      val adminIsConf = ScalaUtils.getFileExtension(selectedAdminConfigPath) matches """(?i)conf"""
      val serverIsConf =
        if (selectedServerConfigPath.isEmpty()) true //no extension filter if not defined (not mandatory)
        else ScalaUtils.getFileExtension(selectedServerConfigPath) matches """(?i)conf"""

      if (!adminIsConf || !serverIsConf) {
        var str = if (adminIsConf) "" else "ProlineAdmin configuration file extension should be .conf."
        if (serverIsConf == false) str += "\nProline server configuration file extension should be .conf."

        ShowPopupWindow("Invalid configuration file", str)

      } else {

        /* Test if files exist, offer to create them if not */
        val adminFile = new File(selectedAdminConfigPath)
        val serverFile = new File(selectedServerConfigPath)
        val adminExists = adminFile.exists()
        val serverExists =
          if (selectedServerConfigPath.isEmpty()) true //no extension filter if not defined (not mandatory)
          else serverFile.exists()

        var errorCount = 0
        var str = ""
        if (!adminExists) {
          errorCount += 1
          str += "The specified ProlineAdmin configuration file doesn't exist."
        }
        if (!serverExists) {
          errorCount += 1
          str += "\nThe specified Proline server configuration file doesn't exist."
        }

        var isConfirmed = true
        if (errorCount != 0) {
          isConfirmed = GetConfirmation(
            title = if (errorCount == 1) "Unexisting file" else "Unexisting files",
            text = str,
            yesText = if (errorCount == 1) "Create file" else "Create files"
          )

          if (isConfirmed) {
            if (!adminExists) { adminFile.createNewFile() }
            if (!serverExists) { serverFile.createNewFile() }
          }
        }

        /* If file(s) exist(s) */
        if (errorCount == 0 || isConfirmed) {

          /* Store path to server config in admin config if needed */
          if (
            isEmpty(selectedServerConfigPath) == false &&
            (serverConfigPathInAdminConfig == null || serverConfigPathInAdminConfig != selectedServerConfigPath)
          ) {
            adminConfigFile.setServerConfigPath(selectedServerConfigPath)
          }

          /* Update global variables */
          if (Main.adminConfPath != selectedAdminConfigPath) Main.adminConfPath = selectedAdminConfigPath
          if (Main.serverConfPath != selectedServerConfigPath) Main.serverConfPath = selectedServerConfigPath

          /* Reset warnings and close dialog */
          serverConfigNbLabel.visible = false
          serverConfigWarningLabel.visible = false
          dialog.close()

          /* Logback */
          val sb = new StringBuilder()
          
          sb ++= "[INFO]-- Configuration files' paths --\n"
          sb ++= "[INFO]ProlineAdmin @ " + selectedAdminConfigPath + "\n"
          sb ++= "[INFO]-- Configuration files' paths --\n"

          if (selectedServerConfigPath.isEmpty()) sb ++= "[INFO]Proline server: undefined\n"
          else sb ++= "[INFO]Proline server @ " + selectedServerConfigPath + "\n"

          sb ++= "[INFO]------------------"
          
          val msg = sb.result()
          println(msg)
          logger.debug(msg)
        }
      }
    }

    Main.stage.scene().setCursor(Cursor.DEFAULT)
  }
}