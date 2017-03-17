package fr.proline.admin.gui.component.configuration

import scala.collection.mutable.ArrayBuffer

import scalafx.Includes._

import fr.proline.admin.gui.component.wizard._
import fr.proline.admin.gui.component.configuration.tab.ConfigFilesSelectionTab
import fr.proline.admin.gui.component.configuration.tab.IConfigTab
import fr.proline.admin.gui.util.AbstractTabbedWindow
import fr.proline.admin.gui.util.ShowPopupWindow

/**
 * ********************************************************************************* *
 * Tabbed window for the configuration of PostgreSQL, Proline datastore              *
 * ********************************************************************************* *
 */
class ConfigurationTabbedWindowWizard extends AbstractTabbedWindow {

  /* Stage's properties */
  title = s"Configuration editor"

  /*
   * ********** *
   * COMPONENTS *
   * ********** *
   */

  // Tabs are classes so this Stage can be implicitly known as parent
  val iTabs: Seq[IConfigTab] = Seq(
    new StepTwoPgConfig(),
    new StepTwoPostgresConfig() //TODO: enable me: new SeqRepoConfigTab()
    )

  tabPanel.tabs = iTabs

  /*
   * ************** *
   * INITIALIZATION *
   * ************** *
   */

  /* Initialize first tab content on creation */
  iTabs.head.asInstanceOf[IConfigTab].setContent()

  /* Then build tabs content on tab change */
  tabPanel.selectionModel().selectedIndex.onChange { (a, b, newlySelectedIndex) =>
    iTabs(newlySelectedIndex.intValue).setContent()
  }

  /** Trigger 'Apply' action of every tab ('OK' pressed) **/
  protected def runOnOkPressed() {

    val tabsInError = new ArrayBuffer[String](iTabs.length)

    /* For each tab, save form if defined and correct */
    for (
      tab <- iTabs;
      form = tab.getForm();
      if (form != null)
    ) {

      if (form.checkForm()) {
        form.saveForm()
      } else {
        tabsInError += tab.name
      }
    }

    /* If all checks passed, close window */
    if (tabsInError.isEmpty) {
      thisWindow.close()
    } /* Otherwise show a popup */ else {
      ShowPopupWindow(
        wTitle = "Errors",
        wText = "There are some errors in the following tab(s):\n" + tabsInError.mkString("\n - "))
    }
  }
}