package fr.proline.admin.gui.wizard.service

import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.control.Button
import scalafx.scene.layout.Priority
import scalafx.scene.layout.VBox
import scalafx.scene.layout.HBox
import scalafx.scene.control.TextField
import scalafx.scene.Node
import scalafx.stage.Stage
import java.io.File
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.PostInstall
import fr.proline.admin.gui.wizard.component.panel.main.INotification
import fr.proline.admin.gui.wizard.component.panel.main.ConfFileChooser
import fr.proline.admin.gui.wizard.component.items._
import fr.proline.admin.gui.process.config.AdminConfigFile
import fr.proline.admin.gui.wizard.util.ItemName._
import fr.proline.admin.gui.wizard.util._
import fr.profi.util.scala.ScalaUtils
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.profi.util.scalafx.TitledBorderPane
import com.typesafe.scalalogging.LazyLogging
import scalafx.scene.control.CheckBox.sfxCheckBox2jfx
import scalafx.scene.control.TextField.sfxTextField2jfx

/**
 * builds home panel
 *
 */

object PostInstallPanel extends VBox with INotification with LazyLogging {

  var iniServerPath = ""
  var iniSeqReposPath = ""
  var iniPgDirPath = ""
  var iniPwxPath = ""
  try {
    val adminConf = new AdminConfigFile(PostInstall.adminConfPath)
    iniServerPath = adminConf.getServerConfigPath().getOrElse("")
    iniSeqReposPath = adminConf.getSeqRepoConfigPath().getOrElse("")
    iniPgDirPath = adminConf.getPostgreSqlDataDir().getOrElse("")
    corruptedFileLabel.visible = false
  } catch {
    case pe: com.typesafe.config.ConfigException.Parse => {
      logger.error("Error while trying to parse initial settings from Proline Admin configuration file. Default settings will be reset.")
      corruptedFileLabel.visible = true
      resetAdminConfig(PostInstall.adminConfPath)
    }
  }
  /* component of post install home panel */
  val confChooser = new ConfFileChooser(PostInstall.targetPath)
  val postgreSQLField = new TextField() {
    disable <== !postgreSQLChBox.selected
    if (iniPgDirPath != null) {
      PostInstall.pgDataDirPath = iniPgDirPath
      text = PostInstall.pgDataDirPath
    }
    text.onChange { (_, oldText, newText) =>
      PostInstall.pgDataDirPath = newText
    }
  }
  val postgresBrowseButton = new Button("Browse...") {
    graphic = FxUtils.newImageView(IconResource.LOAD)
    disable <== !postgreSQLChBox.selected
    onAction = handle {
      _browseDataDir(PostInstall.stage)
    }
  }
  val seqReposField = new TextField() {
    disable <== !seqReposChBox.selected
    if (iniSeqReposPath != null) {
      PostInstall.seqRepoConfPath = iniSeqReposPath
      text = PostInstall.seqRepoConfPath
    }
    text.onChange { (_, oldText, newText) =>
      PostInstall.seqRepoConfPath = newText
    }
  }
  val seqReposBrowseButton = new Button("Browse ...") {
    graphic = FxUtils.newImageView(IconResource.LOAD)
    disable <== !seqReposChBox.selected
    onAction = handle {
      _browseSeqReposConfigFile(PostInstall.stage)
    }
  }

  val prolineServerField = new TextField() {
    disable <== !prolineServerChBox.selected
    if (iniServerPath != null) {
      PostInstall.serverConfPath = iniServerPath
      text = PostInstall.serverConfPath
    }
    text.onChange { (_, oldText, newText) =>
      PostInstall.serverConfPath = newText
    }
  }
  val prolineServerBrowseButton = new Button("Browse...") {
    graphic = FxUtils.newImageView(IconResource.LOAD)
    disable <== !prolineServerChBox.selected
    onAction = handle {
      _browseServerConfigFile(PostInstall.stage)
    }
  }

  /* Style */
  Seq(postgreSQLPanel, seqReposPanel, prolineServerPanel).foreach(_.minWidth = 60)
  Seq(postgreSQLField, seqReposField, prolineServerField).foreach {
    f => f.hgrow = Priority.Always
  }
  /* layout */
  private val V_SPACING = 10
  private val H_SPACING = 5
  val warningBox = new VBox {
    spacing = 0.5
    children = Seq(corruptedFileLabel, invalidServerFileLabel, invalidSeqReposFileLabel, invalidPgDataLabel)
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
          children = Seq(ScalaFxUtils.newHSpacer(minW = 60, maxW = 60), prolineServerPanel, prolineServerField, prolineServerBrowseButton)
        },
        ScalaFxUtils.newVSpacer(minH = 1, maxH = 1),
        prolineModulesChBox,
        new HBox {
          spacing = 5
          children = Seq(ScalaFxUtils.newHSpacer(minW = 30, maxW = 30), seqReposChBox)
        }, new HBox {
          spacing = 5
          children = Seq(ScalaFxUtils.newHSpacer(minW = 60, maxW = 60), seqReposPanel, seqReposField, seqReposBrowseButton)
        },
        ScalaFxUtils.newVSpacer(minH = 1, maxH = 1),
        postgreSQLChBox, new HBox {
          spacing = 5
          children = Seq(ScalaFxUtils.newHSpacer(minW = 60, maxW = 60), postgreSQLPanel, postgreSQLField, postgresBrowseButton)
        }, ScalaFxUtils.newVSpacer(10))
    })

  /* Post Install home panel content */
  alignment = Pos.Center
  alignmentInParent = Pos.Center
  spacing = 1
  fillWidth = true
  children = Seq(ScalaFxUtils.newVSpacer(minH = 20),
    configItemsPane)

  private val checkBoxList = List(prolineServerChBox.getId, seqReposChBox.getId, postgreSQLChBox.getId)

  /* get selected items */
  def getSelectedItems {
    checkBoxList.map {
      case "serverChBoxId" => //selectServerItem
      case "seqReposChBoxId" => //selectSeqRepositoryItem
      case "postgresChBoxId" => //selectPostgreSQLItem
      case _ => logger.error(s"Error while trying to select configuration file")
    }
  }
  /* select Proline server item */
  private def selectServerItem: Boolean = {
    var isValidPath = false
    if (prolineServerChBox.isSelected) {
      if (ScalaUtils.isConfFile(PostInstall.serverConfPath)) {
        val jmsNodeConfPath = new File(PostInstall.serverConfPath).getParent() + File.separator + """jms-node.conf"""
        if (new File(jmsNodeConfPath).exists) {
          ScalaFxUtils.NodeStyle.remove(prolineServerField)
          ScalaFxUtils.NodeStyle.hide(invalidServerFileLabel)
          val server = new ServerConfig(2)
          PostInstall.items += (server.orderId -> Some(server))
          isValidPath = true
        } else {
          removeItem(invalidServerFileLabel, prolineServerField, 2)
        }
      } else {
        removeItem(invalidServerFileLabel, prolineServerField, 2)
      }
    } else {
      PostInstall.items -= (2)
      hideItem(invalidServerFileLabel, prolineServerField)
    }
    isValidPath
  }

  /* select Sequence Repository item */
  private def selectSeqRepositoryItem: Boolean = {
    var isValidPath = false
    if (seqReposChBox.isSelected) {
      if (ScalaUtils.isConfFile(PostInstall.seqRepoConfPath)) {
        ScalaFxUtils.NodeStyle.remove(seqReposField)
        val jmsNodeConfPath = new File(seqReposField.getText).getParent() + File.separator + """jms-node.conf"""
        val parsingRules = new File(seqReposField.getText).getParent() + File.separator + """parsing-rules.conf"""
        if (new File(jmsNodeConfPath).exists && new File(parsingRules).exists) {
          hideItem(invalidSeqReposFileLabel, seqReposField)
          PostInstall.SeqJmsNodeConfPath = jmsNodeConfPath
          PostInstall.parsingRulesPath = parsingRules
          val moduleConfig = new SeqReposConfig(3)
          PostInstall.items += (moduleConfig.orderId -> Some(moduleConfig))
          isValidPath = true
        } else {
          removeItem(invalidSeqReposFileLabel, seqReposField, 3)
        }
      } else {
        removeItem(invalidSeqReposFileLabel, seqReposField, 3)
      }
    } else {
      PostInstall.items -= (3)
      hideItem(invalidSeqReposFileLabel, seqReposField)
    }
    isValidPath
  }

  /* select PostgreSQL item */
  private def selectPostgreSQLItem(): Boolean = {
    var isValidPath = false
    if (postgreSQLChBox.isSelected) {
      if (ScalaUtils.isValidDataDir(PostInstall.pgDataDirPath)) {
        hideItem(invalidPgDataLabel, postgreSQLField)
        val pgServerConfig = new PgServerConfig(1)
        PostInstall.items += (pgServerConfig.orderId -> Some(pgServerConfig))
        isValidPath = true
      } else {
        removeItem(invalidPgDataLabel, postgreSQLField, 1)
      }
    } else {
      PostInstall.items -= (1)
      hideItem(invalidPgDataLabel, postgreSQLField)
    }
    isValidPath
  }

  /* check selected fields */
  def setStyleSelectedItems: Boolean = {

    true
  }

  /* remove item from items map */
  def removeItem(label: Node, field: Node, id: Int) {
    PostInstall.items -= (id)
    ScalaFxUtils.NodeStyle.show(label)
    ScalaFxUtils.NodeStyle.set(field)
  }
  /* hide warning label and remove red border */
  def hideItem(label: Node, field: Node) {
    ScalaFxUtils.NodeStyle.hide(label)
    ScalaFxUtils.NodeStyle.remove(field)
  }

  /* browse configuration files dialog */
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

