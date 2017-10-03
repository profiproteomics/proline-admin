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
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.wizard.util.GetConfirmation
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.profi.util.scalafx.TitledBorderPane
import fr.proline.admin.gui.process.DatabaseConnection
import fr.proline.repository.DriverType
import fr.proline.admin.gui.wizard.process.config.SeqConfigFile
import fr.proline.admin.gui.wizard.component.items.form.ITabForm
import fr.proline.admin.gui.wizard.process.config._

/**
 * PostGreSQLSeq builds a tab with the database server properties
 *
 */

class PostGreSQLSeq(path: String, stage: Stage) extends VBox with ITabForm with LazyLogging {
  private val driver = DriverType.POSTGRESQL

  /*
	 * ********** *
	 * COMPONENTS *
	 * ********** *
	 */

  // initialize postgres properties 
  private var _userName, _passWord, _hostName = ""
  private var _port = 5432
  private val seqConfigFile = new SeqConfigFile(path)
  private val seqConfigOpt = seqConfigFile.read
  require(seqConfigOpt.isDefined, "sequence repository config is undefined")
  private val seqConfig = seqConfigOpt.get

  def isPrompt(str: String): Boolean = str matches """<.*>"""

  val dbUserName = seqConfig.dbUserName.get
  if (isPrompt(dbUserName)) _userName = dbUserName
  else _userName = dbUserName

  val dbPassword = seqConfig.dbPassword.get
  if (isPrompt(dbPassword)) _passWord = dbPassword
  else _passWord = dbPassword

  val dbHost = seqConfig.dbHost.get
  if (isPrompt(dbHost)) _hostName = dbHost
  else _hostName = dbHost

  val dbPort = seqConfig.dbPort.get
  _port = dbPort

  //host 
  val hostLabel = new Label("Host: ")
  val hostField = new TextField {
    if (_hostName != null) text = _hostName
    text.onChange { (_, oldText, newText) =>
      _hostName = newText
      checkForm
    }
  }
  //warning 
  val warningAboutRestartText = "WARNING: Using localhost or 127.0.0.1 is not advised, as it will make Proline available from this computer only."
  val warningAboutHostLabel = new Label {
    graphic = ScalaFxUtils.newImageView(IconResource.WARNING)
    text = warningAboutRestartText
  }
  //port
  val portLabel = new Label("Port: ")
  val portField = new NumericTextField {
    text = _port.toString
    text.onChange {
      (_, oldText, newText) =>
        if ((newText != null) && !newText.equals("")) {
          _port = newText.toInt
          checkForm
        }
    }
  }

  //user
  val userLabel = new Label("User: ")
  val userField = new TextField {
    if (_userName != null) text = _userName
    text.onChange { (_, oldText, newText) =>
      _userName = newText
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
    if (_passWord != null) text = _passWord
    text.onChange { (_, oldText, newText) =>
      _passWord = newText
      checkForm
    }
  }
  val dbPwdPane = new StackPane {
    alignmentInParent = Pos.BottomLeft
    children = List(passwordPWDField, passwordTextField)
  }

  /* test database connection */
  val testConnectionButton = new Button {
    graphic = FxUtils.newImageView(IconResource.CONNECTION)
    text = "Test connection"
    onAction = handle {
      _testDbConnection(true, true)
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
      prefWidth <== stage.width - 85
      prefHeight <== stage.height - 35
      spacing = 2 * V_SPACING
      children = Seq(
        warningDatalabel,
        hostLabel,
        new HBox {
          spacing = H_SPACING
          children = Seq(new VBox {
            prefWidth <== stage.width - 35
            spacing = 2
            children = Seq(hostField, warningAboutHostLabel)
          })
        },
        ScalaFxUtils.newVSpacer(minH = 10),
        portLabel,
        new HBox {
          spacing = H_SPACING
          children = Seq(portField)
        },
        ScalaFxUtils.newVSpacer(minH = 10),
        userLabel,
        new HBox {
          spacing = H_SPACING
          children = Seq(userField)
        },
        ScalaFxUtils.newVSpacer(minH = 10),
        passWordLabel,
        new HBox {
          spacing = H_SPACING
          children = List(dbPwdPane, showPwdBox)
        }, ScalaFxUtils.newVSpacer(minH = 10),
        new HBox {
          children = List(ScalaFxUtils.newHSpacer(minW = 100), testConnectionButton)
        }, ScalaFxUtils.newVSpacer(minH = 12))
    })

  alignment = Pos.Center
  alignmentInParent = Pos.Center
  spacing = V_SPACING
  margin = Insets(5, 5, 5, 5)
  children = List(
    ScalaFxUtils.newVSpacer(minH = 20),
    dbConnectionSettingPane)

  /** check Sequence Repository fields form  */
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

  /** test connection to database server */
  private def _testDbConnection(
    showSuccessPopup: Boolean = false,
    showFailurePopup: Boolean = false): Boolean = { //return connectionEstablished
    DatabaseConnection.testDbConnectionToWizard(DriverType.POSTGRESQL, _userName, _passWord, _hostName, _port, showSuccessPopup, showFailurePopup)
  }

  /** get GUI information to create a new SeqRepos Object */
  private def _toSeqConfig() = SeqConfig(
    driverType = Option(DriverType.POSTGRESQL),
    maxPoolConnection = Option(3),
    dbUserName = Option(_userName),
    dbPassword = Option(_passWord),
    dbHost = Option(_hostName),
    dbPort = Option(_port),
    dbUdsDb = Option("uds_db"))

  /** save new Sequence Repository properties */
  def saveForm() {
    val newSeqConfig = _toSeqConfig()
    seqConfigFile.write(newSeqConfig)
  }

  /** get database connection */
  def getInfos: String = {
    if (DatabaseConnection.testDbConnectionToWizard(driver, _userName, _passWord, _hostName, _port, false, false))
      s"""PostgreSQL: OK""" else s"""PostgreSQL: NOK"""
  }
}

