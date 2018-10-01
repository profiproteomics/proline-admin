package fr.proline.admin.service.user

import com.typesafe.scalalogging.LazyLogging
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.context.DatabaseConnectionContext
import fr.proline.admin.service.db.SetupProline
import fr.proline.core.dal.context._
import fr.proline.core.orm.uds.ExternalDb
import scala.collection.Set
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Change external database properties.
 * @author aromdhani
 *
 * @param udsDbContext The connection context to UDSDb to change external database(s) properties into.
 * @param ExternalDbId The External database(s) id to update.
 * @param host the host name.
 * @param port The port number.
 */
class ChangeExtDbProperties(
    udsDbContext: DatabaseConnectionContext,
    extDbIdSet: Set[Long],
    host: String,
    port: Int) extends LazyLogging {
  var isSuccess: Boolean = false
  def run() {
    val isTxOk = udsDbContext.tryInTransaction {
      val batchSize = 20
      val udsEM = udsDbContext.getEntityManager
      extDbIdSet.zipWithIndex.foreach {
        case (extDbId, index) =>
          val udsExtDb = udsEM.find(classOf[ExternalDb], extDbId)
          require(udsExtDb != null, s"The external database with id= ${extDbId} does not exist.")
          // change properties
          udsExtDb.setHost(host)
          udsExtDb.setPort(port)
          udsEM.merge(udsExtDb)
          if (index % batchSize == 0 && index > 0) {
            udsEM.flush()
            udsEM.clear()
          }
      }
    }
    isSuccess = isTxOk
    if (isTxOk) {
      logger.info(s"The properties of external database(s) with id(s)= #${extDbIdSet.mkString(",")} have been changed successfully.")
    } else {
      logger.error(" can't change the properties of external database(s) !")
    }
  }
}

object ChangeExtDbProperties extends LazyLogging {

  /**
   * Change external database properties.
   * @param ExternalDbId The set of External database(s) id to update.
   * @param host the host name.
   * @param port The port number.
   * @return <code>true</code> if the external database(s) have been updated successfully.
   */
  def apply(extDbIdSet: Set[Long], host: String, port: Int): Boolean = {
    // Retrieve Proline configuration
    val prolineConf = SetupProline.config
    var localUdsDbConnector: Boolean = false
    var isSuccess: Boolean = false
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
        // disable  user 
        val changeExtDbProperties = new ChangeExtDbProperties(
          udsDbContext,
          extDbIdSet,
          host,
          port)
        changeExtDbProperties.run()
        isSuccess = changeExtDbProperties.isSuccess
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
    isSuccess
  }
}
