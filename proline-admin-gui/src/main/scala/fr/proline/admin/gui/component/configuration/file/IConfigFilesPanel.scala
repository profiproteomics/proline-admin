package fr.proline.admin.gui.component.configuration.file

import java.io.File

import scalafx.scene.Node
import scalafx.scene.control.Label
import scalafx.scene.control.TextField

import fr.profi.util.scala.ScalaUtils

/**
 * ************************************** *
 * Trait for each tab 'ConfigFiles' panel *
 * ************************************** *
 */
trait IConfigFilesPanel extends Node {

  def checkForm(allowEmptyPaths: Boolean = true): Boolean
  def saveForm(): Unit //Boolean
  
   /* Check utilities */

  private val NO_FOLDER_SELECTED = "Please select a folder."
  private val UNEXISTING_FOLDER = "This folder doesn't exist."
  private val NOT_A_FOLDER = "This path leads to a file. The target should be a folder (directory)."

  private val NO_FILE_SELECTED = "Please select a file."
  private val UNEXISTING_FILE = "This file doesn't exist."
  private val NOT_A_FILE = "This path leads to a folder. The target should be a file."
  private val WRONG_FILE_EXTENSION = "The file extension doesn't meet requierements: "

  /** Check a file path in a TextField, return a boolean. Display or hide warnings depending on path conformity **/
  protected def checkFileFromField(textField: TextField, warningLabel: Label, allowEmpty: Boolean, expectedExtension: Option[String] = None): Boolean = {
    checkFileOrFolderFromField(textField, warningLabel, true, allowEmpty, expectedExtension)
  }

  /** Check a folder path in a TextField, return a boolean. Display or hide warnings depending on path conformity **/
  protected def checkFolderFromField(textField: TextField, warningLabel: Label, allowEmpty: Boolean, expectedExtension: Option[String] = None): Boolean = {
    checkFileOrFolderFromField(textField, warningLabel, false, allowEmpty, expectedExtension)
  } 

  /** Check a file or folder path in a TextField, return a boolean. Display or hide warnings depending on path conformity **/
  protected def checkFileOrFolderFromField(
    textField: TextField,
    warningLabel: Label,
    fileExpected: Boolean,
    allowEmpty: Boolean,
    expectedExtension: Option[String] = None
  ): Boolean = {

    val folderExpected = !fileExpected
    val path = textField.text()
    var pathIsOk: Boolean = false

    /* No file/folder is specified */
    if (path.isEmpty()) {
      
      if (allowEmpty) {
        warningLabel.visible = false
        return true //return directly if path is allowed to be empty (no more test needed)
      } else {
        warningLabel.text = if (fileExpected) NO_FILE_SELECTED else NO_FOLDER_SELECTED
        warningLabel.visible = true
      }

    } else {
      val fileOrFolder = new File(path)

      /* Given path doesn't exist */
      if (fileOrFolder.exists() == false) {
        warningLabel.text = if (fileExpected) UNEXISTING_FILE else UNEXISTING_FOLDER
        warningLabel.visible = true

      } else {

        /* Path exists but target isn't the expected type */
        if (fileOrFolder.isDirectory()) {

          if (fileExpected) warningLabel.text = NOT_A_FILE
          warningLabel.visible = fileExpected
          pathIsOk = folderExpected

        } else {

          if (folderExpected) warningLabel.text = NOT_A_FOLDER
          warningLabel.visible = folderExpected
          pathIsOk = fileExpected
          
          /* Wrong file extension */
          if (fileExpected){
            if (expectedExtension.isDefined) {
              val ext = expectedExtension.get
              ScalaUtils.getFileExtension(path) matches s"""(?i)$ext"""
              warningLabel.text = WRONG_FILE_EXTENSION + "ext"
              warningLabel.visible = true
              pathIsOk = false
            }
          }
        }
      }
    }
    
    if (warningLabel.visible()) {
      warningLabel.style = fr.profi.util.scalafx.ScalaFxUtils.TextStyle.RED_ITALIC
    }

    pathIsOk
  }
}