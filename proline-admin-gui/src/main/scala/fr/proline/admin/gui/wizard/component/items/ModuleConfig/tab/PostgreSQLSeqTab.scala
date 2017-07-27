package fr.proline.admin.gui.wizard.component.items.ModuleConfig.tab

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

import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scala.ScalaUtils
import fr.proline.admin.gui.wizard.util.GetConfirmation
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.profi.util.scalafx.TitledBorderPane
import fr.proline.admin.gui.process.DatabaseConnection
import fr.proline.admin.gui.Wizard
import fr.proline.repository.DriverType
import fr.proline.admin.gui.wizard.util.FieldProperties
import fr.proline.admin.gui.wizard.process.config.SeqConfigFile
import fr.proline.admin.gui.wizard.component.items.form.TabForm
import fr.proline.admin.gui.wizard.process.config._

/**
 * Tab of properties in SqeRepos to connect to the database server
 */

class PostGreSQLSeqTab(path: String) extends VBox with TabForm with LazyLogging {
  private val driver = DriverType.POSTGRESQL

  /*
	 * ********** *
	 * COMPONENTS *
	 * ********** *
	 */

  //get jms-node configuration file

  try {
    if (new File(path).exists()) {
      val jmsNodeConfDir = new File(path).getParent()
      if (new File(jmsNodeConfDir + File.separator + """jms-node.conf""").exists()) {
        Wizard.SeqJmsNodeConfPath = jmsNodeConfDir + File.separator + """jms-node.conf"""
      }
    }
  } catch {
    case e: IOException => logger.error("can not find the file jms-node.conf")
  }

  //initialize some properties 
  private var userName, passWord, hostName = ""
  private var port = 5432
  private val seqConfigFile = new SeqConfigFile(path)
  private val seqConfigOpt = seqConfigFile.read
  require(seqConfigOpt.isDefined, "sequence repository config is undefined")
  private val seqConfig = seqConfigOpt.get

  def isPrompt(str: String): Boolean = str matches """<.*>"""

  val dbUserName = seqConfig.dbUserName.get
  if (isPrompt(dbUserName)) userName = dbUserName
  else userName = dbUserName

  //   we cannot initialize password ?!

  val dbPassword = seqConfig.dbPassword.get
  if (isPrompt(dbPassword)) passWord = dbPassword
  else passWord = dbPassword

  val dbHost = seqConfig.dbHost.get
  if (isPrompt(dbHost)) hostName = dbHost
  else hostName = dbHost

  val dbPort = seqConfig.dbPort.get
  port = dbPort

  //host 

  val hostLabel = new Label("Host: ")
  val hostField = new TextField {
    if (hostName != null) text = hostName
    text.onChange { (_, oldText, newText) =>
      hostName = newText
      checkForm
    }
  }

  //port

  val portLabel = new Label("Port: ")
  val portField = new NumericTextField {
    text = port.toString
    text.onChange {
      (_, oldText, newText) =>
        if ((newText != null) && !newText.equals("")) {
          port = newText.toInt
          checkForm
        }
    }
  }

  //user

  val userLabel = new Label("User: ")
  val userField = new TextField {
    if (userName != null) text = userName
    text.onChange { (_, oldText, newText) =>
      userName = newText
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
    if (passWord != null) text = passWord
    text.onChange { (_, oldText, newText) =>
      passWord = newText
      checkForm
    }
  }

  val dbPwdPane = new StackPane {
    alignmentInParent = Pos.BottomLeft
    content = List(passwordPWDField, passwordTextField)
  }

  val testConnectionButton = new Button {
    text = "Test connection"
    onAction = handle {
      /*test connection database*/
      if (!ScalaUtils.isEmpty(userName) && !ScalaUtils.isEmpty(passWord) && !ScalaUtils.isEmpty(hostName) && (port > 0))
        DatabaseConnection.testDbConnectionToWizard(driver, userName, passWord, hostName, port, true, true)
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
      content = Seq(
        warningDatalabel,
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
    dbConnectionSettingPane)

  /** check seq repos form  **/
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
  private def _testDbConnection(
    showSuccessPopup: Boolean = false,
    showFailurePopup: Boolean = false): Boolean = { //return connectionEstablished
    DatabaseConnection.testDbConnection(DriverType.POSTGRESQL, userName, passWord, hostName, port, showSuccessPopup, showFailurePopup)
  }
  /** get GUI information to create a new SeqRepos Object **/
  private def _toSeqConfig() = SeqConfig(
    driverType = Option(DriverType.POSTGRESQL),
    maxPoolConnection = Option(3),
    dbUserName = Option(userName),
    dbPassword = Option(passWord),
    dbHost = Option(hostName),
    dbPort = Option(port),
    dbUdsDb = Option("uds_db"))

  /** save Sequence Repository form **/
  def saveForm() {
    val newSeqConfig = _toSeqConfig()
    val connectionEstablished = _testDbConnection(false, false)
    if (connectionEstablished) {
      seqConfigFile.write(newSeqConfig)
    } else {
      val isConfirmed = GetConfirmation(
        title = "Invalid configuration",
        text = "The connection to the database can't be established with these settings.\n Check your Sequence Repository connection properties.")
      if (isConfirmed) {
        seqConfigFile.write(newSeqConfig)
      }
    }
  }
  def getInfos: String = {
    if (DatabaseConnection.testDbConnectionToWizard(driver, userName, passWord, hostName, port, false, false))
      s"""PostgreSQL: OK""" else s"""PostgreSQL: NOK"""
  }
}

