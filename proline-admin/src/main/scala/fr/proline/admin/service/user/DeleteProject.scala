package fr.proline.admin.service.user

import com.typesafe.scalalogging.LazyLogging
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.context.DatabaseConnectionContext
import fr.proline.admin.service.db.SetupProline
import fr.proline.core.dal.context._
import fr.proline.core.dal.DoJDBCWork
import fr.proline.core.orm.uds.Project
import fr.proline.core.orm.uds.repository.ExternalDbRepository
import scala.collection.Set
import scala.util.{ Try, Success, Failure }
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 *  Delete or disable a Proline project(s).
 *  @param udsDbContext  The connection context to UDSDb to delete opr to disable the project(s) into.
 *  @param projectIdSet The set of project(s) id to delete or to disable.
 *  @param dropDatabases <code>true</code> will drop the MSI and LCMS databases of the project(s).
 */
class DeleteProject(
    udsDbContext: DatabaseConnectionContext,
    projectIdSet: Set[Long],
    dropDatabases: Boolean) extends LazyLogging {
  var isSuccess = false
  def run() {
    val batchSize = 20
    if (dropDatabases) {
      //delete permanently a set of project with its MSI and LCMS databases. 
      val isTxOk = udsDbContext.tryInTransaction {
        val udsEM = udsDbContext.getEntityManager
        projectIdSet.zipWithIndex.foreach {
          case (projectId, index) =>
            val project = udsEM.find(classOf[Project], projectId)
            require(project != null, "Undefined project with id=" + projectId)
            // remove externalDb type='MSI' and projectId
            val externalDbMsi = ExternalDbRepository.findExternalByTypeAndProject(udsEM, fr.proline.repository.ProlineDatabaseType.MSI, project)
            udsEM.remove(externalDbMsi)
            // remove externalDb type LCMS and projectId
            val externalDbLcms = ExternalDbRepository.findExternalByTypeAndProject(udsEM, fr.proline.repository.ProlineDatabaseType.LCMS, project)
            udsEM.remove(externalDbLcms)
            //remove the project
            udsEM.remove(project)
            if (index % batchSize == 0 && index > 0) {
              udsEM.flush()
              udsEM.clear()
            }
        }
      }
      if (isTxOk) {
        val dropDbs: Try[Boolean] =
          Try {
            projectIdSet.forall { projectId =>
              logger.debug(s"Dropping MSI and LCMS databases of project #$projectId please wait...")
              try {
                DoJDBCWork.withEzDBC(udsDbContext) { ezDBC =>
                  ezDBC.execute("DROP DATABASE IF EXISTS msi_db_project_" + projectId)
                  ezDBC.execute("DROP DATABASE IF EXISTS lcms_db_project_" + projectId)
                }
                true
              } catch {
                case t: Throwable => {
                  logger.error("Error while trying to drop MSI and LCMS databases of the project(s).", t.printStackTrace())
                  false
                }
              }
            }
          }
        dropDbs match {
          case Success(isDbsDropped) if (isDbsDropped) => {
            isSuccess = true
            logger.info(s"The project(s) with id(s)= #${projectIdSet.mkString(",")} has been deleted successfully.")
          }
          case Failure(t) => {
            isSuccess = false
            logger.info(s"Error while trying to drop the project(s) databases: ", t.printStackTrace())
          }
        }
      } else {
        logger.info(s"can't delete the The project(s) with id(s)= #${projectIdSet.mkString(",")} !")
      }
    } else {
      // this action will disable the set of project(s) 
      val isTxOk = udsDbContext.tryInTransaction {
        val udsEM = udsDbContext.getEntityManager
        projectIdSet.zipWithIndex.foreach {
          case (projectId, index) =>
            val project = udsEM.find(classOf[Project], projectId)
            require(project != null, "Undefined project with id=" + projectId)
            val properties = project.getSerializedProperties()
            var parser = new JsonParser()
            var array: JsonObject = Try(parser.parse(properties).getAsJsonObject()).getOrElse(parser.parse("{}").getAsJsonObject())
            array.addProperty("is_active", false)
            project.setSerializedProperties(array.toString())
            udsEM.merge(project)
            if (index % batchSize == 0 && index > 0) {
              udsEM.flush()
              udsEM.clear()
            }
        }
      }
      isSuccess = isTxOk
      if (isTxOk) {
        logger.info(s"The project(s) with id(s)= #${projectIdSet.mkString(",")} has been deleted successfully.")
      } else {
        logger.info(s"can't disable the The project(s) with id(s)= #${projectIdSet.mkString(",")} !")
      }
    }
  }
}

object DeleteProject {
  /**
   *  Delete or disable a Proline project(s)
   *  @param projectIdSet The set of project(s) id to delete or to disable.
   *  @param dropDatabases <code>true</code> will drop the MSI and LCMS databases of the project(s).
   *  @return <code>true</code> if the project(s) disabled or deleted successfully.
   */
  def apply(projectIdSet: Set[Long], dropDatabases: Boolean): Boolean = {
    val prolineConf = SetupProline.config
    var localUdsDbConnector: Boolean = false
    var isSuccess: Boolean = false
    val connectorFactory = DataStoreConnectorFactory.getInstance()
    val udsDbConnector = if (connectorFactory.isInitialized) {
      connectorFactory.getUdsDbConnector
    } else {
      // Instantiate a database manager
      val udsDBConfig = prolineConf.udsDBConfig
      val newUdsDbConnector = udsDBConfig.toNewConnector()
      localUdsDbConnector = true
      newUdsDbConnector
    }
    try {
      val udsDbContext = new DatabaseConnectionContext(udsDbConnector)
      try {
        val deleteUdsProject = new DeleteProject(udsDbContext,
          projectIdSet,
          dropDatabases)
        deleteUdsProject.run()
        isSuccess = deleteUdsProject.isSuccess
      } finally {
        try {
          udsDbContext.close()
        } catch {
          case exClose: Exception => print("Error while trying to close UDS Db Context", exClose)
        }
      }
    } finally {
      if (localUdsDbConnector && (udsDbConnector != null)) {
        udsDbConnector.close()
      }
    }
    isSuccess
  }
}
