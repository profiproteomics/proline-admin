package fr.proline.admin.gui.install.view

import com.typesafe.scalalogging.LazyLogging
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.profi.util.scalafx.{ScalaFxUtils, TitledBorderPane}
import fr.proline.admin.gui.{IconResource, Install}
import fr.proline.admin.gui.install.model._
import fr.proline.admin.gui.task.TaskRunner
import fr.proline.admin.gui.util.{FxUtils, GetConfirmation}
import javafx.scene.control.ContentDisplay
import scalafx.Includes._
import scalafx.beans.property.BooleanProperty
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Node
import scalafx.scene.control._
import scalafx.scene.layout.{HBox, Priority, StackPane, VBox}
import scalafx.scene.text.{Font, FontWeight}

import java.io.File

/**
 * Creates and displays the home panel of Proline-Admin-GUI Install.
 * @author aromdhani
 *
 */

class HomePanel(model: HomePanelViewModel) extends VBox with LazyLogging {

  private val homePanel = this
  private val isCorruptedFile = BooleanProperty(false)
  private var itemsList: List[Node] = _
  private var nodeIndex = 0
  private var currNode: Node = _

  // Load initial configurations
  private val adminConfigOpt = if (model.adminConfigOpt.isDefined) { model.adminConfigOpt }
  else {
    // Set default values when Proline-Admin configuration file is corrupted
    isCorruptedFile.value = true
    model.defaultAdminConfig()
  }

  // Set initial configuration files path
  Install.serverConfPath = adminConfigOpt.map(_.serverConfigFilePath.getOrElse("")).get
  Install.seqReposConfPath = adminConfigOpt.map(_.seqRepoConfigFilePath.getOrElse("")).get
  Install.pwxConfPath = adminConfigOpt.map(_.pwxConfigFilePath.getOrElse("")).get

  /*
   * ********************************
   *  Error and warning notification*
   * ********************************
   */

  private val corruptedFileErrorLabel = new Label {
    graphic = FxUtils.newImageView(IconResource.WARNING)
    text = "The configuration file Proline Admin is corrupted. This may be due to improper existing settings. Default settings have been reset."
    style = TextStyle.ORANGE_ITALIC
    visible = isCorruptedFile.value
    managed <== visible
  }
  private val serverFileErrorLabel = new Label {
    graphic = FxUtils.newImageView(IconResource.EXCLAMATION)
    text = "Please select a valid configuration file to set up Proline server."
    style = TextStyle.RED_ITALIC
    visible = false
    managed <== visible
  }
  private val seqReposFileErrorLabel = new Label {
    graphic = FxUtils.newImageView(IconResource.EXCLAMATION)
    text = "Please select a valid configuration file to set up Sequence repository."
    style = TextStyle.RED_ITALIC
    visible = false
    managed <== visible
  }
//  val pwxFileErrorLabel = new Label {
//    graphic = FxUtils.newImageView(IconResource.EXCLAMATION)
//    text = "Please select a valid configuration file to set up Proline web."
//    style = TextStyle.RED_ITALIC
//    visible = false
//    managed <== visible
//  }


  /*
   * ***********************
   *  Home panel components*
   * ***********************
   */

  // Help icon
  private val headerHelpIcon = new Hyperlink {
    graphic = FxUtils.newImageView(IconResource.HELP)
    alignmentInParent = Pos.BaselineRight
    onAction = _ => {
      model.openAdminGuide()
    }
  }

  // Proline server
  private val serverChBox = new CheckBox("Proline Server Configuration File") {
    underline = true
    font = Font.font("SanSerif", FontWeight.Bold, 12)
    onAction = _ => {
      checkServerConfigItem()
      goButton.disable = !isSelectedItemsOk()
    }
  }
  private val serverLabel = new Label {
    disable <== !serverChBox.selected
    text = "Select the configuration file application.conf\nIt should be located under </config>"
  }
  private val serverTxtField = new TextField {
    disable <== !serverChBox.selected
    text = Install.serverConfPath
    text.onChange { (_, oldText, newText) =>
      if (checkServerConfigItem()) {
        Install.serverConfPath = newText
        getJmsConfFilePath(Install.serverConfPath).foreach { Install.serverJmsPath = _ }
      }
      goButton.disable = !isSelectedItemsOk()
    }
  }
  private val serverBrowseButton = new Button("Browse...") {
    graphic = FxUtils.newImageView(IconResource.LOAD)
    disable <== !serverChBox.selected
    onAction = _ => {
      model.browseServerConfigFile(serverTxtField.getText, serverTxtField)
    }
  }

  // Proline modules
  private val prolineModulesChBox = new CheckBox("Proline Modules") {
    underline = true
    font = Font.font("SanSerif", FontWeight.Bold, 12)
    onAction = _ => {
      bindToChild()
      checkSeqReposConfigItem()
      goButton.disable = !isSelectedItemsOk()

    }
  }

  // Sequence repository
  private val seqReposChBox = new CheckBox("Sequence Repository Configuration File") {
    underline = true
    onAction = _ => {
      bindToParent()
      checkSeqReposConfigItem()
      //checkPwxConfigItem()
      goButton.disable = !isSelectedItemsOk()
    }
  }
  private val seqReposLabel = new Label {
    disable <== !seqReposChBox.selected
    text = "Select the configuration file application.conf\nIt should be located under </config>"
  }
  private val seqReposTxtField = new TextField() {
    disable <== !seqReposChBox.selected
    text = Install.seqReposConfPath
    text.onChange { (_, oldText, newText) =>
      if (checkSeqReposConfigItem()) {
        Install.seqReposConfPath = newText
        getJmsConfFilePath(Install.seqReposConfPath).foreach { Install.seqReposJmsPath = _ }
        getParsingRulesFilePath(Install.seqReposConfPath).foreach { Install.seqReposParsigRulesPath = _ }
      }
      goButton.disable = !isSelectedItemsOk()
    }
  }
  private val seqReposBrowseButton = new Button("Browse ...") {
    graphic = FxUtils.newImageView(IconResource.LOAD)
    disable <== !seqReposChBox.selected
    onAction = _ => {
      model.browseSeqReposConfigFile(seqReposTxtField.getText, seqReposTxtField)
    }
  }

  // Proline Web Extension
//  private val pwxChBox = new CheckBox("Proline Web Configuration File") {
//    underline = true
//    onAction = _ => {
//      bindToParent()
//      checkPwxConfigItem()
//    }
//  }
//  private val pwxLabel = new Label {
//    disable <== !pwxChBox.selected
//    text = "Select the configuration file application.conf\nIt should be located under </conf>"
//  }
//  private val pwxTxtField = new TextField() {
//    disable <== !pwxChBox.selected
//    text = Install.pwxConfPath
//    text.onChange { (_, oldText, newText) =>
//      if (checkPwxConfigItem()) Install.pwxConfPath = newText
//    }
//  }
//  private val pwxBrowseButton = new Button("Browse...") {
//    graphic = FxUtils.newImageView(IconResource.LOAD)
//    disable <== !pwxChBox.selected
//    onAction = _ => {
//      model.browsePwxConfigFile(pwxTxtField.getText, pwxTxtField)
//    }
//  }



  // Task Progress Indicator
  private val glassPane = new VBox {
    children = new ProgressIndicator {
      progress = ProgressIndicator.IndeterminateProgress
      visible = true
    }
    alignment = Pos.Center
    visible = false
  }
  // Task Status
  private val statusLabel = new Label {
    maxWidth = Double.MaxValue
    padding = Insets(0, 10, 10, 10)
  }

  // Buttons
  // Start Proline-Admin-GUI Install
  val goButton = new Button(" Go ") {
    graphic = FxUtils.newImageView(IconResource.EXECUTE)
  }
  // Exit and close Proline-Admin GUI
  val exitButton = new Button("Exit") {
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    onAction = _ => {
      model.exit()
    }
  }

  val nextButton = new Button("Next") {
    graphic = FxUtils.newImageView(IconResource.ARROWRIGHT)
    contentDisplay = ContentDisplay.RIGHT
    onAction = _ => {
      onNext()
    }
  }
  val prevButton = new Button("Previous") {
    graphic = FxUtils.newImageView(IconResource.ARROWLEFT)
    onAction = _ => {
      onPrevious()
    }
  }
  val validateButton = new Button("Apply") {
    disable = true
    graphic = FxUtils.newImageView(IconResource.SAVE)
    onAction = _ => {
      // Valid and save modifications
      val confirmed = GetConfirmation("Are you sure you want to save the new Proline configurations?", "Confirm your action", "Yes", "Cancel", Install.stage)
      if (confirmed) {
        onValidate()
      }
    }
  }

  /* Style */
  Seq(
    serverLabel,
    seqReposLabel,
//    pwxLabel,
    ).foreach(_.minWidth = 60)

  Seq(
    serverTxtField,
    seqReposTxtField,
//    pwxTxtField,
    ).foreach {
      f => f.hgrow = Priority.Always
    }
  Seq(
    exitButton,
    goButton,
    prevButton,
    nextButton,
    validateButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }

  /* Layout */
  private val notificationPanel = new VBox {
    vgrow = Priority.Always
    spacing = 0.5
    children = Seq(
      corruptedFileErrorLabel,
      serverFileErrorLabel,
      seqReposFileErrorLabel,
//      pwxFileErrorLabel,
//      pgDataDirErrorLabel
     )
  }

  private val configItemsPane = new TitledBorderPane(
    title = "Select Configuration Item",
    contentNode = new VBox {
      vgrow = Priority.Always
      spacing = 5
      children = List(
        notificationPanel,
//        ScalaFxUtils.newVSpacer(minH = 1),
        serverChBox,
        new HBox {
          spacing = 5
          hgrow = Priority.Always
          children = Seq(ScalaFxUtils.newHSpacer(minW = 60, maxW = 60), serverLabel, serverTxtField, serverBrowseButton)
        },
//        ScalaFxUtils.newVSpacer(minH = 1),
        prolineModulesChBox,
        new HBox {
          spacing = 5
          hgrow = Priority.Always
          children = Seq(ScalaFxUtils.newHSpacer(minW = 30, maxW = 30), seqReposChBox)
        }, new HBox {
          spacing = 5
          hgrow = Priority.Always
          children = Seq(ScalaFxUtils.newHSpacer(minW = 60, maxW = 60), seqReposLabel, seqReposTxtField, seqReposBrowseButton)
        }, /*new HBox {
          spacing = 5
          hgrow = Priority.Always
          children = Seq(ScalaFxUtils.newHSpacer(minW = 30, maxW = 30), pwxChBox)
        }, new HBox {
          spacing = 5
          hgrow = Priority.Always
          children = Seq(ScalaFxUtils.newHSpacer(minW = 60, maxW = 60), pwxLabel, pwxTxtField, pwxBrowseButton)
        },*/
//        ScalaFxUtils.newVSpacer(minH = 1),
//        ScalaFxUtils.newVSpacer(10)
      )
    })

  // Help Pane
  val helpPane = new HBox {
    children = Seq(ScalaFxUtils.newHSpacer(minW = configItemsPane.getWidth - 50), headerHelpIcon)
  }

  // Create navigation buttons pane
  val buttonsPane = new HBox {
    hgrow = Priority.Always
    children = Seq(
      ScalaFxUtils.newHSpacer(minW = 200),
      new HBox {
        padding = Insets(10)
        spacing = 10
        children = Seq(
          goButton,
          exitButton)
      })
  }

  // Set initial selected CkeckBox
  serverChBox.selected_=(!serverTxtField.getText.isEmpty)
  seqReposChBox.selected_=(!seqReposTxtField.getText.isEmpty)
//  pwxChBox.selected_=(!pwxTxtField.getText.isEmpty)

  prolineModulesChBox.selected_=(seqReposChBox.isSelected /*|| pwxChBox.isSelected*/)

  // Set initial warning/errors notifications and get jms-node.conf path
  if (checkServerConfigItem()) getJmsConfFilePath(Install.serverConfPath).foreach { Install.serverJmsPath = _ }
  if (checkSeqReposConfigItem()) {
    getJmsConfFilePath(Install.seqReposConfPath).foreach { Install.seqReposJmsPath = _ }
    getParsingRulesFilePath(Install.seqReposConfPath).foreach { Install.seqReposParsigRulesPath = _ }
  }

//  checkPwxConfigItem()

  val toRemovePane = new VBox {
    children = Seq(configItemsPane)
  }
  // Create task Runner
  val mainPane = new VBox {
    children = Seq(new StackPane {
      children = Seq(
        toRemovePane,
        glassPane)
    })
  }

  /* Install home panel content */
  alignment = Pos.BottomCenter
  alignmentInParent = Pos.BottomCenter
  spacing = 1
  vgrow = Priority.Always
  children = Seq(
    //ScalaFxUtils.newVSpacer(minH = 5),
    helpPane,
    mainPane,
    statusLabel,
    buttonsPane)

  checkServerConfigItem()
  checkSeqReposConfigItem()
  goButton.disable = !isSelectedItemsOk()

  // Create task Runner
  Install.taskRunner = new TaskRunner(toRemovePane, glassPane, statusLabel)

  //TODO Disable go button on error item selection?
  // Go button action
  goButton.onAction = _ => {
    if ((ConfigItemPanel.configItemMap.size > 1) && (isSelectedItemsOk())) {
      onGo()
    }
  }

  /** Indicates whether Proline modules CheckBox is checked */
  private def bindToChild(): Unit = {
//    pwxChBox.selected = prolineModulesChBox.isSelected
    seqReposChBox.selected = prolineModulesChBox.isSelected
  }
  private def bindToParent(): Unit = {
    prolineModulesChBox.selected = (/*pwxChBox.isSelected ||*/ seqReposChBox.isSelected)
  }


  /**  Determines whether Server configuration item is valid */
  private def checkServerConfigItem(): Boolean = {
    if (model.isValidConfigItem(serverChBox, serverTxtField, serverFileErrorLabel)) {
      ConfigItemPanel.add(ServerConfigPanel)
      true
    } else {
      model.isValidConfigItem(serverChBox, serverTxtField, serverFileErrorLabel)
      ConfigItemPanel.remove(2)
      false
    }
  }

  /**  Determines whether sequence repository configuration item is valid */
  private def checkSeqReposConfigItem(): Boolean = {
    if (model.isValidConfigItem(seqReposChBox, seqReposTxtField, seqReposFileErrorLabel)) {
      ConfigItemPanel.add(SeqReposConfigPanel)
      true
    } else {
      model.isValidConfigItem(seqReposChBox, seqReposTxtField, seqReposFileErrorLabel)
      ConfigItemPanel.remove(4)
      false
    }
  }

  /** Determines whether Proline web extension configuration item is valid */
//  private def checkPwxConfigItem(): Boolean = {
//    if (model.isValidConfigItem(pwxChBox, pwxTxtField, pwxFileErrorLabel)) {
//      ConfigItemPanel.add(PWXConfigPanel)
//      true
//    } else {
//      model.isValidConfigItem(pwxChBox, pwxTxtField, pwxFileErrorLabel)
//      ConfigItemPanel.remove(3)
//      false
//    }
//  }

  /** Return jms-node.conf file path */
  private def getJmsConfFilePath(path: String): Option[String] = {
    Option { new File(path).getParentFile.getAbsolutePath + File.separator + """jms-node.conf""" }
  }

  /** Return parsing-rules.conf file path */
  private def getParsingRulesFilePath(path: String): Option[String] = {
    Option { new File(path).getParentFile.getAbsolutePath + File.separator + """parsing-rules.conf""" }
  }

  /** Create navigation buttons panel */
  private def creatNavButtonsPane(): HBox = {
    new HBox {
      children = Seq(
        ScalaFxUtils.newHSpacer(minW = 200),
        new HBox {
          padding = Insets(10)
          spacing = 5
          children = Seq(
            prevButton,
            nextButton,
            validateButton,
            exitButton)
        })
    }
  }

  /** Start Proline-admin GUI Install */
  private def onGo(): Unit = {
    // Disable previous button
    prevButton.disable = true
    // Remove home panel and set first item
    itemsList = ConfigItemPanel.configItemMap.values.map(_()).toList
    currNode = itemsList.head
    toRemovePane.getChildren().clear()
    toRemovePane.getChildren().add(currNode)
    // Remove home buttons panel and set navigation buttons panel
    homePanel.getChildren.remove(buttonsPane)
    val navButtonsPane = creatNavButtonsPane()
    homePanel.getChildren().add(navButtonsPane)
  }

  /** Set the next item */
  private def onNext(): Unit = {
    prevButton.disable = false
    validateButton.disable = true
    nodeIndex = nodeIndex + 1
    currNode = itemsList.apply(nodeIndex)
    toRemovePane.getChildren().clear()
    toRemovePane.getChildren().add(currNode)
    if (itemsList.last.equals(currNode)) {
      SummaryConfigPanel.sum()
      nextButton.disable = true
      validateButton.disable = false
      // Set summary configItem
    }
  }

  /** Set the previous item */
  private def onPrevious(): Unit = {
    if (nodeIndex > 0) {
      nextButton.disable = false
      validateButton.disable = true

      nodeIndex = nodeIndex - 1
      currNode = itemsList.apply(nodeIndex)
      toRemovePane.getChildren().clear()
      toRemovePane.getChildren().add(currNode)
      if (itemsList.head.equals(currNode)) {
        prevButton.disable = true
      }
    }
  }

  /** Validate the new configurations */
  private def onValidate(): Unit = {
    ConfigItemPanel.configItemMap.values.toList.foreach { _.save() }
  }

  /** Determines whether selected items are valid items */
  private def isSelectedItemsOk(): Boolean = {
    Seq(
      (serverChBox, checkServerConfigItem()),
      (seqReposChBox, checkSeqReposConfigItem()),
      /*(pwxChBox, checkPwxConfigItem())*/ ).filter(_._1.isSelected)
      .forall(_._2 == true)
  }
}

