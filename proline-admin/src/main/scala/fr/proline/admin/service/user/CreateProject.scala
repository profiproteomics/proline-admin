package fr.proline.admin.service.user

import com.weiglewilczek.slf4s.Logging
import fr.profi.jdbc.easy.{ date2Formattable, int2Formattable, string2Formattable }
import fr.proline.admin.service.db.{ CreateProjectDBs, DatabaseConnectionContext, SetupProline }
import fr.proline.admin.service.ICommandWork

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
    if (!wasConnOpened) udsDbContext.closeConnection()

    // import fr.proline.core.orm.uds.{Project => UdsProject,UserAccount => UdsUser}
    // Creation UDS entity manager
    //val udsEM = dbManager.udsEMF.createEntityManager()

    // Retrieve the owner => we assume it has been already created
    //val query = udsEM.createQuery("Select user from UserAccount user where id = :id", classOf[UdsUser])
    //query.setParameter("id", CreateProjectCommand.ownerId )

    //val owner = query.getSingleResult
    //println("creating project for user with login '"+ owner.getLogin +"'...")

    // Create the project
    //val udsProject = new UdsProject( owner )
    //udsProject.setName( CreateProjectCommand.projectName )
    //udsProject.setDescription( CreateProjectCommand.projectDescription )          
    //udsProject.addMember( owner )

    //udsEM.persist( udsProject )

    //val projectId = udsProject.getId
    //println("project with id='"+ projectId +"' has been created !")

    // Close entity manager
    //udsEM.close()
  }

}

object CreateProject extends Logging {

  def apply(name: String, description: String, ownerId: Int): Int = {

    import fr.proline.admin.service.db.{ CreateProjectDBs, DatabaseConnectionContext, ProlineDatabaseContext }

    // Retrieve Proline configuration
    val prolineConf = SetupProline.config

    // Instantiate a database manager
    val udsDBConfig = prolineConf.udsDBConfig
    val udsDbConnector = udsDBConfig.connector
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
        udsDbContext.closeAll()
      } catch {
        case exClose: Exception => logger.error("Error closing UDS Db Context", exClose)
      }

    }

    projectCreator.projectId
  }

}