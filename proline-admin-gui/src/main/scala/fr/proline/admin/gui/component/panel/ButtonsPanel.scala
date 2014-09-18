package fr.proline.admin.gui.component.panel

import com.typesafe.scalalogging.slf4j.Logging

import fr.proline.admin.gui.Util
import fr.proline.admin.gui.component.dialog.ConfFileEditor
import fr.proline.admin.gui.component.dialog.GetConfirmation
import fr.proline.admin.gui.component.dialog.NewProjectDialog
import fr.proline.admin.gui.component.dialog.NewUserDialog
import fr.proline.admin.gui.process.LaunchAction
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.service.db.SetupProline
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.core.orm.util.DataStoreUpgrader

import scalafx.Includes.handle
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.Includes.when
import scalafx.application.Platform
import scalafx.beans.property.BooleanProperty
import scalafx.beans.property.BooleanProperty.sfxBooleanProperty2jfx
import scalafx.geometry.Insets
import scalafx.scene.control.Button
import scalafx.scene.layout.VBox

/**
 * Create the buttons of the main window, one for each feature of Proline Admin.
 */
object ButtonsPanel extends Logging {

  /**
   * ******* *
   * BUTTONS *
   * ******* *
   */

  /**
   *  Edit configuration file
   */
  val editConfButton = new Button("Edit Proline configuration") {
    onAction = handle { new ConfFileEditor() }
  }

  /**
   *  Set up Proline
   */
  val setupProlineButton = new Button("Set up Proline") {
    onAction = handle {

      /** Get confirmation  first */
      val confirmed = GetConfirmation(text = "Are you sure you want to set up Proline ?")

      /** Set up Proline if confirmed */
      if (confirmed) {

        LaunchAction(
          actionButton = this,
          actionString = Util.mkCmd("setup"),
          action = () => synchronized { SetupProline() }
        )
      }
    }
  }

  /**
   *  Create a new user
   */
  val createUserButton = new Button("Create a new user") {
    onAction = handle { new NewUserDialog() }
  }
  /* val createUserButton = new Button("Sing the Pokemon song") {
      onAction = handle {
        LaunchAction(
          actionButton = this,
          actionString = Util.mkCmd("sing_pokemon"),
          action = () => _singPokemon()
        )
      }
    } */

  /**
   *  Create a new project
   */
  val createProjectButton = new Button("Create a new project") {
    onAction = handle { new NewProjectDialog() }
  }

  /** Upgrade databases */
  val upgradeDBsButton = new Button("Upgrade databases") {
    onAction = handle {

      /** Get confirmation  first */
      val confirmed = GetConfirmation("Are you sure you want to upgrade Proline databases ?")

      /** Upgrade databases if confirmed */
      if (confirmed) {

        LaunchAction(
          actionButton = this,
          actionString = Util.mkCmd("upgrade_dbs"),

          action = () => {

            val dsConnectorFactory = _getConnectorFactory()

            if (DataStoreUpgrader.upgradeAllDatabases(dsConnectorFactory)) {
              println("INFO - Databases successfully upgraded !")
            } else {
              println("ERROR - Databases upgrade failed !")
            }

            dsConnectorFactory.closeAll()
          }
        )
      }
    }

    /**
     * Get data store connector factory
     */
    def _getConnectorFactory(): DataStoreConnectorFactory = {

      //TODO: sure? (many changes in UdsRpository)

      val connectorFactory = DataStoreConnectorFactory.getInstance()
      if (!connectorFactory.isInitialized) {
        connectorFactory.initialize(SetupProline.config.udsDBConfig.toNewConnector)
      }

      connectorFactory
    }

  } //end of 'upgrade db' button

  /**
   * ********************** *
   * ENABLE/DISABLE BUTTONS *
   * ********************** *
   */

  /** Some booleans describing UDS db state in order to smartly enable/disable buttons */
  val prolineMustBeSetUp = new BooleanProperty()
  val dbCanBeUsed = new BooleanProperty()
  val someUserInDb = new BooleanProperty()

  /** Define when buttons shall be enabled/disbaled */
  //  updateBooleans() : don't call it now because ProlineAdmin conf is not up to date
  setupProlineButton.disable <== when(prolineMustBeSetUp) choose false otherwise true
  createUserButton.disable <== when(dbCanBeUsed) choose false otherwise true
  createProjectButton.disable <== when(dbCanBeUsed && someUserInDb) choose false otherwise true
  upgradeDBsButton.disable <== when(dbCanBeUsed) choose false otherwise true

  /**
   * Compute boolean values
   */
  def computeButtonsAvailability() {

    var _prolineConfIsOk = false

    /** Config file is OK if udsDBConfig can be built with it */
    try {

      val udsDBConfig = UdsRepository.getUdsDbConfig()
      logger.info("_prolineConfIsOk")
      _prolineConfIsOk = true

    } catch {

      /** If config file is ok, abort computation */
      case e: Throwable => {
        synchronized {
          logger.warn("Unable to read Proline configuration : invalid configuration file.")
          logger.warn(e.getLocalizedMessage())
          println("ERROR - Invalid configuration. Please edit the configuration file or choose a valid one.")
        }
        _prolineConfIsOk = false
        this.disableAllButEdit()

        //throw e // if re-thrown, infinite load 
      }
    }

    /** If config file is ok, compute the other booleans */
    if (_prolineConfIsOk == true) {

      /** Check if Proline is already set up */
      val _prolineIsSetUp = UdsRepository.isUdsDbReachable()
      logger.info("_prolineIsSetUp (uds reachable) : " + _prolineIsSetUp)

      Platform.runLater {
        if (_prolineIsSetUp) {
          prolineMustBeSetUp.set(false)
          dbCanBeUsed.set(true)

          /** Forbid to add project if no user (owner) is registered */
          try {
            someUserInDb.set(UdsRepository.getAllUserAccounts().isEmpty == false)

          } catch {
            case fxt: java.lang.IllegalStateException => logger.warn(fxt.getLocalizedMessage())

            case e: Throwable => {
              synchronized {
                logger.warn("Unable to retrieve users")
                logger.warn(e.getLocalizedMessage())
                println("ERROR - Unable to retrieve users : " + e.getMessage())
              }
              someUserInDb.set(false)
              //TODO ? throw e // if re-thrown, infinite load 
            }
          }

        } else {
          prolineMustBeSetUp.set(true)
          dbCanBeUsed.set(false)
          someUserInDb.set(false)
        }
      }
    }
  }

  /**
   * Disable all buttons execpt the 'Edit Proline configuration' one
   */
  def disableAllButEdit() {
    ButtonsPanel.dbCanBeUsed.set(false)
    ButtonsPanel.prolineMustBeSetUp.set(false)
    ButtonsPanel.someUserInDb.set(false)
  }

  //  /**
  //   * Print these booleans
  //   */
  //  def printAllBool() {
  //    println(s"""  
  //    dbCanBeUsed    $dbCanBeUsed
  //    mustBeSetUp    $mustBeSetUp
  //    someUserInDb  $someUserInDb
  //    
  //    """)
  //  }

  /**
   * ****** *
   * LAYOUT *
   * ****** *
   */

  Seq(
    editConfButton,
    setupProlineButton,
    createUserButton,
    createProjectButton,
    upgradeDBsButton
  ).foreach { b =>
      b.prefHeight = 40
      b.prefWidth = 250
      b.styleClass += ("mainButtons")
    }

  /**
   * ******* *
   * APPLY() *
   * ******* *
   */

  /**
   *  Display these buttons in a VBox
   */
  def apply(): VBox = {
    new VBox {
      padding = Insets(10)
      spacing = 10

      content = Seq(
        editConfButton,
        Util.newVSpacer,
        setupProlineButton,
        Util.newVSpacer,
        createUserButton,
        Util.newVSpacer,
        createProjectButton,
        Util.newVSpacer,
        upgradeDBsButton
      )
    }
  }

  //  /**
  //   * ******************* *
  //   * ADDITIONAL FEATURES *
  //   * ******************* *
  //   */
  //
  //  private def _singPokemon() = println("""INFO
  //Un jour je serai le meilleur dresseur
  //Je me battrai sans répit
  //Je ferai tout pour être vainqueur
  //Et gagner les défis
  //
  //Je parcourrai la Terre entière
  //Battant avec espoir 
  //Les Pokemon et leur mystère
  //Le secret de leur pouvoir
  //
  //POKEMOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOONNNNNNNNNNNNNNNNN
  //""")

}