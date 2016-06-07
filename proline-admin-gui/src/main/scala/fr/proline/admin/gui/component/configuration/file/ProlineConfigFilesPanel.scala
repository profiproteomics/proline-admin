package fr.proline.admin.gui.component.configuration.file

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.TextField
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.VBox
import scalafx.stage.Stage

import fr.proline.admin.gui.Main
import fr.proline.admin.gui.process.ProlineAdminConnection
import fr.proline.admin.gui.process.config.AdminConfigFile

import fr.profi.util.scala.ScalaUtils._
import fr.profi.util.scala.ScalaUtils
import fr.profi.util.scalafx.BoldLabel
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle._

/**
 * *************************************************************** *
 * Create a panel to select Proline configuration files. 					 *
 * Includes ProlineAdmin, Proline server, and PWX config files.    *
 * *************************************************************** *
 */
class ProlineConfigFilesPanel(onAdminConfigChange: AdminConfigFile => Unit = null)(implicit val parentStage: Stage) extends VBox with IConfigFilesPanel with LazyLogging {

  /* Proline config */
  private var serverConfigPathInAdminConfig: String = _
  private var pwxConfigPathInAdminConfig: String = _
  private var adminConfigFile: AdminConfigFile = _
  adminConfigFile = Main.getAdminConfigFile().getOrElse(null)

  /*
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
  val adminConfigWarningLabel = new Label {
    alignmentInParent = Pos.BottomLeft
    style = RED_ITALIC
    visible = false
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
    if (Main.serverConfPath != null) text = Main.serverConfPath
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
    style = GREY_ITALIC
    visible = false
  }
  
  // These values may be overwritten by checkFileFromField(), so they need to be reset after
  val NOT_MATCHING_WARNING_TEXT = "Warning: This does not correspond to the path specified in ProlineAdmin configuration."
  val NOT_MATCHING_WARNING_STYLE = ITALIC
  
  val serverConfigWarningLabel = new Label {
    text = NOT_MATCHING_WARNING_TEXT
    alignmentInParent = Pos.BottomLeft
    style = NOT_MATCHING_WARNING_STYLE
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
    if (Main.pwxConfPath != null) text = Main.pwxConfPath
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
    style = GREY_ITALIC
    visible = false
  }
  val pwxConfigWarningLabel = new Label {
    text = NOT_MATCHING_WARNING_TEXT
    alignmentInParent = Pos.BottomLeft
    style = NOT_MATCHING_WARNING_STYLE
    visible = false
  }

  /*
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
    }

  /* Organize and render */
  content = new VBox {
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
      }
    )
  }

  /*
   * ******** *
   * FEATURES *
   * ******** *
   */

  /** Actions run when the admin config path changes **/
  private def _onAdminConfigTextChange(newText: String) {
    if (isEmpty(newText) == false) {

      /* Update adminConfigFile with new path */
      adminConfigFile = new AdminConfigFile(newText)
      _setServerConfPathField()
      _setPwxConfPathField()

      /* Additional action */
      if (onAdminConfigChange != null) onAdminConfigChange(adminConfigFile)
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
    serverConfigWarningLabel.text = NOT_MATCHING_WARNING_TEXT
    serverConfigWarningLabel.style = NOT_MATCHING_WARNING_STYLE

    _onConfigTextChange(
      newText,
      pathInConfig = serverConfigPathInAdminConfig,
      nbLabel = serverConfigNbLabel,
      warningLabel = serverConfigWarningLabel
    )
  }
  private def _onPwxConfigTextChange(newText: String) {
    pwxConfigWarningLabel.text = NOT_MATCHING_WARNING_TEXT
    pwxConfigWarningLabel.style = NOT_MATCHING_WARNING_STYLE

    _onConfigTextChange(
      newText,
      pathInConfig = pwxConfigPathInAdminConfig,
      nbLabel = pwxConfigNbLabel,
      warningLabel = pwxConfigWarningLabel
    )
  }
  private def _onConfigTextChange(newText: String, pathInConfig: String, nbLabel: Label, warningLabel: Label) {
  
    /* Update labels visibility */
    if (newText.isEmpty() == false &&
      adminConfigField.text().isEmpty() == false) {

      if (newText == pathInConfig ||
        ScalaUtils.doubleBackSlashes(newText) == pathInConfig) {
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
    val filePath = ProlineConfigFileChooser.showIn(parentStage)
    if (filePath != null) adminConfigField.text = filePath
  }

  /** Browse Proline server configuration file: set global variable and update field **/
  private def _browseServerConfigFile() {
    ProlineConfigFileChooser.setForProlineServerConf(serverConfigField.text())
    val filePath = ProlineConfigFileChooser.showIn(parentStage)
    if (filePath != null) serverConfigField.text = filePath
  }

  /** Browse PWX configuration file: set global variable and update field **/
  private def _browsePwxConfigFile() {
    ProlineConfigFileChooser.setForPwxConf(pwxConfigField.text())
    val filePath = ProlineConfigFileChooser.showIn(parentStage)
    if (filePath != null) pwxConfigField.text = filePath
  }
  
  
  /* Getters/Setters for textFields */
  def getProlineAdminConfFile(): String = adminConfigField.text()
  def setProlineAdminConfFile(newPath: String) { adminConfigField.text = newPath }
  def setProlineAdminConfFile(newPathOpt: Option[String]) { setProlineAdminConfFile(newPathOpt.getOrElse("")) }

  def getProlineServerConfFile(): String = serverConfigField.text()
  def setProlineServerConfFile(newPath: String) { serverConfigField.text = newPath }
  def setProlineServerConfFile(newPathOpt: Option[String]) { setProlineServerConfFile(newPathOpt.getOrElse("")) }

  def getPwxConfFile(): String = pwxConfigField.text()
  def setPwxConfFile(newPath: String) { pwxConfigField.text = newPath }
  def setPwxConfFile(newPathOpt: Option[String]) { setPwxConfFile(newPathOpt.getOrElse("")) }

  /** Check the form, return a boolean. Display or hide warnings depending on form conformity **/
  def checkForm(allowEmptyPaths: Boolean = true): Boolean = Seq(
    (adminConfigField, adminConfigWarningLabel),
    (serverConfigField, serverConfigWarningLabel),
    (pwxConfigField, pwxConfigWarningLabel)
  ).forall { case (f, w) => this.checkFileFromField(f, w, allowEmptyPaths) }

  /** Save conf file(s) path in global variables and in admin config file **/
  def saveForm() {

    val selectedAdminConfigPath = adminConfigField.text()
    val selectedServerConfigPath = serverConfigField.text()
    val selectedPwxConfigPath = pwxConfigField.text()

    /* Update global variables */
    if (Main.adminConfPath != selectedAdminConfigPath) Main.adminConfPath = selectedAdminConfigPath
    if (Main.serverConfPath != selectedServerConfigPath) Main.serverConfPath = selectedServerConfigPath
    if (Main.pwxConfPath != selectedPwxConfigPath) Main.pwxConfPath = selectedPwxConfigPath
    Main.getAdminConfigFile().map{ adminConf =>
      adminConf.getPostgreSqlDataDir().map( Main.postgresqlDataDir = _ )
      adminConf.getSeqRepoConfigPath().map( Main.seqRepoConfPath = _ )
    }
    
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

    /* Load new config */
    ProlineAdminConnection.loadProlineConf(verbose = true)

    /* Reset warnings */
    Seq(
      serverConfigNbLabel,
      serverConfigWarningLabel,
      pwxConfigNbLabel,
      pwxConfigWarningLabel
    ).foreach(_.visible = false)

    /* Logback */
    val sb = new StringBuilder()
    sb ++= "[INFO]-- Configuration files paths --\n"
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