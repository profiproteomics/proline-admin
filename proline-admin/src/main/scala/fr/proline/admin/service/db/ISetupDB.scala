package fr.proline.admin.service.db

import java.io.File
import com.googlecode.flyway.core.Flyway
import com.weiglewilczek.slf4s.Logging
import fr.proline.admin.service.DatabaseSetupConfig
import fr.proline.repository.DatabaseConnector

/**
 * @author David Bouyssie
 *
 */
trait ISetupDB extends Logging {
  
  val config: DatabaseSetupConfig
  
  // Interface
  def loadDefaults()
  
  def run() {
    this.initSchema( config.connector, config.scriptDirectory )
    this.loadDefaults()
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
      
      val source = scala.io.Source.fromFile(scriptFile)    
      val blockIter = new LineIterator(source.buffered, ";")
      while( blockIter.hasNext ) {
        val block = blockIter.next()
        stmt.executeUpdate(block)
        //val lines = block.split("\r\n")
      }
      
      dbConn.close()
    }
          
  }
  
}

// TODO: put in some_package.utils.io
/** Source: http://asoftsea.tumblr.com/post/529750770/a-transitional-suitcase-for-source */
class LineIterator(iter: BufferedIterator[Char], separator: String) extends Iterator[String] {
  require(separator.length < 3, "Line separator may be 1 or 2 characters only.")
  
  private[this] val isNewline: Char => Boolean =
    separator.length match {
      case 1 => _ == separator(0)
      case 2 => {
        _ == separator(0) && iter.hasNext && {
          val res = iter.head == separator(1) // peek ahead
          if (res) { iter.next } // incr iter
            res
          }
        }
      }
  
  private[this] val builder = new StringBuilder

  private def buildingLine() = iter.next match {
    case nl if(isNewline(nl)) => false
    case ch =>  { 
      builder append ch
      true
    }
  }

  def hasNext = iter.hasNext
  def next = {
    builder.clear
    while (hasNext && buildingLine()) {}
    builder.toString
  }
}

/*object Transitioning {
  import scala.io.Source
  class TransitionalSource(src: Source) {    
    def lines = new LineIterator(src.buffered, compat.Platform.EOL)
  }
  implicit def src2transitionalSrc(src: Source) = new TransitionalSource(src)
}*/