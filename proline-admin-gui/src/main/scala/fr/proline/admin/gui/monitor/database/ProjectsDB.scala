package fr.proline.admin.gui.monitor.database

import com.typesafe.scalalogging.LazyLogging

import fr.proline.core.dal.context._
import fr.proline.core.dal.DoJDBCWork
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.orm.uds.{ Project => UdsProject }
import fr.proline.admin.service.user.ChangeProjectState
import fr.proline.admin.service.user.ArchiveProject
import fr.proline.admin.service.user.RestoreProject
import fr.proline.admin.service.user.CreateProject
import fr.proline.admin.service.user.DeleteProject
import fr.proline.admin.service.user.{ CreateProject, ProjectUtils }
import fr.proline.admin.service.db.{ CreateProjectDBs, SetupProline }
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.monitor.model.AdapterModel._
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._

/**
 * ProjectsDB perform some operations (add, update, delete, archive and restore) with Proline databases.
 * @author aromdhani
 *
 */
object ProjectsDB extends LazyLogging {
  lazy val udsDbCtx: DatabaseConnectionContext = UdsRepository.getUdsDbContext()
  /** TODO Remove all projects to clean UDS database?  */
  def clear(): Unit = {

  }

  /** Return a sequence of projects from UDS database to initialize the table View */
  def initialize(): Seq[Project] = {
    queryProjectsAsView()
  }

  /** Return a sequence of project from UDS database as User adapted to table view */
  def queryProjectsAsView(): Seq[Project] = {
    queryProjects().toBuffer[UdsProject].sortBy(_.getId).map(Project(_))
  }

  /** Return the current content of UDS database projects */
  def queryProjects(): Array[UdsProject] = {
    run(
      try {
        logger.debug("load projects from UDS database.")
        var udsProjectsArray = Array[UdsProject]()
        udsDbCtx.tryInTransaction {
          val udsEM = udsDbCtx.getEntityManager()
          udsEM.clear()
          val UdsProjectClass = classOf[UdsProject]
          val jpqlSelectProject = s"FROM ${UdsProjectClass.getName}"
          val udsProjects = udsEM.createQuery(jpqlSelectProject, UdsProjectClass).getResultList()
          udsProjectsArray = udsProjects.asScala.toArray
        }
        udsProjectsArray
      } catch {
        case t: Throwable => {
          synchronized {
            logger.error("Cannot load projects from UDS database!")
            logger.error(t.getMessage())
            throw t
          }
        }
      })
  }

  /** Add Proline project */
  def add(name: String, description: String, ownerId: Long) {
    try {
      val projectCreator = new CreateProject(udsDbCtx, name, description, ownerId)
      projectCreator.doWork()
      val projectId = projectCreator.projectId
      if (projectId > 0L) {
        val prolineConf = SetupProline.getUpdatedConfig
        // Create project  databases
        new CreateProjectDBs(udsDbCtx, prolineConf, projectId).doWork()
        // Update ExetrnalDb with created project version
        val dataStoreConnFactory = UdsRepository.getDataStoreConnFactory()
        val msiDbConnector = dataStoreConnFactory.getMsiDbConnector(projectId)
        val msiDbVersionOpt = ProjectUtils.retrieveDbVersion(msiDbConnector)
        val lcmsDbConnector = dataStoreConnFactory.getLcMsDbConnector(projectId)
        val lcmsDbVersionOpt = ProjectUtils.retrieveDbVersion(lcmsDbConnector)
        ProjectUtils.updateExtDbs(udsDbCtx, projectId, msiDbVersionOpt, lcmsDbVersionOpt)
      }
    } catch {
      case t: Throwable => {
        synchronized {
          logger.error("Cannot add Proline project!")
          logger.error(t.getMessage())
          throw t
        }
      }

    }

  }

  /** Change Proline project state */
  def changeState(projects: Set[Project], isActive: Boolean) = {
    run(new ChangeProjectState(udsDbCtx, projects.map(_.id.value), isActive).run())
  }

  /** Delete Proline project */
  def delete(projects: Set[Project], dropDbs: Boolean) {
    run(new DeleteProject(udsDbCtx, projects.map(_.id.value), dropDbs).run())
  }

  /** Archive Proline project */
  def archive(projectId: Long, binDirPath: String, archiveLocationPath: String) {
    run {
      val userName = UdsRepository.getUdsDbConfig().userName
      new ArchiveProject(udsDbCtx, userName, projectId, binDirPath, archiveLocationPath).run()
    }

  }

  /** Restore Proline project */
  def restore(ownerId: Long, binDirPath: String, archivedProjDirPath: String, projectName: Option[String]) {
    run {
      val userName = UdsRepository.getUdsDbConfig().userName
      new RestoreProject(udsDbCtx, userName, ownerId, binDirPath, archivedProjDirPath, projectName).doWork()
    }
  }

  /** Compute LCMS database size */
  def computeLcmsSize(projectId: Long): Option[String] = {
    var size: Option[String] = None
    DoJDBCWork.withEzDBC(udsDbCtx) { ezDBC =>
      ezDBC.selectAndProcess("SELECT pg_size_pretty(pg_database_size(datname)) as size FROM pg_database WHERE datname='lcms_db_project_" + projectId + "'") { record =>
        size = Option(record.getString("size"))
      }
    }
    size
  }

  /** Compute MSI database size*/
  def computeMsiSize(projectId: Long): Option[String] = {
    var size: Option[String] = None
    DoJDBCWork.withEzDBC(udsDbCtx) { ezDBC =>
      ezDBC.selectAndProcess("SELECT pg_size_pretty(pg_database_size(datname)) as size FROM pg_database WHERE datname='msi_db_project_" + projectId + "'") { record =>
        size = Option(record.getString("size"))
      }
    }
    size
  }

  /** Perform database actions and wait for completion */
  private def run[R](actions: => R): R = {
    Await.result(Future { actions }, Duration.Inf)
  }
}