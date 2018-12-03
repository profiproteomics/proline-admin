package fr.proline.admin.gui.monitor.view

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.geometry.{ Pos, Insets }
import scalafx.scene.layout.{ VBox, HBox, StackPane, Priority, Region }
import scalafx.scene.control.Label
import scalafx.scene.control.TextField
import scalafx.scene.control.Hyperlink
import scalafx.scene.control.Button
import scalafx.scene.text.{ Font, FontWeight }
import scalafx.scene.control.ProgressIndicator
import scalafx.beans.property.BooleanProperty

import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.task.TaskRunner
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.wizard.util.WindowSize
import fr.proline.admin.gui.monitor.model.HomeViewModel
import fr.profi.util.scalafx.{ BoldLabel, TitledBorderPane }
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scala.ScalaUtils._
import fr.proline.admin.gui.util.ExitPopup
import fr.proline.admin.gui.process.PostgreSQLUtils

/**
 * Creates and displays home panel of Proline-Admin GUI monitor .
 * @author aromdhani
 *
 */

class HomePanel(model: HomeViewModel) extends VBox with LazyLogging {

  // Load initial configurations
  model.setNewConfig()

  /* Proline error and warning labels */

  val udsDbErrorLabel = new Label {
    text = "Error Proline is not set up. Make sure that you have already setup Proline."
    graphic = ScalaFxUtils.newImageView(IconResource.CANCEL)
    style = TextStyle.RED_ITALIC
    managed <== visible
  }
  val connectionErrorLabel = new Label {
    text = "Error establishing a database connection. Please check database connection parameters."
    graphic = ScalaFxUtils.newImageView(IconResource.CANCEL)
    style = TextStyle.RED_ITALIC
    managed <== visible
  }
  val serverConfigWarningLabel = new Label {
    text = "The path of Proline server and jms-node configuration files not found."
    graphic = ScalaFxUtils.newImageView(IconResource.WARNING)
    style = TextStyle.ORANGE_ITALIC
    managed <== visible
  }
  val seqReposWarningLabel = new Label {
    text = "The path of the sequence repository configuration file not found."
    graphic = ScalaFxUtils.newImageView(IconResource.WARNING)
    style = TextStyle.ORANGE_ITALIC
    managed <== visible
  }

  val pgsqlDataDirWarningLabel = new Label {
    text = "The path of the Proline data directory not found."
    graphic = ScalaFxUtils.newImageView(IconResource.WARNING)
    style = TextStyle.ORANGE_ITALIC
    managed <== visible
  }
  // Help icon
  val headerHelpIcon = new Hyperlink {
    graphic = FxUtils.newImageView(IconResource.HELP)
    alignmentInParent = Pos.BASELINE_RIGHT
    onAction = handle {
      model.openAdminGuide()
    }
  }

  // JMS components
  val infoMessage = new BoldLabel("(By default, same server as Proline Server Cortex)")
  private val V_SPACING = 10
  private val H_SPACING = 5
  val jmsServerLabel = new HBox {
    vgrow = Priority.Always
    children = List(new Label {
      text = "JMS Server"
      font = Font.font("SanSerif", FontWeight.Bold, 12)
    })
  }

  val jmsHostLabel = new Label("Host: ")
  val jmsHostField = new TextField() {
    disable = true
    text = model.serverNodeConfigOpt.map(_.jmsServerHost.getOrElse("localhost")).getOrElse("localhost")
  }

  val jmsPortLabel = new Label("Port: ")
  val jmsPortField = new TextField() {
    disable = true
    text = model.serverNodeConfigOpt.map(_.jmsServePort.getOrElse(5442).toString()).getOrElse(5442).toString()
  }

  val jmsProlineQueueLabel = new Label("Proline Queue Name:")
  val jmsProlineQueueField = new TextField {
    disable = true
    text = model.serverNodeConfigOpt.map(_.requestQueueName.getOrElse("ProlineServiceRequestQueue")).getOrElse("ProlineServiceRequestQueue")
  }
  val jmsTiteledPane = new HBox {
    children = new TitledBorderPane(
      title = "",
      titleTooltip = "JMS Server properties",
      contentNode = new VBox {
        prefWidth = (WindowSize.prefWitdh)
        spacing = V_SPACING * 2
        children = List(infoMessage, new HBox {
          spacing = H_SPACING * 3
          children = List(jmsHostLabel, jmsHostField)
        }, new HBox {
          spacing = H_SPACING * 3
          children = List(jmsPortLabel, jmsPortField)
        },
          new HBox {
            spacing = H_SPACING * 3
            children = List(jmsProlineQueueLabel, jmsProlineQueueField)
          })
      })
  }

  // JMS server pane
  val jmsServerPane = new HBox {
    spacing = H_SPACING * 2
    vgrow = Priority.Always
    hgrow = Priority.Always
    fillWidth = true
    children = List(jmsServerLabel, jmsTiteledPane)
  }

  // postgreSQL server components
  val pgServerLabel = new HBox {
    vgrow = Priority.Always
    children = List(new Label {
      text = "PostgreSQL Server"
      font = Font.font("SanSerif", FontWeight.Bold, 12)
    })
  }

  val pgHostLabel = new Label("Host: ")
  val pgHostField = new TextField() {
    disable = true
    text = model.adminConfigOpt.get.dbHost.getOrElse("<db_host>")
  }

  val pgPortLabel = new Label("Port: ")
  val pgPortField = new TextField() {
    disable = true
    text = model.adminConfigOpt.get.dbPort.getOrElse(5432).toString()
  }

  val pgUserLabel = new Label("User: ")
  val pgUserField = new TextField() {
    disable = true
    text = model.adminConfigOpt.get.dbUserName.getOrElse("<db_user>")
  }

  val pgPasswordLabel = new Label("Password: ")
  val pgPasswordField = new TextField() {
    disable = true
    text = model.adminConfigOpt.get.dbPassword.getOrElse("<db_password>")
  }

  val pgTitledPane = new HBox {
    children = Seq(new TitledBorderPane(
      title = "",
      titleTooltip = "PostgreSQL Server Properties",
      contentNode = new VBox {
        prefWidth = (WindowSize.prefWitdh)
        spacing = V_SPACING * 2
        children = List(new HBox {
          spacing = H_SPACING * 3
          children = List(pgHostLabel, pgHostField)
        }, new HBox {
          spacing = H_SPACING * 3
          children = List(pgPortLabel, pgPortField)
        },
          new HBox {
            spacing = H_SPACING * 3
            children = List(pgUserLabel, pgUserField)
          }, new HBox {
            spacing = H_SPACING * 3
            children = List(pgPasswordLabel, pgPasswordField)
          })
      }))
  }

  //PostgreSQL server pane
  val pgServerPane = new HBox {
    spacing = H_SPACING * 2
    fillWidth = true
    children = List(pgServerLabel, pgTitledPane)
  }

  //task Progress Indicator
  private val glassPane = new VBox {
    children = new ProgressIndicator {
      progress = ProgressIndicator.IndeterminateProgress
      visible = true
    }
    alignment = Pos.Center
    visible = false
  }
  //task Status
  private val statusLabel = new Label {
    maxWidth = Double.MaxValue
    padding = Insets(0, 10, 10, 10)
  }

  //exit button to exit Admin-GUI
  val exitButton = new Button("Exit") {
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    onAction = handle {
      exit()
    }
  }
  //go button to start
  val goButton = new Button(" Go ") {
    graphic = FxUtils.newImageView(IconResource.EXECUTE)
    onAction = handle {
      go()
    }
  }

  // Buttons pane
  val space = new Region {
    prefWidth = 200
    hgrow = Priority.ALWAYS
  }
  val buttonsPane = new HBox {
    children = Seq(
      space,
      new HBox {
        padding = Insets(10)
        spacing = 10
        children = Seq(
          goButton,
          exitButton)
      })
  }

  // Layout
  Seq(exitButton,
    goButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }

  Seq(pgServerLabel,
    jmsServerLabel,
    pgPortLabel,
    pgHostLabel,
    pgUserLabel,
    pgPasswordLabel,
    jmsHostLabel,
    jmsPortLabel,
    jmsProlineQueueLabel).foreach(_.minWidth = 150)

  Seq(
    pgPortField,
    pgUserField,
    pgPasswordField,
    pgHostField,
    jmsHostField,
    jmsProlineQueueField,
    jmsPortField).foreach {
      f => f.hgrow = Priority.Always
    }

  val helpPane = new HBox {
    children = Seq(
      ScalaFxUtils.newHSpacer(minW = pgServerPane.getWidth - 50),
      headerHelpIcon)
  }
  val notificationsPane = new VBox {
    spacing = 5
    children = Seq(
      udsDbErrorLabel,
      connectionErrorLabel,
      serverConfigWarningLabel,
      seqReposWarningLabel,
      pgsqlDataDirWarningLabel)
  }
  val propertiesPane = new VBox {
    spacing = V_SPACING
    children = Seq(
      jmsServerPane,
      ScalaFxUtils.newVSpacer(5),
      pgServerPane)
  }
  val toRemovePane = new VBox {
    spacing = V_SPACING
    children = Seq(
      notificationsPane,
      helpPane,
      propertiesPane)
  }
  val mainPane = new VBox {
    spacing = V_SPACING
    children = Seq(
      toRemovePane,
      ScalaFxUtils.newVSpacer(10),
      buttonsPane)
  }

  // Final monitor pane
  alignment = Pos.CENTER
  alignmentInParent = Pos.CENTER
  spacing = 1
  hgrow = Priority.Always
  vgrow = Priority.ALWAYS
  children = Seq(
    mainPane)

  // Show error and warning labels 
  val isConnectionEstablished = BooleanProperty(!model.isConnectionEstablished())
  connectionErrorLabel.visible <== isConnectionEstablished
  val isUdsDbSetup = BooleanProperty(!model.isUdsDbReachable())
  udsDbErrorLabel.visible <== isUdsDbSetup
  serverConfigWarningLabel.visible <== !BooleanProperty(model.isServerConfigFileOK())
  seqReposWarningLabel.visible <== !BooleanProperty(model.isSeqRepoConfigFileOK())
  pgsqlDataDirWarningLabel.visible <== !BooleanProperty(model.isPgSQLDataDirOK())

  // Create task Runner
  val taskRunner = new TaskRunner(mainPane, glassPane, statusLabel)

  def go() {
    val toAdd = new VBox {
      children = Seq(new StackPane {
        children = Seq(
          TabsPanel,
          glassPane)
      }, statusLabel,
        buttonsPane)
    }
    this.toRemovePane.getChildren.clear()
    this.getChildren.addAll(toAdd)
    goButton.visible = false
  }

  // Disable go button when connection to UDS database failed or UDS database is not setup .
  goButton.disable <== BooleanProperty(isConnectionEstablished.value || isUdsDbSetup.value)

  /** Exit and close Proline-Admin GUI window */
  def exit() {
    ExitPopup("Exit", "Are you sure that you want to exit Proline-Admin-GUI Monitor ?", Some(Monitor.stage), false)
  }
}