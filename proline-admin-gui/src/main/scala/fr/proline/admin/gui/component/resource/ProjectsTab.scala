package fr.proline.admin.gui.component.resource

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.HPos
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control.ComboBox
import scalafx.scene.control.Hyperlink
import scalafx.scene.control.Label
import scalafx.scene.control.SelectionMode
import scalafx.scene.control.TableCell
import scalafx.scene.control.TableColumn
import scalafx.scene.control.TableColumn.sfxTableColumn2jfx
import scalafx.scene.control.TableView
import scalafx.scene.control.TextArea
import scalafx.scene.control.TextField
import scalafx.scene.control.Button
import scalafx.scene.layout.ColumnConstraints
import scalafx.scene.layout.ColumnConstraints.sfxColumnConstraints2jfx
import scalafx.scene.layout.GridPane
import scalafx.scene.layout.Priority
import scalafx.scene.layout.VBox

import fr.proline.admin.gui.Main
import fr.proline.admin.gui.component.ButtonsPanel
import fr.proline.admin.gui.component.resource.implicits.ProjectView
import fr.proline.admin.gui.process.LaunchAction
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.util.ShowPopupWindow
import fr.proline.admin.gui.util.Utils
import fr.proline.admin.service.db.CreateProjectDBs
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.service.user.CreateProject
import fr.proline.core.orm.uds.Project
import fr.proline.core.orm.uds.UserAccount

import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.EnhancedTableView
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle

/**
 * ********************** *
 * Content of ProjectsTab *
 * ********************** *
 */
class ProjectsTab() extends IResourceManagementTab {

  text = "Projects"

  protected val newEntryPanel: INewEntryPanel = new NewProjectPanel()
  val table = new ProjectsTable()

  val tabContent = new VBox {
    prefHeight = CONTENT_PREF_HEIGHT
    spacing = 5
    padding = Insets(20)
    content = Seq(newEntryPanel, table)
  }

  content = tabContent

  //  /** Get this tab **/
  //  def apply() = this
}

/**
 * ************************* *
 * Table to see all projects *
 * ************************* *
 */

class ProjectsTable() extends AbstractResourceTableView[ProjectView] {

  /* Build store */
  val projectViews = UdsRepository.getAllProjects().toBuffer[Project].sortBy(_.getId).map(new ProjectView(_))

  protected lazy val tableLines = ObservableBuffer(projectViews)
  /* Project ID */
  val idCol = new TableColumn[ProjectView, Long]("ID") {
    cellValueFactory = { _.value.id }
    cellFactory = { _ =>
      new TableCell[ProjectView, Long] {
        style = "-fx-alignment: CENTER;"
        item.onChange { (_, _, newValue) => text = newValue.toString }
      }
    }
  }

  /* Owner login */
  val ownerCol = new TableColumn[ProjectView, String]("Owner") {
    cellValueFactory = { _.value.ownerLogin }
    cellFactory = { _ =>
      new TableCell[ProjectView, String] {
        style = "-fx-alignment: CENTER;"
        item.onChange { (_, _, newValue) => text = newValue }
      }
    }
  }

  /* Project name */
  val projectNameCol = new TableColumn[ProjectView, String]("Name") {
    cellValueFactory = { _.value.name }
       cellFactory = { _ =>
      new TableCell[ProjectView, String] {
        style = "-fx-alignment: CENTER;"
        item.onChange { (_, _, newValue) => text = newValue }
      }
    }
  }

  /* Project description */
  val projectDescCol = new TableColumn[ProjectView, String]("Description") {
    visible = false
    cellValueFactory = { _.value.description }
    cellFactory = { _ =>
      new TableCell[ProjectView, String] {
        //TODO: hyperlink with popup, or multiple line cell, or...
      }
    }
  }

  /*column of schema version default  = no.version */

  val projectVersionCol = new TableColumn[ProjectView, String]("Schema version (Msi-Lcms)") {
    cellValueFactory = { _.value.version }
    cellFactory = { _ =>
      new TableCell[ProjectView, String] {
        style = "-fx-alignment: CENTER;"
        item.onChange { (_, _, newValue) => text = newValue }
      }
    }
  }

  /* column of database size */

  val projectSizeCol = new TableColumn[ProjectView, String]("Size (Msi-Lcms)") {
    cellValueFactory = { _.value.size }
    cellFactory = { _ =>
      new TableCell[ProjectView, String] {
        style = "-fx-alignment: CENTER;"
        item.onChange { (_, _, newValue) => text = newValue }
      }
    }
  }

  /* column contain some button */

  //val actionCol = new TableColumn[ProjectView, Boolean]("Action")

  /*button to delete , update */
  /* Get JavaFx columns to fill table view */
  protected lazy val tableColumns: List[javafx.scene.control.TableColumn[ProjectView, _]] = List(idCol, ownerCol, projectNameCol, projectDescCol, projectVersionCol, projectSizeCol)

  /* Set columns width */
  this.applyPercentWidth(List(
    (idCol, 10),
    (ownerCol, 10),
    (projectVersionCol, 20),
    (projectNameCol, 40),
    (projectDescCol, 40),
    (projectSizeCol, 20)
    ))

  /* Initialize table content */
  this.init()
}

/**
 * ************************** *
 * Panel to add a new project *
 * ************************** *
 */
class NewProjectPanel() extends INewEntryPanel with LazyLogging {

  /*
   * ********** *
   * COMPONENTS *
   * ********** *
   */

  /* Border title */
  protected val title: String = "Add new project"

  /* Project owner */
  val userAccounts = UdsRepository.getAllUserAccounts()
  val ownerLabel = new Label("Owner* : ")
  val ownerComboBox = new ComboBox[PrintableUserAccount] {

    val obsBuf = new ObservableBuffer[PrintableUserAccount]
    userAccounts.map { userAccount => new PrintableUserAccount(userAccount) }.sortBy(ua => ua.getLogin().toLowerCase).copyToBuffer(obsBuf)

    items = obsBuf
    selectionModel().select(-1)
    prefWidth = Double.MaxValue
  }

  val ownerWarningLabel = new Label {
    text = "Please select the project's owner."
    visible = false
    style = TextStyle.RED_ITALIC
    minHeight = 15
  }

  /* See user's projects */
  val seeUserProjects = new Hyperlink("See user's projects...") {
    style = TextStyle.BLUE_HYPERLINK
    onAction = handle {

      val userAccount = ownerComboBox.selectionModel().getSelectedItem()
      if (userAccount == null) {
        ShowPopupWindow(wTitle = "No owner selected", wText = "You must select an owner first.", wParent = Option(Main.stage))
        //TODO: print all 

      } else {
        val projects = UdsRepository.findProjectsByOwnerId(userAccount.getId)

        val text: String = {
          if (projects.isEmpty) "No projects"
          else {
            projects.map { p =>
              if (p.getDescription().isEmpty()) s"${p.getName()}"
              else s"${p.getName()} [${p.getDescription()}]"
            }.mkString("\n")
          }
        }

        ShowPopupWindow(
          wTitle = s"Projects belonging to user '${userAccount.getLogin}'",
          wText = text,
          wParent = Option(Main.stage),
          isResizable = true)
      }
    }
  }

  /* Project name */
  val EMPTY_NAME = "Please enter a name for the project."
  val UNAVAILABLE_NAME = "This project's name is not available."
  val nameLabel = new Label("Project name* : ")
  val nameField = new TextField
  val nameWarningLabel = new Label {
    style = TextStyle.RED_ITALIC
    minHeight = 15
  }

  /* Project description */
  val descLabel = new Label("Description : ")
  val descField = new TextArea {
    prefHeight = 100
    wrapText = true
  }

  /*
   * ****** *
   * LAYOUT *
   * ****** *
   */
  protected val form: Node = new GridPane {
    hgrow = Priority.Always
    vgap = 10
    hgap = 10
    padding = Insets(20)
    prefWidth = 500

    columnConstraints ++= Seq(
      new ColumnConstraints { percentWidth = 20 },
      new ColumnConstraints { percentWidth = 80 })

    content = ScalaFxUtils.getFormattedGridContent5(Seq(
      (ownerLabel, 0, 0, 1, 1),
      (ownerComboBox, 1, 0, 1, 1),
      (ownerWarningLabel, 1, 1, 1, 1),
      (seeUserProjects, 1, 1, 1, 1),
      (nameLabel, 0, 2, 1, 1),
      (nameField, 1, 2, 1, 1),
      (nameWarningLabel, 1, 3, 2, 1),
      (descLabel, 0, 4, 1, 1),
      (descField, 1, 4, 1, 1)))
  }

  Seq(
    ownerLabel,
    seeUserProjects,
    nameLabel,
    descLabel).foreach(GridPane.setHalignment(_, HPos.Right))

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
  private def _hideWarnigs() = Seq(ownerWarningLabel, nameWarningLabel).foreach(_.visible = false)

  /** Clear the form **/
  protected def clearForm(): Unit = {
    ownerComboBox.selectionModel().select(-1)
    Seq(nameField, descField).foreach(_.text = "")
    _hideWarnigs()
  }

  /** Check if the form is correct, display warning if needed **/
  protected def checkForm(): Boolean = {

    _hideWarnigs()

    /* Owner is defined */
    val ownerIdx = ownerComboBox.selectionModel().getSelectedIndex()
    val isOwnerDefined: Boolean = ownerIdx >= 0

    if (isOwnerDefined == false) {
      ownerWarningLabel.visible = true
    }

    /* Project name is defined */
    val newProjectName = nameField.text()
    val isNameDefined = newProjectName.isEmpty == false
    var isNameAvailable = isNameDefined

    if (isNameDefined == false) {
      nameWarningLabel.text = EMPTY_NAME
      nameWarningLabel.visible = true
    } else {

      /* Project name is available for user */
      val ownerOpt: Option[PrintableUserAccount] = Option(ownerComboBox.selectionModel().getSelectedItem())
      //          if (isOwnerDefined) Option(ownerComboBox.selectionModel().getSelectedItem())
      //          else None
      val userProjects: Array[Project] = ownerOpt.map(owner => UdsRepository.findProjectsByOwnerId(owner.getId)).getOrElse(Array())
      isNameAvailable = (userProjects.exists(_.getName() == newProjectName) == false) //for this user

      if (isNameAvailable == false) {
        nameWarningLabel.text = UNAVAILABLE_NAME
        nameWarningLabel.visible = true
      }
    }

    /* Result */
    isOwnerDefined && isNameDefined && isNameAvailable
  }

  /** Save the form **/
  protected def saveForm(): Unit = {

    /* Create command line (ProlineAdmin) */
    //TODO: don't cmopute me twice (check and save in the same code ?)
    val ownerOpt: Option[PrintableUserAccount] = Option(ownerComboBox.selectionModel().getSelectedItem())
    val ownerID: Long = ownerOpt.get.getId
    val newProjectName = nameField.text()
    val newProjectDesc = descField.text()

    val cmd = s"""create_project --owner_id ${ownerID} --name "${newProjectName}"  --description "${newProjectDesc}""""

    /* Insert project in database using ProlineAdmin */
    logger.debug("Create project")
    LaunchAction(
      actionButton = ButtonsPanel.manageResourcesButton,
      //      disableNode = ResourcesTabbedWindow.tabPanel,
      actionString = Utils.mkCmd(cmd),
      action = () => {

        val udsDbContext = UdsRepository.getUdsDbContext()
        val prolineConf = SetupProline.getUpdatedConfig

        //try {

        /* Create project */
        val projectCreator = new CreateProject(udsDbContext, newProjectName, newProjectDesc, ownerID)
        projectCreator.doWork()
        val projectId = projectCreator.projectId

        /* Create project databases */
        if (projectId > 0L) {
          new CreateProjectDBs(udsDbContext, prolineConf, projectId).doWork()
        } else {
          logger.error("Invalid Project Id: " + projectId)
        }

        /*} finally {
          /* Close udsDbContext */
          logger.debug("Closing current UDS Db Context")
          try {
            udsDbContext.close()
          } catch {
            case exClose: Exception => logger.error("Error closing UDS Db Context", exClose)
          }
        }*/
      })
  }
}

/**
 * ******************************** *
 * Simplified model for UserAccount *
 * ******************************** *
 */
case class PrintableUserAccount(wrappedUser: UserAccount) {
  override def toString() = wrappedUser.getLogin()

  // Define proxy methods
  def getId() = wrappedUser.getId()
  def getLogin() = wrappedUser.getLogin()
}