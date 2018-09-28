package fr.proline.admin.gui.component.resource

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.geometry.HPos
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Node
import scalafx.scene.control.Hyperlink
import scalafx.scene.control.Label
import scalafx.scene.control.PasswordField
import scalafx.scene.control.TableCell
import scalafx.scene.control.TableColumn
import scalafx.scene.control.TextField
import scalafx.scene.layout.ColumnConstraints
import scalafx.scene.layout.ColumnConstraints.sfxColumnConstraints2jfx
import scalafx.scene.layout.GridPane
import scalafx.scene.layout.Priority
import scalafx.scene.layout.VBox
import scalafx.scene.control.CheckBox
import fr.proline.admin.gui.Main
import fr.proline.admin.gui.component.ButtonsPanel
import fr.proline.admin.gui.component.resource.implicits.UserView
import fr.proline.admin.gui.process.LaunchAction
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.util.ShowPopupWindow
import fr.proline.admin.service.user.CreateUser
import fr.proline.core.orm.uds.UserAccount
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.EnhancedTableView
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle
import javafx.scene.shape._
import javafx.animation._
import javafx.util.Duration

import fr.profi.util.security.sha256Hex
/**
 * ******************* *
 * Content of UsersTab *
 * ******************* *
 */
class UsersTab() extends IResourceManagementTab {

  text = "Users"

  protected val newEntryPanel: INewEntryPanel = new NewUserPanel()
  val table = new UsersTable()

  content = new VBox {
    prefHeight = CONTENT_PREF_HEIGHT
    spacing = 5
    padding = Insets(20)
    children = Seq(newEntryPanel, table)
  }
}

/**
 * ********************** *
 * Table to see all users *
 * ********************** *
 */
class UsersTable() extends AbstractResourceTableView[UserView] {

  /* Build store */
  val usersViews = UdsRepository.getAllUserAccounts().toBuffer[UserAccount].sortBy(_.getId).map(new UserView(_))
  protected lazy val tableLines = ObservableBuffer(usersViews)

  /* ID */
  val idCol = new TableColumn[UserView, Long]("ID") {
    cellValueFactory = { _.value.id }
    cellFactory = { _ =>
      new TableCell[UserView, Long] {
        style = "-fx-alignment: CENTER;"
        item.onChange { (_, _, newValue) => text = newValue.toString() }
      }
    }
  }

  /* Login */
  val loginCol = new TableColumn[UserView, String]("Login") {
    cellValueFactory = { _.value.login }
    cellFactory = { _ =>
      new TableCell[UserView, String] {
        style = "-fx-alignment: CENTER;"
        item.onChange { (_, _, newValue) => text = newValue }
      }
    }
  }

  /* Password hash */
  val pwdHashCol = new TableColumn[UserView, String]("Password hash") {
    cellValueFactory = { _.value.pwdHash }
    cellFactory = { _ =>
      new TableCell[UserView, String] {
        style = "-fx-alignment: CENTER;"
        item.onChange { (_, _, newValue) => text = newValue }
      }
    }

  }

  /* creation mode */
  val creationModeCol = new TableColumn[UserView, String]("Creation mode") {
    cellValueFactory = { _.value.mode }
    cellFactory = { _ =>
      new TableCell[UserView, String] {
        style = "-fx-alignment: CENTER;"
        item.onChange { (_, _, newValue) => text = newValue }
      }
    }
  }
  /* user group */

  val userGroupCol = new TableColumn[UserView, String]("User group") {
    cellValueFactory = { _.value.userGroup }
    cellFactory = { _ =>
      new TableCell[UserView, String] {
        style = "-fx-alignment: CENTER;"
        item.onChange { (_, _, newValue) => text = newValue }
      }
    }
  }
  /*user isActive */
  val isActiveCol = new TableColumn[UserView, String]("State") {
    cellValueFactory = { _.value.userIsActive }
    cellFactory = { _ =>
      new TableCell[UserView, String] {
        style = "-fx-alignment: CENTER;"
        item.onChange { (_, _, newValue) => text = newValue }
      }
    }
  }
  /* Get JavaFx columns to fill table view */
  protected lazy val tableColumns: List[javafx.scene.control.TableColumn[UserView, _]] = List(idCol, loginCol, pwdHashCol, creationModeCol, userGroupCol, isActiveCol)

  /* Set columns width */
  this.applyPercentWidth(List(
    (idCol, 10),
    (loginCol, 15),
    (pwdHashCol, 30),
    (creationModeCol, 15),
    (userGroupCol, 15),
    (isActiveCol, 15)))

  /* Initialize table content */
  this.init()
}

/**
 * *********************** *
 * Panel to add a new user *
 * *********************** *
 */
class NewUserPanel() extends INewEntryPanel with LazyLogging {

  /*
   * ********** *
   * COMPONENTS *
   * ********** *
   */
  var userCreator: CreateUser = _
  /* Border title */
  protected val title: String = "Add new user"

  /* Login */
  val loginLabel = new Label("Login* : ")
  val loginField = new TextField

  val EMPTY_LOGIN_MSG = "Empty login"
  val ALREADY_ATTRIBUTED_LOGIN_MSG = "This login is already attributed."

  val loginWarningLabel = new Label {
    style = TextStyle.RED_ITALIC
    minHeight = 15
  }

  /* Password */
  val pwLabel = new Label("Password : ")
  val pwField = new PasswordField

  val pwConfirmLabel = new Label("Confirm password : ")
  val pwConfirmField = new PasswordField

  val pwWarningLabel = new Label {
    // styleClass += ("warningLabels")
    style = TextStyle.RED_ITALIC
    minHeight = 15
  }
  // create this user as an administartor 

  val isAdmin = new CheckBox("Add user to administrator group") {
    selected = false
  }
  val createdUserWarningLabel = new Label {
    text = "User has been created successfully."
    style = TextStyle.RED_ITALIC
    minHeight = 15
    visible = false
  }
  /*
   * ****** *
   * LAYOUT *
   * ****** *
   */
  protected val form: Node = new GridPane {
    hgrow = Priority.Always
    vgap = 5
    hgap = 10
    padding = Insets(10)
    prefWidth = 500

    columnConstraints ++= Seq(
      new ColumnConstraints { percentWidth = 25 },
      new ColumnConstraints { percentWidth = 75 })

    children = ScalaFxUtils.getFormattedGridContent5(Seq(
      //col, row, colSpan, rowSpan
      (loginLabel, 0, 0, 1, 1),
      (loginField, 1, 0, 1, 1),
      (loginWarningLabel, 1, 1, 1, 1),
      (pwLabel, 0, 3, 1, 1),
      (pwField, 1, 3, 1, 1),
      (pwConfirmLabel, 0, 4, 1, 1),
      (pwConfirmField, 1, 4, 1, 1),
      (isAdmin, 1, 5, 1, 1),
      (pwWarningLabel, 1, 6, 2, 1),
      (createdUserWarningLabel, 1, 7, 1, 1)))
  }

  Seq(
    loginLabel,
    pwLabel,
    pwConfirmLabel).foreach(GridPane.setHalignment(_, HPos.Right))

  /*
   * ************** *
   * INITIALIZATION *
   * ************** *
   */
  this.initContent()

  /*
   * ******** *
   * FEATURES *
   * ******** *
   */

  /** Hide warning labels **/
  private def _hideWarnigs() = Seq(loginWarningLabel, pwWarningLabel, createdUserWarningLabel).foreach(_.visible = false)

  /** Clear the form **/
  protected def clearForm(): Unit = {
    Seq(loginField, pwField, pwConfirmField).foreach(_.text = "")
    _hideWarnigs()
  }

  /** Show a Warning Label when user created **/
  private def warningCreatedUser(): Unit = {

    createdUserWarningLabel.visible = true
    val ft = new FadeTransition(Duration.millis(2000), createdUserWarningLabel)
    ft.setFromValue(1.0)
    ft.setToValue(0.0)
    ft.setCycleCount(3)
    ft.setAutoReverse(false)
    ft.play()
  }
  /** Check if the form is correct, display warning if needed **/
  protected def checkForm(): Boolean = {

    _hideWarnigs()

    var isLoginAvailable = false

    /* Login is defined */
    val _login = loginField.text()
    val isLoginDefined = !_login.isEmpty

    if (isLoginDefined == false) {
      loginWarningLabel.text = EMPTY_LOGIN_MSG
      loginWarningLabel.visible = true
    } /* Login is available */ else {
      isLoginAvailable = {
        if (isLoginDefined) {
          UdsRepository.getAllUserAccounts().exists(_.getLogin() == _login) == false
        } else false
      }

      if (isLoginAvailable == false) {
        loginWarningLabel.text = ALREADY_ATTRIBUTED_LOGIN_MSG
        loginWarningLabel.visible = true
      }
    }

    /* Password and confirmation match */
    val _pw = pwField.text()
    val _pwConfirm = pwConfirmField.text()
    val isPwdConfirmed = _pw == _pwConfirm

    if (isPwdConfirmed == false) {
      pwWarningLabel.visible = true
    }

    isLoginDefined && isLoginAvailable && isPwdConfirmed
  }

  /** Save the form **/
  protected def saveForm(): Unit = {
    val _login = loginField.text()
    val _pw = pwField.text()
    //TODO: don't cmopute me twice (check and save in the same code ?)

    /* Create command line (ProlineAdmin) */
    val (pswdOpt, cmd) =
      if (_pw.isEmpty())
        (None, s"create_user --login ${_login}")
      else
        (Some(_pw), s"create_user --login ${_login} --password ${"*" * _pw.length()}")

    /* Insert user in database using ProlineAdmin */
    logger.debug("Create user")
    import fr.proline.admin.gui.util.Utils

    LaunchAction(
      actionButton = ButtonsPanel.manageResourcesButton,
      actionString = Utils.mkCmd(cmd),
      action = () => {

        val udsDbContext = UdsRepository.getUdsDbContext()

        /* Create user */

        val pswd = if (pswdOpt.isDefined) pswdOpt.get else "proline" //TODO: define in config!
        if (isAdmin.isSelected) { userCreator = new CreateUser(udsDbContext, _login, pswd, false) }
        else { userCreator = new CreateUser(udsDbContext, _login, pswd, true) }
        if (userCreator != null) {
          userCreator.run()
          warningCreatedUser()
          isAdmin.selected = false
        }
      })
  }

}