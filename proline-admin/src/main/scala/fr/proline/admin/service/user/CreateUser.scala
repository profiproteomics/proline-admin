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
class CreateUser( dbManager: DatabaseManagement,
                  login: String ) extends Logging {
  
  var userId = 0
  
  def run() {

    import fr.proline.core.orm.uds.{UserAccount => UdsUser}
    
    // Creation UDS entity manager
    val udsEM = dbManager.udsEMF.createEntityManager()
    
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
    val dbManager = new DatabaseManagement(prolineConf.udsDBConfig.connector)
    
    // Create user
    val userCreator = new CreateUser( dbManager, login )
    userCreator.run()
    
    // Close the database manager
    dbManager.closeAll()
    
    userCreator.userId
    
  }

  
}