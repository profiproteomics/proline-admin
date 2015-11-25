package fr.proline.admin.gui.component.dialog

import com.typesafe.scalalogging.LazyLogging
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
import scala.collection.mutable.HashMap

object SelectProlineConfigFilesDialog extends Stage with LazyLogging {

  val dialog = this

  /* Stage's properties */
  title = s"Select configuration files"
  initModality(Modality.WINDOW_MODAL)
  initOwner(Main.stage)

  private var serverConfigPathInAdminConfig: String = _
  private var pwxConfigPathInAdminConfig: String = _
  private var adminConfigFile: AdminConfigFile = _
  if (isEmpty(Main.adminConfPath) == false ) adminConfigFile = new AdminConfigFile(Main.adminConfPath)
  
  private var closedWithSave = false

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

  /* PWX configuration file */
    val pwxConfigLabel = new HBox {
    content = List(
      new Label("Full path to "),
      new BoldLabel("Proline Web Extension (PWX)", upperCase = false),
      new Label(" configuration file :")
    )
  }
    
  val pwxConfigField = new TextField() {
    text.onChange { (_, oldText, newText) =>
      _onPwxConfigTextChange(newText)
    }
  }
  val pwxConfigBrowse = new Button("Browse...") {
    onAction = handle { _browsePwxConfigFile }
  }
  val pwxConfigNbLabel = new Label {
    text = "NB: this information was taken from ProlinAdmin configuration."
    alignmentInParent = Pos.BottomLeft
    style = "-fx-font-style: italic;-fx-text-fill:grey"
    visible = false
  }
  val pwxConfigWarningLabel = new Label {
    text = "Warning: This does not correspond to the path specified in ProlineAdmin configuration."
    alignmentInParent = Pos.BottomLeft
    style = "-fx-font-style: italic;"
    visible = false
  }
  // TODO: Button("select file specified in admin")

  /* Buttons */
  val saveButton = new Button("Save") { onAction = handle { _onApplyPressed() } }
  val cancelButton = new Button("Cancel") {
    onAction = handle {
      closedWithSave = false
      dialog.close()
    } 
  }

  /**
   * ****** *
   * LAYOUT *
   * ****** *
   */

  Seq(
    adminConfigField,
    serverConfigField,
    pwxConfigField
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

        pwxConfigLabel,
        new HBox {
          spacing = 5
          content = Seq(pwxConfigField, pwxConfigBrowse)
        },
        new StackPane {
          content = List(pwxConfigNbLabel, pwxConfigWarningLabel)
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
  def apply(): Boolean = {
    
    adminConfigField.text = Main.adminConfPath
    if (Main.serverConfPath != null) serverConfigField.text = Main.serverConfPath
    if (Main.pwxConfPath != null) pwxConfigField.text = Main.pwxConfPath

    this.showAndWait()
    this.closedWithSave
  }
  
  /** Actions run when the admin config path changes **/
  private def _onAdminConfigTextChange(newText: String) {

    if (isEmpty(newText) == false) {

      /* Update adminConfigFile with new path */
      adminConfigFile = new AdminConfigFile(newText)

      _setServerConfPathField()
      _setPwxConfPathField()
    }
  }

  /** Try to get and set server config file path in dedicated textField **/
  private def _setServerConfPathField() {
    _setConfPathField(
      pathOptInAdminConfig = adminConfigFile.getServerConfigPath(),
      field = serverConfigField,
      nbLabel = serverConfigNbLabel,
      warningLabel = serverConfigWarningLabel
    )
  }
  /** Try to get and set PWX config file path in dedicated textField **/
  private def _setPwxConfPathField() {
    _setConfPathField(
      pathOptInAdminConfig = adminConfigFile.getPwxConfigPath(),
      field = pwxConfigField,
      nbLabel = pwxConfigNbLabel,
      warningLabel = pwxConfigWarningLabel
    )
  }
  /** Try to get and set config file path in dedicated textField **/
  private def _setConfPathField(pathOptInAdminConfig: Option[String], field: TextField, nbLabel: Label, warningLabel: Label) {
    /* If there is some path in ProlineAdmin config */
    if (pathOptInAdminConfig.isDefined) {

      val pathInAdminConfig = pathOptInAdminConfig.get
      val pathInField = field.text()

      /* If field was empty, set retrieved path */
      if (pathInField.isEmpty()) {
        field.text = pathInAdminConfig
        nbLabel.visible = true
        warningLabel.visible = false
      }

      /* If not, test if it's the same */
      else {
        //TODO ? get me back?
        //serverConfigPathInAdminConfig = pathInAdminConfig
        // let the previously referred one ???

        nbLabel.visible = false
        warningLabel.visible = pathInField != pathInAdminConfig
      }
    }

    /* If there is no path in ProlineAdmin config, just hide the putative NB / warning */
    else {
      serverConfigNbLabel.visible = false
      serverConfigWarningLabel.visible = false
    }
  }

  /** Actions run when the server config path changes **/
  private def _onServerConfigTextChange(newText: String) {
    _onConfigTextChange(
      newText,
      pathInConfig = serverConfigPathInAdminConfig,
      nbLabel = serverConfigNbLabel,
      warningLabel = serverConfigWarningLabel
    )
  }
  private def _onPwxConfigTextChange(newText: String) {
	  _onConfigTextChange(
			  newText,
			  pathInConfig = pwxConfigPathInAdminConfig,
			  nbLabel = pwxConfigNbLabel,
			  warningLabel = pwxConfigWarningLabel
			  )
  }
  private def _onConfigTextChange(newText: String, pathInConfig: String, nbLabel: Label, warningLabel: Label) {
    /* Update labels visibility */
    if (
      newText.isEmpty() == false &&
      adminConfigField.text().isEmpty() == false
    ) {

      if (
        newText == pathInConfig ||
        ScalaUtils.doubleBackSlashes(newText) == pathInConfig
      ) {
        nbLabel.visible = true
        warningLabel.visible = false
      } else {
        nbLabel.visible = false
        warningLabel.visible = true
      }

    } else {
      nbLabel.visible = false
      warningLabel.visible = false
    }
    //set serverConfigPathInAdminConfig = pathInAdminConfig if needed
  }
  
  /** Browse ProlineAdmin configuration file: set global variable and update field **/
  private def _browseAdminConfigFile() {
    ProlineConfigFileChooser.setForProlineAdminConf(adminConfigField.text())
    val filePath = ProlineConfigFileChooser.showIn(dialog)
    if (filePath != null) adminConfigField.text = filePath
  }

  /** Browse Proline server configuration file: set global variable and update field **/
  private def _browseServerConfigFile() {
    ProlineConfigFileChooser.setForProlineServerConf(serverConfigField.text())
    val filePath = ProlineConfigFileChooser.showIn(dialog)
    if (filePath != null) serverConfigField.text = filePath
  }
  
  /** Browse PWX configuration file: set global variable and update field **/
  private def _browsePwxConfigFile() {
	  ProlineConfigFileChooser.setForPwxConf(pwxConfigField.text())
	  val filePath = ProlineConfigFileChooser.showIn(dialog)
	  if (filePath != null) pwxConfigField.text = filePath
  }

  /** Action run when "Apply" in pressed **/
  private def _onApplyPressed() {

    Main.stage.scene().setCursor(Cursor.WAIT)

    val selectedAdminConfigPath = adminConfigField.text()
    val selectedServerConfigPath = serverConfigField.text()
    val selectedPwxConfigPath = pwxConfigField.text()

    /* Make sure Admin config file is provided */
    if (isEmpty(selectedAdminConfigPath)) {
      ShowPopupWindow(
        wTitle = "ProlineAdmin configuration file is missing",
        wText = "Path to ProlineAdmin configuration file must be provided to use this application."
      )
    }
    else {

      def _isConf(filePath: String): Boolean = {
        if (filePath.isEmpty()) true //no extension filter if not defined (WARNING: not mandatory files only)
        else ScalaUtils.getFileExtension(filePath) matches """(?i)conf"""
      }

      /* Make sure extension is CONF */
      val adminIsConf = ScalaUtils.getFileExtension(selectedAdminConfigPath) matches """(?i)conf"""
      val serverIsConf = _isConf(selectedServerConfigPath)
      val pwxIsConf = _isConf(selectedPwxConfigPath)

      if (!adminIsConf || !serverIsConf || !pwxIsConf) {
        var str = if (adminIsConf) "" else "ProlineAdmin configuration file extension should be .conf."
        if (serverIsConf == false) str += "\nProline server configuration file extension should be .conf."
        if (pwxIsConf == false) str += "\nPWX configuration file extension should be .conf."

        ShowPopupWindow(wTitle = "Invalid configuration file", wText = str)

      }
      else {

        /* Test if files exist */
        def _fileExists(filePath: String): Boolean = {
          if (filePath.isEmpty()) true //WARNING! not mandatory files
          else new File(filePath).exists()
        }
        val adminExists = new File(selectedAdminConfigPath).exists()
        val serverExists = _fileExists(selectedServerConfigPath)
        val pwxExists = _fileExists(selectedPwxConfigPath)

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
        if (!pwxExists) {
          errorCount += 1
          str += "\nThe specified PWX configuration file doesn't exist."
        }

        //TODO: get me back, but write default config into new files
        /*var isConfirmed = true
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
        }*/

        /* If one file doesn't exist */
        if (errorCount > 0) {
          ShowPopupWindow(
            wTitle = if (errorCount == 1) "Unexisting file" else "Unexisting files",
            wText = str
          )
        }

        /* If file(s) exist(s) */
        else {

          /* Store server config path in admin config if needed */
          if (isEmpty(selectedServerConfigPath) == false &&
            (serverConfigPathInAdminConfig == null || serverConfigPathInAdminConfig != selectedServerConfigPath)) {
            adminConfigFile.setServerConfigPath(selectedServerConfigPath)
          }
          /* Store PWX config path in admin config if needed */
          if (isEmpty(selectedPwxConfigPath) == false &&
            (pwxConfigPathInAdminConfig == null || pwxConfigPathInAdminConfig != selectedPwxConfigPath)) {
            adminConfigFile.setPwxConfigPath(selectedPwxConfigPath)
          }

          /* Update global variables */
          if (Main.adminConfPath != selectedAdminConfigPath) Main.adminConfPath = selectedAdminConfigPath
          if (Main.serverConfPath != selectedServerConfigPath) Main.serverConfPath = selectedServerConfigPath
          if (Main.pwxConfPath != selectedPwxConfigPath) Main.pwxConfPath = selectedPwxConfigPath

          /* Reset warnings and close dialog */
          serverConfigNbLabel.visible = false
          serverConfigWarningLabel.visible = false
          pwxConfigNbLabel.visible = false
          pwxConfigWarningLabel.visible = false
          closedWithSave = true
          dialog.close()

          /* Logback */
          val sb = new StringBuilder()
          
          sb ++= "[INFO]-- Configuration files' paths --\n"
          sb ++= "[INFO]ProlineAdmin @ " + selectedAdminConfigPath + "\n"

          if (selectedServerConfigPath.isEmpty()) sb ++= "[INFO]Proline server: undefined\n"
          else sb ++= "[INFO]Proline server @ " + selectedServerConfigPath + "\n"

          if (selectedPwxConfigPath.isEmpty()) sb ++= "[INFO]PWX: undefined\n"
          else sb ++= "[INFO]PWX @ " + selectedPwxConfigPath + "\n"

          sb ++= "[INFO]------------------"
          
          val msg = sb.result()
          println(msg) //for integrated console output
          logger.debug(msg)
        }
      }
    }

    Main.stage.scene().setCursor(Cursor.DEFAULT)
  }
}