package fr.proline.admin.gui.install.view.server

import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.control.Button
import scalafx.scene.control.CheckBox
import scalafx.scene.control.Label
import scalafx.scene.control.PasswordField
import scalafx.scene.control.TextField
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.StackPane

import fr.proline.repository.DriverType
import fr.proline.admin.gui.Install
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.install.model.AdminModelView
import fr.proline.admin.gui.process.config.AdminConfig
import fr.proline.admin.gui.process.DatabaseConnection
import fr.proline.admin.gui.util.FxUtils

import fr.profi.util.scalafx.NumericTextField
import fr.profi.util.scalafx.TitledBorderPane
import fr.profi.util.scalafx.CustomScrollPane
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scala.ScalaUtils.doubleBackSlashes
import fr.profi.util.scala.TypesafeConfigWrapper._
/**
 * Creates and displays Proline server postgreSQL panel.
 * @author aromdhani
 *
 */

class ServerPgPanel(model: AdminModelView) extends VBox {

  // Load initial Proline server configurations
  private val dbConfigOpt = model.serverConfigOpt()
  require(dbConfigOpt.isDefined, "Server configurations must not be null!")
  private val dbConfig = dbConfigOpt.get
  private var host: String = dbConfig.dbHost.getOrElse("localhost")
  private var port: Int = dbConfig.dbPort.getOrElse(5432)
  private var user: String = dbConfig.dbUserName.getOrElse("postgres")
  private var password: String = dbConfig.dbPassword.getOrElse("postgres")
  private var driverType = dbConfig.driverType.getOrElse(DriverType.POSTGRESQL)

  /*
	 * ********** *
	 * COMPONENTS *
	 * ********** *
	 */

  // Host pane
  private val hostLabel = new Label("Host: ")
  private val hostField = new TextField {
    tooltip = "Enter host name value"
    text = host
    text.onChange { (_, oldText, newText) =>
      host = newText
    }
  }
  private val hostWarningLabel = new Label {
    graphic = ScalaFxUtils.newImageView(IconResource.WARNING)
    text = "WARNING: Using localhost or 127.0.0.1 is not advised, as it will make Proline available from this computer only."
  }
  // Port pane
  private val portLabel = new Label("Port: ")
  private val portField = new NumericTextField {
    tooltip = "Enter port value"
    text = port.toString
    text.onChange {
      (_, oldText, newText) =>
        if (!newText.trim.isEmpty) {
          port = Option(newText.toInt).getOrElse(5432)
        }
    }
  }
  // User pane
  private val userLabel = new Label("User: ")
  private val userField = new TextField {
    tooltip = "Enter user name value"
    text = user
    text.onChange { (_, oldText, newText) =>
      user = newText
    }
  }
  // Password pane
  private val showPwdBox = new CheckBox("Show password") {
    selected = false
    hgrow = Priority.Always
    minWidth = 112
    maxWidth = 112
  }
  private val passwordLabel = new Label("Password: ")
  private val passwordPWDField = new PasswordField() {
    tooltip = "Enter user password value"
    text = password
    visible <== !showPwdBox.selected
  }
  private val passwordTextField = new TextField() {

    text <==> passwordPWDField.text
    promptText <==> passwordPWDField.promptText
    visible <== !passwordPWDField.visible
    text.onChange { (_, oldText, newText) =>
      password = newText
    }
  }
  private val dbPwdPane = new StackPane {
    alignmentInParent = Pos.BottomLeft
    children = List(passwordPWDField, passwordTextField)
  }

  // Button to test database connection
  private val testDbConnectionButton = new Button {
    graphic = FxUtils.newImageView(IconResource.CONNECTION)
    text = "Test connection"
    onAction = _ => {
      model.onTestDbConn(
        driverType,
        user,
        password,
        host,
        port)
    }
  }

  /*
 * ****** *
 * STYLE *
 * ****** *
 */
  Seq(userLabel, hostLabel, portLabel, passwordLabel).foreach(_.minWidth = 60)
  Seq(passwordTextField, hostField).foreach(_.prefWidth = Install.stage.width.value)
  Seq(userField, portField, hostField).foreach {
    f => f.hgrow = Priority.Always
  }

  /*
 * ****** *
 * LAYOUT *
 * ****** *
 */
  private val V_SPACING = 5
  private val H_SPACING = 5
  //TitledBorderPane
  val dbConnectionSettingPane = new TitledBorderPane(
    title = "Database Server",
    contentNode = new VBox {
      vgrow = Priority.Always
      hgrow = Priority.Always
      prefHeight <== (Install.stage.height - 350)
      spacing = V_SPACING
      children = Seq(
        hostLabel,
        new HBox {
          hgrow = Priority.Always
          spacing = H_SPACING
          children = Seq(new VBox {
            vgrow = Priority.Always
            spacing = 2
            children = Seq(new HBox {
              hgrow = Priority.Always
              children = Seq(hostField)
            }, hostWarningLabel)
          })
        },
        ScalaFxUtils.newVSpacer(V_SPACING),
        portLabel,
        new HBox {
          hgrow = Priority.Always
          spacing = H_SPACING
          children = Seq(portField)
        },
        ScalaFxUtils.newVSpacer(V_SPACING),
        userLabel,
        new HBox {
          hgrow = Priority.Always
          spacing = H_SPACING
          children = Seq(userField)
        },
       ScalaFxUtils.newVSpacer(V_SPACING),
        passwordLabel,
        new HBox {
          spacing = H_SPACING
          children = List(dbPwdPane, showPwdBox)
        },
        ScalaFxUtils.newVSpacer(V_SPACING),
        new HBox {
          children = List(ScalaFxUtils.newHSpacer(minW = 100), testDbConnectionButton)
        })
    })

  /** Test database connection parameters from GUI fields */
  def onTestDbConn(): Boolean = {
    DatabaseConnection.testDbConnection(
      driverType,
      user,
      password,
      host,
      port,
      showSuccessPopup = false,
      showFailurePopup = false,
      Option(Install.stage))
  }

  /** Return Proline-Admin configurations */
  def toAdminConfig(): AdminConfig = {
    AdminConfig(
      filePath = Install.adminConfPath,
      serverConfigFilePath = Option(Install.serverConfPath).map(doubleBackSlashes), //FIXME: windows-specific
      pwxConfigFilePath = Option(Install.pwxConfPath).map(doubleBackSlashes), //FIXME: windows-specific
//      pgsqlDataDir = Option(Install.pgDataDirPath).map(doubleBackSlashes), //FIXME: windows-specific
      seqRepoConfigFilePath = Option(Install.seqReposConfPath).map(doubleBackSlashes),
      driverType = Option(driverType),
      prolineDataDir = None, //FIXME: windows-specific
      dbUserName = Option(this.user),
      dbPassword = Option(this.password),
      dbHost = Option(this.host),
      dbPort = Option(this.port.toInt) //FIXME
      )
  }

  // Create node content
  this.vgrow = Priority.Always
  this.hgrow = Priority.Always
  this.padding = Insets(35, 5, 5, 5)
  children = Seq(dbConnectionSettingPane)
}