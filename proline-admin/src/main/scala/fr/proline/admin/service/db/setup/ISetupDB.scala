package fr.proline.admin.service.db.setup

import java.io.File
import scala.io.Source
import com.googlecode.flyway.core.Flyway
import com.weiglewilczek.slf4s.Logging
import fr.proline.admin.service.DatabaseSetupConfig
import fr.proline.core.dal.DatabaseManagement
import fr.proline.core.utils.io._
import fr.proline.repository.DatabaseConnector

/**
 * @author David Bouyssie
 *
 */
trait ISetupDB extends Logging {  
  
  val config: DatabaseSetupConfig
  val dbManager: DatabaseManagement
  private var _executed = false
  
  // Interface
  protected def importDefaults()
  
   /** Execution state. */
  def isExecuted = this._executed
  
  /** Execute the setup of the database. */
  def run() {
    if( _executed )
      throw new IllegalStateException("the setup has been already executed")
    
    this.initSchema( config.connector, config.scriptDirectory )
    
    // TODO: store Admin Information
    
    this.importDefaults()
    
    this._executed = true
  }
  
  protected def initSchema( connector: DatabaseConnector, scriptDirectory: File ) {
    
    // If driver type is SQLite (flyway doesn't support SQLite at the moment)
    if( config.driverType == "sqlite" ) {
      val firstScript = scriptDirectory.listFiles.filter(_.getName.endsWith(".sql")).first
      createSQLiteDB(connector,firstScript)
    } else {
      val flyway = new Flyway()
      flyway.setBaseDir(scriptDirectory.toString())
      flyway.setDataSource(connector.getDataSource)
      flyway.migrate()
    }

  }
  
  protected def createSQLiteDB( connector: DatabaseConnector, scriptFile: File ) {
    
    // Check that database script exists and as a valid extension
    require( scriptFile.exists && scriptFile.isFile() && scriptFile.getName.endsWith(".sql") )
    
    // If connection mode is file
    var createDB = true
    if( config.connectionConfig.getString("connectionMode") == "FILE" ) {
      
      val dbPath = config.dataDirectory + "/"+ config.connectionConfig.getString("dbName")
      if( new File(dbPath).exists == true ) {
        this.logger.warn("database file already exists")
        createDB = false
      }
      else
        this.logger.info("create new database file: "+dbPath)        
    }
    
    if( createDB ) {
      val dbConn = connector.getConnection
      val stmt = dbConn.createStatement
      
      Source.fromFile(scriptFile).eachLine(";", sqlQuery => {
        stmt.executeUpdate(sqlQuery)
      })
      
      stmt.close()
      dbConn.close()
    }
          
  }
  
}
