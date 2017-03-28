package src.main.scala.fr.proline.admin.gui.component.resource
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
import fr.proline.admin.gui.component.resource.implicits.ExternalDbView
import fr.proline.admin.gui.process.LaunchAction
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.util.ShowPopupWindow
import fr.proline.admin.gui.util.Utils
import fr.proline.core.orm.uds.ExternalDb
import fr.proline.admin.gui.component.resource._
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.EnhancedTableView
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle
/**
 *
 * content of databases tab
 */
class DataBasesTab() extends IResourceManagementTab {

  text = "Databases"
  protected val newEntryPanel: INewEntryPanel = new NewProjectPanel()
  val table = new DataBasesTable()
  val tabContent = new VBox {
    prefHeight = CONTENT_PREF_HEIGHT
    spacing = 5
    padding = Insets(20)
    content = table
  }
  content = tabContent
}
/**
 * table to see all the parameters of all dataBases
 */
class DataBasesTable() extends AbstractResourceTableView[ExternalDbView] {

  val externalDbViews = UdsRepository.getAllDataBases().toBuffer[ExternalDb].sortBy(_.getId).map(new ExternalDbView(_))
  protected lazy val tableLines = ObservableBuffer(externalDbViews)
  /* ExternalDb ID */

  val idCol = new TableColumn[ExternalDbView, Long]("ID") {
    cellValueFactory = { _.value.dbId }
        cellFactory = { _ =>
      new TableCell[ExternalDbView, Long] {
        style = "-fx-alignment: CENTER;"
        item.onChange { (_, _, newValue) => text = newValue.toString }
      }
    }
  }
  /* ExternalDb name */

  val dbNameCol = new TableColumn[ExternalDbView, String]("name") {
    cellValueFactory = { _.value.dbName }
        cellFactory = { _ =>
      new TableCell[ExternalDbView, String] {
        style = "-fx-alignment: CENTER;"
        item.onChange { (_, _, newValue) => text = newValue }
      }
    }
  }
  /* db password */

  val dbPasswordCol = new TableColumn[ExternalDbView, String]("Password") {
    cellValueFactory = { _.value.dbPassword }
        cellFactory = { _ =>
      new TableCell[ExternalDbView, String] {
        style = "-fx-alignment: CENTER;"
        item.onChange { (_, _, newValue) => text = newValue }
      }
    }
  }
  /* db Driver */

  val dbUserCol = new TableColumn[ExternalDbView, String]("User") {
    cellValueFactory = { _.value.dbUser }
        cellFactory = { _ =>
      new TableCell[ExternalDbView, String] {
        style = "-fx-alignment: CENTER;"
        item.onChange { (_, _, newValue) => text = newValue }
      }
    }
  }
  /* db Version */

  val dbVersionCol = new TableColumn[ExternalDbView, String]("Version") {
    cellValueFactory = { _.value.dbVersion }
        cellFactory = { _ =>
      new TableCell[ExternalDbView, String] {
        style = "-fx-alignment: CENTER;"
        item.onChange { (_, _, newValue) => text = newValue }
      }
    }
  }
  /* db port */

  val dbPortCol = new TableColumn[ExternalDbView, String]("Port") {
    cellValueFactory = { _.value.dbPort }
        cellFactory = { _ =>
      new TableCell[ExternalDbView, String] {
        style = "-fx-alignment: CENTER;"
        item.onChange { (_, _, newValue) => text = newValue }
      }
    }
  }
  /* db host */

  val dbHostCol = new TableColumn[ExternalDbView, String]("Host") {
    cellValueFactory = { _.value.dbHost }
        cellFactory = { _ =>
      new TableCell[ExternalDbView, String] {
        style = "-fx-alignment: CENTER;"
        item.onChange { (_, _, newValue) => text = newValue }
      }
    }
  }

  /* action for each row */
  val actionCol = new TableColumn[ExternalDbView, String]("Action") {
  }
  actionCol.setEditable(true)
  protected lazy val tableColumns: List[javafx.scene.control.TableColumn[ExternalDbView, _]] = List(idCol, dbNameCol, dbVersionCol, dbPortCol, dbHostCol)
  /* Set columns width */
  this.applyPercentWidth(List(
    (idCol, 15),
    (dbNameCol, 25),
    (dbVersionCol, 20),
    (dbPortCol, 20),
    (dbHostCol, 20)))

  /* Initialize table content */
  this.init()
}

 