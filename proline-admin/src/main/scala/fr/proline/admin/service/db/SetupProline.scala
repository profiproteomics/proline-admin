package fr.proline.admin.service.db

import java.io.File
import com.typesafe.scalalogging.slf4j.Logging
import com.typesafe.config.{ Config, ConfigFactory, ConfigList }
import fr.proline.admin.helper.sql._
import fr.proline.admin.service.db.setup._
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.repository.{ IDatabaseConnector, DatabaseUpgrader, DriverType, ProlineDatabaseType }
import fr.proline.util.resources._
import fr.proline.util.ThreadLogger
import org.dbunit.DataSourceDatabaseTester
import org.dbunit.database.DatabaseConfig
import org.dbunit.database.DatabaseConnection
import org.dbunit.database.DatabaseSequenceFilter
import org.dbunit.dataset.FilteredDataSet
import org.dbunit.operation.DatabaseOperation
import org.dbunit.util.fileloader.FlatXmlDataFileLoader
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder


class ProlineDatabaseContext(
  val udsDbContext: DatabaseConnectionContext,
  val psDbContext: DatabaseConnectionContext,
  val pdiDbContext: DatabaseConnectionContext,
  val msiDbContext: DatabaseConnectionContext,
  val lcmsDbContext: DatabaseConnectionContext) {

  def this(dsConnectorFactory: DataStoreConnectorFactory,
           msiDbConnector: IDatabaseConnector = null,
           lcmsDbConnector: IDatabaseConnector = null) {
    this(new DatabaseConnectionContext(dsConnectorFactory.getUdsDbConnector),
      new DatabaseConnectionContext(dsConnectorFactory.getPsDbConnector),
      new DatabaseConnectionContext(dsConnectorFactory.getPdiDbConnector),
      new DatabaseConnectionContext(msiDbConnector),
      new DatabaseConnectionContext(lcmsDbConnector)
    )
  }

  def closeAll() {
    udsDbContext.close()
    psDbContext.close()
    pdiDbContext.close()
    msiDbContext.close()
    lcmsDbContext.close()
  }

}

/*
class UdsDbInitiator(val driverType: DriverType) extends DatabaseTestCase {

  override def getProlineDatabaseType() = {
    ProlineDatabaseType.UDS
  }

}

class PsDbInitiator(val driverType: DriverType) extends DatabaseTestCase {

  override def getProlineDatabaseType() = {
    ProlineDatabaseType.PS
  }

}

class PdiDbInitiator(val driverType: DriverType) extends DatabaseTestCase {

  override def getProlineDatabaseType() = {
    ProlineDatabaseType.PDI
  }

}

class MsiDbInitiator(val driverType: DriverType) extends DatabaseTestCase {

  override def getProlineDatabaseType() = {
    ProlineDatabaseType.MSI
  }

}

class LcMsDbInitiator(val driverType: DriverType) extends DatabaseTestCase {

  override def getProlineDatabaseType() = {
    ProlineDatabaseType.LCMS
  }

}*/


/**
 * @author David Bouyssie
 *
 */
class SetupProline(config: ProlineSetupConfig) extends Logging {

  def run() {

    val currentThread = Thread.currentThread

    if (!currentThread.getUncaughtExceptionHandler.isInstanceOf[ThreadLogger]) {
      currentThread.setUncaughtExceptionHandler(new ThreadLogger(logger.underlying.getName))
    }

    val connectorFactory = DataStoreConnectorFactory.getInstance()

    if (connectorFactory.isInitialized) {
      throw new IllegalStateException("A DataStoreConnectorFactory is ALREADY initialized : cannot run SetupProline !")
    }

    // Set Up the UDSdb
    logger.info("setting up the 'User Data Set' database...")
    setupDb( config.udsDBConfig, "/dbunit_init_datasets/uds-db_dataset.xml" )
    
    // TODO: re-enable this importation ?
    /*val udsAdminInfos = new UdsAdminInfos()
    udsAdminInfos.setModelVersion(dbConfig.schemaVersion)
    udsAdminInfos.setDbCreationDate(getTimeAsSQLTimestamp)
    //udsAdminInfos.setModelUpdateDate()
    udsAdminInfos.setConfiguration("""{}""")

    udsEM.persist(udsAdminInfos)
    
    logger.info("UDSdb admin information imported !")
    */

    // Set Up the PSdb
    logger.info("setting up the 'Peptide Sequence' database...")
    setupDb( config.psDBConfig, "/dbunit_init_datasets/ps-db_dataset.xml" )
    
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
    setupDb( config.pdiDBConfig, "/dbunit_init_datasets/pdi-db_dataset.xml" )

    logger.info("Proline has been successfully set up !")
  }
  
  // TODO: retrieve the datasetName from the config ?
  // Inspired from: http://www.marcphilipp.de/blog/2012/03/13/database-tests-with-dbunit-part-1/
  def setupDb( dbConfig: DatabaseSetupConfig, datasetName: String ) {
    
    // Create connector
    val connector = dbConfig.toNewConnector()
    
    if( initSchema( dbConfig, connector ) ) {
      logger.info(s"schema initiated for database '${dbConfig.dbName}'")
       
      // Load the dataset
      //val dataLoader = new FlatXmlDataFileLoader()
      //val dataSet = dataLoader.load(datasetName)
      val datasetBuilder = new FlatXmlDataSetBuilder()
      datasetBuilder.setColumnSensing(true)
      
      val dsInputStream = this.getClass().getResourceAsStream(datasetName)
      val dataSet = datasetBuilder.build(dsInputStream)
      
      // Connect to the data source
      val dataSource = connector.getDataSource()
      val dbTester = new DataSourceDatabaseTester(connector.getDataSource())   
      val dbUnitConn = dbTester.getConnection()
      
      // Tell DbUnit to be case sensitive
      dbUnitConn.getConfig.setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, true)
      
      // Filter the dataset if the driver is not SQLite
      val filteredDS = if( dbConfig.driverType == DriverType.SQLITE ) dataSet
      else {
        new FilteredDataSet( new DatabaseSequenceFilter(dbUnitConn) , dataSet)
      }

      // Import the dataset
      dbTester.setDataSet(filteredDS)
      dbTester.setSetUpOperation(DatabaseOperation.CLEAN_INSERT)
      dbTester.onSetup()
      
      dbUnitConn.close()
    }

    connector.close()    
  }
  
  def initSchema( dbConfig: DatabaseSetupConfig, dbConnector: IDatabaseConnector ): Boolean = {

    // Create database if driver type is PostgreSQL
    if (dbConfig.driverType == DriverType.POSTGRESQL) {
      createPgDatabase(dbConnector, dbConfig, Some(logger))
    }

    // Initialize database schema
    // TODO: find an other way to handle the SCHEMA VERSION
    dbConfig.schemaVersion = "0.1"

    val upgradeStatus = if (DatabaseUpgrader.upgradeDatabase(dbConnector) > 0) true else false

    upgradeStatus
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
  lazy val config = parseProlineSetupConfig(getConfigParams)

  private def parseProlineSetupConfig(config: Config): ProlineSetupConfig = {

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

  /** Instantiates a SetupProline object and call the run() method. */
  def apply() {
    new SetupProline(config).run()
  }

}



