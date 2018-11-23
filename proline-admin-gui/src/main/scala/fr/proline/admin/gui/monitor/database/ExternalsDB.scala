package fr.proline.admin.gui.monitor.model

import com.typesafe.scalalogging.LazyLogging

import fr.proline.core.dal.context._
import fr.proline.context.DatabaseConnectionContext
import fr.proline.repository.IDataStoreConnectorFactory

import fr.proline.core.orm.uds.{ ExternalDb => UdsExternalDb }
import fr.proline.admin.service.user.{ ChangeExtDbProperties, DeleteObsoleteDbs, CheckForUpdates }
import fr.proline.admin.service.db.migration.UpgradeAllDatabases
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.monitor.model.AdapterModel._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Future, Await }
import scala.collection.JavaConverters._

/**
 * ExternalsDB perform some operations (check for updates, update parameters and upgrade all databases) with Proline databases.
 * @author aromdhani
 *
 */
object ExternalsDB extends LazyLogging {

  private val dsConnectorFactory: IDataStoreConnectorFactory = UdsRepository.getDataStoreConnFactory()
  private val udsDbCtx: DatabaseConnectionContext = UdsRepository.getUdsDbContext()
  
  /** Remove all ExternalDb */
  def clear(): Unit = {}

  /** Return a sequence of ExternalDb from UDS database to initialize the table View */
  def initialize(): Seq[ExternalDb] = {
    queryExternalDbsAsView()
  }

  /** Return a sequence of ExternalDb from UDS database as ExternalDb adapted to table view */
  def queryExternalDbsAsView(): Seq[ExternalDb] = {
    queryExternalDbs().toBuffer[UdsExternalDb].sortBy(_.getId).map(ExternalDb(_))
  }

  /** Return the current content of UDS database from ExternalDb */
  def queryExternalDbs(): Array[UdsExternalDb] = {
    run(
      try {
        var udsExternalDbsArray = Array[UdsExternalDb]()
        udsDbCtx.tryInTransaction {
          val udsEM = udsDbCtx.getEntityManager()
          udsEM.clear()
          val UdsExternalDbClass = classOf[UdsExternalDb]
          val jpqlSelectUdsExternalDb = s"FROM ${UdsExternalDbClass.getName}"
          val udsExternalDbs = udsEM.createQuery(jpqlSelectUdsExternalDb, UdsExternalDbClass).getResultList()
          udsExternalDbsArray = udsExternalDbs.asScala.toArray
        }
        udsExternalDbsArray
      } catch {
        case t: Throwable => {
          synchronized {
            logger.error("Cannot load ExternalDb from UDS database")
            logger.error(t.getMessage())
            throw t
          }
        }
      })
  }

  /** Change ExternalDB  parameters */
  def change(extDbId: Set[Long], host: String, port: Int) {
    run {
      new ChangeExtDbProperties(
        udsDbCtx,
        extDbId,
        host,
        port).run()
    }
  }

  /** Check for updates */
  def checkForUpdates() {
    run { new CheckForUpdates(dsConnectorFactory).doWork() }
  }

  /** Upgrade all Proline databases */
  def upgradeAllDbs() {
    run { new UpgradeAllDatabases(dsConnectorFactory).doWork() }
  }

  /** Delete obsolete databases */
  def deleteObsoleteDbs() {
    run { new DeleteObsoleteDbs(dsConnectorFactory).run() }
  }

  /** Perform database actions and wait for completion */
  private def run[R](actions: => R): R = {
    Await.result(Future { actions }, Duration.Inf)
  }
}