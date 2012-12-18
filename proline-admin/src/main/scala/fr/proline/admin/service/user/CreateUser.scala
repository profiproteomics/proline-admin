package fr.proline.admin.service.user

import com.weiglewilczek.slf4s.Logging

import fr.proline.admin.service.db.DatabaseConnectionContext
import fr.proline.admin.service.db.SetupProline
import fr.proline.core.orm.uds.{Project => UdsProject,UserAccount => UdsUser}
import fr.proline.core.orm.util.DatabaseManager

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
    val udsEM = udsDbContext.entityManager
    
    // Begin transaction
    udsEM.getTransaction().begin()
    
    this.logger.info("creating user with login '"+ login +"'...")
    
    // Create the project
    val udsUser = new UdsUser()
    udsUser.setLogin( login )
    udsUser.setCreationMode( "MANUAL" )
    
    udsEM.persist( udsUser )
    udsEM.getTransaction().commit()
    
    val userId = udsUser.getId
    this.logger.info("user with id='"+ userId +"' has been created !")
    
    // Close entity manager
    udsEM.close()
  }
  
}

object CreateUser {
  
  def apply( login: String ): Int = {
    
    // Retrieve Proline configuration
    val prolineConf = SetupProline.parseProlineSetupConfig( SetupProline.appConf )
    
    // Instantiate a database manager
    val dbManager = DatabaseManager.getInstance()
    dbManager.initialize(prolineConf.udsDBConfig.toNewConnector)
    
    val udsDbContext = new DatabaseConnectionContext(dbManager.getUdsDbConnector)
    
    // Create user
    val userCreator = new CreateUser( udsDbContext, login )
    userCreator.run()
    
    // Close the database manager
    udsDbContext.closeAll()
    dbManager.closeAll()
    
    userCreator.userId
    
  }

  
}