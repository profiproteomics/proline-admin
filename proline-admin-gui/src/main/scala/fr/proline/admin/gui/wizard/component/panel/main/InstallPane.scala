package fr.proline.admin.gui.wizard.component.panel.main

import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.control.Button
import scalafx.geometry.Pos
import scalafx.scene.layout.Priority
import scalafx.scene.layout.VBox
import scalafx.scene.layout.HBox
import scalafx.scene.control.TextField
import scalafx.scene.control.Hyperlink
import scalafx.scene.Node
import scalafx.stage.Stage
import java.io.File
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.wizard.util.ItemName._
import fr.proline.admin.gui.wizard.util._
import fr.proline.admin.gui.wizard.component.items._
import fr.proline.admin.gui.process.config.AdminConfigFile
import fr.profi.util.scala.ScalaUtils
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.profi.util.scalafx.TitledBorderPane
import com.typesafe.scalalogging.LazyLogging
import fr.proline.admin.gui.util.AdminGuide

/**
 * Builds home panel of Proline install.
 * @author aromdhani
 *
 */

object InstallPane extends VBox with INotification with LazyLogging {

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
    corruptedFileLabel.visible = false
  } catch {
    case pe: com.typesafe.config.ConfigException.Parse => {
      logger.error("Error while trying to parse initial settings from Proline Admin configuration file. Default settings will be reset.")
      corruptedFileLabel.visible = true
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
  val headerHelpIcon = new Hyperlink {
    graphic = FxUtils.newImageView(IconResource.HELP)
    alignmentInParent = Pos.BASELINE_RIGHT
    onAction = handle {
      _openUserGuide()
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
  //set initial values 
  prolineServerChBox.selected_=(!prolineServerField.getText.trim().isEmpty())
  seqReposChBox.selected_=(!seqReposField.getText.trim().isEmpty())
  prolineWebChBox.selected_=(!prolineWebField.getText.trim().isEmpty())
  postgreSQLChBox.selected_=(!postgreSQLField.getText.trim().isEmpty())
  prolineModulesChBox.selected_=(seqReposChBox.isSelected || prolineWebChBox.isSelected)
  /* Style */
  Seq(postgreSQLPanel, pwxPanel, seqReposPanel, prolineServerPanel).foreach(_.minWidth = 60)
  Seq(postgreSQLField, prolineWebField, seqReposField, prolineServerField).foreach {
    f => f.hgrow = Priority.Always
  }

  /* layout */
  private val V_SPACING = 10
  private val H_SPACING = 5
  val warningBox = new VBox {
    spacing = 0.5
    children = Seq(corruptedFileLabel, invalidServerFileLabel, invalidSeqReposFileLabel, invalidPwxFileLabel, invalidPgDataLabel)
  }
  Seq(corruptedFileLabel, invalidServerFileLabel, invalidSeqReposFileLabel, invalidPwxFileLabel, invalidPgDataLabel).foreach { node => node.managed <== node.visible }
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
        }, new HBox {
          spacing = 5
          children = Seq(ScalaFxUtils.newHSpacer(minW = 30, maxW = 30), prolineWebChBox)
        }, new HBox {
          spacing = 5
          children = Seq(ScalaFxUtils.newHSpacer(minW = 60, maxW = 60), pwxPanel, prolineWebField, prolineWebBrowseButton)
        },
        ScalaFxUtils.newVSpacer(minH = 1, maxH = 1),
        postgreSQLChBox, new HBox {
          spacing = 5
          children = Seq(ScalaFxUtils.newHSpacer(minW = 60, maxW = 60), postgreSQLPanel, postgreSQLField, postgresBrowseButton)
        }, ScalaFxUtils.newVSpacer(10))
    })

  val helpPane = new HBox {
    children = Seq(ScalaFxUtils.newHSpacer(minW = configItemsPane.getWidth - 50), headerHelpIcon)
  }

  /* Install home panel content */
  alignment = Pos.BottomCenter
  alignmentInParent = Pos.BottomCenter
  spacing = 1
  fillWidth = true
  children = Seq(ScalaFxUtils.newVSpacer(minH = 10),
    helpPane,
    configItemsPane)

  /* select Proline server item */
  def isValidServerPath: Boolean = {
    var isValidPath = false
    if (prolineServerChBox.isSelected) {
      if (ScalaUtils.isConfFile(Wizard.serverConfPath)) {
        val jmsNodeConfPath = new File(Wizard.serverConfPath).getParent() + File.separator + """jms-node.conf"""
        if (new File(jmsNodeConfPath).exists) {
          hideItem(invalidServerFileLabel, prolineServerField)
          Wizard.jmsNodeConfPath = jmsNodeConfPath
          Wizard.items += (2 -> Some(ItemFactory(SERVER)))
          isValidPath = true
        } else {
          removeItem(invalidServerFileLabel, prolineServerField, 2)
        }
      } else {
        removeItem(invalidServerFileLabel, prolineServerField, 2)
      }
    } else {
      Wizard.items -= (2)
      hideItem(invalidServerFileLabel, prolineServerField)
    }
    isValidPath
  }

  /* select Sequence Repository item */
  def isValidSeqreposPath: Boolean = {
    var isValidPath = false
    if (seqReposChBox.isSelected) {
      if (ScalaUtils.isConfFile(Wizard.seqRepoConfPath)) {
        val jmsNodeConfPath = new File(seqReposField.getText).getParent() + File.separator + """jms-node.conf"""
        val parsingRules = new File(seqReposField.getText).getParent() + File.separator + """parsing-rules.conf"""
        if (new File(jmsNodeConfPath).exists && new File(parsingRules).exists) {
          hideItem(invalidSeqReposFileLabel, seqReposField)
          Wizard.SeqJmsNodeConfPath = jmsNodeConfPath
          Wizard.parsingRulesPath = parsingRules
          Wizard.items += (3 -> Some(ItemFactory(SEQREPOS)))
          isValidPath = true
        } else {
          removeItem(invalidSeqReposFileLabel, seqReposField, 2)
        }
      } else {
        removeItem(invalidSeqReposFileLabel, seqReposField, 2)
      }
    } else {
      Wizard.items -= (3)
      hideItem(invalidSeqReposFileLabel, seqReposField)
    }
    isValidPath
  }

  /* select  PWX item */
  def isValidPwxPath: Boolean = {
    var isValidPath = false
    if (prolineWebChBox.isSelected) {
      if (ScalaUtils.isConfFile(Wizard.webRootPath)) {
        hideItem(invalidPwxFileLabel, prolineWebField)
        Wizard.items += (4 -> Some(ItemFactory(PWX)))
        isValidPath = true
      } else {
        removeItem(invalidPwxFileLabel, prolineWebField, 4)
      }
    } else {
      Wizard.items -= (4)
      hideItem(invalidPwxFileLabel, prolineWebField)
    }
    isValidPath
  }

  /* select PostgreSQL item */
  def isValidPgDataDir: Boolean = {
    var isValidPath = false
    if (postgreSQLChBox.isSelected) {
      if (!Wizard.pgDataDirPath.isEmpty) {
        if (ScalaUtils.isValidDataDir(Wizard.pgDataDirPath)) {
          hideItem(invalidPgDataLabel, postgreSQLField)
          Wizard.items += (1 -> Some(ItemFactory(PGSERVER)))
          isValidPath = true
        } else {
          removeItem(invalidPgDataLabel, postgreSQLField, 1)
        }
      } else {
        removeItem(invalidPgDataLabel, postgreSQLField, 1)
      }
    } else {
      Wizard.items -= (1)
      hideItem(invalidPgDataLabel, postgreSQLField)
    }
    isValidPath
  }

  def _openUserGuide() {
    AdminGuide.openUrl(Wizard.targetPath + File.separator + "classes" + File.separator + "documentation" + File.separator + "Proline_AdminGuide_1.7.pdf")
  }

  /**
   *  remove an item from items map
   *  @param label The node
   *  @param field the field
   *  @param the order of the node
   */
  def removeItem(label: Node, field: Node, order: Int) {
    Wizard.items -= (order)
    ScalaFxUtils.NodeStyle.show(label)
    ScalaFxUtils.NodeStyle.set(field)
  }

  /**
   *  hide an item from items map
   *  @param label The node
   *  @param field the field
   *  @param the order of the node
   */
  def hideItem(label: Node, field: Node) {
    ScalaFxUtils.NodeStyle.hide(label)
    ScalaFxUtils.NodeStyle.remove(field)
  }

  /**
   *  browse configuration files dialog
   *  @param stage The stage to show in the file path
   */
  def _browseServerConfigFile(stage: Stage) {
    confChooser.setForProlineServerConf(prolineServerField.text())
    try {
      val filePath = confChooser.showIn(stage)
      if (filePath != null) prolineServerField.text = filePath
    } catch {
      case t: Throwable => logger.error("error in file's path ")
    }
  }
  /**
   * browse Sequence repository configuration file
   * @param stage The  stage to show in the configuration file.
   */
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
  /**
   * browse  Pwx configuration file
   * @param stage  The stage to show in the proline web configuration  file
   */
  def _browseProlineWebConfigFile(stage: Stage) {
    confChooser.setForPwxConf(prolineWebField.text())
    try {
      val filePath = confChooser.showIn(stage)
      if (filePath != null) prolineWebField.text = filePath
    } catch {
      case t: Throwable => logger.error("error in file's path ")
    }
  }
  /**
   * browse PostgreSQL data directory
   * @param stage The stage to show in the data directory
   */
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
      case t: Throwable => logger.error("error in file's path")
    }
  }

}

