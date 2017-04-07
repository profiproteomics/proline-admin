package fr.proline.admin.gui.component.configuration.form

import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

import java.io.File
import java.io.FileWriter

import scalafx.Includes._
import scalafx.beans.property.IntegerProperty
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Cursor
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.CheckBox
import scalafx.scene.control.{ RadioButton, ToggleGroup }
import scalafx.scene.control.ComboBox
import scalafx.scene.control.Hyperlink
import scalafx.scene.control.Label
import scalafx.scene.control.ScrollPane
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import scalafx.scene.control.TextField
import scalafx.scene.control.Tooltip
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.VBox
import scalafx.stage.Modality
import scalafx.stage.Stage

import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.Main
import fr.proline.admin.gui.process.config.postgres._
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.util.ShowPopupWindow

import fr.profi.util.StringUtils.LINE_SEPARATOR
import fr.profi.util.primitives._
import fr.profi.util.scala.ScalaUtils._
import fr.profi.util.scala.ScalaUtils
import fr.profi.util.scalafx.BoldLabel
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.profi.util.scalafx.ScalaFxUtils.newHSpacer
import fr.profi.util.scalafx.TitledBorderPane

import NewDatabaseNameDialog._

/**
 * ************************************************************ *
 * Form to edit and update PostgreSQL connections (pg_hba.conf) *
 * ************************************************************ *
 */
class PgHbaConfigForm(pgHbaConfigFilePath: String) extends VBox with IConfigFilesForm with LazyLogging {

  /* Read initial settings */
  val pgHbaConfigFile = new PgHbaConfigFile(pgHbaConfigFilePath)
  val pgHbaConfigInitSettings = pgHbaConfigFile.connectionLines

  /*
   * ********** *
   * COMPONENTS *
   * ********** *
   */

  /* Warning on commented lines */
  val warningLabel = new Label {
    graphic = FxUtils.newImageView(IconResource.WARNING)
    text = """Commented lines within "IPv4 connections" and "IPv6 connections" sections will be erased when changes are applied."""
    padding = Insets(0, 0, 10, 0)
  }

  /* Column names */
  private def _columnNames(addressType: AddressType.Value) = List(

    newHSpacer(70),

    new BoldLabel("Type", upperCase = false) {
      tooltip = new Tooltip() {
        text = "Connection type:\n\n" +
          "\"local\" is a Unix-domain socket,\n" +
          "\"host\" is either a plain or SSL-encrypted TCP/IP socket,\n" +
          "\"hostssl\" is an SSL-encrypted TCP/IP socket, and \"hostnossl\" is a plain TCP/IP socket."
      }
    },

    newHSpacer(110),

    new BoldLabel("Database", upperCase = false) {
      tooltip = new Tooltip() {
        text = "Database(s) name(s):\n\n" +
          "Can be \"all\", \"sameuser\", \"samerole\", \"replication\", a database name, or a comma-separated list thereof.\n" +
          "The \"all\" keyword does not match \"replication\". Access to replication must be enabled in a separate record.\n\n" +
          "An editor id provided to help you (icon on the rightof the DATABASE field)."
      }
    },

    newHSpacer(150),

    new BoldLabel("User", upperCase = false) {
      tooltip = new Tooltip() {
        text = "User name:\n\n" +
          "Can be \"all\", a user name, a group name prefixed with \"+\", or a comma-separated list thereof.\n" +
          "In both the DATABASE and USER fields you can also write a file name prefixed with \"@\"\n" +
          "to include names from a separate file."
      }
    },

    newHSpacer(80),

    new BoldLabel("IP address - Host name", upperCase = false) {
      tooltip = new Tooltip() {
        val sb = new StringBuilder()
        sb ++= "IP address:\n\n"
        sb ++= "Specifies the set of hosts the record matches.\n"
        sb ++= "The IP you must write here is the min IP the server can accept.\n\n"
        sb ++= "Ex: if you want to authorize all IPs between\n"
        sb ++= "127.0.0.0 and 127.0.0.4 (5 IPs),\n"
        sb ++= "write 127.0.0.0 in the IP ADDRESS field and\n"
        sb ++= "select 8 in the IPs COUNT field, since you need 5 (and 8 is the first choice above 5)."
        //TODO: allow hostbnames

        if (addressType == AddressType.IPv4) {
          sb ++= "\n\nAn IPv4 address must look like:\n"
          sb ++= "X.X.X.X where X is an integer between 0 and 256."
        } else {
          sb ++= "\n\nAn IPv6 address must look like:\n"
          sb ++= "X.X.X.X.X.X.X.X where X is an integer between 0 and 256."
        }

        text = sb.result()
      }
    },

    newHSpacer(50),

    new BoldLabel("IPs count", upperCase = false) {
      tooltip = new Tooltip() {
        text = "IPs count:\n\n" +
          "Defines the number of IPs that are given permission, starting from the IP\n" +
          "written in 'IP address' field. It will automatically compute the CIDR for the IP.\n\n" +
          "Ex: if you want to authorize all IPs between\n" +
          "127.0.0.0 and 127.0.0.4 (5 IPs),\n" +
          "write 127.0.0.0 in the IP ADDRESS field and\n" +
          "select 8 in the IPs COUNT field, since you need 5 (and 8 is the first choice above 5)."
      }
    },

    newHSpacer(55),

    new BoldLabel("Method", upperCase = false) {
      tooltip = new Tooltip() {
        text = "Method:\n\n" +
          "Can be \"trust\", \"reject\", \"md5\", \"password\", \"gss\", \"sspi\",\n" +
          "\"ident\", \"peer\", \"pam\", \"ldap\", \"radius\" or \"cert\".\n" +
          "Note that \"password\" sends passwords in clear text;\n" +
          "\"md5\" is preferred since it sends encrypted passwords."
      }
    })

  /* IPv4 */
  val ipv4Lines = ArrayBuffer[PgHbaLine]()
  val ipv4Label = new BoldLabel("IPv4 :", upperCase = false)
  val addIPv4LineButton = new Button("Add") {
    style = "-fx-inner-border : black;"
    onAction = handle { _addIPv4Line() }
  }
  val ipv4LinesBox = new VBox { spacing = 10 }

  /* IPv6 */
  val ipv6Lines = ArrayBuffer[PgHbaLine]()
  val ipv6Label = new BoldLabel("IPv6 :", upperCase = false)
  val addIPv6LineButton = new Button("Add") {
    style = "-fx-inner-border : black;"
    onAction = handle { _addIPv6Line() }
  }
  val ipv6LinesBox = new VBox { spacing = 10 }

  /*
   * ****** *
   * LAYOUT *
   * ****** *
   */

  Seq(ipv4Label, ipv6Label).foreach(_.minHeight = 25)

  //VBox & HBox spacing
  private val H_SPACING = 5

  /* IPv4 */
  val ipv4Panel = new VBox {
    vgrow = Priority.Always
    content = List(
      //      new HBox {
      //        spacing = H_SPACING
      //        content = List(ipv4Label, addIPv4LineButton)
      //      },
      addIPv4LineButton,

      newVSpacer(15, 25),

      new HBox {
        content = _columnNames(AddressType.IPv4)
      },

      newVSpacer(10, 20),

      ipv4LinesBox)
  }

  val ipV4BorderPane = new TitledBorderPane(
    "IPv4",
    ipv4Panel)

  /* IPv6 */
  val ipv6Panel = new VBox {
    vgrow = Priority.Always
    content = List(
      addIPv6LineButton,
      new HBox { content = _columnNames(AddressType.IPv6) },
      newVSpacer(15, 25),
      ipv6LinesBox)
  }

  val ipV6BorderPane = new TitledBorderPane(
    "IPv6",
    ipv6Panel)

  /* VBox content */
  spacing = 20
  content = Seq(warningLabel, ipV4BorderPane, ipV6BorderPane, wrappedApplyButton)

  /*
   * ************* *
   * INIT. CONTENT *
   * ************* *
   */

  pgHbaConfigInitSettings.foreach { line =>

    /* IP adress */
    val isAdressOfTypeIpCidr = line.addressWithCIDR contains "/"
    val (address, cidr) = {

      if (isAdressOfTypeIpCidr) {
        val split = line.addressWithCIDR.split("/")
        require(split.length == 2, s"""Address should be of type: IP/CIDR (found: ${line.addressWithCIDR} within line '$line')""")
        (split.head, toInt(split.last))
      } else {
        println(s"""Address is not of type: IP/CIDR (found: ${line.addressWithCIDR} within line '$line')""")
        // TODO: handle server names and keywords 
        if (line.addressWithCIDR.equals("samenet"))
          ("samenet", -1)
        else if (line.addressWithCIDR.equals("samehost"))
          ("samehost", -1)
        else if (line.addressWithCIDR.equals("all"))
          ("all", -1)
        else ("", -1)
      }
    }

    /* IPv4 */
    if (line.addressType == AddressType.IPv4) {
      val maxIpsCount =
        if (isAdressOfTypeIpCidr) math.pow(2, (32 - cidr)).toInt
        else -1 // TODO: handle server names and keywords

      _addIPv4Line(
        line.connectionType,
        line.database,
        line.user,
        address,
        maxIpsCount,
        line.method,
        line.commented)
    } /* IPv6 */ else {
      val maxIpsCount = math.pow(2, (128 - cidr)).toInt

      _addIPv6Line(
        line.connectionType,
        line.database,
        line.user,
        address,
        maxIpsCount,
        line.method,
        line.commented)
    }
  }

  /*
   * ******** *
   * FEATURES *
   * ******** *
   */

  /** Add components to define another connection **/
  private def _addLine(
    linesBuffer: ArrayBuffer[PgHbaLine],
    linesBox: VBox,
    addressType: AddressType.Value,
    connectionType: ConnectionType.Value,
    database: String,
    user: String,
    address: String,
    maxIPCount: Int,
    method: Method.Value,
    commented: Boolean) {

    /* Action run when line is deleted */
    def _onDeleteAction(line: PgHbaLine): Unit = {
      linesBuffer -= line

      // Update indexes
      for (i <- 0 until linesBuffer.length) {
        val line = linesBuffer(i)
        line.index.set(i) //property linked to UI
      }

      // Update VBox content
      linesBox.content = linesBuffer
    }

    /* Add new line */
    linesBuffer += new PgHbaLine(
      _onDeleteAction,
      linesBuffer.length, //new line index
      addressType,
      connectionType,
      database,
      user,
      address,
      maxIPCount,
      method,
      commented)

    linesBox.content = linesBuffer
  }

  /** Add components to define another IPv4 connection **/
  private def _addIPv4Line(
    connectionType: ConnectionType.Value = ConnectionType.HOST,
    database: String = "",
    user: String = "",
    address: String = "",
    maxIPCount: Int = -1,
    method: Method.Value = Method.MD5,
    commented: Boolean = false) = {
    _addLine(
      ipv4Lines, ipv4LinesBox, AddressType.IPv4,
      connectionType, database, user, address, maxIPCount, method, commented)
  }

  /** Add components to define another IPv6 connection **/
  private def _addIPv6Line(
    connectionType: ConnectionType.Value = ConnectionType.HOST,
    database: String = "",
    user: String = "",
    address: String = "",
    maxIPCount: Int = -1,
    method: Method.Value = Method.MD5,
    commented: Boolean = false) = {
    _addLine(
      ipv6Lines, ipv6LinesBox, AddressType.IPv6,
      connectionType, database, user, address, maxIPCount, method, commented)
  }

  /** Check if the form is correct, show a popup describing errors if not **/
  def checkForm(): Boolean = {

    val sb = new StringBuilder()
    val sb4 = new StringBuilder()
    val sb6 = new StringBuilder()

    /* Check IPv4 lines one by one */
    for (i <- 0 until ipv4Lines.length) {
      val line = ipv4Lines(i)
      val errorString = line.checkLine()
      if (errorString.isEmpty() == false) {
        sb4 ++= s"\nLine #${i + 1}:\n"
        sb4 ++= errorString
      }
    }

    /* Header for IPv4 lines */
    if (sb4.isEmpty == false) {
      sb ++= "Errors in IPv4 addresses:"
      sb ++= sb4.result()
    }

    /* Check IPv6 lines one by one */
    for (i <- 0 until ipv6Lines.length) {
      val line = ipv6Lines(i)
      val errorString = line.checkLine()
      if (errorString.isEmpty() == false) {
        sb6 ++= s"\nLine #${i + 1}:\n"
        sb6 ++= errorString
      }
    }

    /* Header for IPv6 lines */
    if (sb6.isEmpty == false) {
      sb ++= "\n\n\nErrors in IPv6 addresses:"
      sb ++= sb6.result()
    }

    /* Display a popup to show errors */
    val errorString = sb.result()
    val errorStringIsEmpty = ScalaUtils.isEmpty(errorString)
    if (errorStringIsEmpty == false) ShowPopupWindow(
      wTitle = "Errors in form",
      wText = errorString)

    errorStringIsEmpty
  }

  /** Save the form (write in config file) **/
  def saveForm() {
    val configFile = new File(pgHbaConfigFile.filePath)

    /* Save current file state in backup file */
    // this method is already wrapped in a 'synchronized' block
    ScalaUtils.createBackupFile(configFile)

    /* Update model */
    pgHbaConfigFile.updateLines(
      ipv4Lines.result().toArray,
      ipv6Lines.result().toArray)

    /* Update file */
    synchronized {
      val out = new FileWriter(configFile)
      try {
        val linesToBeWritten = pgHbaConfigFile.lines.mkString(LINE_SEPARATOR)
        out.write(linesToBeWritten)
      } finally {
        out.close
      }
    }

    println("<br>INFO - pg_hba.conf successfully updated !<br>")
    logger.info("pg_hba.conf successfully updated !")
  }
}

/**
 * ********************* *
 * Build one pg_hba line *
 * ********************* *
 */
case class PgHbaLine(
  onDeleteAction: (PgHbaLine) => Unit,
  initIndex: Int,
  addressType: AddressType.Value = AddressType.IPv4,

  connectionType: ConnectionType.Value = ConnectionType.HOST,
  database: String = "",
  user: String = "",
  address: String = "",
  maxIPCount: Int = -1,
  method: Method.Value = Method.MD5,
  commented: Boolean = false) extends HBox {

  val thisLine = this

  /* 
   * ********** *
   * COMPONENTS *
   * ********** *
   */
  val indexLabel = new Label {
    //    text <== StringProperty('#' + (index() + 1).toString())
  }
  val index = new IntegerProperty() {
    value = -1
    onChange { (_, _, i) =>
      indexLabel.text = '#' + (this() + 1).toString()
    }
  }
  index.set(initIndex)

  //  val commentedBox = new CheckBox(){
  //    selected = ! commented
  //  }

  val connectionTypeBox = new ComboBox[ConnectionType.Value] {
    items = ConnectionType.values.toSeq
    selectionModel().select(connectionType)
    minWidth = 92
    //disable <== ! commentedBox.selected
  }

  val databaseField = new TextField {
    promptText = "database"
    text = database
    minWidth = 144
    //disable <== !commentedBox.selected
  }

  val databaseDialogIcon = new Hyperlink {
    graphic = FxUtils.newImageView(IconResource.EDIT)
    onAction = handle {
      databaseField.text = NewDatabaseNameDialog(databaseField.text())
    }
    //disable <== !commentedBox.selected
  }

  //disable <== !commentedBox.selected

  val userField = new TextField {
    promptText = "user"
    text = user
    minWidth = 125
    //disable <== !commentedBox.selected
  }

  val addressField = new TextField {
    promptText = "address"
    text = address
    minWidth = 125
    //disable <== ! commentedBox.selected
  }
  val adressDialogIcon = new Hyperlink {
    graphic = FxUtils.newImageView(IconResource.EDIT)
    onAction = handle {
      import NewAdressDialog._
      addressField.text = NewAdressDialog(addressField.text())
    }
  }
  val maxIpCountBox = new ComboBox[Int] {
    items = (0 to 16).map(math.pow(2, _).toInt)
    minWidth = 75
    //disable <== ! commentedBox.selected
  }
  if (maxIPCount > 0) maxIpCountBox.selectItem(maxIPCount) //use custom wrapper utility because of select(index: Int) ambiguity

  val methodBox = new ComboBox[Method.Value] {
    items = Method.values.toSeq
    selectionModel().select(method)
    minWidth = 93
    //disable <== ! commentedBox.selected
  }

  val removeButton = new Button("Remove") {
    minWidth = 59
    style = "-fx-inner-border : black;"
    onAction = handle { onDeleteAction(thisLine) }
  }

  /* 
   * ****** *
   * LAYOUT *
   * ****** *
   */
  val SPACING = 15
  alignment = Pos.Center

  content = List(
    indexLabel,
    newHSpacer(SPACING),
    //    commentedBox,
    //    newHSpacer(SPACING),
    connectionTypeBox,
    newHSpacer(SPACING),
    databaseField,
    databaseDialogIcon,
    newHSpacer(SPACING),
    userField,
    newHSpacer(SPACING),
    addressField,
    adressDialogIcon,
    newHSpacer(5),
    maxIpCountBox,
    newHSpacer(SPACING),
    methodBox,
    newHSpacer(SPACING),
    removeButton)

  /*
   * ******** *
   * FEATURES *
   * ******** *
   */

  /** Get content **/
  def getAddressType(): AddressType.Value = addressType
  def getConnectionType(): ConnectionType.Value = connectionTypeBox
  def getDatabases(): String = databaseField
  def getUser(): String = userField
  //def getAddress(): String = addressField
  //def getMaxIpCount(): Int = maxIpCountBox
  def getMethod(): Method.Value = methodBox

  /** Get IP and mask **/
  def getAddressWithCIDR(): String = {

    // Compute CIDR: 
    // (number of bytes of IP) - (power of 2 asked to set variable)
    val adressPattern = """\d+\.\d+\.\d+\.\d+"""
    val adressPatternIp = """([\da-f]*:*)+"""
    if ((addressField.getText matches adressPattern) || (addressField.getText matches adressPatternIp)) {
      val maxIpCountIdx = maxIpCountBox.selectionModel().selectedIndex()
      val cidr: Int =
        if (addressType == AddressType.IPv4) 32 - maxIpCountIdx
        else 128 - maxIpCountIdx

      // return <adress>/<CIDR>
      addressField + '/' + cidr
    } else {
      return addressField
    }
  }

  /** Check line conformity **/
  def checkLine(): String = { //error string

    val errorString = new StringBuilder()

    /* All fields are required */
    if (connectionTypeBox.selectionModel().selectedItem() == null) {
      errorString ++= "The connection type must be specified.\n"
    }
    if (databaseField.text().isEmpty()) {
      errorString ++= "The database(s) must be specified.\n"
    }
    if (userField.text().isEmpty()) {
      errorString ++= "The user must be specified.\n"
    }
    if (addressField.text().isEmpty()) {
      errorString ++= "The IP address must be specified.\n"
    }
    //    if (maxIpCountBox.selectionModel().selectedItem() < 1) {
    //      errorString ++= "The maximum of accepted IPs must be specified.\n"
    //    }
    if (methodBox.selectionModel().selectedItem() == null) {
      errorString ++= "The method for password encryption must be specified.\n"
    }

    if (errorString.isEmpty == false) errorString ++= "\n"

    /* Database name(s): comma-separated list */
    // From PostgreSQL doc:
    // Database names must have an alphabetic first character and are limited to 63 bytes in length.
    val dbPattern = """([a-zA-Z]\w+)(,[a-zA-Z]\w+)*"""
    if ((databaseField matches dbPattern) == false) {
      errorString ++= "Incorrect database name(s). Please refer to help (hover your mouse pointer over column names to reveal more information).\n"
    }

    /* IP address follows a correct pattern */
    val ipPattern = {
      if (addressType == AddressType.IPv4) """(\d+\.\d+\.\d+\.\d+|samenet|samehost|all)"""
      else """(([\da-f]*:*)+|samenet|samehost|all)"""
    }
    if ((addressField matches ipPattern) == false) {
      errorString ++= "\nIncorrect IP address. Please refer to help.\n"
    }

    errorString.result()
  }

  /** Create tab-separated string from components **/
  def toTabbedLine(): String = {
    val columns = ArrayBuffer[String]()
    columns += getConnectionType().toString()
    columns += getDatabases()
    columns += getUser()
    columns += getAddressWithCIDR()
    columns += getMethod().toString()

    columns.result().mkString("\t")
  }
}

/**
 * ********************************* *
 * Dialog for database names edition *
 * ********************************* *
 */
class DatabaseNameDialog(
  all: Boolean,
  sameUser: Boolean,
  sameRole: Boolean,
  replication: Boolean,
  names: String //comma-separated list
  ) extends Stage {

  val thisDialog = this

  title = "Edit database(s) name(s)"

  /*
   * ********** *
   * COMPONENTS *
   * ********** *
   */
  val allBox = new CheckBox("All") {
    selected = all
    id = "all"
  }
  val sameUserBox = new CheckBox("Same user") {
    selected = sameUser
    id = "sameuser"
  }
  val sameRoleBox = new CheckBox("Same role") {
    selected = sameRole
    id = "samerole"
  }
  val replicationBox = new CheckBox("Replication") {
    selected = replication
    id = "replication"
  }
  val namesBox = new CheckBox("Names (comma-seperated):") {
    selected = !names.isEmpty()
  }
  val namesField = new TextField {
    text = names
  }

  val applyButton = new Button("Apply") {
    onAction = handle { _onApplyPressed() }
  }
  val cancelButton = new Button("Cancel") {
    onAction = handle { thisDialog.close() }
  }

  /*
   * ****** *
   * LAYOUT *
   * ****** *
   */

  val defaultNames = List(
    allBox,
    sameUserBox,
    sameRoleBox,
    replicationBox)

  /* Buttons */
  val buttonList = Seq(applyButton, cancelButton)
  buttonList.foreach { b =>
    b.prefWidth <== thisDialog.width
    b.minHeight = 30
  }
  val buttons = new HBox {
    alignmentInParent = Pos.Center
    spacing = 20
    content = buttonList
  }

  /* Scene */
  scene = new Scene {

    onKeyPressed = (ke: KeyEvent) => {
      closeIfEscapePressed(thisDialog, ke)
      fireIfEnterPressed(applyButton, ke)
    }

    root = new VBox {
      alignment = Pos.BaselineLeft
      padding = Insets(20)
      spacing = 20
      content = defaultNames ++ List(

        new HBox {
          spacing = 10
          content = List(namesBox, namesField)
        },

        newHSpacer(30, 50),

        buttons)
    }
  }

  /*
   * ****** *
   * FEATURES *
   * ****** *
   */
  /** Make sure names string is ok **/
  private def _checkNames(): Boolean = {
    if (namesBox.selected()) namesField matches """\w+(,\w+)*"""
    else true
  }

  /** Make string from UI information **/
  def getString(): String = {
    val defaults = defaultNames.filter(_.selected()).map(_.id())
    val sb = new StringBuilder(defaults.mkString(","))

    if (namesBox.selected()) {
      sb += ','
      sb ++= namesField
    }

    sb.result()
  }

  /** Check names when "apply" is pressed, close dialog and retrun string if OK **/
  private def _onApplyPressed(): Unit = {
    if (_checkNames()) thisDialog.close()
    else namesField.style = "-fx-background-color : #F6CECE;"
  }
}

object NewDatabaseNameDialog {

  /** Create and show a pre-filled dialog **/
  def apply(
    all: Boolean = true,
    sameUser: Boolean = false,
    sameRole: Boolean = false,
    replication: Boolean = false,
    names: String = "" //comma-separated list
    ): DatabaseNameDialog = {

    val dialog = new DatabaseNameDialog(all, sameUser, sameRole, replication, names)
    dialog.showAndWait()
    dialog
  }

  /** Secondary constructor: parse String in PgHbaFormDialog **/
  def apply(string: String): DatabaseNameDialog = {
    val namesBuff = new ListBuffer[String]()
    var (all, sameUser, sameRole, replication) = (false, false, false, false)

    val split = string.split(",")
    split.foreach { name =>

      if (name == "all") all = true
      else if (name == "sameuser") sameUser = true
      else if (name == "samerole") sameRole = true
      else if (name == "replication") replication = true
      else namesBuff += name
    }

    this(all, sameUser, sameRole, replication, namesBuff.mkString(","))

  }

  /** Implicitly return a String from the dialog **/
  implicit def dialog2string(dialog: DatabaseNameDialog): String = dialog.getString()
}
/**
 * ********************************* *
 * Dialog for host names edition *
 * ********************************* *
 */
class AdressDialog(
  all: Boolean,
  samenet: Boolean,
  samehost: Boolean) extends Stage {

  val thisDialog = this
  title = "Choose a host name"

  /*
   * ********** *
   * COMPONENTS *
   * ********** *
   */
  val tog = new ToggleGroup()
  val allBox = new RadioButton("all") {
    selected = all
    id = "all"
    toggleGroup = tog
  }
  val sameNetBox = new RadioButton("samenet") {
    selected = samenet
    id = "samenet"
    toggleGroup = tog
  }

  val sameHostBox = new RadioButton("samehost") {
    selected = samehost
    id = "samehost"
    toggleGroup = tog
  }

  val applyButton = new Button("Apply") {
    onAction = handle { _onApplyPressed() }
  }
  val cancelButton = new Button("Cancel") {
    onAction = handle {
      thisDialog.close()
    }
  }

  /*
   * ****** *
   * LAYOUT *
   * ****** *
   */

  val defaultNames = List(
    allBox,
    sameNetBox,
    sameHostBox)

  /* Buttons */

  val buttonList = Seq(applyButton, cancelButton)
  buttonList.foreach { b =>
    b.prefWidth <== thisDialog.width
    b.minHeight = 10
  }
  val buttons = new HBox {
    alignmentInParent = Pos.Center
    spacing = 20
    content = buttonList
  }

  /* Scene */
  scene = new Scene {

    onKeyPressed = (ke: KeyEvent) => {
      closeIfEscapePressed(thisDialog, ke)
      fireIfEnterPressed(applyButton, ke)
    }

    root = new VBox {
      alignment = Pos.BaselineLeft
      padding = Insets(20)
      spacing = 20
      minWidth = 300
      content = defaultNames ++ List(
        newHSpacer(30, 50),
        buttons)
    }
  }

  /*
   * ****** *
   * FEATURES *
   * ****** *
   */
  /** Make sure names string is ok **/

  def getString(): String = {
    val defaults = defaultNames.filter(_.selected()).map(_.id())
    val sb = new StringBuilder(defaults.mkString(","))
    sb.result()
  }

  /** Check names when "apply" is pressed, close dialog and return string if OK **/
  private def _onApplyPressed(): Unit = {
    thisDialog.close()
  }
}
object NewAdressDialog {

  /** Create and show a pre-filled dialog **/
  def apply(
    all: Boolean = false,
    samenet: Boolean = true,
    samehost: Boolean = false): AdressDialog = {

    val dialog = new AdressDialog(all, samenet, samehost)
    dialog.showAndWait()
    dialog
  }

  /** Secondary constructor: parse String in PgHbaFormDialog **/

  def apply(string: String): AdressDialog = {
    val namesBuff = new ListBuffer[String]()
    var (all, samenet, samehost) = (false, true, false)

    val split = string.split(",")
    split.foreach { name =>
      if (name == "samenet") samenet = true
      else if (name == "samehost") samehost = true
      else if (name == "all") all = true

    }
    this(all, samenet, samehost)
  }
  /** Implicitly return a String from the dialog :samanet or samehost **/

  implicit def dialog2string(dialog: AdressDialog): String = dialog.getString()
}