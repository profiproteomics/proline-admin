package fr.proline.admin.gui.wizard.component.panel.main

import scalafx.Includes._
import scalafx.scene.control.Label
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle
import scalafx.scene.control.CheckBox
import fr.proline.admin.gui.wizard.util._
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.util.FxUtils
import scalafx.scene.control.CheckBox
import scalafx.scene.text.{ Font, FontWeight }
import scalafx.scene.layout.Priority
import scalafx.scene.layout.HBox
import scalafx.scene.control.Label
import fr.proline.admin.gui.process.config.AdminConfigFile
import fr.proline.admin.gui.process.config.AdminConfig
import fr.proline.repository.DriverType

/**
 * ItemsPanelForm contains warning label to check path's form
 *
 */

trait INotification {

  private val CORRUPTEDFILE = "The configuration file Proline Admin is corrupted. This may be due to improper existing settings. Default Proline Admin settings have been reset."
  val warningCorruptedFile: Label = new Label {
    graphic = FxUtils.newImageView(IconResource.EXCLAMATION)
    text = CORRUPTEDFILE
    style = TextStyle.RED_ITALIC
    visible = false
  }

  private val NOTVALIDPROLINESERVERFILE = "Please select a validated configuration file to set up Proline Server. "
  val errorNotValidServerFile: Label = new Label {
    graphic = FxUtils.newImageView(IconResource.EXCLAMATION)
    text = NOTVALIDPROLINESERVERFILE
    style = TextStyle.RED_ITALIC
    visible = false
  }

  private val NOTVALIDSEQREPOSFILE = "Please select a validated configuration file to set up Sequence Repository. "
  val errorNotValidSeqReposFile: Label = new Label {
    graphic = FxUtils.newImageView(IconResource.EXCLAMATION)
    text = NOTVALIDSEQREPOSFILE
    style = TextStyle.RED_ITALIC
    visible = false
  }

  private val NOTVALIDPROLINEWEBFILE = "Please select a validated configuration file to set up Proline Web. "
  val errorNotValidWebFile: Label = new Label {
    graphic = FxUtils.newImageView(IconResource.EXCLAMATION)
    text = NOTVALIDPROLINEWEBFILE
    style = TextStyle.RED_ITALIC
    visible = false
  }

  private val NOTVALIDPGDATA = "Please select a validated PostgreSQL data directory to set up PostgreSQL optimization and authorizations. Data directory should contain pg_hba.conf file and postgresql.conf file"
  val errorNotValidPgData: Label = new Label {
    graphic = FxUtils.newImageView(IconResource.EXCLAMATION)
    text = NOTVALIDPGDATA
    style = TextStyle.RED_ITALIC
    visible = false
  }

  /* set style on fields */
  def setStyleSelectedItems: Boolean

  /* get the selected items  */
  def getSelectedItems: Unit

  val postgreSQLChBox = new CheckBox("PostgreSQL Server Data Directory") {
    id = "postgresChBoxId"
    selected = true
    underline = true
    font = Font.font("SanSerif", FontWeight.Bold, 12)
    vgrow = Priority.Always
  }
  val postgreSQLLabel = new HBox {
    prefWidth = 250
    disable <== !postgreSQLChBox.selected
    children = List(new Label("Path to PostgreSQL Data Directory: "))
  }

  /* common components */

  //Proline modules  
  val prolineModulesChBox = new CheckBox("Proline Modules") {
    id = "moduleConfigId"
    underline = true
    selected = true
    vgrow = Priority.Always
    font = Font.font("SanSerif", FontWeight.Bold, 12)
    onAction = handle { getSelectedChildren }
  }

  //Sequence Repository  
  val seqReposChBox = new CheckBox("Sequence Repository Configuration File") {
    id = "seqReposChBoxId"
    selected = true
    underline = true
    vgrow = Priority.Always
    onAction = handle { getSelectedParent }
  }
  val seqReposLabel = new HBox {
    prefWidth = 250
    disable <== !seqReposChBox.selected
    children = List(new Label("Path to SeqRepo Root ( File application.conf ): "))
  }
  //Proline web  
  val prolineWebChBox = new CheckBox("Proline Web Configuration File") {
    id = "prolineWebChBoxId"
    selected = true
    underline = true
    vgrow = Priority.Always
    onAction = handle { getSelectedParent }
  }
  val prolineWebLabel = new HBox {
    prefWidth = 250
    disable <== !prolineWebChBox.selected
    children = List(new Label("Path to Web Root ( File application.conf ): "))
  }

  // Proline server  
  val prolineServerChBox = new CheckBox("Proline Server Configuration File") {
    id = "serverChBoxId"
    selected = true
    underline = true
    vgrow = Priority.Always
    font = Font.font("SanSerif", FontWeight.Bold, 12)
  }
  val prolineServerLabel = new HBox {
    prefWidth = 250
    disable <== !prolineServerChBox.selected
    children = List(new Label("Path to Server Root ( File application.conf ): "))
  }
  /* selected checkbox */

  def getSelectedChildren {
    if (prolineModulesChBox.isSelected) {
      prolineWebChBox.selected = true
      seqReposChBox.selected = true
    } else {
      prolineWebChBox.selected = false
      seqReposChBox.selected = false
    }
  }

  def getSelectedParent {
    if (prolineWebChBox.isSelected || seqReposChBox.isSelected)
      prolineModulesChBox.selected = true
    else
      prolineModulesChBox.selected = false
  }
  //reset default setting when the configuration file is corrupted
  def resetAdminConfig(path: String) = {
    val defaultConfig = AdminConfig(path,
      Some(""),
      Some(""),
      Some(""),
      Some(""),
      Some(DriverType.POSTGRESQL),
      Some("<path/to/proline/data>"),
      Some("<db_user>"),
      Some("<db_password>"),
      Some("<db_host>"),
      Some(5432))
    new AdminConfigFile(path).write(defaultConfig)
  }
}

/**
 * TabForm contains tab form of each item
 *
 */

trait ITabForm {

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
