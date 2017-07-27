package fr.proline.admin.gui.wizard.component

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.control.CheckBox
import scalafx.scene.control.Button
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.layout.Priority
import scalafx.scene.layout.VBox
import scalafx.scene.layout.HBox
import scalafx.scene.control.TextField
import scalafx.scene.control.Hyperlink
import fr.profi.util.scalafx.BoldLabel
import scalafx.scene.control.Label
import fr.profi.util.scalafx.TitledBorderPane
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.Priority
import com.typesafe.scalalogging.LazyLogging
import javafx.scene.control.Tooltip
import scala.collection.mutable.ListBuffer
import scalafx.scene.text.{ Font, FontWeight, Text }
import java.io.IOException
import java.io.FileNotFoundException
import java.io.File
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.wizard.component.FileChooser._
import fr.proline.admin.gui.wizard.util._
import fr.proline.admin.gui.wizard.component.items._
import fr.profi.util.StringUtils
import java.io.File
import fr.proline.admin.gui.wizard.component.items.form.ItemsPanelForm
import fr.profi.util.scalafx
import fr.profi.util.scala.ScalaUtils
import fr.proline.admin.gui.process.config.AdminConfigFile
import fr.proline.admin.gui.process.config.AdminConfig
import fr.proline.repository.DriverType

/**
 * first panel to choose Config Items
 *
 */

object ItemsPanel extends VBox with ItemsPanelForm with LazyLogging {

  /** component of panel **/

  // help icon
  val headerHelpIcon = new Hyperlink {
    graphic = FxUtils.newImageView(IconResource.HELP)
    onAction = handle {
      _openHelpDialog()
    }
  }
  /** try to read proline admin configuration file */
  var iniServerPath = ""
  var iniSeqReposPath = ""
  var iniPgDirPath = ""
  var iniPwxPath = ""
  try {
    val adminConf = new AdminConfigFile(Wizard.adminConfPath)
    iniServerPath = adminConf.getServerConfigPath().getOrElse("")
    iniSeqReposPath = adminConf.getSeqRepoConfigPath().getOrElse("")
    iniPgDirPath = adminConf.getPostgreSqlDataDir().getOrElse("")
    iniPwxPath = adminConf.getPwxConfigPath().getOrElse("")
    warningCorruptedFile.visible = false
  } catch {
    case pe: com.typesafe.config.ConfigException.Parse => {
      //try to reinitialize initial  values of configuration files 
      logger.error("Error while trying to parse initial settings from Proline Admin configuration file.", pe)
      warningCorruptedFile.visible = true
      resetAdminConfig()
    }

  }

  // postgreSQL component 
  val postgreSQLChBox = new CheckBox("PostgreSQL Server Data Directory") {
    id = "postgresChBoxId"
    selected = true
    underline = true
    font = Font.font("SanSerif", FontWeight.Bold, 12)
    vgrow = Priority.Always
    minWidth = 112
  }
  val postgreSQLLabel = new HBox {
    minWidth = 150
    prefWidth = 250
    disable <== !postgreSQLChBox.selected
    content = List(new Label("Path to PostgreSQL Data Directory: "))
  }
  val postgreSQLField = new TextField() {
    disable <== !postgreSQLChBox.selected
    if (iniPgDirPath != null) {
      Wizard.pgDataDirPath = iniPgDirPath
      text = Wizard.pgDataDirPath
    }
    text.onChange { (_, oldText, newText) =>
      Wizard.pgDataDirPath = newText
    }
  }
  val postgresBrowseButton = new Button("Browse...") {
    disable <== !postgreSQLChBox.selected
    onAction = handle {
      _browseDataDir()
    }
  }

  //proline modules component 
  val prolineModulesChBox = new CheckBox("Proline Modules") {
    id = "moduleConfigId"
    selected = true
    underline = true
    vgrow = Priority.Always
    font = Font.font("SanSerif", FontWeight.Bold, 12)
    minWidth = 112
    onAction = handle { selectChildren }
  }
  //proline web componnent 
  val prolineWebChBox = new CheckBox("Proline Web Configuration File") {
    id = "prolineWebChBoxId"
    selected = false
    underline = true
    vgrow = Priority.Always
    minWidth = 112
    onAction = handle { selecteParent }
  }
  val prolineWebLabel = new HBox {
    minWidth = 150
    prefWidth = 250
    disable <== !prolineWebChBox.selected
    content = List(new Label("Path to Web Root ( File application.conf ): "))
  }
  val prolineWebField = new TextField() {
    disable <== !prolineWebChBox.selected
    if (iniPwxPath != null) {
      Wizard.webRootPath = iniPwxPath
      text = Wizard.webRootPath
    }
    text.onChange { (_, oldText, newText) =>
      Wizard.webRootPath = newText
    }
  }
  val prolineWebBrowseButton = new Button("Browse...") {
    disable <== !prolineWebChBox.selected
    onAction = handle {
      _browseProlineWebConfigFile()
    }
  }

  //seqRepos component 
  val seqReposChBox = new CheckBox("Sequence Repository Configuration File") {
    id = "seqReposChBoxId"
    selected = true
    underline = true
    vgrow = Priority.Always
    minWidth = 112
    onAction = handle { selecteParent }
  }
  val seqReposLabel = new HBox {
    minWidth = 150
    prefWidth = 250
    disable <== !seqReposChBox.selected
    content = List(new Label("Path to SeqRepo Root ( File application.conf ): "))
  }
  val seqReposField = new TextField() {
    disable <== !seqReposChBox.selected
    if (iniSeqReposPath != null) {
      Wizard.seqRepoConfPath = iniSeqReposPath
      text = Wizard.seqRepoConfPath
    }
    text.onChange { (_, oldText, newText) =>
      Wizard.seqRepoConfPath = newText
    }
  }
  val seqReposBrowseButton = new Button("Browse...") {
    disable <== !seqReposChBox.selected
    onAction = handle {
      _browseSeqReposConfigFile
    }
  }

  // proline server component 
  val prolineServerChBox = new CheckBox("Proline Server Configuration File") {
    id = "prolineServerChBoxId"
    selected = true
    underline = true
    vgrow = Priority.Always
    font = Font.font("SanSerif", FontWeight.Bold, 12)
    minWidth = 112

  }
  val prolineServerLabel = new HBox {
    minWidth = 150
    prefWidth = 250
    disable <== !prolineServerChBox.selected
    content = List(new Label("Path to Server Root ( File application.conf ): "))
  }
  val prolineServerField = new TextField() {
    disable <== !prolineServerChBox.selected
    if (iniServerPath != null) {
      Wizard.serverConfPath = iniServerPath
      text = Wizard.serverConfPath
    }
    text.onChange { (_, oldText, newText) =>
      Wizard.serverConfPath = newText
    }
  }
  val prolineServerBrowseButton = new Button("Browse...") {
    disable <== !prolineServerChBox.selected
    onAction = handle { _browseServerConfigFile() }
  }
  /* Style */
  Seq(postgreSQLField, prolineWebField, seqReposField, prolineServerField).foreach {
    f => f.hgrow = Priority.Always
  }
  /* layout */

  // used in HBox and VBox

  private val V_SPACING = 10
  private val H_SPACING = 5
  val warningBox = new VBox {
    spacing = 0.5
    content = Seq(warningCorruptedFile, errorNotValidServerFile, errorNotValidSeqReposFile, errorNotValidWebFile, errorNotValidPgData)
  }
  val configItemsPane = new TitledBorderPane(
    title = "Select Configuration Item",
    contentNode = new VBox {
      minWidth = 350
      minHeight = 400
      spacing = 10
      content = List(
        warningBox,
        ScalaFxUtils.newVSpacer(minH = 1, maxH = 1),
        prolineServerChBox,
        new HBox {
          spacing = 5
          content = Seq(ScalaFxUtils.newHSpacer(minW = 60, maxW = 60), prolineServerLabel, prolineServerField, prolineServerBrowseButton)
        },
        ScalaFxUtils.newVSpacer(minH = 1, maxH = 1),
        prolineModulesChBox,
        new HBox {
          spacing = 5
          content = Seq(ScalaFxUtils.newHSpacer(minW = 30, maxW = 30), prolineWebChBox)
        }, new HBox {
          spacing = 5
          content = Seq(ScalaFxUtils.newHSpacer(minW = 60, maxW = 60), prolineWebLabel, prolineWebField, prolineWebBrowseButton)
        }, new HBox {
          spacing = 5
          content = Seq(ScalaFxUtils.newHSpacer(minW = 30, maxW = 30), seqReposChBox)
        }, new HBox {
          spacing = 5
          content = Seq(ScalaFxUtils.newHSpacer(minW = 60, maxW = 60), seqReposLabel, seqReposField, seqReposBrowseButton)
        },
        ScalaFxUtils.newVSpacer(minH = 1, maxH = 1),
        postgreSQLChBox, new HBox {
          spacing = 5
          content = Seq(ScalaFxUtils.newHSpacer(minW = 60, maxW = 60), postgreSQLLabel, postgreSQLField, postgresBrowseButton)
        }, ScalaFxUtils.newVSpacer(150))
    })

  // final content 

  alignment = Pos.Center
  alignmentInParent = Pos.Center
  spacing = 1
  fillWidth = true
  content = List(ScalaFxUtils.newVSpacer(minH = 20), new HBox {
    alignment = Pos.TOP_RIGHT
    content = Seq(headerHelpIcon)
  }, configItemsPane)

  /* functions */

  //selected checkBox
  def selectChildren {
    if (prolineModulesChBox.isSelected == true) {
      prolineWebChBox.selected = true
      seqReposChBox.selected = true
    } else {
      prolineWebChBox.selected = false
      seqReposChBox.selected = false
    }
  }

  def selecteParent {
    if (prolineWebChBox.isSelected == true || seqReposChBox.isSelected == true) {
      prolineModulesChBox.selected = true
    }
    if (prolineWebChBox.isSelected == false && seqReposChBox.isSelected == false) {
      prolineModulesChBox.selected = false
    }
  }

  private val checkBoxList = List(prolineServerChBox, prolineWebChBox, seqReposChBox, postgreSQLChBox)

  // get the selected item(selected checkBox)
  def getSelectedItems: Unit = {
    if (!checkBoxList.isEmpty) {
      checkBoxList.foreach(checkBox => {
        checkBox.getId match {
          case "prolineServerChBoxId" => selectServerItem
          case "seqReposChBoxId" => selectSeqRepositoryItem
          case "prolineWebChBoxId" => selectProlineWebItem
          case "postgresChBoxId" => selectPostgreSQLItem
          case _ => logger.error("Error: selected item not found.")
        }
      })
    }
  }

  // select the Proline Server item 
  private def selectServerItem(): Boolean = {
    var isValidPath = false
    if (prolineServerChBox.isSelected) {
      if (checkFile(Wizard.serverConfPath)) {
        FieldProperties.removeBorder(prolineServerField)
        val jmsNodeConfPath = new File(Wizard.serverConfPath).getParent() + File.separator + """jms-node.conf"""
        if (new File(jmsNodeConfPath).exists) {
          errorNotValidServerFile.visible = false
          Wizard.jmsNodeConfPath = jmsNodeConfPath
          val server = new ServerConfig("server")
          Wizard.items += (server.name -> server)
          isValidPath = true
        } else {
          errorNotValidServerFile.visible = true
        }
      } else {
        errorNotValidServerFile.visible = true
        Wizard.items -= ("server")
      }
    } else {
      errorNotValidServerFile.visible = false
      Wizard.items -= ("server")
    }
    isValidPath
  }

  // select Sequence Repository  item
  private def selectSeqRepositoryItem(): Boolean = {
    var isValidPath = false
    if (seqReposChBox.isSelected) {
      if (checkFile(Wizard.seqRepoConfPath)) {
        FieldProperties.removeBorder(seqReposField)
        val jmsNodeConfPath = new File(seqReposField.getText).getParent() + File.separator + """jms-node.conf"""
        val parsingRules = new File(seqReposField.getText).getParent() + File.separator + """parsing-rules.conf"""
        if (new File(jmsNodeConfPath).exists && new File(parsingRules).exists) {
          errorNotValidSeqReposFile.visible = false
          Wizard.SeqJmsNodeConfPath = jmsNodeConfPath
          Wizard.parsingRulesPath = parsingRules
          val moduleConfig = new ModuleConfig("modules")
          Wizard.items += (moduleConfig.name -> moduleConfig)
          isValidPath = true
        } else {
          errorNotValidSeqReposFile.visible = true
        }
      } else {
        Wizard.items -= ("modules")
        errorNotValidSeqReposFile.visible = true
      }
    } else {
      Wizard.items -= ("modules")
      errorNotValidSeqReposFile.visible = false
    }
    isValidPath
  }

  //select the Proline Web item 
  private def selectProlineWebItem(): Boolean = {
    var isValidPath = false
    if (prolineWebChBox.isSelected) {
      if (checkFile(Wizard.webRootPath)) {
        errorNotValidWebFile.visible = false
        isValidPath = true
      } else {
        errorNotValidWebFile.visible = true
      }
    } else {
      errorNotValidWebFile.visible = false
    }
    isValidPath
  }

  // select PostgreSQL item :authorization and optimization tab
  private def selectPostgreSQLItem(): Boolean = {
    var isValidPath = false
    if (postgreSQLChBox.isSelected) {
      if (!Wizard.pgDataDirPath.isEmpty) {
        FieldProperties.removeBorder(postgreSQLField)
        if (validDataDirectory(Wizard.pgDataDirPath)) {
          val pgServerConfig = new PgServerConfig("pgServer")
          Wizard.items += (pgServerConfig.name -> pgServerConfig)
          errorNotValidPgData.visible = false
          isValidPath = true
        } else {
          Wizard.items -= ("pgServer")
          errorNotValidPgData.visible = true
        }
      } else {
        Wizard.items -= ("pgServer")
        errorNotValidPgData.visible = true
      }
    } else {
      Wizard.items -= ("pgServer")
      errorNotValidPgData.visible = false
    }
    isValidPath
  }

  // check selected fields when go button is pressed 
  def setStyleSelectedItems: Boolean = {
    var validPath, seqReposPath, serverPath, postGresPath, webPath = true
    if (postgreSQLChBox.isSelected) {
      if (!selectPostgreSQLItem) {
        FieldProperties.setBorder(postgreSQLField)
        postGresPath = false
      } else {
        postGresPath = true
      }
    } else {
      FieldProperties.removeBorder(postgreSQLField)
    }
    if (prolineServerChBox.isSelected) {
      if (!selectServerItem) {
        FieldProperties.setBorder(prolineServerField)
        serverPath = false
      } else {
        serverPath = true

      }
    } else FieldProperties.removeBorder(prolineServerField)
    if (seqReposChBox.isSelected) {
      if (!selectSeqRepositoryItem) {
        FieldProperties.setBorder(seqReposField)
        seqReposPath = false
      } else {
        FieldProperties.removeBorder(seqReposField)
        seqReposPath = true
      }
    } else {
      FieldProperties.removeBorder(seqReposField)
    }
    if (prolineWebChBox.isSelected) {
      if (!selectProlineWebItem) {
        FieldProperties.setBorder(prolineWebField)
        webPath = false
      } else {
        webPath = true
      }
    } else {
      FieldProperties.removeBorder(prolineWebField)
    }
    if (seqReposPath && serverPath && postGresPath && webPath) {
      validPath = true
    } else {
      validPath = false
    }
    validPath
  }
  // check configuration file : application.conf
  private def checkFile(filePath: String): Boolean = (new File(filePath).exists) && (new File(filePath).getName.equals("application.conf"))

  // to browse configurations files
  private def _browseServerConfigFile() {
    ConfFileChooser.setForProlineServerConf(prolineServerField.text())
    try {
      val filePath = ConfFileChooser.showIn(Wizard.stage)
      if (filePath != null) prolineServerField.text = filePath
    } catch {
      case e: Exception => logger.error("error in file's path ")
    }
  }
  private def _browseSeqReposConfigFile() {
    ConfFileChooser.setForSeqRepoConf(seqReposField.text())
    try {
      val filePath = ConfFileChooser.showIn(Wizard.stage)
      if (filePath != null) {
        seqReposField.text = filePath
      }
    } catch {
      case e: Exception => logger.error("error in file's path ")
    }
  }
  private def _browseProlineWebConfigFile() {
    ConfFileChooser.setForPwxConf(prolineWebField.text())
    try {
      val filePath = ConfFileChooser.showIn(Wizard.stage)
      if (filePath != null) prolineWebField.text = filePath
    } catch {
      case e: Exception => logger.error("error in file's path ")
    }
  }
  private def _browseDataDir() {
    val file = FxUtils.browseDirectory(
      dcTitle = "Select PostgreSQL data directory",
      dcInitialDir = postgreSQLField.text(),
      dcInitOwner = Wizard.stage)
    try {
      val newPath = file.getPath()
      if (file != null) {
        postgreSQLField.text = newPath
      }
    } catch {
      case e: Exception => logger.error("error in file's path ")
    }
  }

  //reset default setting when the configuration file is corrupted
  def resetAdminConfig() = {
    val defaultConfig = AdminConfig(Wizard.adminConfPath,
      Some(""),
      Some(""),
      Some(""),
      Some(""),
      Some(DriverType.POSTGRESQL),
      Some("<path/to/proline/data>"),
      Some("<db_user>"),
      Some("<db_password>"),
      Some("<db_host>"),
      Some(5432))
    new AdminConfigFile(Wizard.adminConfPath).write(defaultConfig)
  }

  // help dialog
  val helpTextBuilder = new StringBuilder()
  helpTextBuilder.append("Path to PostgreSQL data: The PostgreSQL data directory is defined when PostgreSQL is installed on the machine.\n")
    .append("This is the folder in which you will find the \"postgresql.conf\" and \"pg_hba.conf\" files.\n\n")
    .append("Path to Web Root: the full path to proline web configuration file\n\n")
    .append("Path to SeqRepos Root: the full path to Sequence Repository configuarion file application.conf\n\n")
    .append("Path to Proline Server Root: the full path to Proline Server configuarion file application.conf\n")
  private def _openHelpDialog() = PopupHelpWindow(
    wTitle = "Help",
    wText = helpTextBuilder.toString())
}

