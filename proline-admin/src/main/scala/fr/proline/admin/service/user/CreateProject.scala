package fr.proline.admin.service.user

import com.weiglewilczek.slf4s.Logging
import net.noerd.prequel.SQLFormatterImplicits._

import fr.proline.admin.service.db.SetupProline
import fr.proline.core.dal.DatabaseManagement
import fr.proline.core.dal.UdsDb
import fr.proline.core.orm.uds.{Project => UdsProject,UserAccount => UdsUser}
    

/**
 * @author David Bouyssie
 *
 */
class CreateProject( dbManager: DatabaseManagement,
                     projectName: String,
                     projectDescription: String,
                     ownerId: Int ) extends Logging {
  
  var projectId = 0
  
  def run() {
    
    // Create a database transaction
    val udsDb = UdsDb( dbManager.udsDBConnector )    
    val udsDbTx = udsDb.getOrCreateTransaction()
    
    this.projectId = udsDbTx.executeBatch(
    "INSERT INTO project (name,description,creation_timestamp,owner_id) VALUES (?,?,?,?)") { stmt =>
      stmt.executeWith( projectName,
                        projectDescription,
                        new java.util.Date(),
                        ownerId
                       )
      udsDb.extractGeneratedInt( stmt.wrapped )
    }
    
    udsDbTx.commit()
    //udsDb.closeConnection()    

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
    
    // Retrieve Proline configuration
    val prolineConf = SetupProline.parseProlineSetupConfig( SetupProline.appConf )
    
    // Instantiate a database manager
    val dbManager = new DatabaseManagement(prolineConf.udsDBConfig.connector)
    
    // Create project
    val projectCreator = new CreateProject( dbManager, name, description, ownerId )
    projectCreator.run()
    
    // Close the database manager
    dbManager.closeAll()
    
    projectCreator.projectId
    
  }

  
}