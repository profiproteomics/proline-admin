package fr.proline.admin.gui.wizard.component.items.serverconfig.tab

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Cursor
import scalafx.scene.Cursor.sfxCursor2jfx
import scalafx.scene.control.Button
import scalafx.scene.control.CheckBox
import scalafx.scene.control.Hyperlink
import scalafx.scene.control.Label
import javafx.scene.control.Alert._
import javafx.scene.control.Alert.AlertType
import scalafx.scene.control.PasswordField
import scalafx.scene.control.TextField
import fr.profi.util.scalafx.NumericTextField
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.StackPane
import scalafx.stage.Screen
import scalafx.stage.Stage
import scalafx.scene.text.{ Font, FontWeight, Text }
import java.io.File
import java.io.File.separator
import java.io.IOException
import scalafx.scene.layout.Priority

import fr.profi.util.scala.ScalaUtils.doubleBackSlashes
import fr.profi.util.scalafx.ScalaFxUtils
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.profi.util.scalafx.TitledBorderPane
import fr.proline.admin.gui.process.DatabaseConnection
import fr.proline.admin.gui.Wizard
import fr.proline.repository.DriverType
import fr.proline.admin.gui.process.config.AdminConfigFile
import fr.proline.admin.gui.wizard.component.items.ServerConfig
import fr.proline.admin.gui.wizard.component.items.form.TabForm
import fr.profi.util.scala.ScalaUtils
import fr.proline.admin.gui.process.config.AdminConfigFile
import fr.proline.admin.gui.process.config.AdminConfig
import fr.proline.admin.gui.wizard.util.GetConfirmation
import fr.proline.repository.DriverType

import scala.concurrent._
import ExecutionContext.Implicits.global

/**
 * PostGreSQLTab build tab of PostGreSQL database server properties.
 *
 */
class PostGreSQLTab(path: String) extends VBox with TabForm with LazyLogging {
  private val driver = DriverType.POSTGRESQL

  /*
	 * ********** *
	 * COMPONENTS *
	 * ********** *
	 */

  /* get Jms-node configuration file */

  try {
    if (new File(path).exists()) {
      val jmsNodeConfDir = new File(path).getParent()
      if (new File(jmsNodeConfDir + File.separator + """jms-node.conf""").exists()) {
        Wizard.jmsNodeConfPath = jmsNodeConfDir + File.separator + """jms-node.conf"""
      }
    }
  } catch {
    case e: IOException => logger.error("File jms-node.conf not found.")
  }

  private val adminConfigFile = new AdminConfigFile(path)
  private val adminConfigOpt = adminConfigFile.read()
  require(adminConfigOpt.isDefined, "admin config is undefined to get database server properties. ")
  private val adminConfig = adminConfigOpt.get

  def isPrompt(str: String): Boolean = str matches """<.*>"""

  val dbUserName = adminConfig.dbUserName.get
  if (isPrompt(dbUserName)) {
    Wizard.userName = dbUserName
  } else {
    Wizard.userName = dbUserName
  }

  val dbPassword = adminConfig.dbPassword.get
  if (isPrompt(dbPassword)) {
    Wizard.passWord = dbPassword
  } else {
    Wizard.passWord = dbPassword
  }

  val dbHost = adminConfig.dbHost.get
  if (isPrompt(dbHost)) {
    Wizard.hostName = dbHost
  } else {
    Wizard.hostName = dbHost
  }

  val dbPort = adminConfig.dbPort.get
  Wizard.port = dbPort

  /**
   * *
   * component
   */

  //host 
  val hostLabel = new Label("Host: ")
  val hostField = new TextField {
    if (Wizard.hostName != null) text = Wizard.hostName
    text.onChange { (_, oldText, newText) =>
      Wizard.hostName = newText
      checkForm
    }
  }
  //port
  val portLabel = new Label("Port: ")
  val portField = new NumericTextField {
    text = Wizard.port.toString
    text.onChange {
      (_, oldText, newText) =>
        if ((newText != null) && !newText.equals("")) {
          Wizard.port = newText.toInt
          checkForm
        }
    }
  }
  //user
  val userLabel = new Label("User: ")
  val userField = new TextField {
    if (Wizard.userName != null) text = Wizard.userName
    text.onChange { (_, oldText, newText) =>
      Wizard.userName = newText
      checkForm
    }
  }
  //password
  val passWordLabel = new Label("Password: ")
  val showPwdBox = new CheckBox("Show password") {
    selected = false
    vgrow = Priority.Always
    minWidth = 112
    maxWidth = 112
  }
  val passwordPWDField = new PasswordField() {
    visible <== !showPwdBox.selected
  }
  val passwordTextField = new TextField() {
    text <==> passwordPWDField.text
    promptText <==> passwordPWDField.promptText
    visible <== !passwordPWDField.visible
    if (Wizard.passWord != null) text = Wizard.passWord
    text.onChange { (_, oldText, newText) =>
      Wizard.passWord = newText
      checkForm
    }
  }

  val dbPwdPane = new StackPane {
    alignmentInParent = Pos.BottomLeft
    content = List(passwordPWDField, passwordTextField)
  }

  val testConnectionButton = new Button {
    graphic = FxUtils.newImageView(IconResource.CONNECTION)
    text = "Test connection"
    onAction = handle {
      /*test connection database*/
      if (!ScalaUtils.isEmpty(Wizard.userName) && !ScalaUtils.isEmpty(Wizard.passWord) && !ScalaUtils.isEmpty(Wizard.hostName) && (Wizard.port > 0)) {
        DatabaseConnection.testDbConnectionToWizard(driver, Wizard.userName, Wizard.passWord, Wizard.hostName, Wizard.port, true, true)
      }
    }
  }

  /*   
 * ****** *
 * LAYOUT *
 * ****** *
 */
  Seq(userLabel, hostLabel, portLabel, passWordLabel).foreach(_.minWidth = 60)
  Seq(passwordTextField).foreach(_.prefWidth = 1500)
  Seq(userField, portField, hostField).foreach {
    f => f.hgrow = Priority.Always
  }
  //VBox & HBox spacing
  private val V_SPACING = 10
  private val H_SPACING = 5

  //TitledBorderPane
  val dbConnectionSettingPane = new TitledBorderPane(
    title = "Database Server",
    contentNode = new VBox {
      prefWidth <== Wizard.configItemsPanel.width - 30
      prefHeight <== Wizard.configItemsPanel.height - 30
      spacing = 2 * V_SPACING
      content = Seq(warningDatalabel,
        hostLabel,
        new HBox {
          spacing = H_SPACING
          content = Seq(hostField)
        },
        ScalaFxUtils.newVSpacer(minH = 10),
        portLabel,
        new HBox {
          spacing = H_SPACING
          content = Seq(portField)
        },
        ScalaFxUtils.newVSpacer(minH = 10),
        userLabel,
        new HBox {
          spacing = H_SPACING
          content = Seq(userField)
        },
        ScalaFxUtils.newVSpacer(minH = 10),
        passWordLabel,
        new HBox {
          spacing = H_SPACING
          content = List(dbPwdPane, showPwdBox)
        }, ScalaFxUtils.newVSpacer(minH = 10),
        new HBox {
          content = List(ScalaFxUtils.newHSpacer(minW = 100), testConnectionButton)
        }, ScalaFxUtils.newVSpacer(minH = 12))
    })

  // position in center

  alignment = Pos.Center
  alignmentInParent = Pos.Center
  spacing = V_SPACING
  content = List(
    ScalaFxUtils.newVSpacer(minH = 20),
    new VBox {
      vgrow = Priority.Always
      hgrow = Priority.Always
      content = List(dbConnectionSettingPane)
    })

  /** check Proline Admin Form */
  def checkForm: Boolean = {
    if (ScalaUtils.isEmpty(hostField.getText) || ScalaUtils.isEmpty(userField.getText)
      || ScalaUtils.isEmpty(portField.getText) || ScalaUtils.isEmpty(passwordPWDField.getText)) {
      warningDatalabel.visible = true
      false
    } else {
      warningDatalabel.visible = false
      true
    }
  }
  /** get GUI information to create a new Proline Admin Config Object **/
  private def _toAdminConfig() = AdminConfig(
    filePath = Wizard.adminConfPath,
    serverConfigFilePath = Option(Wizard.serverConfPath).map(doubleBackSlashes), //FIXME: windows-specific
    pwxConfigFilePath = Option(Wizard.webRootPath).map(doubleBackSlashes), //FIXME: windows-specific
    pgsqlDataDir = Option(Wizard.pgDataDirPath).map(doubleBackSlashes), //FIXME: windows-specific
    seqRepoConfigFilePath = Option(Wizard.seqRepoConfPath).map(doubleBackSlashes),
    driverType = Option(DriverType.POSTGRESQL),
    prolineDataDir = Option(Wizard.pgDataDirPath).map(doubleBackSlashes), //FIXME: windows-specific
    dbUserName = Option(Wizard.userName),
    dbPassword = Option(Wizard.passWord),
    dbHost = Option(Wizard.hostName),
    dbPort = Option(Wizard.port) //FIXME
    )

  /** save Proline-Admin form  **/
  def saveForm() {
    /* save  new AdminConf properties */
    val newConfig = Future {
      val newAdminConfig = _toAdminConfig()
      adminConfigFile.write(newAdminConfig)
    }
    newConfig onFailure {
      case (t) => logger.error(s"An error has occured: ${t.getMessage}")
    }
  }

  private def _testDbConnection(
    adminConfig: AdminConfig,
    showSuccessPopup: Boolean = true,
    showFailurePopup: Boolean = true): Boolean = {
    DatabaseConnection.testDbConnection(adminConfig, showSuccessPopup, showFailurePopup)
  }
  /**  database connection */
  def getInfos: String = {
    if (DatabaseConnection.testDbConnectionToWizard(driver, Wizard.userName, Wizard.passWord, Wizard.hostName, Wizard.port, false, false)) s"""PostgreSQL: OK""" else s"""PostgreSQL: NOK"""
  }
}