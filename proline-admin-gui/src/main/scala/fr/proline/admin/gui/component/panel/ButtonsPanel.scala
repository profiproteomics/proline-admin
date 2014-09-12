package fr.proline.admin.gui.component.panel

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
import scalafx.beans.property.BooleanProperty
import scalafx.beans.property.BooleanProperty.sfxBooleanProperty2jfx
import scalafx.geometry.Insets
import scalafx.scene.control.Button
import scalafx.scene.layout.VBox

/**
 * Create the buttons of the main window, one for each feature of Proline Admin.
 */
object ButtonsPanel {

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
      val confirmed = GetConfirmation("Are you sure you want to set up Proline ?")

      /** Set up Proline if confirmed */
      if (confirmed) {

        LaunchAction(
          actionButton = this,
          actionString = Util.mkCmd("setup"),
          action = () => SetupProline()
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
  //  val createUserButton = new Button("Sing the Pokemon song") {
  //    onAction = handle {
  //      LaunchAction(
  //        actionButton = this,
  //        actionString = Util.mkCmd("sing_pokemon"),
  //        action = () => _singPokemon()
  //      )
  //    }
  //  }

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
              println("Databases successfully upgraded !")
            } else {
              println("Databases upgrade failed !")
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

    try {

      /** Config file is OK if udsDBConfig can be built with it */
      val udsDBConfig = SetupProline.getUpdatedConfig().udsDBConfig
      _prolineConfIsOk = true

      /** Check if Proline is already set up */
      val _prolineIsSetUp = UdsRepository.isUdsDbReachable()

      if (_prolineIsSetUp) {
        prolineMustBeSetUp.set(false)
        dbCanBeUsed.set(true)

        /** Forbid to add project if no user (owner) is registered */
        someUserInDb.set(!UdsRepository.getAllUserAccounts().isEmpty)

      } else {
        prolineMustBeSetUp.set(true)
        dbCanBeUsed.set(false)
        someUserInDb.set(false)
      }

      /** If config file is not ok */
    } catch {
      case e: Throwable => {

        _prolineConfIsOk = false
        dbCanBeUsed.set(false)
        prolineMustBeSetUp.set(false)
        someUserInDb.set(false)
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