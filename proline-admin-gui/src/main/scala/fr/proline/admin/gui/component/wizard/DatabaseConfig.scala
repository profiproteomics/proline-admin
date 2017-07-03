package fr.proline.admin.gui.component.wizard

import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ArrayBuffer

import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Cursor
import scalafx.scene.Cursor.sfxCursor2jfx
import scalafx.scene.control.Button
import scalafx.scene.control.CheckBox
import scalafx.scene.control.ComboBox
import scalafx.scene.control.Hyperlink
import scalafx.scene.control.Label
import javafx.scene.control.Alert
import javafx.scene.control.Alert._
import javafx.scene.control.Alert.AlertType
import scalafx.scene.control.PasswordField
import scalafx.scene.control.TextField
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.VBox
import scalafx.stage.Screen
import scalafx.stage.Stage

import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.Main
import fr.proline.admin.gui.process.DatabaseConnection
import fr.proline.admin.gui.process.ProlineAdminConnection
import fr.proline.admin.gui.process.config._
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.util.GetConfirmation
import fr.proline.repository.DriverType
import fr.proline.admin.gui.QuickStart

import fr.profi.util.scala.ScalaUtils.doubleBackSlashes
import fr.profi.util.scala.ScalaUtils.isEmpty
import fr.profi.util.scala.ScalaUtils.stringOpt2string
import fr.profi.util.scalafx.BoldLabel
import fr.profi.util.scalafx.FileBrowsing
import fr.profi.util.scalafx.NumericTextField
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.profi.util.scalafx.TitledBorderPane
import fr.profi.util.scala.ScalaUtils

import java.net.InetAddress;
import java.net.UnknownHostException;

import fr.proline.repository.DriverType
import javafx.scene.control.Tooltip
import fr.proline.admin.gui.component.configuration.form._
import fr.proline.admin.gui.util.ShowPopupWindow
import java.io.File
import fr.proline.admin.gui.component.configuration._
import fr.proline.admin.gui.component.configuration.ConfigurationTabbedWindowWizard
import fr.proline.admin.gui.component.resource.ResourcesTabbedWindow

/**
 * *********************************************************
 * Step 2 : a panel to initialize and to edit
 *          database configuration's file .
 * **********************************************************
 */
class DatabaseConfig extends VBox with LazyLogging {

  /*
	 * ********** *
	 * COMPONENTS *
	 * ********** *
	 */
  var dataDir = ""
  var dbUserName = ""
  var dbUserNam = ""
  var dbHost = ""
  var dbPassword = ""
  var adminConfigFile = new AdminConfigFile(QuickStart.adminConfPath)
  try {
    val adminConfigOpt = adminConfigFile.read()
    require(adminConfigOpt.isDefined, "admin config is undefined")
    val adminConfig = adminConfigOpt.get
    dataDir = adminConfig.pgsqlDataDir
    dbPassword = adminConfig.dbPassword
    dbUserName = adminConfig.dbUserName
    dbHost = adminConfig.dbHost
  } catch {
    case e: Exception =>
      adminConfigFile.write(new AdminConfig(QuickStart.adminConfPath, Some(""), Some(""), Some(""), Some(""), Some(DriverType.POSTGRESQL), Some(""), Some(""), Some(""), Some("")))
      ErrorInConfig(
        wTitle = "Error",
        wText = "The file application.conf is corrupted and can not be opened.\nMake  sure that the paths in the file application.conf are correct.\nDefault settings will be reset.",
        wParent = Option(QuickStart.stage))

  }
  val driver = DriverType.POSTGRESQL

  def isPrompt(str: String): Boolean = str matches """<.*>"""
  // warning label 
  val warningLabel = new Label {
    text = "Error while trying to read postgreSQL data directory path. Make sure that you have entered the correct path."
    visible = false
    style = TextStyle.RED_ITALIC
  }

  if (new File(dataDir).exists()) {
    QuickStart.postgresqlDataDir = dataDir
  }

  if (isPrompt(dbUserName)) QuickStart.userName = dbUserName
  else QuickStart.userName = dbUserName

  if (isPrompt(dbPassword)) QuickStart.passwordUser = dbPassword
  else QuickStart.passwordUser = dbPassword

  if (isPrompt(dbHost)) QuickStart.hostNameUser = dbHost
  else QuickStart.hostNameUser = dbHost

  /* DB connection */

  /* user name  */
  val userNameLabel = new Label("User name: ")
  val userNameField = new TextField {
    if (QuickStart.userName != null) text = QuickStart.userName
    text.onChange { (_, oldText, newText) =>
      updateUSername(newText)
    }
  }
  userNameField.setTooltip(new Tooltip("enter the username of your database."))
  userNameField.setPromptText("Example : postgres")
  /* password */
  val pwdLabel = new Label("Password: ")
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
    if (QuickStart.passwordUser != null) text = QuickStart.passwordUser
    text.onChange { (_, oldText, newText) =>
      updatePassword(newText)
    }
  }
  passwordTextField.setTooltip(new Tooltip("enter the password of your database."))
  passwordTextField.setPromptText("Password")
  /* host name */
  val hostNameLabel = new Label("Host name: ")
  val hostNameField = new TextField {
    if (QuickStart.hostNameUser != null) text = QuickStart.hostNameUser
    text.onChange { (_, oldText, newText) =>
      updateHost(newText)
    }
  }
  hostNameField.setPromptText("Example: localhost")
  hostNameField.setTooltip(new Tooltip("enter your hostname."));
  /* Port */
  val portLabel = new Label("Port : ")
  val portField = new NumericTextField {
    text.onChange {
      (_, oldText, newText) =>
        if (!newText.equals("") && (newText != null)) {
          updatePort(newText.toInt)
        }
    }
  }
  portField.setPromptText("Example: 5432")
  portField.setText("5432")
  portField.setTooltip(new Tooltip("enter the port of your database(default: 5432)."))
  val testConnectionButton = new Button("Test connection") {
    alignmentInParent = Pos.BASELINE_LEFT
    onAction = handle {

      /*test connection database*/
      if ((QuickStart.userName != null) && (passwordTextField.getText() != null) && (QuickStart.hostNameUser != null) && (QuickStart.port > 0))
        DatabaseConnection.testDbConnectionToWizard(driver, QuickStart.userName, passwordTextField.getText(), QuickStart.hostNameUser, QuickStart.port, true, true)

    }
  }
  /* optimize configuration of database server postgresql.conf */

  val optimizePostgresqlButton = new Button("Manage PostgreSQL") {
    alignmentInParent = Pos.BASELINE_RIGHT
    onAction = handle {
      try {
        new ConfigurationTabbedWindowWizard().showAndWait()
      } catch {
        case e: Exception =>
          logger.error(s"Error while trying to read postgreSQL data directory $e")
          warningLabel.visible = true
        //          SetUpPathDialog(wTitle = "WARNING",
        //            wText = "Error while trying to read postgreSQL data directory path.\nMake sure that you have the correct path in the application.conf.",
        //            wParent = Option(QuickStart.stage))
      }
    }
  }
  val buttonsBox = new HBox {
    spacing = 500
    content = List(optimizePostgresqlButton, testConnectionButton)
  }
  /*
 * ****** *
 * LAYOUT *
 * ****** *
 */

  /* Size & Resize properties */
  Seq(
    userNameLabel, pwdLabel, hostNameLabel, portLabel).foreach(_.minWidth = 60)

  Seq(
    passwordTextField).foreach { node =>

      node.prefWidth <== QuickStart.mainPanel.width - 100
    }
  Seq(
    userNameField, hostNameField, portField, buttonsBox).foreach { node =>
      node.prefWidth <== QuickStart.mainPanel.width - 20
    }

  //VBox & HBox spacing
  private val V_SPACING = 10
  private val H_SPACING = 5

  /* DB connection */

  val dbPwdPane = new StackPane {
    alignmentInParent = Pos.BottomLeft
    content = List(passwordPWDField, passwordTextField)
  }
  val dbConnectionSettings = new TitledBorderPane(

    title = "Step 2: edit database connection",
    contentNode = new VBox {
      minWidth = 360
      prefWidth = 360
      minHeight = 300
      spacing = 5
      content = Seq(
        warningLabel,
        userNameLabel,
        new HBox {
          spacing = 5
          content = Seq(userNameField)
        },
        ScalaFxUtils.newVSpacer(minH = 10),
        pwdLabel,
        new HBox {
          spacing = 5
          content = List(dbPwdPane, showPwdBox)
        },
        ScalaFxUtils.newVSpacer(minH = 10),
        hostNameLabel,
        new HBox {
          spacing = 5
          content = Seq(hostNameField)
        },
        ScalaFxUtils.newVSpacer(minH = 10),
        portLabel,
        new HBox {
          spacing = 5
          content = Seq(portField)
        },
        ScalaFxUtils.newVSpacer(minH = 10),
        buttonsBox,
        ScalaFxUtils.newVSpacer(minH = 12))
    })

  /* VBox layout and content */
  alignment = Pos.Center
  alignmentInParent = Pos.Center
  spacing = 4 * V_SPACING
  content = List(
    ScalaFxUtils.newVSpacer(minH = 1),
    dbConnectionSettings)

  /* update global variables */

  private def updateUSername(name: String) {
    QuickStart.userName = name
  }
  private def updatePassword(passUser: String) {
    QuickStart.passwordUser = passUser
  }
  private def updateHost(hostname: String) {
    QuickStart.hostNameUser = hostname
  }
  private def updatePort(portnumber: Int) {
    QuickStart.port = portnumber
  }

}