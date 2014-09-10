package fr.proline.admin.gui.process

import java.io.File

import scala.collection.JavaConverters.asScalaBufferConverter

import com.typesafe.config.ConfigFactory

import fr.proline.admin.gui.Main
import fr.proline.admin.service.db.SetupProline
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.orm.uds.Project
import fr.proline.core.orm.uds.UserAccount
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.repository.IDatabaseConnector

import scalafx.application.Platform

/**
 * Some utilities relative to UDS database connection
 */
object UdsConnection {

  /**
   *  Update SetupProline.config when CONF file changes
   */
  def setNewProlineConfig() {

    /** Reload CONF file */
    var classLoader = SetupProline.getClass().getClassLoader()
    val newConfigFile = ConfigFactory.load(classLoader, "application")
    val dataDir = newConfigFile.getConfig("proline-config").getString("data-directory")

    /** Update displayed window, and Proline configuration if data directory exists */
    if (new File(dataDir).exists()) {
      SetupProline.setConfigParams(newConfigFile)
      Platform.runLater(Main.stage.title = s"Proline Admin @ $dataDir")

    } else {
      Platform.runLater(Main.stage.title = s"Proline Admin (invalid configuration)")
      throw new Exception("Unknown data directory " + dataDir)
    }
  }

  /**
   *  Test connexion with database
   */
  def isUdsDbReachable(): Boolean = {

    /** Look for uds.sqlite file in database directory */
    val (dir, file) = (SetupProline.config.udsDBConfig.dbDirectory, SetupProline.config.udsDBConfig.dbName)
    val udsFile = new File(dir + "/" + file)
    if (udsFile.exists()) true else false
  }

  /**
   *  Get the exhaustive list of UserAccount instances in database as a map of type "login ->  id"
   */
  def getUserMap(): Map[String, Long] = {

    val udsDbConnector = _getUdsDbConnector()
    val udsDbContext = new DatabaseConnectionContext(udsDbConnector)

    try {

      // from fr.profi.pwx.dal.ProlineDataStore (pwx-scala daemons)
      val udsEM = udsDbContext.getEntityManager()
      val UdsUserAccountClass = classOf[UserAccount]
      val jpqlSelectUserAccount = s"FROM ${UdsUserAccountClass.getName}"

      val udsUsers = udsEM.createQuery(jpqlSelectUserAccount, UdsUserAccountClass).getResultList()
      udsUsers.asScala.map { user => (user.getLogin(), user.getId()) }.toMap[String, Long]

    } catch {
      case e: Exception => { Map() }

    } finally {
      udsDbContext.close()
      udsDbConnector.close()
    }

  }

  /**
   *  Get the exhaustive list of Project instances in database as a map of type "name ->  owner"
   */
  def getProjectMap(): Map[String, UserAccount] = {

    val udsDbConnector = _getUdsDbConnector()
    val udsDbContext = new DatabaseConnectionContext(udsDbConnector)

    val projectMap = try {

      // from fr.profi.pwx.dal.ProlineDataStore (pwx-scala daemons)
      val udsEM = udsDbContext.getEntityManager()
      val UdsProjectClass = classOf[Project]
      val jpqlSelectProject = s"FROM ${UdsProjectClass.getName}"

      val udsProjects = udsEM.createQuery(jpqlSelectProject, UdsProjectClass).getResultList()
      udsProjects.asScala.map { p => (p.getName(), p.getOwner()) }.toMap[String, UserAccount]

    } finally {
      udsDbContext.close()
      udsDbConnector.close()
    }

    projectMap
  }

  /**
   *  Get projects name' list for a given user (owner)
   */
  def getUserProjects(userName: String): Array[String] = {
    //TODO: refine database request rather than filter on project map

    lazy val map = getProjectMap()

    val userProjects = map.filter {
      case (name: String, ua: UserAccount) => ua.getLogin() == userName //WARNING: assume logins are unique
    }.map { _._1 }

    userProjects.toArray
  }

  /**
   * Get UDS database connector
   */
  private def _getUdsDbConnector(): IDatabaseConnector = {

    // from fr.proline.admin.service.user.CreateUser
    val connectorFactory = DataStoreConnectorFactory.getInstance()

    var udsDbConnector =
      if (connectorFactory.isInitialized) {
        connectorFactory.getUdsDbConnector

      } else {
        val udsDBConfig = SetupProline.config.udsDBConfig
        udsDBConfig.toNewConnector()
      }

    udsDbConnector
  }
}