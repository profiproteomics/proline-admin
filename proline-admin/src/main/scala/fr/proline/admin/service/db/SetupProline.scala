package fr.proline.admin.service.db

import java.io.File
import javax.persistence.EntityManager
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import fr.proline.admin.helper.sql._
import fr.proline.admin.service.db.setup._
import fr.proline.repository.{IDatabaseConnector, DriverType, ProlineDatabaseType}
import fr.profi.util.ThreadLogger
import fr.proline.core.orm.uds.{ UserAccount => UdsUser }
import fr.profi.util.security._


/**
 * @author David Bouyssie
 *
 */
class SetupProline(prolineConfig: ProlineSetupConfig, udsDbConnector: IDatabaseConnector, initUdsPath : String) extends LazyLogging {
  
  private var localConnector = false
  
  def this(prolineConfig: ProlineSetupConfig) = {
    this(prolineConfig, prolineConfig.udsDBConfig.toNewConnector, SetupProline.defaultUdsInitPath)
    
    localConnector = true
  }

  def this(prolineConfig: ProlineSetupConfig, udsDbConnector: IDatabaseConnector) = {
    this(prolineConfig, prolineConfig.udsDBConfig.toNewConnector, SetupProline.defaultUdsInitPath)

    localConnector = true
  }

  def this(prolineConfig: ProlineSetupConfig, newUdsInitPath : String) = {
    this(prolineConfig, prolineConfig.udsDBConfig.toNewConnector, newUdsInitPath)

    localConnector = true
  }

  def run() {

    val currentThread = Thread.currentThread

    if (!currentThread.getUncaughtExceptionHandler.isInstanceOf[ThreadLogger]) {
      currentThread.setUncaughtExceptionHandler(new ThreadLogger(logger.underlying.getName))
    }
    
    try {
      // Set Up the UDSdb
      logger.info("setting up the 'User Data Set' database... using "+initUdsPath)
      setupDbFromDataset( udsDbConnector, prolineConfig.udsDBConfig, initUdsPath )
      
      //create default admin user 
       tryInTransaction(udsDbConnector, { udsEM =>
        createDefaultAdmin(udsEM)
      })
      
    } finally {
      if( localConnector ) {
        // Release the connector
        udsDbConnector.close()
      }
    }
  }
  /** create default admin user */
  private def createDefaultAdmin(udsEM: EntityManager) {
    try {
      val defaultAdminQuery = udsEM.createQuery("select user from UserAccount user where user.login='admin'")
      val defaultAdmin = defaultAdminQuery.getResultList()
      if (defaultAdmin.isEmpty) {
        logger.info("Creating default admin user...")
        val udsUser = new UdsUser()
        udsUser.setLogin("admin")
        udsUser.setPasswordHash(sha256Hex("proline"))
        udsUser.setCreationMode("AUTO")
        var serializedPropertiesMap = new java.util.HashMap[String, Object]
        serializedPropertiesMap.put("user_group", UdsUser.UserGroupType.ADMIN.name())
        udsUser.setSerializedPropertiesAsMap(serializedPropertiesMap)
        udsEM.persist(udsUser)
        if(udsUser.getId > 0L) logger.info("Default admin user has been created successfully!")
      }
    } catch {
      case t: Throwable => logger.error("Error while trying to create default admin user ", t.getMessage)
    }
  }

}

/**
 * Static Proline setup.
 * Setup will be performed using application defaults stored in "application.conf"
 */
object SetupProline {

  var classLoader = SetupProline.getClass().getClassLoader()
  //ClassLoadergetSystemClassLoader()
  val defaultUdsInitPath = "/dbunit_init_datasets/uds-db_dataset.xml"

  private var _appConfParams: Config = null
  
  /** Instantiates a SetupProline object and call the run() method. */
  def apply() {
    new SetupProline(config).run()
  }

  def apply( pathUdsInit : String) : Unit = {
    new SetupProline(config,pathUdsInit).run()
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

    val dsDbNaming = GetDataStoreDefaultDbNaming(driver)
    
    val dbNamingMapping = Map(
      "uds" -> dsDbNaming.udsDbName,
      "msi"-> dsDbNaming.msiDbName,
      "lcms"-> dsDbNaming.lcMsDbName
    )

    // Load database specific settings
    val dbList = dbNamingMapping.keys
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
    }.toMap

    ProlineSetupConfig(
      dataDirectoryOpt = dataDirOpt,
      udsDBConfig = dbSetupConfigByType("uds"),
      msiDBConfig = dbSetupConfigByType("msi"),
      lcmsDBConfig = dbSetupConfigByType("lcms")
      //prolineServerConfigFile= serverConfigFileOpt
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



