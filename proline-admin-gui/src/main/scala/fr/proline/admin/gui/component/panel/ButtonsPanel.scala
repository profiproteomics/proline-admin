package fr.proline.admin.gui.component.panel

import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.property.BooleanProperty
import scalafx.beans.property.BooleanProperty.sfxBooleanProperty2jfx
import scalafx.geometry.Insets
import scalafx.scene.control.Button
import scalafx.scene.layout.VBox

import com.typesafe.scalalogging.LazyLogging

import fr.profi.util.scalafx.ScalaFxUtils
import fr.proline.admin.gui.Main
import fr.proline.admin.gui.component.dialog._
import fr.proline.admin.gui.component.dialog.ShowPopupWindow
import fr.proline.admin.gui.process._
import fr.proline.admin.gui.util._
import fr.proline.admin.service.db.SetupProline
import fr.proline.core.orm.util.DataStoreUpgrader

/**
 * Create the buttons of the main window, one for each feature of Proline Admin.
 */
object ButtonsPanel extends LazyLogging {

  /**
   * ******* *
   * BUTTONS *
   * ******* *
   */

  val editPgHbaConfigButton = new Button("Edit pg_hba.conf") {
    onAction = handle {
      //      _showIfPgDataDirIsDefined(new PgHbaConfigForm() )

      if (Main.postgresqlDataDir == null) {
        ShowPopupWindow(
          "Unknown data directory",
          "PostgreSQL data directory must be known in order to its configuration.\n" ++
            """You can choose it in the "Select PostgreSQL data directory" menu."""
        )

      } else {
        new PgHbaConfigForm().showAndWait()
      }
    }

    //TODO: disable <== + tooltip
    // tooltip = new Toolip("PostgreSQL data directory must be defined to use this feature.\n" +
    //   """You can choose them in the "Select PostgreSQL data dircetory" menu.""" )
    // disable <== when(Main.postgresqlDataDir == null) choose true otherwise false
  }
  
    val editPostgreSQLConfigButton = new Button("Edit postgresql.conf") {
    onAction = handle {
      //_showIfPgDataDirIsDefined(new PostgresConfigForm() ) //println

      if (Main.postgresqlDataDir == null) {
        ShowPopupWindow(
          "Unknown data directory",
          "PostgreSQL data directory must be known in order to its configuration.\n" ++
            """You can choose it in the "Select PostgreSQL data directory" menu."""
        )

      } else {
        new PostgresConfigForm().showAndWait()
      }
    }
  }

  /* Edit configuration file */
  val editProlineConfigButton = new Button("Edit Proline configuration") {
    onAction = handle {
      if (Main.adminConfPath == null || Main.adminConfPath.isEmpty() //||
      //Main.serverConfPath == null || Main.serverConfPath.isEmpty()
      ) {
        ShowPopupWindow(
          "Unknown configuration files",
          "Configuration files must be known in order to edit Proline configuration.\n" ++
            """You can choose them in the "Select configuration files" menu."""
        )

      } else {
        new ProlineConfigForm().showAndWait()
      }
    }
  }

  /* Set up Proline */
  val setupProlineButton = new Button("Set up Proline") {
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

  /* Create a new user */
  val createUserButton = new Button("Create a new user") {
    onAction = handle { new NewUserDialog() }
  }
  /*val createUserButton = new Button("Sing the Pokemon song") {
    onAction = handle {
      import scalafx.scene.Cursor
      synchronized {
        LaunchAction(
          actionButton = this,
          actionString = Util.mkCmd("sing_pokemon"),
          action = () => {
            _singPokemon()
            //Thread.sleep(3 * 1000)
          }
        )
        //Platform.runLater(Main.stage.scene().setCursor(Cursor.HAND))
        //Main.stage.scene().setCursor(Cursor.HAND)
      }
    }
  }*/

  /* Create a new project */
  val createProjectButton = new Button("Create a new project") {
    onAction = handle { new NewProjectDialog() }
  }

  /* Upgrade databases */
  val upgradeDBsButton = new Button("Upgrade databases") {
    onAction = handle {

      val confirmed = GetConfirmation("Are you sure you want to upgrade Proline databases ?\n(This process may take hours.)")

      if (confirmed) {
        //someActionRunning.set(true)
        //logger.warn("someActionRunning true " + someActionRunning)

        /* Run upgrade async */
        LaunchAction(
          actionButton = this,
          actionString = Utils.mkCmd("upgrade_dbs"),

          action = () => {

            val dsConnectorFactory = UdsRepository.getDataStoreConnFactory()

            /* Create missing databases */
            fr.proline.admin.helper.sql.createMissingDatabases(SetupProline.config.udsDBConfig, dsConnectorFactory)

            /* Logback */
            if (DataStoreUpgrader.upgradeAllDatabases(dsConnectorFactory)) {
              println("INFO - Databases successfully upgraded !")
            } else {
              println("ERROR - Databases upgrade failed !")
            }

            dsConnectorFactory.closeAll()
          }
        ) //someActionRunning.set(false)
      }
    }

    /*
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
    */

  } //end of 'upgrade db' button

  /**
   * ********************** *
   * ENABLE/DISABLE BUTTONS *
   * ********************** *
   **/

  /* Some booleans describing UDS db state in order to smartly enable/disable buttons */
  val prolineMustBeSetUp = new BooleanProperty()
  val dbCanBeUsed = new BooleanProperty()
  val someUserInDb = new BooleanProperty()
  //  val someActionRunning = BooleanProperty(false)

  /* Define when buttons shall be enabled/disbaled */
  setupProlineButton.disable <== when(prolineMustBeSetUp) choose false otherwise true
  createUserButton.disable <== when(dbCanBeUsed) choose false otherwise true
  createProjectButton.disable <== when(dbCanBeUsed && someUserInDb) choose false otherwise true
  upgradeDBsButton.disable <== when(dbCanBeUsed) choose false otherwise true

  //  editConfButton.disable <== when(someActionRunning) choose true otherwise false
  //  setupProlineButton.disable <== when(prolineMustBeSetUp && !someActionRunning) choose false otherwise true
  //  createUserButton.disable <== when(dbCanBeUsed && !someActionRunning) choose false otherwise true
  //  createProjectButton.disable <== when((dbCanBeUsed && someUserInDb) && !someActionRunning) choose false otherwise true
  //  upgradeDBsButton.disable <== when(dbCanBeUsed && !someActionRunning) choose false otherwise true

  /** Compute boolean values **/
  def computeButtonsAvailability(verbose: Boolean = true) {

    //    someActionRunning.set(false)

    logger.debug("Computing buttons availability...")
    var _prolineConfIsOk = false

    /* Config file is OK if udsDBConfig can be built with it */
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
        this.disableAllButEdit()

        //throw e // if re-thrown, infinite load 
      }
    }

    /* If config file is ok, compute the other booleans */
    if (_prolineConfIsOk == true) {

      /** Check if Proline is already set up **/
      val _prolineIsSetUp = UdsRepository.isUdsDbReachable(verbose)
      //logger.info("_prolineIsSetUp (uds reachable) : " + _prolineIsSetUp)

      Platform.runLater {
        if (_prolineIsSetUp) {
          prolineMustBeSetUp.set(false)
          dbCanBeUsed.set(true)

          /* Forbid to add project if no user (owner) is registered */
          try {
            someUserInDb.set(UdsRepository.getAllUserAccounts().isEmpty == false)

          } catch {
            case fxt: java.lang.IllegalStateException => logger.warn(fxt.getLocalizedMessage())

            case e: Throwable => {
              synchronized {
                logger.warn("Unable to retrieve users", e)
                println("ERROR - Unable to retrieve users :")
                println(e.getMessage())
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

    logger.debug("Finished computing buttons availability.")
  }

  /** Disable all buttons execpt the 'Edit Proline configuration' one **/
  def disableAllButEdit() {
    ButtonsPanel.dbCanBeUsed.set(false)
    ButtonsPanel.prolineMustBeSetUp.set(false)
    ButtonsPanel.someUserInDb.set(false)
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
    editPgHbaConfigButton,
    editPostgreSQLConfigButton,

    editProlineConfigButton,
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
        editPgHbaConfigButton,
        ScalaFxUtils.newVSpacer(),
        editPostgreSQLConfigButton,
        ScalaFxUtils.newVSpacer(),

        editProlineConfigButton,
        ScalaFxUtils.newVSpacer(),
        setupProlineButton,
        ScalaFxUtils.newVSpacer(),
        createUserButton,
        ScalaFxUtils.newVSpacer(),
        createProjectButton,
        ScalaFxUtils.newVSpacer(),
        upgradeDBsButton
      )
    }
  }

//  /** Make sure PG datadir is known before showing a stage **/
//  private def _showIfPgDataDirIsDefined(stage: Stage) {
//
//    println("Main.postgresqlDataDir == null::" +Main.postgresqlDataDir == null)
//    println(Main.postgresqlDataDir )
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