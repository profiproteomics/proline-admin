package fr.proline.admin.gui.wizard.component.items.form
import scalafx.scene.control.Label
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle
import scalafx.scene.control.CheckBox
import scalafx.scene.control.TextField
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import fr.profi.util.scala.ScalaUtils
import fr.proline.admin.gui.wizard.util._
import java.io.File
/**
 * TabForm contains tab form of each item
 *
 */

trait TabForm {

  // warning label 
  val warningDatalabel: Label = new Label {
    text = "The following field(s) cannot be empty."
    style = TextStyle.RED_ITALIC
    visible = false
  }

  // check the form of the fields in each tab 
  def checkForm: Boolean

  // get the state of form to show it in the summary panel 
  def getInfos: String
}

/**
 * ItemsPanelForm contains warning label to check path's form
 *
 */

trait ItemsPanelForm {

  private val CORRUPTEDFILE = "The configuration file Proline Admin is corrupted. This may be due to improper existing settings. Default settings have been reset."
  val warningCorruptedFile: Label = new Label {
    text = CORRUPTEDFILE
    style = TextStyle.RED_ITALIC
    visible = false
  }
  private val NOTVALIDPROLINESERVERFILE = "Please select a valid configuration file to set up Proline Server. "
  val errorNotValidServerFile: Label = new Label {
    text = NOTVALIDPROLINESERVERFILE
    style = TextStyle.RED_ITALIC
    visible = false
  }
  private val NOTVALIDSEQREPOSFILE = "Please select a valid configuration file to set up Sequence Repository. "
  val errorNotValidSeqReposFile: Label = new Label {
    text = NOTVALIDSEQREPOSFILE
    style = TextStyle.RED_ITALIC
    visible = false
  }
  private val NOTVALIDPROLINEWEBFILE = "Please select a valid configuration file to set up Proline Web. "
  val errorNotValidWebFile: Label = new Label {
    text = NOTVALIDPROLINEWEBFILE
    style = TextStyle.RED_ITALIC
    visible = false
  }
  private val NOTVALIDPGDATA = "Please select your PostgreSQL data directory to set up PostgreSQL optimization and authorizations. "
  val errorNotValidPgData: Label = new Label {
    text = NOTVALIDPGDATA
    style = TextStyle.RED_ITALIC
    visible = false
  }
  
  /* set style on fields */
  def setStyleSelectedItems: Boolean

  /* get the selected items  */
  def getSelectedItems: Unit

  /* check valid data directory contains:postgresql.conf and pg_hba.conf */
  def validDataDirectory(path: String): Boolean = {
    var validDir = false
    if (new File(path).exists) {
      if (new File(path).listFiles().filter { file => (file.getName.equals("pg_hba.conf") || file.getName.equals("postgresql.conf")) }.size == 2) validDir = true
    }
    validDir
  }
}