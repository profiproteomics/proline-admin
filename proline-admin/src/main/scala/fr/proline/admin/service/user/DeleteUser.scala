package fr.proline.admin.service.user

import com.typesafe.scalalogging.LazyLogging
import fr.proline.admin.service.db.SetupProline
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.dal.context._
import fr.proline.core.orm.uds.{ UserAccount => UdsUser }
import fr.proline.core.orm.util.DataStoreConnectorFactory
import javax.persistence.EntityTransaction
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 *  Delete user : update serialized properties.
 *
 */
class DeleteUser(
  udsDbContext: DatabaseConnectionContext,
  userId: Long) extends LazyLogging {

  def run() {

    val isTxOk = udsDbContext.tryInTransaction {

      // Creation UDS entity manager
      val udsEM = udsDbContext.getEntityManager
      val udsUser = udsEM.find(classOf[UdsUser], userId)
      if (udsUser != null) {
        // delete user
        var parser = new JsonParser()
        var array: JsonObject = null
        val properties = udsUser.getSerializedProperties()
        try {
          array = parser.parse(properties).getAsJsonObject()
        } catch {
          case e: Exception =>
            array = parser.parse("{}").getAsJsonObject()
        }
        array.addProperty("is_active", false)
        udsUser.setSerializedProperties(array.toString())
        udsEM.merge(udsUser)
        // to do delete user projects ?

      } else {
        logger.info(s" user with id= ${userId} does not exist ")
      }
    }
    if (isTxOk) {
      logger.info(s"user with id=$userId has been deleted ")
    } else {
      logger.error(" can't  delete user !")
    }
  }
}

object DeleteUser extends LazyLogging {

  def apply(userId: Long) {

    // Retrieve Proline configuration
    val prolineConf = SetupProline.config

    var localUdsDbConnector: Boolean = false

    val connectorFactory = DataStoreConnectorFactory.getInstance()

    val udsDbConnector = if (connectorFactory.isInitialized) {
      connectorFactory.getUdsDbConnector
    } else {

      // Create a new connector
      val udsDBConfig = prolineConf.udsDBConfig
      val newUdsDbConnector = udsDBConfig.toNewConnector()

      localUdsDbConnector = true

      newUdsDbConnector
    }

    try {
      val udsDbContext = new DatabaseConnectionContext(udsDbConnector)

      try {
        // delete user 
        val deleteUser = new DeleteUser(udsDbContext, userId)
        deleteUser.run()

      } finally {
        logger.debug("Closing current UDS Db Context")

        try {
          udsDbContext.close()
        } catch {
          case exClose: Exception => logger.error("Error closing UDS Db Context", exClose)
        }
      }

    } finally {
      //close connector
      if (localUdsDbConnector && (udsDbConnector != null)) {
        udsDbConnector.close()
      }

    }

  }

}
