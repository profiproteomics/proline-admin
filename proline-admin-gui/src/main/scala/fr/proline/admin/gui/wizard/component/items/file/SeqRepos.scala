package fr.proline.admin.gui.wizard.component.items.file

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.control.Button
import scalafx.scene.control.CheckBox
import scalafx.scene.control.Label
import javafx.scene.control.Alert._
import scalafx.scene.control.PasswordField
import scalafx.scene.control.TextField
import fr.profi.util.scalafx.NumericTextField
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.StackPane
import scalafx.stage.Stage
import scalafx.scene.layout.Priority
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scala.ScalaUtils
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.profi.util.scalafx.TitledBorderPane
import fr.proline.admin.gui.wizard.process.config.SeqConfigFile
import fr.proline.admin.gui.wizard.component.panel.main.ITabForm
import fr.proline.admin.gui.wizard.process.config._
import fr.proline.repository.DriverType
import fr.proline.admin.gui.process.DatabaseConnection
/**
 * PostGreSQLSeq builds a panel with the database server properties
 *
 */

class SeqRepos(path: String, stage: Stage) extends VBox with IPostgres with ITabForm with LazyLogging {

  /*
	 * ********** *
	 * COMPONENTS *
	 * ********** *
	 */

  // initialize postgres properties 
  val driver = DriverType.POSTGRESQL
  var port: Int = 5432
  var userName, passWord, hostName: String = ""
  private val seqConfigFile = new SeqConfigFile(path)
  private val seqConfigOpt = seqConfigFile.read
  require(seqConfigOpt.isDefined, "sequence repository config is undefined")
  private val seqConfig = seqConfigOpt.get

  def isPrompt(str: String): Boolean = str matches """<.*>"""

  val dbUserName = seqConfig.dbUserName.getOrElse("")
  if (isPrompt(dbUserName)) userName = dbUserName
  else userName = dbUserName

  val dbPassword = seqConfig.dbPassword.getOrElse("")
  if (isPrompt(dbPassword)) passWord = dbPassword
  else passWord = dbPassword

  val dbHost = seqConfig.dbHost.getOrElse("")
  if (isPrompt(dbHost)) hostName = dbHost
  else hostName = dbHost

  val dbPort = seqConfig.dbPort.getOrElse(5432)
  port = dbPort

  //host 
  val hostField = new TextField {
    if (hostName != null) text = hostName
    text.onChange { (_, oldText, newText) =>
      hostName = newText
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
  val userField = new TextField {
    if (userName != null) text = userName
    text.onChange { (_, oldText, newText) =>
      userName = newText
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
    if (passWord != null) text = passWord
    text.onChange { (_, oldText, newText) =>
      passWord = newText
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
  /** get GUI information to create a new SeqRepos Object */
  private def _toSeqConfig() = SeqConfig(
    driverType = Some(driver),
    maxPoolConnection = Some(3),
    dbUserName = Some(userName),
    dbPassword = Some(passWord),
    dbHost = Some(hostName),
    dbPort = Some(port),
    dbUdsDb = Some("uds_db"))
  /** test connection to database server */
  def _testDbConnection(
    showSuccessPopup: Boolean = false,
    showFailurePopup: Boolean = false): Boolean = {
    DatabaseConnection.testDbConnectionToWizard(driver, userName, passWord, hostName, port, showSuccessPopup, showFailurePopup)
  }

  /** get database connection */
  def getInfos: String = {
    if (DatabaseConnection.testDbConnectionToWizard(driver, userName, passWord, hostName, port, false, false))
      s"""PostgreSQL: OK""" else s"""PostgreSQL: NOK"""
  }
  /** save new Sequence Repository properties */
  def saveForm() {
    val newSeqConfig = _toSeqConfig()
    seqConfigFile.write(newSeqConfig)
  }

}

