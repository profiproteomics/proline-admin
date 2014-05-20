package fr.proline.admin.service.user

import com.typesafe.scalalogging.slf4j.Logging

import fr.proline.admin.service.db.SetupProline
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.orm.uds.{Project => UdsProject,UserAccount => UdsUser}
import fr.proline.core.orm.util.DataStoreConnectorFactory

/**
 * @author David Bouyssie
 *
 */
class CreateUser( udsDbContext: DatabaseConnectionContext,
                  login: String , password : String) extends Logging {
  
  var userId: Long = 0L
  
  def run() {

    import fr.proline.core.orm.uds.{UserAccount => UdsUser}
    import fr.profi.util.security._
    
    // Creation UDS entity manager
    val udsEM = udsDbContext.getEntityManager
    
    // Begin transaction
    udsEM.getTransaction().begin()
    
    this.logger.info("creating user with login '"+ login +"'...")
    
    // Create the project
    val udsUser = new UdsUser()
    udsUser.setLogin( login )
    udsUser.setPassword(sha256Hex(password))
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
  
  def apply( login: String, pswd : Option[String] = None ): Long = {
    
    // Retrieve Proline configuration
    val prolineConf = SetupProline.config    
    val udsDbConnector = prolineConf.udsDBConfig.toNewConnector
    val udsDbContext = new DatabaseConnectionContext(udsDbConnector)
    
    // Create user
    val password = if(pswd.isDefined) pswd.get else "proline"
    val userCreator = new CreateUser( udsDbContext, login, password)
    userCreator.run()
    
    // Close the database manager
    udsDbContext.close()
    udsDbConnector.close()
    
    userCreator.userId
    
  }

  
}