package fr.proline.admin.service.db

import java.io.File
import javax.persistence.EntityManager
import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.scalalogging.LazyLogging
import fr.proline.admin.helper.sql._
import fr.proline.admin.service.db.setup._
import fr.proline.core.orm.uds.{ AdminInformation => UdsAdminInfos }
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.repository.{ IDatabaseConnector, DriverType, ProlineDatabaseType }
import fr.profi.util.ThreadLogger
import fr.profi.util.resources._
import fr.profi.util.sql.getTimeAsSQLTimestamp
import fr.proline.core.orm.uds.{ UserAccount => UdsUser }
import fr.profi.util.security._

/**
 * @author David Bouyssie
 *
 */
class SetupProline(prolineConfig: ProlineSetupConfig, udsDbConnector: IDatabaseConnector) extends LazyLogging {

  private var localConnector = false

  def this(prolineConfig: ProlineSetupConfig) = {
    this(prolineConfig, prolineConfig.udsDBConfig.toNewConnector)

    localConnector = true
  }

  def run() {

    val currentThread = Thread.currentThread

    if (!currentThread.getUncaughtExceptionHandler.isInstanceOf[ThreadLogger]) {
      currentThread.setUncaughtExceptionHandler(new ThreadLogger(logger.underlying.getName))
    }

    try {
      // Set Up the UDSdb
      logger.info("setting up the 'User Data Set' database...")
      setupDbFromDataset(udsDbConnector, prolineConfig.udsDBConfig, "/dbunit_init_datasets/uds-db_dataset.xml")

      tryInTransaction(udsDbConnector, { udsEM =>

        // Import Admin information
        _importAdminInformation(udsEM)
        logger.info("Admin information imported !")

        // Import external DBs connections
        _importExternalDBs(udsEM)
        logger.info("External databases connection settings imported !")

      })
      tryInTransaction(udsDbConnector, { udsEM =>
        createDefaultAdmin(udsEM)
      })

    } finally {
      if (localConnector) {
        // Release the connector
        udsDbConnector.close()
      }
    }

    // Set Up the PSdb
    logger.info("setting up the 'Peptide Sequence' database...")
    setupDbFromDataset(prolineConfig.psDBConfig, "/dbunit_init_datasets/ps-db_dataset.xml")

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
    setupDbFromDataset(prolineConfig.pdiDBConfig, "/dbunit_init_datasets/pdi-db_dataset.xml")

    logger.info("Proline has been successfully set up !")
  }
  // create default user Admin
  private def createDefaultAdmin(udsEM: EntityManager) {
    try {
      val query = udsEM.createQuery("select user from UserAccount user where user.login='admin'")
      val listUsers = query.getResultList()
      if (listUsers.isEmpty) {
        logger.info("Creating default admin user")
        val udsUser = new UdsUser()
        udsUser.setLogin("admin")
        udsUser.setPasswordHash(sha256Hex("proline"))
        udsUser.setCreationMode("MANUAL")
        var serializedPropertiesMap = new java.util.HashMap[String, Object]
        serializedPropertiesMap.put("user_group", UdsUser.UserGroupType.ADMIN.name())
        udsUser.setSerializedPropertiesAsMap(serializedPropertiesMap)
        udsEM.persist(udsUser)
        logger.info("Default admin user has been created succefully !")
      }
    } catch {
      case t: Throwable => logger.error("error while creating default admin user", t)
    }
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

  // Parse config if it's not already done
  // TODO: rename to initialConfig
  lazy val config = getUpdatedConfig()

  def getUpdatedConfig() = _parseProlineSetupConfig(getConfigParams)

  private def _parseProlineSetupConfig(config: Config): ProlineSetupConfig = {

    // Load proline main settings
    val prolineConfig = config.getConfig("proline-config")

    // Load shared settings
    val authConfig = config.getConfig("auth-config")
    val hostConfig = config.getConfig("host-config")
    val driverAlias = prolineConfig.getString("driver-type")
    // TODO: add default driver config to make it optional
    val driverConfig = config.getConfig(driverAlias + "-config")
    val driver = DriverType.valueOf(driverAlias.toUpperCase())

    val dataDirStrOpt = if (driver == DriverType.POSTGRESQL) None else Some(prolineConfig.getString("data-directory"))
    val dataDirOpt = dataDirStrOpt.map(new File(_))

    val dsDbNaming = GetDataStoreDbNaming(driver)

    val dbNamingMapping = Map(
      "uds" -> dsDbNaming.udsDbName,
      "pdi" -> dsDbNaming.pdiDbName,
      "ps" -> dsDbNaming.psDbName,
      "msi" -> dsDbNaming.msiDbName,
      "lcms" -> dsDbNaming.lcMsDbName)

    // Load database specific settings
    val dbList = List("uds", "pdi", "ps", "msi", "lcms")
    val dbSetupConfigByType = dbList.map { dbType =>

      // Retrieve settings relative to database connection
      val dbName = dbNamingMapping(dbType)
      val defaultDbConf = ConfigFactory.parseString(s"""connection-properties = {dbName = "$dbName" }""")

      val dbKey = dbType + "-db"
      val dbConfig = if (config.hasPath(dbKey)) config.getConfig(dbKey).withFallback(defaultDbConf)
      else defaultDbConf

      val connectionConfig = dbConfig.getConfig("connection-properties")
      //      val schemaVersion = dbConfig.getString("version")  

      // Merge connection settings with shared settings
      val fullConnConfig = _mergeConfigs(connectionConfig, authConfig, hostConfig, driverConfig.getConfig("connection-properties"))

      val db = ProlineDatabaseType.withPersistenceUnitName(dbType + "db_production")

      // Build the database setup configuration object
      (dbType -> DatabaseSetupConfig(db, driver, dataDirOpt, fullConnConfig))
    } toMap

    ProlineSetupConfig(
      dataDirectoryOpt = dataDirOpt,
      udsDBConfig = dbSetupConfigByType("uds"),
      pdiDBConfig = dbSetupConfigByType("pdi"),
      psDBConfig = dbSetupConfigByType("ps"),
      msiDBConfig = dbSetupConfigByType("msi"),
      lcmsDBConfig = dbSetupConfigByType("lcms") //prolineServerConfigFile= serverConfigFileOpt
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



