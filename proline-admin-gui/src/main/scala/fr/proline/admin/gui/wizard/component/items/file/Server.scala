package fr.proline.admin.gui.wizard.component.items.file

import com.typesafe.scalalogging.LazyLogging

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

import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.process.config.AdminConfigFile
import fr.proline.admin.gui.process.config.AdminConfig
import fr.proline.admin.gui.process.DatabaseConnection
import fr.proline.admin.gui.wizard.component.panel.main.ITabForm
import fr.proline.admin.gui.Wizard

import fr.profi.util.scalafx.{ NumericTextField, TitledBorderPane, CustomScrollPane, ScalaFxUtils }
import fr.profi.util.scala.ScalaUtils.doubleBackSlashes
import fr.proline.repository.DriverType
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import java.io.File
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import fr.profi.util.scala.TypesafeConfigWrapper._
/**
 * build a scroll panel
 * @param path The path of server file configuration
 *
 */
class ServerPane(path: String) extends CustomScrollPane {
  val server = new Server(path)
  setContentNode(
    new VBox {
      prefWidth <== Wizard.stage.width - 80
      prefHeight <== Wizard.stage.height - 220
      padding = Insets(5, 0, 0, 0)
      children = Seq(server)
    })
}
/**
 * Server build a vbox layout with a database server properties.
 * @param path The path of server file configuration
 *
 */
class Server(path: String) extends VBox with IPostgres with ITabForm with LazyLogging {

  /*
	 * ********** *
	 * COMPONENTS *
	 * ********** *
	 */

  val driver = DriverType.POSTGRESQL
  var port: Int = 5432
  private val adminConfigFile = new AdminConfigFile(path)
  private val adminConfigOpt = adminConfigFile.read()
  require(adminConfigOpt.isDefined, "Admin config is undefined. Make sure that proline configuration files exists.")
  private val adminConfig = adminConfigOpt.get
  private val serverCfgOpt: Option[Config] = getServerProperties
  require(serverCfgOpt.isDefined, "Proline server configuration file must not be empty!")
  private val serverCfg = serverCfgOpt.get
  def isPrompt(str: String): Boolean = str matches """<.*>"""

  val dbUserName = serverCfg.getStringOpt("auth-config.user").getOrElse("postgres")
  if (isPrompt(dbUserName)) Wizard.userName = dbUserName
  else Wizard.userName = dbUserName

  val dbPassword = serverCfg.getStringOpt("auth-config.password").getOrElse("postgres")
  if (isPrompt(dbPassword)) Wizard.passWord = dbPassword
  else Wizard.passWord = dbPassword

  val dbHost = serverCfg.getStringOpt("host-config.host").getOrElse("localhost")
  if (isPrompt(dbHost)) Wizard.hostName = dbHost
  else Wizard.hostName = dbHost

  val dbPort = serverCfg.getIntOpt("host-config.port").getOrElse(5432)
  Wizard.port = dbPort

  //host 

  val hostField = new TextField {
    if (Wizard.hostName != null) text = Wizard.hostName
    text.onChange { (_, oldText, newText) =>
      Wizard.hostName = newText
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
  val userField = new TextField {
    if (Wizard.userName != null) text = Wizard.userName
    text.onChange { (_, oldText, newText) =>
      Wizard.userName = newText
      checkForm
    }
  }

  //password
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
    children = List(passwordPWDField, passwordTextField)
  }

  val testConnectionButton = new Button {
    graphic = FxUtils.newImageView(IconResource.CONNECTION)
    text = "Test connection"
    onAction = handle {
      /*test connection database*/
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
  private val V_SPACING = 5
  private val H_SPACING = 5

  //TitledBorderPane
  val dbConnectionSettingPane = new TitledBorderPane(
    title = "Database Server",
    contentNode = new VBox {
      prefWidth <== Wizard.stage.width - 80
      prefHeight <== Wizard.stage.height - 220
      spacing = V_SPACING
      children = Seq(emptyFieldErrorLabel,
        hostLabel,
        new HBox {
          spacing = H_SPACING
          children = Seq(new VBox {
            prefWidth <== Wizard.configItemsPanel.width - 30
            spacing = 2
            children = Seq(hostField, warningAboutHostLabel)
          })
        },
        ScalaFxUtils.newVSpacer(minH = 5),
        portLabel,
        new HBox {
          spacing = H_SPACING
          children = Seq(portField)
        },
        ScalaFxUtils.newVSpacer(minH = 1),
        userLabel,
        new HBox {
          spacing = H_SPACING
          children = Seq(userField)
        },
        ScalaFxUtils.newVSpacer(minH = 1),
        passWordLabel,
        new HBox {
          spacing = H_SPACING
          children = List(dbPwdPane, showPwdBox)
        }, ScalaFxUtils.newVSpacer(minH = 5),
        new HBox {
          children = List(ScalaFxUtils.newHSpacer(minW = 100), testConnectionButton)
        })
    })

  // position in center

  alignment = Pos.Center
  alignmentInParent = Pos.Center
  spacing = V_SPACING
  margin = Insets(5, 5, 5, 5)
  children = List(
    new VBox {
      vgrow = Priority.Always
      hgrow = Priority.Always
      children = List(dbConnectionSettingPane)
    })

  /** check Proline Admin Form */
  def checkForm: Boolean = {
    val isValidatedFields = Seq(hostField, userField, passwordPWDField, portField).forall(!_.getText.trim.isEmpty())
    if (isValidatedFields) {
      emptyFieldErrorLabel.visible = false
    } else {
      emptyFieldErrorLabel.visible = true
    }
    isValidatedFields
  }
  
  /** get GUI information to create a new Proline Admin Config Object **/
  private def _toAdminConfig() = AdminConfig(
    filePath = Wizard.adminConfPath,
    serverConfigFilePath = Some(Wizard.serverConfPath).map(doubleBackSlashes), //FIXME: windows-specific
    pwxConfigFilePath = Some(Wizard.webRootPath).map(doubleBackSlashes), //FIXME: windows-specific
    pgsqlDataDir = Some(Wizard.pgDataDirPath).map(doubleBackSlashes), //FIXME: windows-specific
    seqRepoConfigFilePath = Some(Wizard.seqRepoConfPath).map(doubleBackSlashes),
    driverType = Some(driver),
    prolineDataDir = None, //FIXME: windows-specific
    dbUserName = Some(Wizard.userName),
    dbPassword = Some(Wizard.passWord),
    dbHost = Some(Wizard.hostName),
    dbPort = Some(Wizard.port) //FIXME
    )
    
  /** get initial server properties */
  def getServerProperties: Option[Config] = {
    try {
      Some(ConfigFactory.parseFile(new File(Wizard.serverConfPath)))
    } catch {
      case t: Throwable =>
        logger.error("Error while trying to read Proline server configuration file", t.getMessage())
        None
    }
  }
  /** test connection to database server */
  def _testDbConnection(
    showSuccessPopup: Boolean = false,
    showFailurePopup: Boolean = false): Boolean = {
    DatabaseConnection.testDbConnectionToWizard(driver, Wizard.userName, Wizard.passWord, Wizard.hostName, Wizard.port, showSuccessPopup, showFailurePopup)
  }

  /** get database connection */
  def getInfos: String = {
    if (DatabaseConnection.testDbConnectionToWizard(driver, Wizard.userName, Wizard.passWord, Wizard.hostName, Wizard.port, false, false))
      s"""PostgreSQL: OK""" else s"""PostgreSQL: NOK"""
  }
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
}
