package fr.proline.admin.service.user

import com.weiglewilczek.slf4s.Logging

import fr.proline.admin.service.db.SetupProline
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.orm.uds.{Project => UdsProject,UserAccount => UdsUser}
import fr.proline.core.orm.util.DataStoreConnectorFactory

/**
 * @author David Bouyssie
 *
 */
class CreateUser( udsDbContext: DatabaseConnectionContext,
                  login: String ) extends Logging {
  
  var userId = 0
  
  def run() {

    import fr.proline.core.orm.uds.{UserAccount => UdsUser}
    
    // Creation UDS entity manager
    val udsEM = udsDbContext.getEntityManager
    
    // Begin transaction
    udsEM.getTransaction().begin()
    
    this.logger.info("creating user with login '"+ login +"'...")
    
    // Create the project
    val udsUser = new UdsUser()
    udsUser.setLogin( login )
    udsUser.setCreationMode( "MANUAL" )
    
    udsEM.persist( udsUser )
    udsEM.getTransaction().commit()
    
    this.userId = udsUser.getId
    this.logger.info("user with id='"+ userId +"' has been created !")
    
    // Close entity manager
    //udsDbContext.close()
  }
  
}

object CreateUser {
  
  def apply( login: String ): Int = {
    
    // Retrieve Proline configuration
    val prolineConf = SetupProline.config    
    val udsDbConnector = prolineConf.udsDBConfig.toNewConnector
    val udsDbContext = new DatabaseConnectionContext(udsDbConnector)
    
    // Create user
    val userCreator = new CreateUser( udsDbContext, login )
    userCreator.run()
    
    // Close the database manager
    udsDbContext.close()
    udsDbConnector.close()
    
    userCreator.userId
    
  }

  
}