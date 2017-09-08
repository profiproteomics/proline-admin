package fr.proline.admin.gui.component.resource

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Node
import scalafx.scene.control.Button
import scalafx.scene.layout.HBox
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.VBox
import fr.profi.util.scalafx.ITitledBorderPane
import scalafx.scene.control.Tab
import scalafx.scene.control.TableView
import scalafx.scene.layout.ColumnConstraints.sfxColumnConstraints2jfx
import scalafx.scene.control.TableColumn.sfxTableColumn2jfx
import fr.proline.admin.gui.component.resource.implicits.ProjectView
import scalafx.scene.layout.Priority
import scalafx.scene.control.SelectionMode
import scalafx.collections.ObservableBuffer


/**
 * ********************************************** *
 * Abstraction model for resource management tabs *
 * ********************************************** *
 */
trait IResourceManagementTab extends Tab {
  
  closable = false
  
  protected val CONTENT_PREF_HEIGHT = 512
  protected def newEntryPanel: INewEntryPanel
}

/**
 * *********************************************************************** *
 * Abstraction model for resource management tabs top panel: add new entry *
 * *********************************************************************** *
 */
trait INewEntryPanel extends ITitledBorderPane {
  
  /*
   * ********** *
   * COMPONENTS *
   * ********** *
   */
  protected val form: Node
  
  private val applyButton = new Button("Add") {
    onAction = handle {
      if (checkForm()) saveForm()
    }
  }

  private val cancelButton = new Button("Cancel") {
    onAction = handle { clearForm() }
  }

  /*
   * ****** *
   * LAYOUT *
   * ****** *
   */

  private val buttons = new HBox {
    children = Seq(applyButton, cancelButton)
    alignment = Pos.BottomRight
    spacing = 5
  }

  protected lazy val contentNode = new VBox {
    children = Seq(form, buttons)
    spacing = 5
  }

  /*
   * ******** *
   * FEATURES *
   * ******** *
   */

  /** Clear the form **/
  protected def clearForm(): Unit

  /** Check if the form is correct **/
  protected def checkForm(): Boolean

  /** Save the form **/
  protected def saveForm(): Unit
}

/**
 * ************************************************************************* *
 * Abstraction model for resource management tabs top panel: see all entries *
 * ************************************************************************* *
 */
abstract class AbstractResourceTableView[T] extends TableView[T]{
  
  
  /* Table properties */
  hgrow = Priority.Always
  vgrow = Priority.Always
  editable = true
  tableMenuButtonVisible = true
  selectionModel().selectionMode = SelectionMode.MULTIPLE
    //selectionModel().cellSelectionEnabled = true
  
   /* Fill table */
  protected def tableLines: ObservableBuffer[T]
  protected def tableColumns: List[javafx.scene.control.TableColumn[T, _]]

  /** Initialize table content **/
  protected def init() {
    items = tableLines
    columns ++= tableColumns
  }
}