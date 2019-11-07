package fr.proline.admin.gui.component.configuration.tab

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.control.Button
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import scalafx.scene.control.Tab
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox
import scalafx.stage.Stage
import fr.proline.admin.gui.component.configuration.file.IConfigFilesPanel
import fr.proline.admin.gui.component.configuration.form.IConfigFilesForm
import fr.profi.util.scalafx.CustomScrollPane
import fr.profi.util.scalafx.TitledBorderPane
import scalafx.beans.property.DoubleProperty
import scalafx.scene.Node


/**
 * **************************************************** *
 * Abstraction model for ConfigurationTabbedWindow tabs *
 * **************************************************** *
 */
trait IConfigTab extends Tab {

  protected var _form: IConfigFilesForm = null
  
  def name: String

  /* Forbid to close these tabs */
  closable = false
  text = name
  
  /** Get the form contained within **/
  def getForm(): IConfigFilesForm = _form

  /** Update the tab content **/
  // Implementations will be of type: 
  // val newContent = new <IConfigTabContent>())
  // _form = newContent.getForm()
  // content = newContent
  def setContent(): Unit
}

/**
 * ************************************************************* *
 * Abstraction model for ConfigurationTabbedWindow tabs content, *
 * to wrap them in a ScrollPane																	 *
 * ************************************************************* *
 */
abstract class AbstractConfigTabContent extends CustomScrollPane(hBarPolicy = ScrollBarPolicy.AS_NEEDED) {

  padding = Insets(10, 0, 5, 0)

  /* Initialize form to null */
  protected var form: IConfigFilesForm = null
  
  /** Get this tab form **/
  def getForm(): IConfigFilesForm = form
}

/**
 * ******************************************************** *
 * Abstraction model for ConfigurationTabbedWindow tabs for *
 * configuration files editing															*
 * ******************************************************** *
 */
trait IConfigTabContent extends AbstractConfigTabContent {
  
  /*
   * ********** *
   * COMPONENTS *
   * ********** *
   */

  protected implicit val parentStage: Stage

  /* File browsing */
  protected def configFilesPanelTitle: String
  protected def configFilesPanel: IConfigFilesPanel

  protected val applyWorkingFileButton = new Button("Apply") {
    onAction = handle { onApplyWorkingFilesPressed() }
  }
  protected val cancelWorkingFileButton = new Button("Cancel") {
    onAction = handle { onCancelWorkingFilesPressed() }
  }

  // Form is defined by AbstractConfigTabContent

  /*
   * ****** *
   * LAYOUT *
   * ****** *
   */
  val buttons = new HBox {
    alignment = Pos.BottomCenter
    alignmentInParent = Pos.BottomCenter
    spacing = 5
    children = Seq(applyWorkingFileButton, cancelWorkingFileButton)
  }

  setContentNode(getNodeForScrollPane())
  bindSizeToParent(parentStage) 

  /*
   * ******** *
   * FEATURES *
   * ******** *
   */
  
  // Must contain the lines:
//    this.setContentNode(getNodeForScrollPane())
//  parentStage.onShown = handle {
//    this.bindSizeToParent(parentStage)
//  }
  protected def init(): Unit
  
  protected def onCancelWorkingFilesPressed(): Unit
  
  def onApplyWorkingFilesPressed(): Unit //will be called by parent IConfigTab

  /** Compute content to display within the scrollpane **/
  protected def getNodeForScrollPane(additionalNode: Node = null) = new VBox {
    spacing = 30
    children = if (form == null) Seq(_getWorkingFilesPanel(additionalNode)) else Seq(_getWorkingFilesPanel(additionalNode), form)
  }

  private def _getWorkingFilesPanel(additionalNode: Node = null) = {
    new TitledBorderPane(
      title = configFilesPanelTitle,
      contentNode = new VBox {
        minHeight = 90
        spacing = 5

        children = if (additionalNode == null) {
          Seq(configFilesPanel, buttons)
        } else Seq(configFilesPanel, additionalNode, buttons)

      }
    )
  }

  /** Add a node working files panel and its buttons **/
  protected def setConfigFilePanelEnhancedContent(additionalNode: Node) {
    setContentNode(getNodeForScrollPane(additionalNode))
  }

}