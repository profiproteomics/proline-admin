package fr.proline.admin.gui

import com.typesafe.scalalogging.LazyLogging

import java.io.File

import javafx.application.Application

import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.VBox
import scalafx.stage.Stage
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.wizard.util.Module
import fr.proline.admin.gui.util.FxUtils
import javafx.stage.Screen
import fr.proline.admin.gui.wizard.component.panel.bottom.MonitorBottomsPanel
import fr.proline.admin.gui.wizard.component.panel.main.MonitorPane
import fr.proline.admin.gui.wizard.util.WindowSize

/**
 * Graphical interface for Proline Admin Monitor .
 */

object Monitor extends LazyLogging {

  /* Panels */
  var itemsPanel: VBox = _
  var buttonsPanel: VBox = _

  /* Primary stage's root */
  lazy val root = new VBox {
    id = "root"
    children = new VBox {
      vgrow = Priority.Always
      padding = Insets(10)
      spacing = 20
      children = List(itemsPanel, buttonsPanel)
    }
  }

  var stage: scalafx.stage.Stage = null
  /** Launch application and display main window. */
  def main(args: Array[String]) = {
    Application.launch(classOf[Monitor])
  }

}
class Monitor extends Application {

  def start(stage: javafx.stage.Stage): Unit = {

    require(Monitor.stage == null, "stage is already instantiated")
    Monitor.itemsPanel = MonitorPane
    Monitor.buttonsPanel = MonitorBottomsPanel
    Monitor.stage = new Stage(stage) {
      scene = new Scene(Monitor.root)
      width = 1024
      height = 780
      title = s"${Module.name} ${Module.version}"
    }

    /* Build and show stage (in any case) */

    Monitor.stage.setWidth(WindowSize.prefWitdh)
    Monitor.stage.setHeight(WindowSize.prefHeight)
    Monitor.stage.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
    Monitor.stage.scene.value.getStylesheets.add("/css/Style.css")
    Monitor.stage.show()
  }

  /** Close UDSdb context on application close **/
  override def stop() {
    super.stop()
    if ((UdsRepository.getUdsDbContext() != null) && !(UdsRepository.getUdsDbContext().isClosed))
      UdsRepository.getUdsDbContext().close
  }

}