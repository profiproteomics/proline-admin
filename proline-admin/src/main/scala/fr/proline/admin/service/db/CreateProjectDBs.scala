package fr.proline.admin.service.db

import java.io.File
import com.typesafe.scalalogging.slf4j.Logging
import fr.profi.jdbc.easy.EasyDBC
import fr.proline.admin.helper.sql._
import fr.proline.admin.service.ICommandWork
import fr.proline.admin.service.db.setup.{DatabaseSetupConfig, ProlineSetupConfig, SetupLcmsDB}
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.dal.DoJDBCWork
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.repository.ConnectionMode
import fr.proline.repository.DriverType
import fr.proline.repository.IDatabaseConnector

/**
 * @author David Bouyssie
 *
 */
class CreateProjectDBs(
  udsDbContext: DatabaseConnectionContext,
  config: ProlineSetupConfig, projectId: Long
) extends ICommandWork with Logging {

  def doWork() {
    
    // Prepare MSIdb creation
    val msiDBConfig = this._prepareDBCreation(config.msiDBConfig)

    // Prepare LCMSdb creation
    val lcmsDBConfig = this._prepareDBCreation(config.lcmsDBConfig)

    DoJDBCWork.withEzDBC(udsDbContext, { udsEzDBC =>
      
      // Check that there are no external DBs attached to this project
      val nbExtDbs = udsEzDBC.selectInt(
        "SELECT count(*) FROM external_db, project_db_map " +
        "WHERE project_db_map.external_db_id = external_db.id " +
        "AND project_db_map.project_id = " + projectId
      )
      
      if (nbExtDbs > 0)
        throw new Exception("project of id='%d' is already associated to external databases !".format(projectId))

      // Store MSIdb connection settings
      this._insertExtDb(udsEzDBC, msiDBConfig.toUdsExternalDb)

      // Store LCMSdb connection settings
      this._insertExtDb(udsEzDBC, lcmsDBConfig.toUdsExternalDb)

      // Release UDSdb connection
      //if( wasUdsDbConnectionOpened == false ) udsDbContext.closeConnection()
    })

    val connectorFactory = DataStoreConnectorFactory.getInstance()

    // Create MSI database
    var msiDbConnector: IDatabaseConnector = null
    var localMsiDbConnector: Boolean = false

    if (connectorFactory.isInitialized) {
      msiDbConnector = connectorFactory.getMsiDbConnector(projectId)
    }

    if (msiDbConnector == null) {
      msiDbConnector = msiDBConfig.toNewConnector()
      localMsiDbConnector = true
    }

    try {
      //new SetupMsiDB(msiDbConnector, msiDBConfig, config.msiDBDefaults).run()
      setupDbFromDataset(msiDbConnector, msiDBConfig, "/dbunit_init_datasets/msi-db_dataset.xml")
      
      // TODO: re-enable this importation ?
      /*def _importAdminInformation(msiEM: EntityManager) {

        val msiAdminInfos = new MsiAdminInfos()
        msiAdminInfos.setModelVersion(dbConfig.schemaVersion)
        msiAdminInfos.setDbCreationDate(getTimeAsSQLTimestamp)
        //udsAdminInfos.setModelUpdateDate()
        msiEM.persist(msiAdminInfos)
    
      }*/
      
    // FIXME: remove ExternalDbs and throw new Exception if error occurs
    } finally {

      if (localMsiDbConnector && (msiDbConnector != null)) {
        msiDbConnector.close()
      }

    }

    // Create LCMS database
    var lcmsDbConnector: IDatabaseConnector = null
    var localLcMsDbConnector: Boolean = false

    if (connectorFactory.isInitialized) {
      lcmsDbConnector = connectorFactory.getLcMsDbConnector(projectId)
    }

    if (lcmsDbConnector == null) {
      lcmsDbConnector = lcmsDBConfig.toNewConnector()
      localLcMsDbConnector = true
    }

    try {
      // FIXME: DbUnit throws a CyclicTablesDependencyException  (see issue http://sourceforge.net/p/dbunit/feature-requests/169)
      //setupDbFromDataset(lcmsDbConnector, lcmsDBConfig, "/dbunit_init_datasets/lcms-db_dataset.xml")
      
      new SetupLcmsDB(lcmsDbConnector, lcmsDBConfig).run()
    
    // FIXME: remove ExternalDbs and throw new Exception if error occurs
    } finally {

      if (localLcMsDbConnector && (lcmsDbConnector != null)) {
        lcmsDbConnector.close()
      }

    }

  }

  private def _insertExtDb(udsEzDBC: EasyDBC, extDb: fr.proline.core.orm.uds.ExternalDb) {

    import fr.profi.jdbc.easy._
    import fr.proline.core.dal.tables.uds.{ UdsDbExternalDbTable, UdsDbProjectDbMapTable }

    val extDbInsertQuery = UdsDbExternalDbTable.mkInsertQuery((c, colsList) =>
      colsList.filter(_ != c.ID)
    )
    val projectDbMapInsertQuery = UdsDbProjectDbMapTable.mkInsertQuery()

    udsEzDBC.beginTransaction()

    val extDbId = udsEzDBC.executePrepared(extDbInsertQuery, true) { stmt =>
      stmt.executeWith(extDb.getDbName,
        extDb.getConnectionMode.toString(),
        Option(extDb.getDbUser),
        Option(extDb.getDbPassword),
        Option(extDb.getHost),
        if (extDb.getPort != null) Some(extDb.getPort.toInt) else Option.empty[Int],
        extDb.getType.toString(),
        extDb.getDbVersion(),
        false,
        Option(extDb.getSerializedProperties)
      )
      
      stmt.generatedLong
    }

    // Link external db to the project
    udsEzDBC.execute(projectDbMapInsertQuery, projectId, extDbId)

    // Commit the transaction
    udsEzDBC.commitTransaction()

  }

  private def _prepareDBCreation(dbConfig: DatabaseSetupConfig): DatabaseSetupConfig = {

    dbConfig.connectionMode match {
      case ConnectionMode.FILE => {

        // Create projects directory if not exists
        val projectsDir = CreateProjectDBs.getProjectsDir(dbConfig.dbDirectory)
        if (projectsDir.exists == false) projectsDir.mkdir()

        // Retrieve project directory
        val projectDir = CreateProjectDBs.getProjectDir(projectsDir, this.projectId)
        if (projectDir.exists == false) projectDir.mkdir()

        // Update database config directory
        dbConfig.copy(dbDirectory = projectDir)
      }
      case ConnectionMode.HOST => {

        val newDbConfig = dbConfig.copy()
        newDbConfig.dbName = dbConfig.dbName + "_project_" + this.projectId

        if ((dbConfig.driverType == DriverType.POSTGRESQL) || (dbConfig.driverType == DriverType.H2)) {
          //val pgDbConnector = newDbConfig.toNewConnector()
          //createPgDatabase( pgDbConnector, newDbConfig.dbName, Some(this.logger) )
        } else {
          throw new Exception("NYI : Host db creation is supported only for Postgresql or H2")
        }

        newDbConfig
      }
      case ConnectionMode.MEMORY => {
        throw new Exception("NYI")
        dbConfig
      }
    }

  }

}

object CreateProjectDBs extends Logging {

  def apply(projectId: Long) {

    // Retrieve Proline configuration
    val prolineConf = SetupProline.config

    var localUdsDbConnector: Boolean = false

    val connectorFactory = DataStoreConnectorFactory.getInstance()

    val udsDbConnector = if (connectorFactory.isInitialized) {
      connectorFactory.getUdsDbConnector
    } else {
      // Instantiate a database manager
      val udsDBConfig = prolineConf.udsDBConfig

      val newUdsDbConnector = udsDBConfig.toNewConnector()
      localUdsDbConnector = true
      newUdsDbConnector
    }

    try {
      val udsDbContext = new DatabaseConnectionContext(udsDbConnector)

      try {
        // Create databases
        new CreateProjectDBs(udsDbContext, prolineConf, projectId).doWork()

      } finally {
        logger.debug("Closing current UDS Db Context")

        try {
          udsDbContext.close()
        } catch {
          case exClose: Exception => logger.error("Error closing UDS Db Context", exClose)
        }

      }

    } finally {

      if (localUdsDbConnector && (udsDbConnector != null)) {
        udsDbConnector.close()
      }

    }

  }

  def getProjectsDir(dataDir: File): File = {
    new File(dataDir.toString + "/projects")
  }

  def getProjectDir(projectsDir: File, projectId: Long): File = {
    new File(projectsDir.toString + "/project_" + projectId)
  }

}
