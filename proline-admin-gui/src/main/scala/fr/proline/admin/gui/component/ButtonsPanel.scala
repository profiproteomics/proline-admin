package fr.proline.admin.gui.component

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.property.BooleanProperty
import scalafx.beans.property.BooleanProperty.sfxBooleanProperty2jfx
import scalafx.geometry.Insets
import scalafx.scene.control.Button
import scalafx.scene.layout.VBox

import fr.proline.admin.gui.component.configuration.ConfigurationTabbedWindow
import fr.proline.admin.gui.component.resource.ResourcesTabbedWindow
import fr.proline.admin.gui.process._
import fr.proline.admin.gui.util._
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.service.db.migration.UpgradeAllDatabases

import fr.profi.util.scalafx.ScalaFxUtils

/**
 * Create the buttons of the main window, one for each feature of Proline Admin.
 */
object ButtonsPanel extends LazyLogging {

  /**
   * ******* *
   * BUTTONS *
   * ******* *
   */

  /* Replay the wizard */
  val replayWizardButton = new Button("Replay wizard") {
    disable = true
    onAction = handle {}
  }

  /* Configure PostgreSQL, Proline, SeqRepo */
  val configurationButton = new Button("Configuration") {
    onAction = handle {
      new ConfigurationTabbedWindow().showAndWait()
    }
  }

   /* Set up ProlineDataStore */
  val setupProlineButton = new Button("Set up Proline datastore") {
    onAction = handle {

      val confirmed = GetConfirmation(text = "Are you sure you want to set up Proline ?")

      if (confirmed) {
        //someActionRunning.set(true)
        LaunchAction(
          actionButton = this,
          actionString = Utils.mkCmd("setup"),
          action = () => synchronized {
            new SetupProline(SetupProline.getUpdatedConfig(),UdsRepository.getUdsDbConnector()).run()
          }
        )
      }
    }
  }

  /* Upgrade Proline databases */
  val upgradeDBsButton = new Button("Upgrade Proline databases") {
    onAction = handle {

      val confirmed = GetConfirmation("Are you sure you want to upgrade Proline databases ?\n(This process may take hours.)")

      if (confirmed) {
        //someActionRunning.set(true)
        //logger.warn("someActionRunning true " + someActionRunning)

        /* Run upgrade async */
        LaunchAction(
          actionButton = this,
          actionString = Utils.mkCmd("upgrade_dbs"),
          reloadConfig = false,

          action = () => {
            val dsConnectorFactory = UdsRepository.getDataStoreConnFactory()
            // Create missing databases
            //fr.proline.admin.helper.sql.createMissingDatabases(SetupProline.config.udsDBConfig, dsConnectorFactory)

            /* Logback */
            try {
              new UpgradeAllDatabases(dsConnectorFactory).doWork()
              println("INFO - Databases successfully upgraded !")
            } catch {
              case e: Exception => {
                println("ERROR - Databases upgrade failed: \n", e.getMessage)
                logger.error("ERROR - Databases upgrade failed", e)
              }  
            }

            //dsConnectorFactory.closeAll()
          }
        ) //someActionRunning.set(false)
      }
    }
  }

  /* Manage resources: users, projects, PTMs, instrument configurations... */
  val manageResourcesButton = new Button("Manage resources") {
    onAction = handle {
      new ResourcesTabbedWindow().showAndWait()
    }
  }

  /**
   * ********************** *
   * ENABLE/DISABLE BUTTONS *
   * ********************** *
   **/

  /* Some booleans describing UDS db state in order to smartly enable/disable buttons */
  private val prolineMustBeSetUp = new BooleanProperty()
  private val dbCanBeUsed = new BooleanProperty()
  //val someUserInDb = new BooleanProperty()
  //  val someActionRunning = BooleanProperty(false)

  /* Define when buttons shall be enabled/disbaled */
  setupProlineButton.disable <== when(prolineMustBeSetUp) choose false otherwise true
  upgradeDBsButton.disable <== when(dbCanBeUsed) choose false otherwise true
  manageResourcesButton.disable <== when(dbCanBeUsed) choose false otherwise true
  
  //  editConfButton.disable <== when(someActionRunning) choose true otherwise false
  //  setupProlineButton.disable <== when(prolineMustBeSetUp && !someActionRunning) choose false otherwise true
  //  createUserButton.disable <== when(dbCanBeUsed && !someActionRunning) choose false otherwise true
  //  createProjectButton.disable <== when((dbCanBeUsed && someUserInDb) && !someActionRunning) choose false otherwise true
  //  upgradeDBsButton.disable <== when(dbCanBeUsed && !someActionRunning) choose false otherwise true

  /** Compute boolean values **/
  def computeButtonsAvailability(verbose: Boolean = true, isConfigSemanticsValid: Boolean = true) {

    //    someActionRunning.set(false)

    logger.debug("Computing buttons availability...")
    var _prolineConfIsOk = isConfigSemanticsValid //was computed by ProlineAdminConnection.loadConfig

    if (_prolineConfIsOk == false) disableAllButtonsButConfig()
    else {

      /* Config is OK if udsDBConfig can be built with it */
      try {
        val udsDBConfig = UdsRepository.getUdsDbConfig()
        logger.info("Proline configuration is valid.")
        _prolineConfIsOk = true

      } catch {

        /* If config file is ok, abort computation */
        case e: Throwable => {
          synchronized {
            if (verbose) logger.warn("Unable to read Proline configuration : invalid configuration file.")
            if (verbose) logger.warn(e.getLocalizedMessage())
            println("ERROR - Invalid configuration. Please edit the configuration file or choose a valid one.")
          }
          _prolineConfIsOk = false
          this.disableAllButtonsButConfig()

          //throw e // if re-thrown, infinite load 
        }
      }
    }

    /* If config file is ok, compute the other booleans */
    if (_prolineConfIsOk == true) {

      /** Check if Proline is already set up **/
      val _prolineIsSetUp = UdsRepository.isUdsDbReachable(verbose)
      //logger.info("_prolineIsSetUp (uds reachable) : " + _prolineIsSetUp)

      Platform.runLater {
        prolineMustBeSetUp.set(!_prolineIsSetUp)
        dbCanBeUsed.set(_prolineIsSetUp)
        logger.info("Proline is set up: " +_prolineIsSetUp)
      }
    }

    logger.debug("Finished computing buttons availability.")
  }

  /** Disable all buttons execpt the 'Edit Proline configuration' one **/
  def disableAllButtonsButConfig() {
    ButtonsPanel.dbCanBeUsed.set(false)
    ButtonsPanel.prolineMustBeSetUp.set(false)
  }

  //  def disableAll() {
  //    this.disableAllButEdit()
  //    this.someActionRunning.set(true)
  //  }

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
   * ***** *
   * LAYOUT *
   * **** *
   */

  Seq(
    replayWizardButton,
    configurationButton,
    setupProlineButton,
    upgradeDBsButton,
    manageResourcesButton
  ).foreach { b =>
      b.prefHeight = 40
      b.prefWidth = 250
      b.styleClass += ("mainButtons")
    }

  /**
   * ****** *
   * APPLY() *
   * ***** *
   */

  /** Display these buttons in a VBox **/
  def apply(): VBox = {
    new VBox {
      padding = Insets(10)
      spacing = 10

      content = Seq(
        //TODO: enable me replayWizardButton,
        //ScalaFxUtils.newVSpacer(),
        configurationButton,
        ScalaFxUtils.newVSpacer(),
        setupProlineButton,
        ScalaFxUtils.newVSpacer(),
        upgradeDBsButton,
        ScalaFxUtils.newVSpacer(),
        manageResourcesButton
      )
    }
  }

//  /** Make sure PG datadir is known before showing a stage **/
//  private def _showIfPgDataDirIsDefined(stage: Stage) {
//
//    if (Main.postgresqlDataDir == null) {
//      ShowPopupWindow(
//        "Unknown data directory",
//        "PostgreSQL data directory must be known in order to its configuration.\n" ++
//          """You can choose them in the "Select configuration files" menu."""
//      )
//
//    } else {
//      stage.showAndWait()
//    }
//  }

  /**
   * ****************** *
   * ADDITIONAL FEATURES *
   * ***************** *
   */

  private def _singPokemon() = println("""INFO
  Un jour je serai le meilleur dresseur
  Je me battrai sans répit
  Je ferai tout pour être vainqueur
  Et gagner les défis
  
  Je parcourrai la Terre entière
  Battant avec espoir 
  Les Pokemon et leur mystère
  Le secret de leur pouvoir
  
  POKEMOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOONNNNNNNNNNNNNNNNN
  """)

}