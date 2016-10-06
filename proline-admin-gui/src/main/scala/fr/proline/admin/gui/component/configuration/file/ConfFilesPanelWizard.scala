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

import fr.proline.admin.gui.QuickStart
import fr.proline.admin.gui.process.ProlineAdminConnection
import fr.proline.admin.gui.process.config.AdminConfigFile

import fr.profi.util.scala.ScalaUtils._
import fr.profi.util.scala.ScalaUtils
import fr.profi.util.scalafx.BoldLabel
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle._
import fr.proline.admin.gui.QuickStart

import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter

import fr.profi.util.scalafx.TitledBorderPane
import javafx.scene.control.Tooltip
import fr.proline.admin.postgres.install._
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.component.ButtonsPanelQStart
/**
 * 
 * Step 1 : window to search and add configurations files
 * 
 */
class ProlineConfigFilesPanelQStart(onAdminConfigChange: AdminConfigFile => Unit = null) extends VBox with IConfigFilesPanel with  LazyLogging {

  /* Proline config */
  
  private var serverConfigPathInAdminConfig: String = _
  private var pwxConfigPathInAdminConfig: String = _
  private var adminConfigFile: AdminConfigFile = _
  adminConfigFile = QuickStart.getAdminConfigFile().getOrElse(null)
  QuickStart.panelState="panelConfig"
  private val V_SPACING = 10

   
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
    if (QuickStart.adminConfPath != null) text = QuickStart.adminConfPath
    text.onChange { (_, oldText, newText) =>
      _onAdminConfigTextChange(newText)
      updateAdminConf(newText)
    }
  }
  adminConfigField.setTooltip(new Tooltip("full path to proline admin configuration file."));
  val adminConfigBrowse = new Button("Browse...") {
    onAction = handle { 
      _browseAdminConfigFile() 
      }
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
    if (QuickStart.serverConfPath != null) text = QuickStart.serverConfPath
    text.onChange { (_, oldText, newText) =>
      _onServerConfigTextChange(newText)
      updateServerConf(newText)
    }
  }
  serverConfigField.setTooltip(new Tooltip("full path to proline server configuration file."));
  val serverConfigBrowse = new Button("Browse...") {
    onAction = handle {
      _browseServerConfigFile 
      }
  }
  val serverConfigNbLabel = new Label {
    text = "NB: this information was taken from ProlinAdmin configuration."
    alignmentInParent = Pos.BottomLeft
    style = GREY_ITALIC
    visible = false
  }
  val NOT_MATCHING_WARNING_TEXT = "Warning: This does not correspond to the path specified in ProlineAdmin configuration."
  val NOT_MATCHING_WARNING_STYLE = ITALIC
  val serverConfigWarningLabel = new Label {
    text = NOT_MATCHING_WARNING_TEXT
    alignmentInParent = Pos.BottomLeft
    style = NOT_MATCHING_WARNING_STYLE
    visible = false
  }
  
  /* Sequence Repository configuration file will be selected automatically */
  
  val seqReposConfigLabel = new HBox {
    content = List(
      new Label("Full path to "),
      new BoldLabel("Sequence Repository", upperCase = false),
      new Label(" configuration file :")
    )
  }
  val seqReposConfigField = new TextField() {
   if (QuickStart.seqRepoConfPath != null) text = QuickStart.seqRepoConfPath
    text.onChange { (_, oldText, newText) =>
      _onSeqReposConfigTextChange(newText)
      updateSeqReposConf(newText)
    }
  }
   seqReposConfigField.setTooltip(new Tooltip("full path to sequence repository configuration file."));
  val pwxConfigBrowse = new Button("Browse...") {
    onAction = handle {
       _browseSequenceRepositoryFile
      }
  }
  val pwxConfigNbLabel = new Label {
    text = "NB: this information was taken from ProlineAdmin configuration."
    alignmentInParent = Pos.BottomLeft
    style = GREY_ITALIC
    visible = false
  }
  val seqReposConfigWarningLabel = new Label {
    text = NOT_MATCHING_WARNING_TEXT
    alignmentInParent = Pos.BottomLeft
    style = NOT_MATCHING_WARNING_STYLE
    visible = false
  }
  
  /* Select data directory */
  
  val dataDirectoryLabel = new HBox {
    content = List(
      new Label("Full path to "),
      new BoldLabel("postgreSQL data directory", upperCase = false),
      new Label(" to optimize yout database server (optional) : ")
    )
  }
  val dataDirectoryField = new TextField() {
   if (QuickStart.postgresqlDataDir != null) text =  QuickStart.postgresqlDataDir
    text.onChange { (_, oldText, newText) =>
    updateDataDirectoryPath(newText) 
    
    /* update global variable for the first object prolineConfigurationpanel of ButtonsPAnelQstart */ 
    
    ButtonsPanelQStart.prolineConfigFilesPanel.setDataDirectoryPath(QuickStart.postgresqlDataDir)
    
    }
  }
  dataDirectoryField.setTooltip(new Tooltip("full path to postgreSQL data Directory .Example : ..\\PostgreSQL\\9.x\\data"));
   val dataDirectoryBrowse = new Button("Browse...") {
    onAction = handle {
      
      /* check if postgreSQL is installed on unix and windows */ 
      
      if(CheckInstalledPostgres.isWindows()){
        CheckInstalledPostgres.checkPostgres()
       }
      if(CheckInstalledPostgres.isUnix()){
         CheckInstalledPostgres.checkPostgres()
       }
      _browseDataDir()
     }
  }
  dataDirectoryBrowse.setTooltip(new Tooltip("Browse postgreSQL data Directory. "));
  
  /*
   * ****** *
   * LAYOUT *
   * ****** *
   */

  Seq(
    adminConfigField,
    serverConfigField,
    seqReposConfigField,
    dataDirectoryField
  ).foreach { f =>
      f.hgrow = Priority.Always
    }

  /* Organize and render */
   val configurationsFiles = new TitledBorderPane(
      
    title = "Step 1 : select proline configuration file", 
    contentNode = new VBox {
    minWidth = 360
    prefWidth = 360
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
       ScalaFxUtils.newVSpacer(minH = 10),
      seqReposConfigLabel,
       new HBox {
        spacing = 5
        content = Seq(seqReposConfigField, pwxConfigBrowse)
      },
      ScalaFxUtils.newVSpacer(minH = 10),
      dataDirectoryLabel,
       new HBox {
        spacing = 5
        content = Seq(dataDirectoryField, dataDirectoryBrowse)
      },
      new StackPane {
        content = List(serverConfigNbLabel, serverConfigWarningLabel,seqReposConfigWarningLabel)
        alignmentInParent = Pos.BaselineLeft
      },
      ScalaFxUtils.newVSpacer(minH = 10),
      ScalaFxUtils.newVSpacer(minH = 10),
      ScalaFxUtils.newVSpacer(minH = 10),
      ScalaFxUtils.newVSpacer(minH = 10)
    )
  })
 alignment = Pos.Center
  alignmentInParent = Pos.Center
  spacing = 4 * V_SPACING
  content = List(
     ScalaFxUtils.newVSpacer(minH =1),
     configurationsFiles
  )
 
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
   

      /* Additional action */ 
      
      if (onAdminConfigChange != null) onAdminConfigChange(adminConfigFile)
    }
  }

  /* Try to get and set server config file path in dedicated textField */
  private def _setServerConfPathField() {
    _setConfPathField(
      pathOptInAdminConfig = adminConfigFile.getServerConfigPath(),
      field = serverConfigField,
      nbLabel = serverConfigNbLabel,
      warningLabel = serverConfigWarningLabel
    )
  }
  
  /* Try to get and set config file path in dedicated textField */
  
  private def _setConfPathField(pathOptInAdminConfig: Option[String], field: TextField, nbLabel: Label, warningLabel: Label) {
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

  /* Actions run when the server config path changes */
  
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
  private def _onSeqReposConfigTextChange(newText: String) {
    seqReposConfigWarningLabel.text = NOT_MATCHING_WARNING_TEXT
    seqReposConfigWarningLabel.style = NOT_MATCHING_WARNING_STYLE

    _onConfigTextChange(
      newText,
      pathInConfig = pwxConfigPathInAdminConfig,
      nbLabel = pwxConfigNbLabel,
      warningLabel = seqReposConfigWarningLabel
    )
  }
  def showIn(stage: Stage): String = {
    try {
        val fc = new FileChooser {

    title = "Select Proline configuration file"
    extensionFilters.add(new ExtensionFilter("Configuration files", "*.conf"))
    extensionFilters.add(new ExtensionFilter("All files", "*.*"))
      }

      val confPath = fc.showOpenDialog(stage).getPath()

      /* Validate path */
      require(confPath matches """.+\.conf$""", "invalid path for configuration file")

      confPath

    } catch {
      
      case jfx: java.lang.NullPointerException => {
        logger.debug("configuration file selected.")
        null
      }

      case t: Throwable => {
        throw t
        null
      }
    }
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
  }

  /* Browse ProlineAdmin configuration file: set global variable and update field */
  
  private def _browseAdminConfigFile() {
    ProlineConfigFileChooserWizard.setForProlineAdminConf(adminConfigField.text())
       val filePath = ProlineConfigFileChooserWizard.showIn(QuickStart.stage)
       if (filePath != null) adminConfigField.text = filePath
  }

  /* Browse Proline server configuration file: set global variable and update field */
  
  private def _browseServerConfigFile() {
    ProlineConfigFileChooserWizard.setForProlineServerConf(serverConfigField.text())
     val filePath = ProlineConfigFileChooserWizard.showIn(QuickStart.stage)
    if (filePath != null) serverConfigField.text = filePath
  }
  
  /* browse Sequence Repository configuration file: set global variable and update field */
 
  private def _browseSequenceRepositoryFile() {
     ProlineConfigFileChooserWizard.setForProlineServerConf(seqReposConfigField.text())
     val filePath = ProlineConfigFileChooserWizard.showIn(QuickStart.stage)
    if (filePath != null) seqReposConfigField.text = filePath
    
  }
  
  /* browse data directory of PostgreSQL */
 private def _browseDataDir() {
    val file = FxUtils.browseDirectory(
      dcTitle = "Select PostgreSQL data directory",
      dcInitialDir = dataDirectoryField.text()
    )
    val newPath = file.getPath()
    if (file != null) {
      dataDirectoryField.text = newPath
     QuickStart.postgresqlDataDir=newPath
    }
  }
 /* update adminConf  global variables */
  
  def updateAdminConf(adminconf:String){
    QuickStart.adminConfPath=adminconf
  } 
  
  /*update server global variables  */
  
  def updateServerConf(serverConf:String){
    QuickStart.serverConfPath=serverConf
  }
  
  /* update sequebce repository global variable */
  
  def updateSeqReposConf(seqReposConf:String){
    QuickStart.seqRepoConfPath=seqReposConf
    
  }
  /* update data directory global variable */
  
  def updateDataDirectoryPath(dataDirectoryPath:String){
    QuickStart.postgresqlDataDir=dataDirectoryPath
  }
  
  /* Getters/Setters for textFields */
  
  def getProlineAdminConfFile(): String = adminConfigField.text()
  def setProlineAdminConfFile(newPath: String) { adminConfigField.text = newPath }
  def setProlineAdminConfFile(newPathOpt: Option[String]) { setProlineAdminConfFile(newPathOpt.getOrElse("")) }
  def setDataDirectoryPath(path:String){dataDirectoryField.text=path}
  def getProlineServerConfFile(): String = serverConfigField.text()
  def setProlineServerConfFile(newPath: String) { serverConfigField.text = newPath }
  def setProlineServerConfFile(newPathOpt: Option[String]) { setProlineServerConfFile(newPathOpt.getOrElse("")) }

  def getPwxConfFile(): String = seqReposConfigField.text()
  def setPwxConfFile(newPath: String) { seqReposConfigField.text = newPath }
  def setPwxConfFile(newPathOpt: Option[String]) { setPwxConfFile(newPathOpt.getOrElse("")) }

  /* Check the form, return a boolean. Display or hide warnings depending on form conformity */
  
  def checkForm(allowEmptyPaths: Boolean = true): Boolean = Seq(
    (adminConfigField, adminConfigWarningLabel),
    (serverConfigField, serverConfigWarningLabel),
    (seqReposConfigField, seqReposConfigWarningLabel)
  ).forall { case (f, w) => this.checkFileFromField(f, w, allowEmptyPaths) }

  /* Save conf file(s) path in global variables and in admin config file */
  
  def saveForm() {

    val selectedAdminConfigPath = adminConfigField.text()
    val selectedServerConfigPath = serverConfigField.text()
    val selectedPwxConfigPath = seqReposConfigField.text()

    /* Update global variables */
    
    if (QuickStart.adminConfPath != selectedAdminConfigPath) QuickStart.adminConfPath = selectedAdminConfigPath
    if (QuickStart.serverConfPath != selectedServerConfigPath) QuickStart.serverConfPath = selectedServerConfigPath
    if (QuickStart.pwxConfPath != selectedPwxConfigPath) QuickStart.pwxConfPath = selectedPwxConfigPath
    QuickStart.getAdminConfigFile().map{ adminConf =>
      adminConf.getPostgreSqlDataDir().map( QuickStart.postgresqlDataDir = _ )
      adminConf.getSeqRepoConfigPath().map( QuickStart.seqRepoConfPath = _ )
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
      seqReposConfigWarningLabel
    ).foreach(_.visible = false)
  }
}