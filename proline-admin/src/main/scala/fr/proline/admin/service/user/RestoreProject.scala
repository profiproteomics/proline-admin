package fr.proline.admin.service.user

import com.typesafe.scalalogging.LazyLogging

import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.admin.service.db.SetupProline
import fr.proline.repository.DriverType
import fr.proline.core.orm.uds.ExternalDb
import fr.proline.core.orm.uds.UserAccount
import fr.proline.core.orm.uds.Project
import fr.proline.core.orm.uds.Dataset
import fr.proline.core.orm.uds.Aggregation
import fr.proline.core.orm.uds.Fractionation
import fr.proline.core.orm.uds.QuantitationMethod
import fr.proline.core.orm.uds.IdentificationDataset
import fr.proline.admin.service.ICommandWork
import fr.proline.context._
import fr.proline.core.dal.context._
import fr.proline.repository._
import fr.proline.core.dal.DoJDBCWork
import javax.persistence.EntityManager
import scala.util.Try
import scala.collection.mutable.Map
import scala.sys.process.{ Process, ProcessLogger }

import java.util.Date
import java.text.SimpleDateFormat
import java.io.{ File, FileInputStream }
import com.google.gson.{ JsonObject, JsonParser }
import play.api.libs.json._

/**
 * Restore a Proline project. It restore the databases msi_db_project ,lcms_db_project and the project properties from the project_properties.json file.
 * @author aromdhani
 *
 * @param udsDbCtx The connection context to UDSDb to restore project into.
 * @param ownerId The owner id of the project.
 * @param binDirPath The PostgreSQL bin directory path. It should contains pg_retsore file.
 * @param archivedProjDirPath The path of the archived project directory.
 * @param projectName To rename the project to restore. It's advised when the project name already defined for the same user.
 *
 */

class RestoreProject(
    udsDbCtx: DatabaseConnectionContext,
    pgUserName: String,
    ownerId: Long,
    binDirPath: String,
    archivedProjDirPath: String,
    projectName: Option[String] = None) extends ICommandWork with LazyLogging {
  var projectId: Long = -1L
  var newProjId: Long = -1L
  def doWork() {
    try {
      val udsEM = udsDbCtx.getEntityManager
      // check that the input parameters are valiadted 
      logger.info(s"Checking input parameters to restore the project ...")
      // check that bin directory pg_restore file exists and right to read from archive project directory
      require(isDefinedBinDir(binDirPath).isDefined, s"Invalid parameters. Make sure that the bin directory has pg_restore.exe file.")
      require(isReadableArchiveProjDir(archivedProjDirPath), "Make sure that you are allowed to read from the archived project directory.")

      // check that the archive project directory has the required file to restore a project
      val projFilesAsMap = getProjectFilesAsMap(archivedProjDirPath)
      require(isValidatedProjectDir(projFilesAsMap), "The project directory must contain the SQL backup files and the project_properties.json file!")

      // restore the project for an existing user   
      logger.info(s"The project will be restored for the owner with id =#$ownerId.")
      val userOpt = Option(udsEM.find(classOf[UserAccount], ownerId))
      require(userOpt.isDefined, s"Undefined user with id= #$ownerId. Please restore the project with an existing user account!")

      // rename the project,the new name must not be defined twice for the same owner Id(project_name_owner_idx) 
      projectName.foreach { name =>
        logger.info(s"The project name will be specified by the user.")
        require(isTakenProjectName(udsEM, ownerId, name), s"The project name =$name is already taken for the owner with id= #$ownerId !Please rename your project.")
      }

      // get project_properties file and get Json objects in a Map 
      logger.debug("Start to retrieve project properties from the project_properties.json file ...")
      val projPropFile = projFilesAsMap("projPropFile").get
      val jsValuesMap = readJsonFile(projPropFile.getAbsolutePath)
      require(!jsValuesMap.isEmpty, "The json values must not be empty!")
      var array: JsonObject = null
      var udsProject: Project = null
      projectId = (jsValuesMap("project").as[JsObject] \ "id").as[Long]
      val jsDbVersion = (jsValuesMap("schema_version").as[JsObject] \ "version").as[Int]
      var JsHost, JsMsiDbName, JsLcmsDbName = ""
      var JsPort, dbVersion = 0
      var newProjectId = 0L
      logger.warn("**The version of Proline database UDS that used to archive and restore project must be the same or above 8!")
      DoJDBCWork.withEzDBC(udsDbCtx) { ezDBC =>
        ezDBC.selectAndProcess(s"SELECT MAX(version_rank) AS version FROM schema_version") { record =>
          dbVersion = record.getInt("version")
        }
      }

      // check the schema version 
      logger.info(s"The version of your Proline database UDS is =#$dbVersion and the version used to archive the project is =#$jsDbVersion")
      require((jsDbVersion == dbVersion && jsDbVersion >= 8), "The Proline version that used to archive and to restore a project are different. Make sure that your databases are upgarded.")
      val isTxOk = udsDbCtx.tryInTransaction {

        // load project properties 
        logger.info("Loading project properties from project_properties.json file ...")
        val udsProjectOpt = loadProjectData(userOpt, projectName, jsValuesMap, udsEM)
        require(udsProjectOpt.isDefined, "Failed to restore the Proline project!")
        udsProject = udsProjectOpt.get
        udsEM.persist(udsProject)
        newProjectId = udsProject.getId()
        val properties = udsProject.getSerializedProperties()
        array = Try { JsonParser.parseString(properties).getAsJsonObject() } getOrElse { JsonParser.parseString("{}").getAsJsonObject() }

        // load external_db properties 
        logger.info("Loading external_db rows from project_properties.json file ....")
        val externalDbsAsMap = loadExetrnalDbData(jsValuesMap, udsProject)
        require(externalDbsAsMap.values.forall(_.!=(null)), "Error could not create external db!")
        externalDbsAsMap.values.foreach { extDb => { udsEM.persist(extDb); udsProject.addExternalDatabase(extDb) } }
        JsMsiDbName = externalDbsAsMap("MSI").asInstanceOf[ExternalDb] getDbName ()
        JsLcmsDbName = externalDbsAsMap("LCMS").asInstanceOf[ExternalDb].getDbName()
        JsHost = externalDbsAsMap("MSI").asInstanceOf[ExternalDb].getHost()
        JsPort = externalDbsAsMap("MSI").asInstanceOf[ExternalDb].getPort()
        logger.info("Loading data_set rows from project_properties.json file ...")
        // load data_set properties */
        loadDataSets(jsValuesMap, udsProject, udsEM)
      }
      // create external_db databases */
      if (isTxOk) {
        val isExtDbsCreated =
          try {
            logger.info(s"Creating msi_db_project_$newProjectId and lcms_db_project_$newProjectId ... ")
            DoJDBCWork.withEzDBC(udsDbCtx) { ezDBC =>
              ezDBC.execute(s"CREATE DATABASE msi_db_project_$newProjectId")
              ezDBC.execute(s"CREATE DATABASE lcms_db_project_$newProjectId")
            }
            true
          } catch {
            case t: Throwable =>
              {
                udsDbCtx.rollbackTransaction()
                logger.error("Can't create MSI and LCMS databases", t)
                false
              }
          }
        // execute pg_restore commands to back up databases 
        if (isExtDbsCreated) {
          logger.info("The MSI and LCMS databases have been created successfully.")
          logger.info("Start to execute pg_restore commands to restore MSI and LCMS databases. It could take a while, please wait ...")
          val isRestoredDatabases = restoreDatabases(JsHost, JsPort, pgUserName, JsMsiDbName, pgUserName, archivedProjDirPath, binDirPath, projectId, newProjectId)
          if (isRestoredDatabases) {
            // update project's serialized properties 
            val updateProjSerProps = udsDbCtx.tryInTransaction {
              addProperties(array)
              udsProject.setSerializedProperties(array.toString())
              udsEM.merge(udsProject)
            }
            if (updateProjSerProps) {
              newProjId = newProjectId
              logger.info(s"The Project with id= #$projectId has been restored with a new project id= #$newProjectId.")
            }
          }
        }
      } else {
        logger.error("An error occured while trying to import project properties in UDS database!")
      }
    } catch {
      case e: Exception => throw new Exception(e.getMessage)
    }
  }

  /**
   * Check if it's a defined and validated bin directory.
   * @param binDirPath The path of the bin directory.
   * @return <code>Some(File)</code> if it's validated bin directory otherwise None.
   */
  private val isDefinedBinDir: String => Option[File] = binDirPath => { if (new File(binDirPath).exists) new File(binDirPath).listFiles().find(file => file.getName.matches("^pg_restore.exe$") && file.canExecute()) else None }

  /**
   * Check if user can read from the archive directory.
   * @param filePath The path of the archive directory.
   * @return <code>true</code> if it's validated archived project directory otherwise false.
   */
  private val isReadableArchiveProjDir: String => Boolean = (filePath: String) => (new File(filePath).canRead())

  /**
   * Check that's a validated project directory.
   * @param projectDirFiles  a map of eventual files from  the project directory.
   * @return <code>true</code> if it's validated archived project directory otherwise false.
   */
  private val isValidatedProjectDir: Map[String, Option[File]] => Boolean = projectDirFilesasMap => {
    projectDirFilesasMap.values.forall(_.isDefined)
  }

  /**
   * Get files from project directory.
   * @param filePath The path of the archive project directory.
   * @return <code>Map[String,Option[File]]</code> if it's validated archived project directory.
   */
  private val getProjectFilesAsMap: String => Map[String, Option[File]] = archivedProjPath => {
    val lcmsDbBackup = getFile(archivedProjPath, "^lcms_db_project_[0-9]+.bak$")
    val msiDbBackup = getFile(archivedProjPath, "^msi_db_project_[0-9]+.bak$")
    val projectProperties = getFile(archivedProjPath, "^project_properties.json$")
    Map("lcmsFile" -> lcmsDbBackup, "msiFile" -> msiDbBackup, "projPropFile" -> projectProperties)
  }

  /**
   * get file via its name pattern
   * @param archivedProjPath The project directory path
   * @param pattern The pattern name of file
   */
  private val getFile: (String, String) => Option[File] = (archivedProjPath, pattern) => new File(archivedProjPath).listFiles().find(_.getName.matches(pattern))

  /**
   * Check if the project name is already defined  for a user
   * @param  udsEM The EntityManager.
   * @param ownerId The owner Id.
   * @param name The project name.
   * @param <code>true</code> If the project name already defined for the owner with id ownerId
   */
  private def isTakenProjectName(udsEM: EntityManager, ownerId: Long, name: String): Boolean = {
    logger.info(s"Checking that the project name =#$name is not taken for the user with id=#$ownerId .")
    val PROJECT_NAME_BY_OWNER = "Select p from Project p where p.owner.id=:id and p.name=:name"
    udsEM.createQuery(PROJECT_NAME_BY_OWNER)
      .setParameter("id", ownerId)
      .setParameter("name", name).getResultList.isEmpty
  }

  /**load project data*/
  private def loadProjectData(userOpt: Option[UserAccount], projectName: Option[String], jsValuesMap: Map[String, JsValue], udsEM: EntityManager): Option[Project] = {
    val udsProject = new Project(userOpt.get)
    if (projectName.isDefined && !projectName.get.trim.isEmpty) { udsProject.setName(projectName.get) }
    else {
      val jsProjectName = (jsValuesMap("project").as[JsObject] \ "name").as[String]
      logger.info(s"The project name will be loaded from the project_properties.json file with value = #$jsProjectName.")
      require(isTakenProjectName(udsEM, ownerId, jsProjectName), s"The project name= #$jsProjectName is already taken for the owner with id= #$ownerId! Please rename your project.")
      udsProject.setName((jsValuesMap("project").as[JsObject] \ "name").as[String])
    }
    udsProject.setDescription((jsValuesMap("project").as[JsObject] \ "description").as[String])
    udsProject.setCreationTimestamp(java.sql.Timestamp.valueOf((jsValuesMap("project").as[JsObject] \ "creation_timestamp").as[String]))
    (jsValuesMap("project").as[JsObject] \ "serialized_properties").asOpt[String].foreach(udsProject.setSerializedProperties(_))
    Option(udsProject)

  }

  /** Load external_db data */
  private def loadExetrnalDbData(jsValuesMap: Map[String, JsValue], udsProject: Project): Map[String, ExternalDb] = {
    var extDbsAsMap = Map[String, ExternalDb]()
    try {
      for (externaldb <- jsValuesMap("external_db").as[List[JsObject]]) {
        val udsExternalDb = new ExternalDb()
        udsExternalDb.setDbVersion((externaldb \ "version").as[String])
        udsExternalDb.setHost((externaldb \ "host").as[String])
        udsExternalDb.setPort((externaldb \ "port").as[Int])
        udsExternalDb.setIsBusy((externaldb \ "is_busy").as[Boolean])
        udsExternalDb.setSerializedProperties((externaldb \ "serialized_properties").as[String])
        udsExternalDb.setType(ProlineDatabaseType.valueOf((externaldb \ "type").as[String]))
        udsExternalDb.setConnectionMode(ConnectionMode.valueOf((externaldb \ "connection_mode").as[String]))
        udsExternalDb.setDriverType(DriverType.POSTGRESQL)
        udsExternalDb.addProject(udsProject)
        if ((externaldb \ "name").as[String] contains "msi_db_project_") {
          udsExternalDb.setDbName(s"msi_db_project_${udsProject.getId}")
          extDbsAsMap += ("MSI" -> udsExternalDb)
        } else {
          udsExternalDb.setDbName(s"lcms_db_project_${udsProject.getId}")
          extDbsAsMap += ("LCMS" -> udsExternalDb)
        }
      }
      extDbsAsMap
    } catch {
      case t: Throwable =>
        logger.error("Error occured while trying to create external_db", t)
        extDbsAsMap
    }
  }

  /** Load and restore datasets */
  private def loadDataSets(jsValuesMap: Map[String, JsValue], udsProject: Project, udsEM: EntityManager) {
    var udsDataset: Option[Dataset] = None
    var dataSetIdMap: Map[Long, Dataset] = Map()
    var dataSetParentIdMap: Map[Dataset, Long] = Map()
    for (dataSet <- jsValuesMap("data_set").as[List[JsObject]]) {
      val aggregation = udsEM.find(classOf[Aggregation], (dataSet \ "aggregation_id").as[Long])
      val fraction = udsEM.find(classOf[Fractionation], (dataSet \ "fractionation_id").as[Long])
      val quantitationMethod = udsEM.find(classOf[QuantitationMethod], (dataSet \ "quant_method_id").as[Long])
      if (Dataset.DatasetType.valueOf((dataSet \ "type").as[String]) == Dataset.DatasetType.IDENTIFICATION) {
        udsDataset = Some(new IdentificationDataset())
        udsDataset.get.setType(Dataset.DatasetType.IDENTIFICATION)
      } else {
        udsDataset = Some(new Dataset(udsProject))
        udsDataset.get.setType(Dataset.DatasetType.valueOf((dataSet \ "type").as[String]))
      }
      udsDataset.get.setChildrenCount((dataSet \ "child_count").as[Int])
      udsDataset.get.setCreationTimestamp(java.sql.Timestamp.valueOf((dataSet \ "creation_timestamp").as[String]))
      if (isValidatedProperty((dataSet \ "description").asOpt[String])) {
        udsDataset.get.setDescription((dataSet \ "description").asOpt[String].get)
      } else {
        udsDataset.get.setDescription(null)
      }
      if ((dataSet \ "result_set_id").as[Long] > 0) udsDataset.get.setResultSetId((dataSet \ "result_set_id").as[Long]) else udsDataset.get.setResultSetId(null)
      if ((dataSet \ "result_summary_id").as[Long] > 0) udsDataset.get.setResultSummaryId((dataSet \ "result_summary_id").as[Long]) else udsDataset.get.setResultSummaryId(null)
      if (isValidatedProperty((dataSet \ "keywords").asOpt[String])) {
        udsDataset.get.setKeywords((dataSet \ "keywords").asOpt[String].get)
      } else {
        udsDataset.get.setKeywords(null)
      }
      if (isValidatedProperty((dataSet \ "modification_log").asOpt[String])) {
        udsDataset.get.setModificationLog((dataSet \ "modification_log").asOpt[String].get)
      } else {
        udsDataset.get.setModificationLog(null)
      }
      udsDataset.get.setName((dataSet \ "name").as[String])
      udsDataset.get.setNumber((dataSet \ "number").as[Int])
      if (isValidatedProperty((dataSet \ "serialized_properties").asOpt[String])) {
        udsDataset.get.setSerializedProperties((dataSet \ "serialized_properties").asOpt[String].get)
      } else {
        udsDataset.get.setSerializedProperties(null)
      }
      udsDataset.get.setProject(udsProject)
      udsDataset.get.setFractionation(fraction)
      udsDataset.get.setAggregation(aggregation)
      udsDataset.get.setMethod(quantitationMethod)
      if ((dataSet \ "parent_dataset_id").as[Long] > 0) {
        dataSetParentIdMap += (udsDataset.get -> (dataSet \ "parent_dataset_id").as[Long])
      } else {
        //uds data_set are trash or root
        logger.debug("Loading the data_set: trash or root")
        udsDataset.get.setParentDataset(null)
      }
      dataSetIdMap += ((dataSet \ "id").as[Long] -> udsDataset.get)
      udsEM.persist(udsDataset.get)
    }
    if (!dataSetParentIdMap.isEmpty) {
      for ((ds, parentId) <- dataSetParentIdMap) {
        ds.asInstanceOf[Dataset].setParentDataset(dataSetIdMap.get(parentId).get)
      }
    }
  }
  /**
   *  Read the Json file.
   *  @param file The file path.
   *  return <code>Map[String, JsValue]</code> of JsValue contained in this file.
   */
  private def readJsonFile(file: String): Map[String, JsValue] = {
    var stream: Option[FileInputStream] = None
    var JsValueMap = Map[String, JsValue]()
    try {
      stream = Some(new FileInputStream(new File(file)))
      val json = Json.parse(stream.get)
      JsValueMap += ("schema_version" -> json("schema_version"))
      JsValueMap += ("project" -> json("project"))
      JsValueMap += ("user_account" -> json("user_account"))
      JsValueMap += ("external_db" -> json("external_db"))
      JsValueMap += ("data_set" -> json("data_set"))
      JsValueMap += ("run_identification" -> json("run_identification"))
      JsValueMap += ("project_user_account_map" -> json("project_user_account_map"))
    } catch {
      case t: Throwable => logger.error("Error occured while trying to read project_properties.json file!", t)
    } finally {
      if (stream.isDefined) stream.get.close()
    }
    JsValueMap
  }

  /**
   * Check if it's a validated property
   * @param property
   * @return <code> true</code> if it's a validated property
   */
  private def isValidatedProperty(property: Any): Boolean = property match {
    case Some(property) => true
    case _ => false
  }

  /**
   * Execute command as sequence of strings.
   * @param command the command as sequence of string ,watch out about the white spaces.
   * @return <code>0</code> if the sequence executed successfully.
   *
   */
  private def execute(command: => Seq[String]): Int = {
    var process: Option[Process] = None
    var exitCode: Int = 1
    try {
      logger.debug("Executing " + command.mkString(" "))
      process = Some(Process(command, None, "PGPASSWORD" -> SetupProline.config.udsDBConfig.password).run(ProcessLogger(out => stdout(out), err => stderr(err))))
      exitCode = process.get.exitValue
    } finally {
      if (process.isDefined) {
        process.get.destroy
      }
    }
    logger.debug("Exit code is '" + exitCode + "'")
    if (exitCode != 0) {
      throw new Exception("Command has failed" + command)
    }
    exitCode
  }

  /**
   * execute pg_restore commands in order to pg_restore msi_db and lcms_db databases
   * @param host The host name
   * @param port The port number
   * @param user The user name
   * @param msiDb the msi_db path
   * @param lcmsDb the lcms_db path
   * @param archivedProjectDirPath
   * @param binDirPath
   * @param projectId
   * @param newProjectId
   * @return <code>Success(0)</code> if pg_restore of msi_db and lcms_db databases succeeded.
   */

  private def restoreDatabases(host: String, port: Integer, user: String, msiDb: String, lcmsDb: String, archivedProjectDirPath: String, binDirPath: String, projectId: Long, newProjectId: Long): Boolean =
    try {
      val projectPath = new File(archivedProjectDirPath)
      val pgRestorePath = new File(binDirPath + File.separator + "pg_restore").getCanonicalPath()
      var cmd = Seq(pgRestorePath, "-h", host, "-p", port.toString, "-U", user, "-d", "msi_db_project_" + newProjectId, "-v", projectPath + File.separator + "msi_db_project_" + projectId + ".bak")
      val restoreMsiExitCode = execute(cmd)
      cmd = Seq(pgRestorePath, "-h", host, "-p", port.toString, "-U", user, "-d", "lcms_db_project_" + newProjectId, "-v", projectPath + File.separator + "lcms_db_project_" + projectId + ".bak")
      val restoreLcmsExitCode = execute(cmd)
      Seq(restoreMsiExitCode, restoreLcmsExitCode).forall(_.==(0))
    } catch {
      case e: Exception => throw new Exception(s"Error occured while trying to pg_restore MSI and LCMS databases $e"); false
    }

  /**
   * add properties to project serialized properties
   * @param array JsonObject
   *
   */
  private def addProperties(array: JsonObject) {
    val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    array.addProperty("restore_date", sdf.format(new Date()).toString())
    array.addProperty("is_active", true)
  }

  /**
   * logger debug
   * @param out
   */
  private def stdout(out: String) {
    logger.debug(out)
  }

  /**
   * logger debug or error
   * @param err
   */
  def stderr(err: String) {
    if (err.startsWith("Info: ")) {
      logger.debug(err)
    } else {
      logger.error(err)
    }
  }
}

object RestoreProject {
  /**
   * @param ownerId The owner id of the project.
   * @param binDirPath The bin directory path. It should contains pg_retsore.
   * @param archivedProjPath The path of the archived project.
   * @param projectName To rename the project. It's optional.
   * @return The restored project id.
   */
  def apply(pgUserName: String, ownerId: Long, binDirPath: String, archivedProjPath: String, projectName: Option[String] = None): Long = {
    val prolineConf = SetupProline.config
    var localUdsDbConnector: Boolean = false
    var newProjId: Long = -1L
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
        val restoreProject = new RestoreProject(
          udsDbContext,
          pgUserName,
          ownerId,
          binDirPath,
          archivedProjPath,
          projectName)
        restoreProject.doWork()
        newProjId = restoreProject.newProjId
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
    newProjId
  }
}
