package fr.proline.admin.gui.wizard.monitor.component

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.scene.Scene
import scalafx.stage.Modality
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.layout.{ VBox, HBox }
import scalafx.scene.control.Label
import scalafx.scene.control.ComboBox
import scalafx.scene.control.TextField
import scalafx.scene.control.Button
import scalafx.util.StringConverter
import scalafx.scene.layout.Priority

import java.io.File
import scalafx.scene.input.KeyEvent
import fr.profi.util.scalafx.ScalaFxUtils
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.core.orm.uds.UserAccount
import fr.proline.admin.gui.wizard.component.panel.main.MonitorPane
import fr.proline.admin.gui.wizard.service.ArchiveProject
import fr.proline.admin.gui.wizard.util.ProgressBarPopup
import javafx.event.EventHandler
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle

/**
 * builds archive project pane
 * @author aromdhani
 *
 */

class ArchiveProjectPane(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) extends Stage with LazyLogging {
  val archiveProjectPane = this
  title = wTitle
  minWidth_=(400)
  minHeight_=(250)
  width_=(600)
  height_=(300)
  initModality(Modality.WINDOW_MODAL)
  if (wParent.isDefined) initOwner(wParent.get)
  archiveProjectPane.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
  // Component 
  //warning label 
  val warningSelectdProjectLabel = new Label {
    text = "There is no selected project. Please select a project to archive from the table."
    style = TextStyle.RED_ITALIC
    visible = false
  }
  val warningBinPathLabel = new Label {
    text = "The data directory of PostgreSQL not found."
    style = TextStyle.RED_ITALIC
    visible = false
  }
  val warningLocationPathLabel = new Label {
    text = " The Location archive path must not be empty."
    style = TextStyle.RED_ITALIC
    visible = false
  }

  val warningBox = new VBox {
    spacing = 5
    children = Seq(warningSelectdProjectLabel, warningBinPathLabel, warningLocationPathLabel)
  }
  val projectPathLabel = new Label("Select project archive location")
  val projectPathTextField = new TextField()
  val archiveButton = new Button {
    text = "Archive"
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction = handle {
      archiveProject()
    }
  }
  val cancelButton = new Button {
    text = "Cancel"
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    onAction = handle {
      cancel()
    }
  }
  val browseButton = new Button {
    text = "Browse..."
    graphic = FxUtils.newImageView(IconResource.LOAD)
    onAction = handle {
      _browseProjectSaveDir()
    }
  }

  //Style 
  Seq(projectPathLabel, projectPathTextField).foreach { component =>
    component.prefWidth = 200
    component.hgrow_=(Priority.ALWAYS)
  }

  Seq(archiveButton,
    cancelButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }

  //Layout
  val browseProjectPanel = new HBox {
    spacing = 30
    children = Seq(projectPathLabel, projectPathTextField, browseButton)
  }
  val loadProjectPanel = new VBox {
    spacing = 10
    children = Seq(warningBox, browseProjectPanel)
  }
  scene = new Scene {
    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(archiveProjectPane, ke) }
    root = new VBox {
      alignment = Pos.Center
      spacing = 15
      padding = Insets(10)
      children = List(
        loadProjectPanel,
        new HBox {
          alignment = Pos.Center
          padding = Insets(10)
          spacing = 30
          children = Seq(archiveButton,
            cancelButton)
        })
    }
  }

  /** browse a project save location */
  def _browseProjectSaveDir() {
    val file = FxUtils.browseDirectory(
      dcTitle = "Select save project location",
      dcInitialDir = projectPathTextField.text(),
      dcInitOwner = archiveProjectPane)
    try {
      val newPath = file.getPath()
      if (file != null) {
        projectPathTextField.text = newPath
      }
    } catch {
      case t: Throwable => logger.error("Error while trying to select a project archive location: ", t.printStackTrace())
    }
  }
  /** cancel and close load project popup */
  def cancel(): Unit = {
    archiveProjectPane.close()
  }
  /** archive project */
  def archiveProject(): Unit = {
    try {
      if (ProjectPane.selectedProject.isDefined) {
        warningSelectdProjectLabel.visible_=(false)
        val projectId = ProjectPane.selectedProject.get.id.apply()
        val archiveLocation = projectPathTextField.text.apply()
        if (new File(archiveLocation).exists()) {
          warningLocationPathLabel.visible_=(false)
          val dataDir = MonitorPane.serverPgsqlDataDir
          if (dataDir.isDefined) {
            warningBinPathLabel.visible_=(false)
            val binDir = new File(dataDir.get).getParent + File.separator + "bin"
            val archiveTask = new ArchiveProject(projectId, binDir, archiveLocation)
            ProgressBarPopup("Archive Project", "Archiving project in progress ... ", Some(archiveProjectPane), true, archiveTask.Worker)
          } else {
            warningBinPathLabel.visible_=(true)
          }
        } else {
          warningLocationPathLabel.visible_=(true)
        }
      } else {
        warningSelectdProjectLabel.visible_=(true)
      }
    } catch {
      case t: Throwable => {
        logger.error("Error invalid parameters to archive a project ", t.printStackTrace())
      }
    }
  }
}
object ArchiveProjectPane {
  def apply(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) { new ArchiveProjectPane(wTitle, wParent, isResizable).showAndWait() }
}