package fr.proline.admin.service.user

import com.weiglewilczek.slf4s.Logging

import fr.proline.admin.service.db.DatabaseConnectionContext
import fr.proline.admin.service.db.SetupProline
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
    
    this.userId = udsUser.getId
    this.logger.info("user with id='"+ userId +"' has been created !")
    
    // Close entity manager
    udsDbContext.closeAll
  }
  
}

object CreateUser {
  
  def apply( login: String ): Int = {
    
    // Retrieve Proline configuration
    val prolineConf = SetupProline.config
    
    // Instantiate a database manager
    val dsConnectorFactory = DataStoreConnectorFactory.getInstance()
    if( dsConnectorFactory.isInitialized == false ) dsConnectorFactory.initialize(prolineConf.udsDBConfig.connector)
    
    val udsDbContext = new DatabaseConnectionContext(dsConnectorFactory.getUdsDbConnector)
    
    // Create user
    val userCreator = new CreateUser( udsDbContext, login )
    userCreator.run()
    
    // Close the database manager
    udsDbContext.closeAll()
    //dbManager.closeAll()
    
    userCreator.userId
    
  }

  
}