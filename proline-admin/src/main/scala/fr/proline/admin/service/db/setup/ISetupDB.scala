package fr.proline.admin.service.db.setup

import java.io.{ File, InputStream }
import scala.io.Source
import com.googlecode.flyway.core.Flyway
import com.weiglewilczek.slf4s.Logging
import fr.proline.admin.helper.sql._
import fr.proline.repository.{ IDatabaseConnector, DatabaseUpgrader, DriverType }
import fr.proline.util.io._
import fr.proline.util.resources._
import fr.proline.util.ThreadLogger

/**
 * @author David Bouyssie
 *
 */
trait ISetupDB extends Logging {

  val dbConfig: DatabaseSetupConfig
  private var _executed = false

  // Interface
  protected def importDefaults(): Unit

  /** Execution state. */
  def isExecuted = this._executed

  /** Execute the setup of the database. */
  def run() {
    if (_executed)
      throw new IllegalStateException("the setup has been already executed")

    val currentThread = Thread.currentThread

    if (!currentThread.isInstanceOf[ThreadLogger]) {
      currentThread.setUncaughtExceptionHandler(new ThreadLogger("fr.proline.admin.service.db.setup.ISetupDB"))
    }

    try {
      if (this.initSchema()) {
        this.importDefaults()
        logger.info("database '" + dbConfig.dbName + "' successfully set up !")
      }
    } catch {

      case ex: Exception => {
        logger.error(dbConfig.dbName + " schema initialization failed", ex)
      }

    }

    this._executed = true
  }

  protected def initSchema(): Boolean = {

    val dbConnector = dbConfig.connector

    // Create database if driver type is PostgreSQL
    if (dbConfig.driverType == DriverType.POSTGRESQL) {
      createPgDatabase(dbConfig, Some(this.logger))
    }

    // Initialize database schema
    //    dbConfig.schemaVersion = DatabaseUpgrader.upgradeDatabase(dbConnector);    
    //    if ((dbConfig.schemaVersion == null) || (dbConfig.schemaVersion.isEmpty()) || dbConfig.schemaVersion.equals("no.version")) false else true
    dbConfig.schemaVersion = "0.1"
    if (DatabaseUpgrader.upgradeDatabase(dbConnector) > 0) true else false

    /*    
    // If driver type is SQLite (flyway doesn't support SQLite at the moment)
    if( dbConfig.driverType == DriverType.SQLITE ) {
      
      val scriptPath = dbConfig.scriptDirectory + "/" + dbConfig.scriptName
      this.logger.info( "executing SQL script '"+ scriptPath +"'")
      
      val scriptIS = pathToStreamOrResourceToStream(scriptPath,classOf[IDatabaseConnector])
      createSQLiteDB(connector,scriptIS)
      
    } else {
      
      // Create database if driver type is PostgreSQL
      if( dbConfig.driverType == DriverType.POSTGRESQL ) {
        val pgDbConnector = dbConfig.toNewConnector
        createPgDatabase( pgDbConnector, dbConfig.dbName, Some(this.logger) )
      }
      
      this.logger.info("updating database schema...")
      
      val flyway = new Flyway()
      // TODO: find a workaround for absolute paths
      flyway.setLocations( dbConfig.scriptDirectory + "/" )
      flyway.setDataSource(connector.getDataSource)
      
      if( flyway.migrate() > 0 ) true
      else false
    }*/

  }

  /*protected def createSQLiteDB( connector: IDatabaseConnector, scriptIS: InputStream ): Boolean = {
    
    // If connection mode is file
    var createDB = true
    if( dbConfig.connectionConfig.getString("connectionMode") == "FILE" ) {
      
      val dbPath = dbConfig.dbDirectory + "/"+ dbConfig.connectionConfig.getString("dbName")
      
      if( new File(dbPath).exists == true ) {
        this.logger.warn("database file already exists")
        createDB = false
      }
      else
        this.logger.info("create new database file: "+dbPath)
    }
    
    if( createDB ) {
      val dbConn = connector.getDataSource.getConnection
      val stmt = dbConn.createStatement
      
      this.logger.info("creating database schema...")
      Source.fromInputStream(scriptIS).eachLine(";", stmt.executeUpdate(_) )
      
      stmt.close()
      dbConn.close()
    }
    
    createDB
          
  }*/

}
