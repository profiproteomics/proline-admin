package fr.proline.admin.gui.component.configuration.form

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
 * Step 2 : a window to edit database configuration's file .
 */
class DatabaseConfig extends VBox with LazyLogging {

  /*
	 * ********** *
	 * COMPONENTS *
	 * ********** *
	 */

  QuickStart.hostNameUser = "localhost"
  QuickStart.userName = "postgres"
  private val driver = DriverType.POSTGRESQL
  QuickStart.passwordUser = ""
  /* DB connection */
  /* user name  */
  val userNameLabel = new Label("User name :")
  val userNameField = new TextField {
    if (QuickStart.userName != null) text = QuickStart.userName
    text.onChange { (_, oldText, newText) =>
      updateUSername(newText)
    }
  }
  userNameField.setTooltip(new Tooltip("enter the username of your database."))
  userNameField.setPromptText("Example : postgres")
  /* password */
  val pwdLabel = new Label("Password :")
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
    text.onChange { (_, oldText, newText) =>
      updatePassword(newText)
    }
  }
  passwordTextField.setTooltip(new Tooltip("enter the password of your database."))
  passwordTextField.setPromptText("Password")
  /* host name */
  val hostNameLabel = new Label("Host name :")
  val hostNameField = new TextField {
    if (QuickStart.hostNameUser != null) text = QuickStart.hostNameUser
    text.onChange { (_, oldText, newText) =>
      updateHost(newText)
    }
  }
  hostNameField.setPromptText("Example : localhost")
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
  portField.setPromptText("Example : 5432")
  portField.setText("5432")
  portField.setTooltip(new Tooltip("enter the port of your database(default:5432)."));
  val testConnectionHyperlink = new Hyperlink("Test connection") {
    onAction = handle {

      /*test connection database*/
      if ((QuickStart.userName != null) && (passwordTextField.getText() != null) && (QuickStart.hostNameUser != null) && (QuickStart.port > 0))
        DatabaseConnection.testDbConnection(driver, QuickStart.userName, passwordTextField.getText(), QuickStart.hostNameUser, QuickStart.port, true, true)

    }
  }

  /* optimize configuration of database server postgresql.conf */

  val optimizePostgresql = new Hyperlink("Manage PostgreSQL") {
    onAction = handle {
      new ConfigurationTabbedWindowWizard().showAndWait()
    }
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
      node.minWidth = 370
      node.prefWidth <== 670
    }
  Seq(
    userNameField, hostNameField, portField).foreach { node =>
      node.minWidth = 370
      node.prefWidth <== 760
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

    title = "Step 2 : edit database connection",
    contentNode = new VBox {
      minWidth = 360
      prefWidth = 360
      spacing = 5
      content = Seq(
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
        new HBox {
          spacing = 110 * H_SPACING
          content = List(optimizePostgresql, testConnectionHyperlink)
        },
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
    QuickStart.globalParameters += ("userName" -> QuickStart.userName)
  }
  private def updatePassword(passUser: String) {
    QuickStart.passwordUser = passUser
    QuickStart.globalParameters += ("password" -> QuickStart.passwordUser)
  }
  private def updateHost(hostname: String) {
    QuickStart.hostNameUser = hostname
    QuickStart.globalParameters += ("hostName" -> QuickStart.hostNameUser)
  }
  private def updatePort(portnumber: Int) {
    QuickStart.port = portnumber
    QuickStart.globalParameters += ("port" -> QuickStart.port.toString())
  }
  /*set global variables */
  QuickStart.globalParameters += ("adminConf" -> QuickStart.adminConfPath, "serverConf" -> QuickStart.serverConfPath, "seqReposConf" -> QuickStart.seqRepoConfPath)
  QuickStart.globalParameters += ("userName" -> QuickStart.userName, "password" -> QuickStart.passwordUser, "hostName" -> QuickStart.hostNameUser, "port" -> QuickStart.port.toString())

}