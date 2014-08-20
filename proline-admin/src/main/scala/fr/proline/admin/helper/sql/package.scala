package fr.proline.admin.helper

import java.io.File
import java.io.InputStream
import java.sql.Connection
import java.sql.DriverManager
import javax.persistence.EntityManager
import javax.persistence.EntityTransaction
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import org.dbunit.DataSourceDatabaseTester
import org.dbunit.database.DatabaseConfig
import org.dbunit.database.DatabaseSequenceFilter
import org.dbunit.dataset.AbstractDataSet
import org.dbunit.dataset.Column.AutoIncrement
import org.dbunit.dataset.FilteredDataSet
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder
import org.dbunit.operation.DatabaseOperation
import org.postgresql.Driver
import org.postgresql.util.PSQLException
import com.typesafe.scalalogging.slf4j.Logger
import com.typesafe.scalalogging.slf4j.Logging
import fr.profi.util.StringUtils
import fr.profi.util.primitives._
import fr.profi.util.resources._
import fr.proline.admin.service.db.setup.DatabaseSetupConfig
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.dal.ContextFactory
import fr.proline.core.dal.context._
import fr.proline.core.dal.DoJDBCWork
import fr.proline.core.dal.tables.lcms.LcmsDb
import fr.proline.core.dal.tables.msi.MsiDb
import fr.proline.core.dal.tables.pdi.PdiDb
import fr.proline.core.dal.tables.ps.PsDb
import fr.proline.core.dal.tables.uds.UdsDb
import fr.proline.repository.DatabaseUpgrader
import fr.proline.repository.DriverType
import fr.proline.repository.IDatabaseConnector
import fr.proline.repository.ProlineDatabaseType

/**
 * @author David Bouyssie
 *
 */
package object sql extends Logging {
  
  val sqlTypeByName = getSqlTypeByName()
  
  def getSqlTypeByName(): Map[String,Int] = {
    val mapBuilder = Map.newBuilder[String,Int]

    // Get all field in java.sql.Types
    val fields = classOf[java.sql.Types].getFields
    for ( i <- 0 until fields.length ) {
      val name = fields(i).getName()
      val value = fields(i).get(null).asInstanceOf[Int]
      mapBuilder += (name -> value)
    }
    
    mapBuilder.result()
  }
  
  def initDbSchema( dbConnector: IDatabaseConnector, dbConfig: DatabaseSetupConfig ): Boolean = {

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
  
  // TODO: retrieve the datasetPath from the config ?
  def setupDbFromDataset( dbConfig: DatabaseSetupConfig, datasetPath: String ) {
    
    // Create connector
    val connector = dbConfig.toNewConnector()
    
    try {
      setupDbFromDataset( connector, dbConfig, datasetPath )
    } finally {
      connector.close()
    }
    
  }
  
  // Inspired from: http://www.marcphilipp.de/blog/2012/03/13/database-tests-with-dbunit-part-1/
  def setupDbFromDatasetV1( dbConnector: IDatabaseConnector, dbConfig: DatabaseSetupConfig, datasetPath: String ) {
    
    if( initDbSchema( dbConnector, dbConfig ) ) {
      logger.info(s"schema initiated for database '${dbConfig.dbName}'")
      
      // Load the dataset
      //val dataLoader = new FlatXmlDataFileLoader()
      //val dataSet = dataLoader.load(datasetName)
      val datasetBuilder = new FlatXmlDataSetBuilder()
      datasetBuilder.setColumnSensing(true)
      
      val dsInputStream = this.getClass().getResourceAsStream(datasetPath)
      val dataSet = datasetBuilder.build(dsInputStream)
      
      // Connect to the data source
      val dataSource = dbConnector.getDataSource()
      val dbTester = new DataSourceDatabaseTester(dbConnector.getDataSource())
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
      
      logger.info("database '" + dbConfig.dbName + "' successfully set up !")
    }
    
  }
  
  def setupDbFromDataset( dbConnector: IDatabaseConnector, dbConfig: DatabaseSetupConfig, datasetPath: String ) {
    
    if( initDbSchema( dbConnector, dbConfig ) == false ) return ()
    
    logger.info(s"schema initiated for database '${dbConfig.dbName}'")
       
    val recordsByTableName = _parseDbUnitDataset(datasetPath)
    // TODO: try to retrieve the table meta-data from the database ???
    val colNamesByTableName = _getColNamesByTableName(dbConnector.getProlineDatabaseType)
    val insertQueryByTableName = _getInsertQueryByTableName(dbConnector.getProlineDatabaseType)
    
    // FIXME: this step is too slow (DbUnit issue ?) => find a workaround
    // => one workaround is to disable all FK constraint (see disableForeignKeyConstraints method)
    // but this can't be used currently with Postgres because constraints are "NOT DEFERRABLE" by default
    val filteredDataset = _getFilteredDataset(dbConnector,dbConfig.driverType,datasetPath )
    val sortedTableNames: Array[String] = filteredDataset.getTableNames
    
    val sqlContext = ContextFactory.buildDbConnectionContext(dbConnector, false)
    
    try {
      
      sqlContext.tryInTransaction {
        
        //this.disableForeignKeyConstraints(sqlContext)
        
        DoJDBCWork.withEzDBC(sqlContext, { ezDBC =>
          
          for( tableName <- sortedTableNames; if recordsByTableName.contains(tableName) ) {
          //for( tableName <- recordsByTableName.keys ) {
            
            //val tableMetaData = filteredDataset.getTableMetaData(tableName)
            val colNames = colNamesByTableName(tableName)
            val insertQuery = insertQueryByTableName(tableName)
            val records = recordsByTableName(tableName)
            
            ezDBC.executePrepared(insertQuery, false) { statement =>
              
              //val jdbcStmt = statement.jdbcPrepStmt
              
              for( record <- records ) {
                
                for( colName <- colNames ) {
                  //val colName = col.getColumnName()
                  
                  if( colName != "ID" ) {
                    
                    if( record.contains(colName) == false ) {
                      statement.addNull()
                      //val sqlTypeAsInt = sqlTypeByName( col.getSqlTypeName() )
                      //jdbcStmt.setNull(paramIdx, sqlTypeAsInt)
                    } else {
                      val valueAsStr = record(colName)
                      val parsedValue = parseString(valueAsStr)
                      
                      parsedValue match {
                        case b: Boolean => statement.addBoolean(b)
                        case d: Double => statement.addDouble(d)
                        case f: Float => statement.addFloat(f)
                        case i: Int => statement.addInt(i)
                        case l: Long => statement.addLong(l)
                        case s: String => statement.addString(s)
                        case dt: java.util.Date => statement.addTimestamp(new java.sql.Timestamp(dt.getTime))
                      }
                      
                      /*col.getDataType match {
                        case DataType.BOOLEAN => jdbcStmt.setBoolean(paramIdx, value.toBoolean)
                        case DataType.INTEGER => jdbcStmt.setInt(paramIdx, value.toInt)
                        case DataType.REAL => jdbcStmt.setFloat(paramIdx, value.toFloat)
                        case DataType.FLOAT => jdbcStmt.setFloat(paramIdx, value.toFloat)
                        case _ => "not yet implemented data type"
                      }*/
                    }
                  }
                } // End of col iteration
                
                statement.execute()
              }
            }
          }
  
        }) // END OF DoJDBCWork.withEzDBC
        
      } // END OF tryInTransaction
      
      logger.info("database '" + dbConfig.dbName + "' successfully set up !")
      
    } finally {
      
      if( sqlContext != null ) {
        logger.info(s"Closing connection context for database '${dbConfig.dbName}'")
        sqlContext.close()
      }
      
    }
    
  }
  
  private def _getInsertQueryByTableName( dbType: ProlineDatabaseType ): Map[String,String] =  {
    
    val tableInsertQueryByName = dbType match {
      case ProlineDatabaseType.LCMS => {
        for( table <- LcmsDb.tables )
          yield table.name.toUpperCase() -> table.mkInsertQuery((t,c) => c.filter(_.toString != "id"))
      }
      case ProlineDatabaseType.MSI => {
        for( table <- MsiDb.tables )
          yield table.name.toUpperCase() -> table.mkInsertQuery((t,c) => c.filter(_.toString != "id"))
      }
      case ProlineDatabaseType.PDI => {
        for( table <- PdiDb.tables )
          yield table.name.toUpperCase() -> table.mkInsertQuery((t,c) => c.filter(_.toString != "id"))

      }
      case ProlineDatabaseType.PS => {
        for( table <- PsDb.tables )
          yield table.name.toUpperCase() -> table.mkInsertQuery((t,c) => c.filter(_.toString != "id"))
      }
      case ProlineDatabaseType.UDS => {
        for( table <- UdsDb.tables )
          yield table.name.toUpperCase() -> table.mkInsertQuery((t,c) => c.filter(_.toString != "id"))
      }
      case ProlineDatabaseType.SEQ => throw new Exception("Not yet implemented !")
    }
    
    tableInsertQueryByName.toMap
  }
  
  private def _getColNamesByTableName( dbType: ProlineDatabaseType ): Map[String,List[String]] =  {
    
    val tableInsertQueryByName = dbType match {
      case ProlineDatabaseType.LCMS => {
        for( table <- LcmsDb.tables )
          yield table.name.toUpperCase() -> table.columnsAsStrList.map(_.toUpperCase())
      }
      case ProlineDatabaseType.MSI => {
        for( table <- MsiDb.tables )
          yield table.name.toUpperCase() -> table.columnsAsStrList.map(_.toUpperCase())
      }
      case ProlineDatabaseType.PDI => {
        for( table <- PdiDb.tables )
          yield table.name.toUpperCase() -> table.columnsAsStrList.map(_.toUpperCase())
      }
      case ProlineDatabaseType.PS => {
        for( table <- PsDb.tables )
          yield table.name.toUpperCase() -> table.columnsAsStrList.map(_.toUpperCase())
      }
      case ProlineDatabaseType.UDS => {
        for( table <- UdsDb.tables )
          yield table.name.toUpperCase() -> table.columnsAsStrList.map(_.toUpperCase())
      }
      case ProlineDatabaseType.SEQ => throw new Exception("Not yet implemented !")
    }
    
    tableInsertQueryByName.toMap
  }
  
  private def _parseDbUnitDataset( datasetPath: String ): Map[String,ArrayBuffer[Map[String,String]]] = {
    
    // Workaround for issue "Non-namespace-aware mode not implemented"
    // We use the javax SAXParserFactory with a custom configuration
    // Source:  http://stackoverflow.com/questions/11315439/ignore-dtd-specification-in-scala
    val saxParserFactory = javax.xml.parsers.SAXParserFactory.newInstance()
    saxParserFactory.setValidating(false)

    // Instantiate the XML loader using the javax SAXParser
    val xmlLoader = xml.XML.withSAXParser(saxParserFactory.newSAXParser)

    // Load the dataset
    val xmlDoc = xmlLoader.loadFile( pathToFileOrResourceToFile(datasetPath,this.getClass) )
    
    val recordsByTableName = new HashMap[String,ArrayBuffer[Map[String,String]]]
    
    // Iterate over dataset nodes
    for( xmlNode <- xmlDoc.child ) {
      
      val attrs = xmlNode.attributes
      
      // Check node has defined attributes
      if( attrs.isEmpty == false ) {
        
        val tableName = xmlNode.label
        
        val recordBuilder = Map.newBuilder[String,String]
        
        // Iterate over node attributes
        for( attr <- attrs ) {
          val str = attr.value.text
          recordBuilder += attr.key -> attr.value.text
        }
        
        // Append record to the records of this table
        val records = recordsByTableName.getOrElseUpdate(tableName, new ArrayBuffer[Map[String,String]]() )
        records += recordBuilder.result()
        
      }
    }
    
    recordsByTableName.toMap
  }
        
  // Inspired from: http://www.marcphilipp.de/blog/2012/03/13/database-tests-with-dbunit-part-1/
  private def _getFilteredDataset( dbConnector: IDatabaseConnector, driverType: DriverType, datasetPath: String ): AbstractDataSet = {
    
    val datasetBuilder = new FlatXmlDataSetBuilder()
    datasetBuilder.setColumnSensing(true)
    
    val dataSet = datasetBuilder.build( pathToFileOrResourceToFile(datasetPath,this.getClass) )
    
    // Connect to the data source
    val dataSource = dbConnector.getDataSource()
    val dbTester = new DataSourceDatabaseTester(dbConnector.getDataSource())
    val dbUnitConn = dbTester.getConnection()
    
    // Tell DbUnit to be case sensitive
    dbUnitConn.getConfig.setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, true)
    
    // Filter the dataset if the driver is not SQLite
    val filteredDS = if( driverType == DriverType.SQLITE ) dataSet
    else {
      new FilteredDataSet( new DatabaseSequenceFilter(dbUnitConn), dataSet)
    }
    
    dbUnitConn.close()
    
    filteredDS 
  }
  
  def tryInTransaction( dbConnector: IDatabaseConnector, txWork: EntityManager => Unit ) {
    
    val emf = dbConnector.getEntityManagerFactory()
    val em = emf.createEntityManager()
    var transaction: EntityTransaction = null
    var isTxOK: Boolean = false
    
    try {
      
      // Begin transaction
      transaction = em.getTransaction
      transaction.begin()
      isTxOK = false
      
      // Execute work
      txWork(em)

      // Commit transaction
      if( transaction.isActive )
        transaction.commit()
      
      isTxOK = true
      
    } finally {
      
      val dbType = dbConnector.getProlineDatabaseType()

      if ( (transaction != null) && !isTxOK && dbConnector.getDriverType() != DriverType.SQLITE ) {
        logger.info(s"Rollbacking '${dbType}db' EntityTransaction")

        try {
          transaction.rollback()
        } catch {
          case ex: Exception => logger.error(s"Error rollbacking '${dbType}db' EntityTransaction")
        }

      }

      if (em != null) {
        try {
          em.close()
        } catch {
          case exClose: Exception => logger.error(s"Error closing '${dbType}' EntityManager")
        }
      }

    }
    
  }

  protected def createPgDatabase(pgDbConnector: IDatabaseConnector, dbConfig: DatabaseSetupConfig, logger: Option[Logger] = None) {
    
    // Create database connection and statement
    val pgDbConn = {
      try {
        pgDbConnector.getDataSource.getConnection
      } catch {
        case psqle: PSQLException => {
          val pgClass = classOf[org.postgresql.Driver]
          
          val connConfig = dbConfig.connectionConfig
          val host = connConfig.getString("host")
          val port = connConfig.getString("port")
          require( StringUtils.isNotEmpty(port), "missing port value" )
          val portAsInteger = port.toInt
          
          val templateURL = if( portAsInteger >= 0 && portAsInteger <= 65535 )
            s"jdbc:postgresql://${host}:${port}/template1"
          else
            s"jdbc:postgresql://${host}/template1"
          
          logger.map( _.info(s"creating database from template '${templateURL}'...") )

          val pgTemplateConn = DriverManager.getConnection(
            templateURL,
            connConfig.getString("user"),
            connConfig.getString("password")
          )
          val stmt = pgTemplateConn.createStatement
          
          // Create database if it doesn't exists
          if (_checkDbExists(stmt, dbConfig.dbName) == false) {
            logger.map( _.info(s"creating database '${dbConfig.dbName}'...") )
            stmt.executeUpdate(s"CREATE DATABASE ${dbConfig.dbName};")
          }

          // Close database connection and statement
          stmt.close()
          pgTemplateConn.close()
          
          pgDbConnector.getDataSource.getConnection
        }
      } 
    }.asInstanceOf[Connection]

    val stmt = pgDbConn.createStatement
    
    // Check that database has been created
    if (_checkDbExists(stmt, dbConfig.dbName) == false)
      throw new Exception(s"can't create database '${dbConfig.dbName}'")

    // Close database connection and statement
    stmt.close()
    pgDbConn.close()
  }

  private def _checkDbExists(stmt: java.sql.Statement, dbName: String): Boolean = {
    val jdbcRS = stmt.executeQuery(s"SELECT count(*) FROM pg_catalog.pg_database WHERE datname = '${dbName}'")

    if (jdbcRS.next() && jdbcRS.getInt(1) == 0) false else true
  }
  
  // Inspired from: https://github.com/crazycode/play-factory-boy/blob/master/factory-boy/src/util/DatabaseUtil.java
  protected def disableForeignKeyConstraints( dbContext: DatabaseConnectionContext ) {
    
    DoJDBCWork.withEzDBC(dbContext, { ezDBC =>
      val driverType = dbContext.getDriverType    
      
      driverType match {
        case DriverType.H2 => ezDBC.execute("SET REFERENTIAL_INTEGRITY FALSE")
        case DriverType.POSTGRESQL => ezDBC.execute("SET CONSTRAINTS ALL DEFERRED")
        case DriverType.SQLITE => ezDBC.execute("PRAGMA foreign_keys = OFF;")
      }
    })
    
  }
    
  def enableForeignKeyConstraints( dbContext: DatabaseConnectionContext ) {
    
    DoJDBCWork.withEzDBC(dbContext, { ezDBC =>
      val driverType = dbContext.getDriverType    
      
      driverType match {
        case DriverType.H2 => ezDBC.execute("SET REFERENTIAL_INTEGRITY TRUE")
        case DriverType.POSTGRESQL => ()
        case DriverType.SQLITE => ezDBC.execute("PRAGMA foreign_keys = ON;")
      }
    })
    
  }

}