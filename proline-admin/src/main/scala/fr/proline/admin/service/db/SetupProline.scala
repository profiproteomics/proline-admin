package fr.proline.admin.service.db

import java.io.File
import javax.persistence.EntityManager
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.slf4j.Logging
import fr.proline.admin.helper.sql._
import fr.proline.admin.service.db.setup._
import fr.proline.core.orm.uds.{ AdminInformation => UdsAdminInfos }
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.repository.{DriverType, ProlineDatabaseType}
import fr.profi.util.ThreadLogger
import fr.profi.util.resources._
import fr.profi.util.sql.getTimeAsSQLTimestamp

/**
 * @author David Bouyssie
 *
 */
class SetupProline(prolineConfig: ProlineSetupConfig) extends Logging {

  def run() {

    val currentThread = Thread.currentThread

    if (!currentThread.getUncaughtExceptionHandler.isInstanceOf[ThreadLogger]) {
      currentThread.setUncaughtExceptionHandler(new ThreadLogger(logger.underlying.getName))
    }
    
    // Create a new RAW UDSdb connector
    val udsDbConnector = prolineConfig.udsDBConfig.toNewConnector
    
    try {
      // Set Up the UDSdb
      logger.info("setting up the 'User Data Set' database...")
      setupDbFromDataset( udsDbConnector, prolineConfig.udsDBConfig, "/dbunit_init_datasets/uds-db_dataset.xml" )
      
      tryInTransaction( udsDbConnector, { udsEM =>
        
        // Import Admin information
        _importAdminInformation(udsEM)
        logger.info("Admin information imported !")
  
        // Import external DBs connections
        _importExternalDBs(udsEM)
        logger.info("External databases connection settings imported !")
        
      })
      
    } finally {
      // Release the connector
      udsDbConnector.close()
    }

    // Set Up the PSdb
    logger.info("setting up the 'Peptide Sequence' database...")
    setupDbFromDataset( prolineConfig.psDBConfig, "/dbunit_init_datasets/ps-db_dataset.xml" )
    
    // TODO: re-enable this importation ?
    /*val psAdminInfos = new PsAdminInfos()
    psAdminInfos.setModelVersion(dbConfig.schemaVersion)
    psAdminInfos.setDbCreationDate(getTimeAsSQLTimestamp)
    //psAdminInfos.setModelUpdateDate()    
    psEM.persist(psAdminInfos)

    logger.info("PSdb admin information imported !")
    */

    // Set Up the PDIdb
    logger.info("setting up the 'Protein Database Index' database...")
    setupDbFromDataset( prolineConfig.pdiDBConfig, "/dbunit_init_datasets/pdi-db_dataset.xml" )

    logger.info("Proline has been successfully set up !")
  }
  
  private def _importAdminInformation(udsEM: EntityManager) {
    
    val udsDbConfig = prolineConfig.udsDBConfig

    val udsAdminInfos = new UdsAdminInfos()
    udsAdminInfos.setModelVersion(udsDbConfig.schemaVersion)
    udsAdminInfos.setDbCreationDate(getTimeAsSQLTimestamp)
    //udsAdminInfos.setModelUpdateDate()
    udsAdminInfos.setConfiguration("""{}""")

    udsEM.persist(udsAdminInfos)
  }

  private def _importExternalDBs(udsEM: EntityManager) {

    // Store PSdb connection settings
    udsEM.persist(prolineConfig.psDBConfig.toUdsExternalDb())

    // Store PDIdb connection settings
    udsEM.persist(prolineConfig.pdiDBConfig.toUdsExternalDb())
  }

}

/**
 * Static Proline setup.
 * Setup will be performed using application defaults stored in "application.conf"
 */
object SetupProline {

  var classLoader = SetupProline.getClass().getClassLoader()
  //ClassLoadergetSystemClassLoader()

  private var _appConfParams: Config = null
  
  /** Instantiates a SetupProline object and call the run() method. */
  def apply() {
    new SetupProline(config).run()
  }

  def getConfigParams(): Config = {

    // Parse config if it not already done
    if (_appConfParams == null) {
      synchronized {
        _appConfParams = ConfigFactory.load(classLoader, "application")
      }
    }

    _appConfParams
  }

  def setConfigParams(newConf: Config) {
    synchronized {
      _appConfParams = newConf
    }
  }

  // Parse config if it not already done
  lazy val config = _parseProlineSetupConfig(getConfigParams)

  private def _parseProlineSetupConfig(config: Config): ProlineSetupConfig = {

    // Load proline main settings
    val prolineConfig = config.getConfig("proline-config")
    val dataDirStr = prolineConfig.getString("data-directory")
    val dataDir = new File(dataDirStr)

    // Load shared settings
    val authConfig = config.getConfig("auth-config")
    val hostConfig = config.getConfig("host-config")
    val driverAlias = prolineConfig.getString("driver-type")
    val driverConfig = config.getConfig(driverAlias + "-config")
    val appDriverSpecificConf = ConfigFactory.load(classLoader, "application-" + driverAlias)

    // Load database specific settings
    val dbList = List("uds", "pdi", "ps", "msi", "lcms")
    val dbSetupConfigByType = dbList.map { dbType =>

      // Retrieve settings relative to database connection
      val dbDriverSpecificConf = appDriverSpecificConf.getConfig(dbType + "-db")
      val dbConfig = config.getConfig(dbType + "-db").withFallback(dbDriverSpecificConf)
      val connectionConfig = dbConfig.getConfig("connection-properties")
      //      val schemaVersion = dbConfig.getString("version")  

      // Merge connection settings with shared settings
      val fullConnConfig = _mergeConfigs(connectionConfig, authConfig, hostConfig, driverConfig.getConfig("connection-properties"))

      // Build the script directory corresponding to the current database configuration
      //      val scriptDir = prolineConfig.getString("db-script-root") +
      //                       dbConfig.getString("script-directory") +
      //                       driverConfig.getString("script-directory")
      //      val scriptName = dbConfig.getString("script-name")

      val db = ProlineDatabaseType.withPersistenceUnitName(dbType + "db_production")
      val driver = DriverType.valueOf(driverAlias.toUpperCase()) //fullConnConfig.getString("driver")

      // Build the database setup configuration object
      (dbType -> DatabaseSetupConfig(db, driver, dataDir, fullConnConfig))
    } toMap

    ProlineSetupConfig(
      dataDirectory = dataDir,
      udsDBConfig = dbSetupConfigByType("uds"),
      pdiDBConfig = dbSetupConfigByType("pdi"),
      psDBConfig = dbSetupConfigByType("ps"),
      msiDBConfig = dbSetupConfigByType("msi"),
      lcmsDBConfig = dbSetupConfigByType("lcms")
    )

  }

  /**
   * Merge Config objects consecutively.
   *
   */
  private def _mergeConfigs(configs: Config*): Config = {
    var mergedConfig = configs.head

    configs.tail.foreach { config =>
      mergedConfig = mergedConfig.withFallback(config)
    }

    mergedConfig
  }

}



