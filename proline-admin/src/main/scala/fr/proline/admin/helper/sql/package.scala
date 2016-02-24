package fr.proline.admin.helper

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Collection

import javax.persistence.EntityManager
import javax.persistence.EntityTransaction

import scala.util.Try

import com.typesafe.scalalogging.LazyLogging

import org.dbunit.DataSourceDatabaseTester
import org.dbunit.database.DatabaseConfig
import org.dbunit.database.IDatabaseConnection
import org.dbunit.dataset.AbstractDataSet
import org.dbunit.dataset.datatype.DataType
import org.dbunit.dataset.datatype.DefaultDataTypeFactory
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder
import org.dbunit.ext.h2.H2DataTypeFactory
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory
import org.dbunit.operation.DatabaseOperation
import org.postgresql.Driver

import fr.profi.util.StringUtils
import fr.profi.util.dbunit._
import fr.profi.util.primitives.castToTimestamp
import fr.profi.util.resources._
import fr.proline.admin.service.db.setup.DatabaseSetupConfig
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.dal.BuildDbConnectionContext
import fr.proline.core.dal.DoJDBCWork
import fr.proline.core.dal.context._
import fr.proline.core.dal.tables.lcms.LcmsDb
import fr.proline.core.dal.tables.msi.MsiDb
import fr.proline.core.dal.tables.pdi.PdiDb
import fr.proline.core.dal.tables.ps.PsDb
import fr.proline.core.dal.tables.uds.UdsDb
import fr.proline.core.orm.uds.repository.ProjectRepository
import fr.proline.repository._

/**
 * @author David Bouyssie
 *
 */
package object sql extends LazyLogging {
  
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
      createPgDatabase(dbConnector, dbConfig)
    }

    // Initialize database schema
    // TODO: find an other way to handle the SCHEMA VERSION
    dbConfig.schemaVersion = "0.1"

    val upgradeStatus = if (DatabaseUpgrader.upgradeDatabase(dbConnector, false) > 0) true else false

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
  // WANRING: this method has issue with Pg PK sequence => see https://bioproj.cea.fr/redmine/issues/10643
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
      dsInputStream.close()
      
      // Connect to the data source
      val dataSource = dbConnector.getDataSource()
      val dbTester = createDatabaseTester(dbConnector.getDataSource(), dbConfig.driverType)
      val dbUnitConn = dbTester.getConnection()
      
      // Tell DbUnit to be case sensitive
      //dbUnitConn.getConfig.setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, true)
      
      // Filter the dataset if the driver is not SQLite
      /*val filteredDS = if( dbConfig.driverType == DriverType.SQLITE ) dataSet
      else {
        new FilteredDataSet( new DatabaseSequenceFilter(dbUnitConn) , dataSet)
      }*/
      
      // Import the dataset
      dbTester.setDataSet(dataSet)
      dbTester.setSetUpOperation(DatabaseOperation.CLEAN_INSERT)
      dbTester.onSetup()
      
      dbUnitConn.close()
      
      logger.info("database '" + dbConfig.dbName + "' successfully set up !")
    }
    
  }
  
  def setupDbFromDataset( dbConnector: IDatabaseConnector, dbConfig: DatabaseSetupConfig, datasetPath: String ) {
    
    if( initDbSchema( dbConnector, dbConfig ) == false ) return ()
    
    logger.info(s"schema initiated for database '${dbConfig.dbName}'")
    
    val dsInputStream = this.getClass.getResourceAsStream(datasetPath)
    val recordsByTableName = parseDbUnitDataset( dsInputStream, lowerCase = false )
    dsInputStream.close()
    
    // TODO: try to retrieve the table meta-data from the database ???
    val colNamesByTableName = _getColNamesByTableName(dbConnector.getProlineDatabaseType)
    val insertQueryByTableName = _getInsertQueryByTableName(dbConnector.getProlineDatabaseType)
    
    //val filteredDataset = _getFilteredDataset(dbConnector,dbConfig.driverType,datasetPath )
    //val sortedTableNames: Array[String] = filteredDataset.getTableNames
    
    // Connect to the data source
    val dbTester = createDatabaseTester(dbConnector.getDataSource(), dbConfig.driverType)
    val dbUnitConn = dbTester.getConnection()
    val dbUnitDS = dbUnitConn.createDataSet()
    val tableNamesInDb = dbUnitDS.getTableNames()
    val dbTblNameByUpCasedTblName = tableNamesInDb.map(tbl => tbl.toUpperCase() -> tbl ).toMap
    
    val sqlContext = BuildDbConnectionContext(dbConnector, false)
    
    try {
      
      sqlContext.tryInTransaction {
        
        //this.disableForeignKeyConstraints(sqlContext)
        
        DoJDBCWork.withEzDBC(sqlContext, { ezDBC =>
          
          //for( tableName <- sortedTableNames; if recordsByTableName.contains(tableName) ) {
          //  val records = recordsByTableName(tableName)
          for( (tableName,records) <- recordsByTableName; if records.isEmpty == false ) {
            
            //val tableMetaData = filteredDataset.getTableMetaData(tableName)
            val dbTblName = dbTblNameByUpCasedTblName(tableName)
            val tableMetaData = dbUnitDS.getTableMetaData(dbTblName)
            val cols = tableMetaData.getColumns()
            val dataTypeByColName = cols.map( col => col.getColumnName().toUpperCase() -> col.getDataType() ).toMap
            
            val colNames = colNamesByTableName(tableName)
            val insertQuery = insertQueryByTableName(tableName)
            logger.debug(s"TABLE ${tableName} INSERT query: " + insertQuery)
            
            ezDBC.executePrepared(insertQuery, false) { statement =>
              
              for( record <- records ) {
                
                for( colName <- colNames ) {
                  //val colName = col.getColumnName()
                  
                  if( colName != "ID" ) {
                    
                    val dataType = dataTypeByColName(colName)
                    
                    if( record.contains(colName) == false || record(colName) == null ) {
                      statement.addNull()
                    } else {
                      val valueAsStr = record(colName)
                      
                      /*val parsedValue = parseString(valueAsStr)
                      
                      parsedValue match {
                        case b: Boolean => statement.addBoolean(b)
                        case d: Double => statement.addDouble(d)
                        case f: Float => statement.addFloat(f)
                        case i: Int => statement.addInt(i)
                        case l: Long => statement.addLong(l)
                        case s: String => statement.addString(s)
                        case dt: java.util.Date => statement.addTimestamp(new java.sql.Timestamp(dt.getTime))
                      }*/
                      
                      // For the SQL <-> Java type mapping see http://db.apache.org/ojb/docu/guides/jdbc-types.html                      
                      dataType match {
                        case DataType.BIT => statement.addBoolean(valueAsStr.toBoolean)
                        case DataType.BOOLEAN => statement.addBoolean(valueAsStr.toBoolean)
                        case DataType.DOUBLE => statement.addDouble(valueAsStr.toDouble)
                        case DataType.FLOAT => statement.addFloat(valueAsStr.toFloat)
                        case DataType.REAL => statement.addFloat(valueAsStr.toFloat)
                        case DataType.INTEGER => statement.addInt(valueAsStr.toInt)
                        case DataType.BIGINT => statement.addLong(valueAsStr.toLong)
                        case DataType.CHAR => statement.addString(valueAsStr)
                        case DataType.VARCHAR => statement.addString(valueAsStr)
                        case DataType.LONGVARCHAR => statement.addString(valueAsStr)
                        case DataType.CLOB => statement.addString(valueAsStr)
                        case DataType.DATE => statement.addTimestamp(castToTimestamp(valueAsStr))
                        case DataType.TIMESTAMP => statement.addTimestamp(castToTimestamp(valueAsStr))                        
                        case _ => throw new Exception( "not yet implemented data type: " + dataType )
                      }
                      
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
      
      // Close DbUnit connection
      dbUnitConn.close()
      
      // Close database connection context
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
        
  // Inspired from: http://www.marcphilipp.de/blog/2012/03/13/database-tests-with-dbunit-part-1/
  private def _getFilteredDataset( dbConnector: IDatabaseConnector, driverType: DriverType, datasetPath: String ): AbstractDataSet = {
    
    val datasetBuilder = new FlatXmlDataSetBuilder()
    datasetBuilder.setColumnSensing(true)
    
    //val dataSet = datasetBuilder.build( pathToFileOrResourceToFile(datasetPath,this.getClass) )
    val dataSet = datasetBuilder.build( this.getClass.getResourceAsStream(datasetPath) )
    
    // Connect to the data source
    val dataSource = dbConnector.getDataSource()
    val dbTester = new DataSourceDatabaseTester(dbConnector.getDataSource())
    val dbUnitConn = dbTester.getConnection()
    
    // Tell DbUnit to be case sensitive
    dbUnitConn.getConfig.setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, true)
    
    // FIXME: this step is too slow (DbUnit issue ?) => find a workaround
    // => one workaround is to disable all FK constraint (see disableForeignKeyConstraints method)
    // but this can't be used currently with Postgres because constraints are "NOT DEFERRABLE" by default
    
    // Filter the dataset if the driver is not SQLite
    /*val filteredDS = if( driverType == DriverType.SQLITE ) dataSet
    else {
      new FilteredDataSet( new DatabaseSequenceFilter(dbUnitConn), dataSet)
    }*/
    val filteredDS = dataSet
    
    dbUnitConn.close()
    
    filteredDS 
  }
  
  def tryInTransaction( dbConnector: IDatabaseConnector, txWork: EntityManager => Unit ) {
    
    val em = dbConnector.createEntityManager()
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
  
  protected def createPgDatabase(pgDbConnector: IDatabaseConnector, dbConfig: DatabaseSetupConfig) {
    
    // Create database connection and statement
    logger.info("createPgDatabase: Create database connection and statement")
    val pgDbConn = {
      try {
        pgDbConnector.getDataSource.getConnection
      } catch {
        case psqle: SQLException => {
          val pgClass = classOf[org.postgresql.Driver]
          
          // Create connection template statement to check if database exists
          logger.info("Create connection template statement to check if database exists")
          val pgConnTemplate = _createPgConnectionTemplate(dbConfig)
          val stmt = pgConnTemplate.createStatement
          
          // Create database if it doesn't exists
          if (_checkDbExists(stmt, dbConfig.dbName) == false) {
            logger.info(s"Creating database '${dbConfig.dbName}'...") 
            stmt.executeUpdate(s"CREATE DATABASE ${dbConfig.dbName};")
          }

          // Close database connection and statement
          stmt.close()
          pgConnTemplate.close()

          pgDbConnector.getDataSource.getConnection
        }
        case e: Exception => {
          logger.info("createPgDatabase Exception "+e.getStackTrace);
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


  def checkPgConnection(host: String, port: Int, user: String, password: String): Try[Connection] = Try {

    var connection: Connection = null

    try {
      connection = this._getPgConnectionToTemplate1(host, port, user, password)

    } catch {
      case t: Throwable => throw t
    
    } finally {
      if (connection != null)
        connection.close()
    }

    connection
  }
  
  
  private def _createPgConnectionTemplate(dbConfig: DatabaseSetupConfig): Connection = {

    val connConfig = dbConfig.connectionConfig
    val host = connConfig.getString("host")
    val port = connConfig.getString("port")
    require(StringUtils.isNotEmpty(port), "missing port value")
    logger.info("_createPgConnectionTemplate ")
    
    _getPgConnectionToTemplate1(host, port.toInt, connConfig.getString("user"), connConfig.getString("password") )
  }
  
  private def _getPgConnectionToTemplate1(host: String, port: Int, user: String, password: String): Connection = {
    logger.info("_getPgConnectionToTemplate1 ")
    require(host != null, "DB host must not be null")
    require(user != null, "DB user name must not be null")
    require(password != null, "DB password must not be null")

    val templateURL = if (port >= 0 && port <= 65535)
      s"jdbc:postgresql://${host}:${port}/template1"
    else
      s"jdbc:postgresql://${host}/template1"

    logger.info("_getPgConnectionToTemplate1 "+templateURL)
    DriverManager.getConnection(
      templateURL,
      user,
      password
    )
  }

  private def _checkDbExists(stmt: java.sql.Statement, dbName: String): Boolean = {
    val jdbcRS = stmt.executeQuery(s"SELECT count(*) FROM pg_catalog.pg_database WHERE datname = '${dbName}'")

    if (jdbcRS.next() && jdbcRS.getInt(1) == 0) false else true
  }
  
  def createMissingDatabases(dbConfig: DatabaseSetupConfig, dsConnectorFactory: IDataStoreConnectorFactory): Unit = {
    
    logger.info("Looking for missing databases...")
    
    // Get entity manager
    val udsDbContext = new DatabaseConnectionContext(dsConnectorFactory.getUdsDbConnector())
    val udsEM = udsDbContext.getEntityManager()

    // Create PostGres template connection statement
    val pgConnTemplate = _createPgConnectionTemplate(dbConfig)
    val pgConnTemplateStatement = pgConnTemplate.createStatement

    try {
      // Get projects ids
      val projectIds = ProjectRepository.findAllProjectIds(udsEM)
      
      var createdDbCount = 0

      // Iterate over projects
      import scala.collection.JavaConversions._      
      projectIds.foreach { projectId =>
        
        // Create MSIdb if it does not exist
        if ( _checkDbExists(pgConnTemplateStatement, s"msi_db_project_$projectId") == false) {
          pgConnTemplateStatement.executeUpdate(s"CREATE DATABASE msi_db_project_$projectId;")
          
          logger.debug("Created missing MSI database for project #" + projectId)
          createdDbCount += 1
        }
        
        // Create LCMSdb if it does not exist
        if ( _checkDbExists(pgConnTemplateStatement, s"lcms_db_project_$projectId") == false) {
          pgConnTemplateStatement.executeUpdate(s"CREATE DATABASE lcms_db_project_$projectId;")
          
          logger.debug("Created missing LCMS database for project #" + projectId)
          createdDbCount += 1
        }        
      }
      
      if( createdDbCount > 0 )
        logger.info(s"Created $createdDbCount missing databases.")
      else
        logger.info("No missing database.")

    } finally {
      // Close open resources
      pgConnTemplateStatement.close()
      pgConnTemplate.close()
      udsDbContext.close()      
    }
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
  
  // TODO: remove code redundancy with same method in fr.proline.repository.util.DatabaseTestConnector
  def createDatabaseTester( dataSource: javax.sql.DataSource, driverType: DriverType ) = {
    new DataSourceDatabaseTester(dataSource) {
      
      import DriverType._
      
      override def getConnection(): IDatabaseConnection = {
        val dbUnitConn = super.getConnection()
        
        // Retrieve the IDataTypeFactory corresponding to the DriverType
        val dataTypeFactory = driverType match {
          case H2 => new H2DataTypeFactory()
          case POSTGRESQL => new PostgresqlDataTypeFactory();
          case SQLITE => new DefaultDataTypeFactory() {
            override def getValidDbProducts(): Collection[_] = {
              val products = new java.util.ArrayList[java.lang.String]();
              products.add( new java.lang.String("SQLite") );
              return products;
            }
          }
        }
        
        // Apply the created IDataTypeFactory to the connection config
        val dbUnitConnConfig = dbUnitConn.getConfig()
        dbUnitConnConfig.setProperty(
          DatabaseConfig.PROPERTY_DATATYPE_FACTORY, dataTypeFactory
        )
        
        // Tell Dbunit to be case sensitive
        dbUnitConnConfig.setProperty(
          DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, true
        )
        
        dbUnitConn
      }
    }
  }

}