package fr.proline.admin.service.user

import com.weiglewilczek.slf4s.Logging

import fr.profi.jdbc.easy._
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.service.db.DatabaseConnectionContext
import fr.proline.core.orm.uds.{Project => UdsProject,UserAccount => UdsUser}

import fr.proline.core.orm.util.DatabaseManager

/**
 * @author David Bouyssie
 *
 */
class CreateProject( udsDbContext: DatabaseConnectionContext,
                     projectName: String,
                     projectDescription: String,
                     ownerId: Int ) extends Logging {
  
  var projectId = 0
  
  def run() {
    
    // Get EasyDBC object    
    val udsEzDBC = udsDbContext.ezDBC
    
    // Check connection and transaction
    val wasConnOpened = udsDbContext.isConnectionOpened()
    val wasInTx = udsEzDBC.isInTransaction()
    
    if( !wasInTx ) udsEzDBC.beginTransaction()
    
    this.projectId = udsEzDBC.executePrepared("INSERT INTO project (name,description,creation_timestamp,owner_id) VALUES (?,?,?,?)",true) { stmt =>
      stmt.executeWith( projectName,
                        projectDescription,
                        new java.util.Date(),
                        ownerId
                       )
      //this.projectId = udsDb.extractGeneratedInt( stmt.wrapped )
      stmt.generatedInt
    }
    
    // Manage connection and transaction
    if( !wasInTx ) udsEzDBC.commitTransaction()
    if( !wasConnOpened ) udsDbContext.closeConnection()

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

object CreateProject {
  
  def apply( name: String, description: String, ownerId: Int ): Int = {
    
    import fr.proline.admin.service.db.{CreateProjectDBs,DatabaseConnectionContext,ProlineDatabaseContext}
    
    // Retrieve Proline configuration
    val prolineConf = SetupProline.parseProlineSetupConfig( SetupProline.appConf )
    
    // Instantiate a database manager
    val udsDBConfig = prolineConf.udsDBConfig
    val udsDbConnector = udsDBConfig.toNewConnector
    val udsDbContext = new DatabaseConnectionContext(udsDbConnector)
    
    // Create project
    val projectCreator = new CreateProject(
      udsDbContext,
      name,
      description,
      ownerId
    )
    projectCreator.run()
    
    // Close the database resources
    udsDbContext.closeAll()
    //udsDbConnector.close()
    
    // Create a new database manager to avoid any conflict
    val dbManager = DatabaseManager.getInstance()
    dbManager.initialize(udsDbConnector)
    
    val prolineDbContext = new ProlineDatabaseContext(dbManager)
    //val dbManager2 = new DatabaseManagement( udsDbConnector2 )
    
    // Create project databases
    new CreateProjectDBs( prolineDbContext, prolineConf, projectCreator.projectId ).run()          
    
    // Close the database manager
    prolineDbContext.closeAll()
    dbManager.closeAll()
    
    projectCreator.projectId
    
  }

  
}