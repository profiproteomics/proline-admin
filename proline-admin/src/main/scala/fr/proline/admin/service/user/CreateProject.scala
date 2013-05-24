package fr.proline.admin.service.user

import com.weiglewilczek.slf4s.Logging
import fr.profi.jdbc.easy.{ date2Formattable, int2Formattable, string2Formattable }
import fr.proline.admin.service.db.{ CreateProjectDBs, SetupProline }
import fr.proline.admin.service.ICommandWork
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.orm.uds.{Dataset=> UdsDataset,Project => UdsProject,UserAccount => UdsUser}

/**
 * @author David Bouyssie
 *
 */
class CreateProject(udsDbContext: DatabaseConnectionContext,
                    projectName: String,
                    projectDescription: String,
                    ownerId: Int) extends ICommandWork with Logging {

  var projectId = 0

  def doWork() {

    /*
    // Get EasyDBC object    
    val udsEzDBC = udsDbContext.ezDBC

    // Check connection and transaction
    val wasConnOpened = udsDbContext.isConnectionOpened()
    val wasInTx = udsEzDBC.isInTransaction()

    if (!wasInTx) udsEzDBC.beginTransaction()

    this.projectId = udsEzDBC.executePrepared("INSERT INTO project (name,description,creation_timestamp,owner_id) VALUES (?,?,?,?)", true) { stmt =>
      stmt.executeWith(projectName,
        projectDescription,
        new java.util.Date(),
        ownerId
      )
      //this.projectId = udsDb.extractGeneratedInt( stmt.wrapped )
      stmt.generatedInt
    }

    // Manage connection and transaction
    if (!wasInTx) udsEzDBC.commitTransaction()
    if (!wasConnOpened) udsDbContext.closeConnection()*/
    
    // Retrieve UDS entity manager
    val udsEM = udsDbContext.getEntityManager

    // Retrieve the owner => we assume it has been already created
    val udsUser = udsEM.find(classOf[UdsUser], ownerId)
    require(udsUser != null, "undefined user with id=" + ownerId)
    
    //println("creating project for user with login '"+ owner.getLogin +"'...")

    // Begin transaction
    // TODO :check if is in transaction
    udsEM.getTransaction().begin()
    
    // Create the project
    val udsProject = new UdsProject( udsUser )
    udsProject.setName( projectName )
    udsProject.setDescription( projectDescription )
    udsProject.setCreationTimestamp(fr.proline.util.sql.getTimeAsSQLTimestamp)
    udsProject.setOwner(udsUser)
    
    udsEM.persist( udsProject )
    
    // Create an empty TRASH dataset for this project
    val udsDataset = new UdsDataset(udsProject)
    udsDataset.setNumber(1)
    udsDataset.setName(UdsDataset.DatasetType.TRASH.toString)
    udsDataset.setType(UdsDataset.DatasetType.TRASH)
    udsDataset.setCreationTimestamp(fr.proline.util.sql.getTimeAsSQLTimestamp)
    udsDataset.setChildrenCount(0)
    
    udsEM.persist( udsDataset )
    
    // Commit transaction
    udsEM.getTransaction().commit()

    this.projectId = udsProject.getId
    //println("project with id='"+ projectId +"' has been created !")
    
  }

}

object CreateProject extends Logging {

  def apply(name: String, description: String, ownerId: Int): Int = {

    import fr.proline.admin.service.db.{ CreateProjectDBs, ProlineDatabaseContext }

    // Retrieve Proline configuration
    val prolineConf = SetupProline.config

    // Instantiate a database manager
    val udsDBConfig = prolineConf.udsDBConfig
    val udsDbConnector = udsDBConfig.toNewConnector
    val udsDbContext = new DatabaseConnectionContext(udsDbConnector)

    var projectCreator: CreateProject = null

    try {

      // Create project
      projectCreator = new CreateProject(
        udsDbContext,
        name,
        description,
        ownerId
      )
      projectCreator.doWork()

      // Close the database resources
      //udsDbContext.closeAll()

      // Create a new database manager to avoid any conflict
      //val dsConnectorFactory = DataStoreConnectorFactory.getInstance()
      //if( dsConnectorFactory.isInitialized == false ) dsConnectorFactory.initialize(udsDbConnector)

      //val prolineDbContext = new ProlineDatabaseContext(dsConnectorFactory)

      // Create project databases
      new CreateProjectDBs(udsDbContext, prolineConf, projectCreator.projectId).doWork()

      // Close the database manager

    } finally {
      logger.debug("Closing current UDS Db Context")

      try {
        udsDbContext.close()
        udsDbConnector.close()
      } catch {
        case exClose: Exception => logger.error("Error closing UDS Db Context", exClose)
      }

    }

    projectCreator.projectId
  }

}