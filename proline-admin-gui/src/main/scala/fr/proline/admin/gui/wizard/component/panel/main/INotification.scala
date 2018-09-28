package fr.proline.admin.gui.wizard.component.panel.main

import scalafx.Includes._
import scalafx.scene.control.Label

import scalafx.scene.control.CheckBox

import scalafx.scene.control.CheckBox
import scalafx.scene.text.{ Font, FontWeight }
import scalafx.scene.layout.Priority
import scalafx.scene.layout.HBox
import scalafx.scene.control.Label
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle
import fr.proline.admin.gui.process.config.AdminConfigFile
import fr.proline.admin.gui.process.config.AdminConfig
import fr.proline.repository.DriverType
import fr.proline.admin.gui.wizard.util._
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.util.FxUtils

/**
 * ItemsPanelForm contains warning label to check path's form
 *
 */

trait INotification {

  private val CORRUPTEDFILE = "The configuration file Proline Admin is corrupted. This may be due to improper existing settings. Default Proline Admin settings will be reset."
  val corruptedFileLabel: Label = new Label {
    graphic = FxUtils.newImageView(IconResource.EXCLAMATION)
    text = CORRUPTEDFILE
    style = TextStyle.RED_ITALIC
    visible = false
  }

  private val INVALIDPROLINESERVERFILE = "Please select a validated configuration file to set up Proline server."
  val invalidServerFileLabel: Label = new Label {
    graphic = FxUtils.newImageView(IconResource.EXCLAMATION)
    text = INVALIDPROLINESERVERFILE
    style = TextStyle.RED_ITALIC
    visible = false
  }

  private val INVALIDSEQREPOSFILE = "Please select a validated configuration file to set up Sequence repository."
  val invalidSeqReposFileLabel: Label = new Label {
    graphic = FxUtils.newImageView(IconResource.EXCLAMATION)
    text = INVALIDSEQREPOSFILE
    style = TextStyle.RED_ITALIC
    visible = false
  }

  private val INVALIDPROLINEWEBFILE = "Please select a validated configuration file to set up Proline web."
  val invalidPwxFileLabel: Label = new Label {
    graphic = FxUtils.newImageView(IconResource.EXCLAMATION)
    text = INVALIDPROLINEWEBFILE
    style = TextStyle.RED_ITALIC
    visible = false
  }

  private val INVALIDPGDATA = "Please select a validated PostgreSQL data directory to set up PostgreSQL optimization and authorizations. Data directory should contain pg_hba.conf file and postgresql.conf file."
  val invalidPgDataLabel: Label = new Label {
    graphic = FxUtils.newImageView(IconResource.EXCLAMATION)
    text = INVALIDPGDATA
    style = TextStyle.RED_ITALIC
    visible = false
  }

  val postgreSQLChBox = new CheckBox("PostgreSQL Server Data Directory") {
    id = "postgresChBoxId"
    underline = true
    font = Font.font("SanSerif", FontWeight.Bold, 12)
    vgrow = Priority.Always
  }
  val postgreSQLPanel = new HBox {
    disable <== !postgreSQLChBox.selected
    children = List(new Label("Select the PostgreSQL Data Directory "))
  }

  /* common components */

  //Proline modules  
  val prolineModulesChBox = new CheckBox("Proline Modules") {
    id = "moduleConfigId"
    underline = true
    vgrow = Priority.Always
    font = Font.font("SanSerif", FontWeight.Bold, 12)
    onAction = handle { bindToChildren }
  }

  //Sequence Repository  
  val seqReposChBox = new CheckBox("Sequence Repository Configuration File") {
    id = "seqReposChBoxId"
    underline = true
    vgrow = Priority.Always
    onAction = handle { bindToParent }
  }
  val seqReposPanel = new HBox {

    disable <== !seqReposChBox.selected
    children = List(new Label("Select the configuration file application.conf\nIt should be located under </config>"))
  }

  //Proline web  
  val prolineWebChBox = new CheckBox("Proline Web Configuration File") {
    id = "prolineWebChBoxId"
    underline = true
    vgrow = Priority.Always
    onAction = handle { bindToParent }
  }

  val pwxPanel = new HBox {
    disable <== !prolineWebChBox.selected
    children = List(new Label("Select the configuration file application.conf\nIt should be located under </conf>"))
  }

  // Proline server  
  val prolineServerChBox = new CheckBox("Proline Server Configuration File") {
    id = "serverChBoxId"
    underline = true
    vgrow = Priority.Always
    font = Font.font("SanSerif", FontWeight.Bold, 12)
  }
  val prolineServerPanel = new HBox {
    disable <== !prolineServerChBox.selected
    children = List(new Label("Select the configuration file application.conf\nIt should be located under </config>"))
  }

  /* label size*/
  Seq(prolineServerPanel, seqReposPanel, pwxPanel, postgreSQLPanel).foreach { panel =>
    panel.prefWidth = 320
    panel.prefHeight = 40
  }
  /* selected checkbox */

  def bindToChildren {
    prolineWebChBox.selected = prolineModulesChBox.isSelected
    seqReposChBox.selected = prolineModulesChBox.isSelected
  }

  def bindToParent {
    prolineModulesChBox.selected = (prolineWebChBox.isSelected || seqReposChBox.isSelected)
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
  val emptyFieldErrorLabel: Label = new Label {
    text = "The following field(s) cannot be empty."
    style = TextStyle.RED_ITALIC
    visible = false
  }

  emptyFieldErrorLabel.managed <== emptyFieldErrorLabel.visible
  // check the form of the fields in each tab 
  def checkForm: Boolean

  // get the state of form to show it in the summary panel 
  def getInfos: String
}
