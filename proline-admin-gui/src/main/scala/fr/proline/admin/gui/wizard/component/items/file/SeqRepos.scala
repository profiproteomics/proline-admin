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
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.Priority

import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.wizard.process.config.SeqConfigFile
import fr.proline.admin.gui.wizard.component.panel.main.ITabForm
import fr.proline.admin.gui.wizard.process.config._
import fr.proline.admin.gui.process.DatabaseConnection
import fr.proline.admin.gui.Wizard

import fr.proline.repository.DriverType
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.CustomScrollPane
import fr.profi.util.scalafx.TitledBorderPane
import fr.profi.util.scalafx.NumericTextField
/**
 * PostGreSQLSeq build a panel with the database server properties.
 * @param path The path of Sequence Repository configuration file.
 * @param stage The parent stage of the Sequence repository panel.
 *
 */

class SeqReposPane(path: String, stage: Stage) extends CustomScrollPane {
  val seqRepos = new SeqRepos(path, stage)
  setContentNode(
    new VBox {
      prefWidth <== Wizard.stage.width
      prefHeight <== Wizard.stage.height
      padding = Insets(5, 0, 0, 0)
      children = Seq(seqRepos)
    })
}

/**
 * build a Vbox layout with the sequence repository properties
 * @param path the path of sequence repository configuration file .
 * @param stage the parent stage of sequence repository layout.
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
  var userName, passWord, hostName: String = _
  private val seqConfigFile = new SeqConfigFile(path)
  private val seqConfigOpt = seqConfigFile.read
  require(seqConfigOpt.isDefined, "Sequence repository config is undefined")
  private val seqConfig = seqConfigOpt.get
  //Check if server Config is defined 
  val serverConfigItemOpt = Wizard.items.get(2).flatten
  userName = seqConfig.dbUserName.getOrElse("")
  passWord = seqConfig.dbPassword.getOrElse("")
  hostName = seqConfig.dbHost.getOrElse("")
  port = seqConfig.dbPort.getOrElse(5432)

  //warning 
  val warningHostText = "WARNING: Using localhost or 127.0.0.1 is not advised, as it will make Proline available from this computer only."
  val warningHostLabel = new Label {
    graphic = ScalaFxUtils.newImageView(IconResource.WARNING)
    text = warningHostText
  }
  //host 
  val hostField = new TextField {
    if (hostName != null) text = hostName
    text.onChange { (_, oldText, newText) =>
      hostName = newText
      checkForm
    }
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
  private val V_SPACING = 5
  private val H_SPACING = 5

  //TitledBorderPane
  val dbConnectionSettingPane = new TitledBorderPane(
    title = "Database Server",
    contentNode = new VBox {
      prefWidth <== stage.width - 75
      prefHeight <== stage.height - 220
      spacing = V_SPACING
      children = Seq(
        emptyFieldErrorLabel,
        hostLabel,
        new HBox {
          spacing = H_SPACING
          children = Seq(new VBox {
            prefWidth <== stage.width - 35
            spacing = 2
            children = Seq(hostField, warningHostLabel)
          })
        },
        ScalaFxUtils.newVSpacer(minH = 1),
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
        }, ScalaFxUtils.newVSpacer(minH = 1),
        new HBox {
          children = List(ScalaFxUtils.newHSpacer(minW = 100), testConnectionButton)
        })
    })

  alignment = Pos.Center
  alignmentInParent = Pos.Center
  spacing = V_SPACING
  margin = Insets(5, 5, 5, 5)
  children = List(
    new VBox {
      vgrow = Priority.Always
      hgrow = Priority.Always
      children = Seq(dbConnectionSettingPane)
    })

  /** check Sequence Repository fields form  */
  def checkForm: Boolean = {
    val isValidatedFields = Seq(hostField, userField, portField, passwordPWDField).forall(!_.getText.isEmpty())
    if (isValidatedFields) emptyFieldErrorLabel.visible = false else emptyFieldErrorLabel.visible = true
    isValidatedFields
  }

  /** get GUI information to create a new SeqRepos Object */
  private def _toSeqConfig() = SeqConfig(
    driverType = Some(driver),
    maxPoolConnection = Some(3),
    dbUserName = if (serverConfigItemOpt.isDefined) Some(Wizard.userName) else Some(userName),
    dbPassword = if (serverConfigItemOpt.isDefined) Some(Wizard.passWord) else Some(passWord),
    dbHost = if (serverConfigItemOpt.isDefined) Some(Wizard.hostName) else Some(hostName),
    dbPort = if (serverConfigItemOpt.isDefined) Some(Wizard.port) else Some(port),
    dbUdsDb = Some("uds_db"))

  /** test connection to the database server */
  def _testDbConnection(
    showSuccessPopup: Boolean = false,
    showFailurePopup: Boolean = false): Boolean = {
    DatabaseConnection.testDbConnection(driver, userName, passWord, hostName, port, showSuccessPopup, showFailurePopup)
  }

  /** get database connection */
  def getInfos: String = {
    if (DatabaseConnection.testDbConnection(driver, userName, passWord, hostName, port, false, false))
      s"""PostgreSQL: OK""" else s"""PostgreSQL: NOK"""
  }

  /** save new Sequence Repository properties */
  def saveForm() {
    val newSeqConfig = _toSeqConfig()
    seqConfigFile.write(newSeqConfig)
  }

}


