package fr.proline.admin.service.db

import java.io.File
import com.weiglewilczek.slf4s.Logging
import com.typesafe.config.{Config,ConfigFactory,ConfigList}
import fr.proline.admin.service.db.setup._
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.repository.{IDatabaseConnector,ProlineDatabaseType,DriverType}
import fr.proline.util.resources._

class DatabaseConnectionContext( val dbConnector: IDatabaseConnector ) {
  
  import fr.proline.core.dal.ProlineEzDBC
  
  // Entity Manager
  private var _emOpened: Boolean = false
  def isEmOpened() = _emOpened
    
  lazy val entityManager = {
    if( dbConnector == null )
      throw new Exception("can't create an entity manager creation with a null database connector")
    
    if( _emOpened == true )
      throw new Exception("can't create an entity manager after opening a JDBC connection")
    
    _emOpened = true
    dbConnector.getEntityManagerFactory.createEntityManager()
  }
  
  def closeEM() = if( _emOpened && entityManager.isOpen() == true ) {
    entityManager.close()
    _emOpened = false
  }
  
  // JDBC connection
  private var _connectionOpened: Boolean = false
  def isConnectionOpened() = _connectionOpened
  
  lazy val connection = {
    if( dbConnector == null )
      throw new Exception("can't open a JDBC connection with a null database connector")
    
    if( _emOpened == true )
      throw new Exception("can't open a JDBC connection after creation of an entity manager")
    
    _connectionOpened = true
    dbConnector.getDataSource().getConnection()
  }
  
  lazy val ezDBC = ProlineEzDBC(connection,dbConnector.getDriverType)
  
  def closeConnection() = if( _connectionOpened && connection.isClosed() == false ) {
    connection.close
    _connectionOpened = false
  }
  
  def closeAll() = {
    this.closeEM()
    this.closeConnection()
  }
  
}

class ProlineDatabaseContext(
  val udsDbContext: DatabaseConnectionContext,
  val psDbContext: DatabaseConnectionContext,
  val pdiDbContext: DatabaseConnectionContext,
  val msiDbContext: DatabaseConnectionContext,
  val lcmsDbContext: DatabaseConnectionContext
  ) {

  def this( dsConnectorFactory: DataStoreConnectorFactory,
            msiDbConnector: IDatabaseConnector = null,
            lcmsDbConnector: IDatabaseConnector = null ) {
  this( new DatabaseConnectionContext(dsConnectorFactory.getUdsDbConnector),
        new DatabaseConnectionContext(dsConnectorFactory.getPsDbConnector),
        new DatabaseConnectionContext(dsConnectorFactory.getPdiDbConnector),
        new DatabaseConnectionContext(msiDbConnector),
        new DatabaseConnectionContext(lcmsDbConnector)
      )
  }

  
  def closeAll() {
    udsDbContext.closeAll()
    psDbContext.closeAll()
    pdiDbContext.closeAll()
    msiDbContext.closeAll()
    lcmsDbContext.closeAll()
  }
  
}


/**
 * @author David Bouyssie
 *
 */
class SetupProline( config: ProlineSetupConfig ) extends Logging {
  
  def run() {
    
    // Instantiate a database manager
    //val dbManager = DatabaseManager.getInstance()
    //dbManager.initialize(config.udsDBConfig.connector)
    
    // Set Up the UDSdb
    this.logger.info("setting up the 'User Data Set' database...")
    val udsDbContext = new DatabaseConnectionContext( config.udsDBConfig.connector )
    new SetupUdsDB( udsDbContext, config.udsDBConfig, config ).run()
    udsDbContext.closeAll()
    
    // Set Up the PSdb
    this.logger.info("setting up the 'Peptide Sequence' database...")
    val psDbContext = new DatabaseConnectionContext( config.psDBConfig.connector )
    new SetupPsDB( psDbContext, config.psDBConfig ).run()
    psDbContext.closeAll()
    
    // Close PSdb connections
    // TODO: add this to the db manager closeAll method ?
    /*if( dbManager.psEMF.isOpen ) {
      dbManager.psEMF.close()
      dbManager.psDBConnector.closeAll()
    }*/
    
    // Set Up the PDIdb
    this.logger.info("setting up the 'Protein Database Index' database...")
    val pdiDbContext = new DatabaseConnectionContext( config.pdiDBConfig.connector )
    new SetupPdiDB( pdiDbContext, config.pdiDBConfig, config ).run()
    pdiDbContext.closeAll()
    
    // Close PDIdb connections
    // TODO: add this to the db manager closeAll method ?
    /*if( dbManager.pdiEMF.isOpen ) {
      dbManager.pdiEMF.close()
      dbManager.pdiDBConnector.closeAll()
    }*/
    
    // Release all connections and resources
    pdiDbContext.closeAll()
    
    // TODO: close connectors ?
    
    this.logger.info("Proline has been sucessfuly set up !")
    
  }

}


/** Static Proline setup. 
 * Setup will be performed using application defaults stored in "application.conf"
 */
object SetupProline {
  
  var classLoader = SetupProline.getClass().getClassLoader()
  //ClassLoadergetSystemClassLoader()
  
  val appConf = ConfigFactory.load(classLoader,"application")
  
  lazy val config = parseProlineSetupConfig(appConf)
  
  private def parseProlineSetupConfig( config: Config ): ProlineSetupConfig = {
    
    // Load proline main settings
    val prolineConfig = config.getConfig("proline-config")
    val dataDirStr = prolineConfig.getString("data-directory")
    val dataDir = new File(dataDirStr)
    
    // Load shared settings
    val authConfig = config.getConfig("auth-config")
    val hostConfig = config.getConfig("host-config")
    val driverAlias = prolineConfig.getString("driver-type")
    val driverConfig = config.getConfig(driverAlias + "-config")
    val appDriverSpecificConf = ConfigFactory.load(classLoader,"application-"+driverAlias)
    
    // Load database specific settings
    val dbList = List("uds","pdi","ps","msi","lcms")
    val dbSetupConfigByType = dbList.map { dbType =>
      
      // Retrieve settings relative to database connection
      val dbDriverSpecificConf = appDriverSpecificConf.getConfig(dbType+"-db")
      val dbConfig = config.getConfig(dbType+"-db").withFallback(dbDriverSpecificConf)
      val connectionConfig = dbConfig.getConfig("connection-properties")
//      val schemaVersion = dbConfig.getString("version")  
      
      // Merge connection settings with shared settings
      val fullConnConfig = this._mergeConfigs(connectionConfig,authConfig,hostConfig,driverConfig.getConfig("connection-properties"))
      
      // Build the script directory corresponding to the current database configuration
//      val scriptDir = prolineConfig.getString("db-script-root") +
//                       dbConfig.getString("script-directory") +
//                       driverConfig.getString("script-directory")
//      val scriptName = dbConfig.getString("script-name")
      
      val db = ProlineDatabaseType.withPersistenceUnitName(dbType + "db_production")
      val driver = DriverType.valueOf( driverAlias.toUpperCase() ) //fullConnConfig.getString("driver")
      
      // Build the database setup configuration object
      ( dbType-> DatabaseSetupConfig( db, driver, dataDir, fullConnConfig ) )
    } toMap
    
    ProlineSetupConfig(
      dataDirectory = dataDir,
      udsDBConfig = dbSetupConfigByType("uds"),
      udsDBDefaults = retrieveUdsDBDefaults(),
      pdiDBConfig = dbSetupConfigByType("pdi"),
      pdiDBDefaults = PdiDBDefaults( ConfigFactory.load(classLoader,"pdi_db/resources") ),
      psDBConfig = dbSetupConfigByType("ps"),
      msiDBConfig = dbSetupConfigByType("msi"),
      msiDBDefaults = retrieveMsiDBDefaults(),
      lcmsDBConfig = dbSetupConfigByType("lcms")
    )
    
  }
  
  def retrieveUdsDBDefaults(): UdsDBDefaults = {
    
    UdsDBDefaults(
      ConfigFactory.load(classLoader,"uds_db/resources"),
      ConfigFactory.load(classLoader,"uds_db/instruments")
                   .getConfigList("instruments")
                   .asInstanceOf[java.util.List[Config]],
      ConfigFactory.load(classLoader,"uds_db/peaklist_software")
                   .getConfigList("peaklist_software")
                   .asInstanceOf[java.util.List[Config]],
      ConfigFactory.load(classLoader,"uds_db/quant_methods")
                   .getConfigList("quant_methods")
                   .asInstanceOf[java.util.List[Config]]
   )
  }
  
  def retrieveMsiDBDefaults(): MsiDBDefaults = {
    MsiDBDefaults(
      ConfigFactory.load(classLoader,"msi_db/scorings")
                   .getConfigList("scorings")
                   .asInstanceOf[java.util.List[Config]],
      ConfigFactory.load(classLoader,"msi_db/schemata")
                   .getConfigList("schemata")
                   .asInstanceOf[java.util.List[Config]]
    )
  }
  
  /** Merge Config objects consecutively.
   * 
   */
  private def _mergeConfigs( configs: Config* ): Config = {
    var mergedConfig = configs.head
    
    configs.tail.foreach { config =>
      mergedConfig = mergedConfig.withFallback(config)
    }
    
    mergedConfig
  }
  
  /** Instantiates a SetupProline object and call the run() method. */
  def apply() {
    new SetupProline( parseProlineSetupConfig(appConf) ).run()    
  }
  
}



