package fr.proline.admin.gui.wizard.component

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos

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
import scalafx.stage.Stage
import fr.profi.util.scalafx.TitledBorderPane
import scalafx.scene.layout.StackPane
import com.typesafe.scalalogging.LazyLogging
import javafx.scene.control.Tooltip
import scala.collection.mutable.ListBuffer

import java.io.IOException
import java.io.FileNotFoundException
import java.io.File
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.wizard.component.FileChooser._
import fr.proline.admin.gui.wizard.util._
import fr.proline.admin.gui.wizard.component.items._
import fr.profi.util.StringUtils
import java.io.File
import fr.proline.admin.gui.wizard.component.items.form.HomePanel
import fr.profi.util.scalafx
import fr.profi.util.scala.ScalaUtils
import fr.proline.admin.gui.process.config.AdminConfigFile
import fr.proline.admin.gui.process.config.AdminConfig
import fr.proline.repository.DriverType

/**
 * first panel to choose Config Items
 *
 */

object InstallPanel extends VBox with HomePanel with LazyLogging {

  /** component of panel **/

  /* try to read proline admin configuration file */
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
      logger.error("Error while trying to parse initial settings from Proline Admin configuration file. Default settings will be reset.")
      warningCorruptedFile.visible = true
      resetAdminConfig(Wizard.adminConfPath)
    }
  }

  val confChooser = new ConfFileChooser(Wizard.targetPath)
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
    graphic = FxUtils.newImageView(IconResource.LOAD)
    disable <== !postgreSQLChBox.selected
    onAction = handle {
      _browseDataDir(Wizard.stage)
    }
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
    graphic = FxUtils.newImageView(IconResource.LOAD)
    disable <== !prolineWebChBox.selected
    onAction = handle {
      _browseProlineWebConfigFile(Wizard.stage)
    }
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
  val seqReposBrowseButton = new Button("Browse ...") {
    graphic = FxUtils.newImageView(IconResource.LOAD)
    disable <== !seqReposChBox.selected
    onAction = handle {
      _browseSeqReposConfigFile(Wizard.stage)
    }
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
    graphic = FxUtils.newImageView(IconResource.LOAD)
    disable <== !prolineServerChBox.selected
    onAction = handle {
      _browseServerConfigFile(Wizard.stage)
    }
  }

  /* Style */
  Seq(postgreSQLLabel, prolineWebLabel, seqReposLabel, prolineServerLabel).foreach(_.minWidth = 60)
  Seq(postgreSQLField, prolineWebField, seqReposField, prolineServerField).foreach {
    f => f.hgrow = Priority.Always
  }

  /* layout */
  private val V_SPACING = 10
  private val H_SPACING = 5
  val warningBox = new VBox {
    spacing = 0.5
    children = Seq(warningCorruptedFile, errorNotValidServerFile, errorNotValidSeqReposFile, errorNotValidWebFile, errorNotValidPgData)
  }
  val configItemsPane = new TitledBorderPane(
    title = "Select Configuration Item",
    contentNode = new VBox {
      spacing = 10
      children = List(
        warningBox,
        ScalaFxUtils.newVSpacer(minH = 1, maxH = 1),
        prolineServerChBox,
        new HBox {
          spacing = 5
          children = Seq(ScalaFxUtils.newHSpacer(minW = 60, maxW = 60), prolineServerLabel, prolineServerField, prolineServerBrowseButton)
        },
        ScalaFxUtils.newVSpacer(minH = 1, maxH = 1),
        prolineModulesChBox,
        new HBox {
          spacing = 5
          children = Seq(ScalaFxUtils.newHSpacer(minW = 30, maxW = 30), seqReposChBox)
        }, new HBox {
          spacing = 5
          children = Seq(ScalaFxUtils.newHSpacer(minW = 60, maxW = 60), seqReposLabel, seqReposField, seqReposBrowseButton)
        }, new HBox {
          spacing = 5
          children = Seq(ScalaFxUtils.newHSpacer(minW = 30, maxW = 30), prolineWebChBox)
        }, new HBox {
          spacing = 5
          children = Seq(ScalaFxUtils.newHSpacer(minW = 60, maxW = 60), prolineWebLabel, prolineWebField, prolineWebBrowseButton)
        },
        ScalaFxUtils.newVSpacer(minH = 1, maxH = 1),
        postgreSQLChBox, new HBox {
          spacing = 5
          children = Seq(ScalaFxUtils.newHSpacer(minW = 60, maxW = 60), postgreSQLLabel, postgreSQLField, postgresBrowseButton)
        }, ScalaFxUtils.newVSpacer(10))
    })

  // final content 
  alignment = Pos.Center
  alignmentInParent = Pos.Center
  spacing = 1
  fillWidth = true
  children = Seq(ScalaFxUtils.newVSpacer(minH = 20),
    configItemsPane)

  private val checkBoxList = List(prolineServerChBox.getId, prolineWebChBox.getId, seqReposChBox.getId, postgreSQLChBox.getId)

  // get selected item
  def getSelectedItems {
    checkBoxList.map {
      case "prolineServerChBoxId" => selectServerItem
      case "seqReposChBoxId" => selectSeqRepositoryItem
      case "prolineWebChBoxId" => selectProlineWebItem
      case "postgresChBoxId" => selectPostgreSQLItem
      case _ => None
    }
  }
  // select Proline-Server item 
  private def selectServerItem(): Boolean = {
    var isValidPath = false
    if (prolineServerChBox.isSelected) {
      if (ScalaUtils.isConfFile(Wizard.serverConfPath)) {
        val jmsNodeConfPath = new File(Wizard.serverConfPath).getParent() + File.separator + """jms-node.conf"""
        if (new File(jmsNodeConfPath).exists) {
          ScalaFxUtils.NodeStyle.remove(prolineServerField)
          ScalaFxUtils.NodeStyle.hide(errorNotValidServerFile)
          Wizard.jmsNodeConfPath = jmsNodeConfPath
          val server = new ServerConfig("server")
          Wizard.items += (server.name -> Some(server))
          isValidPath = true
        } else {
          Wizard.items -= ("server")
          ScalaFxUtils.NodeStyle.show(errorNotValidServerFile)
          ScalaFxUtils.NodeStyle.set(prolineServerField)
        }
      } else {
        Wizard.items -= ("server")
        ScalaFxUtils.NodeStyle.show(errorNotValidServerFile)
        ScalaFxUtils.NodeStyle.set(prolineServerField)
      }
    } else {
      Wizard.items -= ("server")
      ScalaFxUtils.NodeStyle.hide(errorNotValidServerFile)
      ScalaFxUtils.NodeStyle.remove(prolineServerField)
    }
    isValidPath
  }

  // select Sequence Repository item
  private def selectSeqRepositoryItem(): Boolean = {
    var isValidPath = false
    if (seqReposChBox.isSelected) {
      if (ScalaUtils.isConfFile(Wizard.seqRepoConfPath)) {
        ScalaFxUtils.NodeStyle.remove(seqReposField)
        val jmsNodeConfPath = new File(seqReposField.getText).getParent() + File.separator + """jms-node.conf"""
        val parsingRules = new File(seqReposField.getText).getParent() + File.separator + """parsing-rules.conf"""
        if (new File(jmsNodeConfPath).exists && new File(parsingRules).exists) {
          ScalaFxUtils.NodeStyle.hide(errorNotValidSeqReposFile)
          Wizard.SeqJmsNodeConfPath = jmsNodeConfPath
          Wizard.parsingRulesPath = parsingRules
          val moduleConfig = new ModuleConfig("modules")
          Wizard.items += (moduleConfig.name -> Some(moduleConfig))
          isValidPath = true
        } else {
          ScalaFxUtils.NodeStyle.show(errorNotValidSeqReposFile)
        }
      } else {
        Wizard.items -= ("modules")
        ScalaFxUtils.NodeStyle.show(errorNotValidSeqReposFile)
        ScalaFxUtils.NodeStyle.set(seqReposField)

      }
    } else {
      Wizard.items -= ("modules")
      ScalaFxUtils.NodeStyle.hide(errorNotValidSeqReposFile)
      ScalaFxUtils.NodeStyle.remove(seqReposField)
    }
    isValidPath
  }

  //select the Proline Pwx item 
  private def selectProlineWebItem(): Boolean = {
    var isValidPath = false
    if (prolineWebChBox.isSelected) {
      if (ScalaUtils.isConfFile(Wizard.webRootPath)) {
        ScalaFxUtils.NodeStyle.remove(prolineWebField)
        ScalaFxUtils.NodeStyle.hide(errorNotValidWebFile)
        val prolineWeb = new ProlineWebConfig("prolineWeb")
        Wizard.items += (prolineWeb.name -> Some(prolineWeb))
        isValidPath = true
      } else {
        Wizard.items -= ("prolineWeb")
        ScalaFxUtils.NodeStyle.show(errorNotValidWebFile)
        ScalaFxUtils.NodeStyle.set(prolineWebField)
      }
    } else {
      Wizard.items -= ("prolineWeb")
      ScalaFxUtils.NodeStyle.hide(errorNotValidWebFile)
      ScalaFxUtils.NodeStyle.remove(prolineWebField)
    }
    isValidPath
  }

  //select PostgreSQL item 
  private def selectPostgreSQLItem(): Boolean = {
    var isValidPath = false
    if (postgreSQLChBox.isSelected) {
      if (!Wizard.pgDataDirPath.isEmpty) {
        ScalaFxUtils.NodeStyle.remove(postgreSQLField)
        if (ScalaUtils.isValidDataDir(Wizard.pgDataDirPath)) {
          ScalaFxUtils.NodeStyle.hide(errorNotValidPgData)
          val pgServerConfig = new PgServerConfig("pgServer")
          Wizard.items += (pgServerConfig.name -> Some(pgServerConfig))
          isValidPath = true
        } else {
          Wizard.items -= ("pgServer")
          ScalaFxUtils.NodeStyle.show(errorNotValidPgData)
          ScalaFxUtils.NodeStyle.set(postgreSQLField)
        }
      } else {
        Wizard.items -= ("pgServer")
        ScalaFxUtils.NodeStyle.show(errorNotValidPgData)
        ScalaFxUtils.NodeStyle.set(postgreSQLField)
      }
    } else {
      Wizard.items -= ("pgServer")
      ScalaFxUtils.NodeStyle.hide(errorNotValidPgData)
      ScalaFxUtils.NodeStyle.remove(postgreSQLField)
    }
    isValidPath
  }

  //check selected fields 
  def setStyleSelectedItems: Boolean = {
    var validPath, seqReposPath, serverPath, postGresPath, webPath = true
    if (postgreSQLChBox.isSelected) {
      if (!selectPostgreSQLItem) {
        ScalaFxUtils.NodeStyle.set(postgreSQLField)
        postGresPath = false
      } else {
        postGresPath = true
      }
    } else {
      ScalaFxUtils.NodeStyle.remove(postgreSQLField)
    }
    if (prolineServerChBox.isSelected) {
      if (!selectServerItem) {
        ScalaFxUtils.NodeStyle.set(prolineServerField)
        serverPath = false
      } else {
        serverPath = true
      }
    } else ScalaFxUtils.NodeStyle.remove(prolineServerField)
    if (seqReposChBox.isSelected) {
      if (!selectSeqRepositoryItem) {
        ScalaFxUtils.NodeStyle.set(seqReposField)
        seqReposPath = false
      } else {
        ScalaFxUtils.NodeStyle.remove(seqReposField)
        seqReposPath = true
      }
    } else {
      ScalaFxUtils.NodeStyle.remove(seqReposField)
    }
    if (prolineWebChBox.isSelected) {
      if (!selectProlineWebItem) {
        ScalaFxUtils.NodeStyle.set(prolineWebField)
        webPath = false
      } else {
        webPath = true
      }
    } else {
      ScalaFxUtils.NodeStyle.remove(prolineWebField)
    }
    if (seqReposPath && serverPath && postGresPath && webPath) {
      validPath = true
    } else {
      validPath = false
    }
    validPath
  }

  // browse configurations files dialog 
  def _browseServerConfigFile(stage: Stage) {
    confChooser.setForProlineServerConf(prolineServerField.text())
    try {
      val filePath = confChooser.showIn(stage)
      if (filePath != null) prolineServerField.text = filePath
    } catch {
      case t: Throwable => logger.error("error in file's path ")
    }
  }
  def _browseSeqReposConfigFile(stage: Stage) {
    confChooser.setForSeqRepoConf(seqReposField.text())
    try {
      val filePath = confChooser.showIn(stage)
      if (filePath != null) {
        seqReposField.text = filePath
      }
    } catch {
      case t: Throwable => logger.error("error in file's path ")
    }
  }
  def _browseProlineWebConfigFile(stage: Stage) {
    confChooser.setForPwxConf(prolineWebField.text())
    try {
      val filePath = confChooser.showIn(stage)
      if (filePath != null) prolineWebField.text = filePath
    } catch {
      case t: Throwable => logger.error("error in file's path ")
    }
  }
  def _browseDataDir(stage: Stage) {
    val file = FxUtils.browseDirectory(
      dcTitle = "Select PostgreSQL data directory",
      dcInitialDir = postgreSQLField.text(),
      dcInitOwner = stage)
    try {
      val newPath = file.getPath()
      if (file != null) {
        postgreSQLField.text = newPath
      }
    } catch {
      case t: Throwable => logger.error("error in file's path ")
    }
  }

}

