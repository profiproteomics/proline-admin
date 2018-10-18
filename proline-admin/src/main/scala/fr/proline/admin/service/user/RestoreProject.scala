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
import fr.proline.core.dal.DoJDBCReturningWork
import javax.persistence.{ EntityManager, EntityTransaction, FlushModeType }
import scala.util.{ Try, Success, Failure }
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
 * @param udsDbContext The connection context to UDSDb to restore project into.
 * @param ownerId The owner id of the project.
 * @param binDirPath The PostgreSQL bin directory path. It should contains pg_retsore file.
 * @param archivedProjPath The path of the archived project directory.
 * @param projectName To rename the project to restore. It's advised when the project name already defined for the same user.
 *
 */

class RestoreProject(
    udsDbCtx: DatabaseConnectionContext,
    pgUserName: String,
    ownerId: Long,
    binDirPath: String,
    archivedProjDirPath: String,
    projectName: Option[String]) extends ICommandWork with LazyLogging {
  var projectId: Long = -1L
  var newProjId: Long = -1L
  def doWork() {
    val udsEM = udsDbCtx.getEntityManager
    var localUdsTransaction: EntityTransaction = null
    var isUdsTxOk: Boolean = false
    var isCreationDbsOk: Boolean = false
    try {
      logger.debug("Check the input parameters of restore_project.")
      /* 1.0- Check the input parameters */
      val projFilesAsMap = getProjectFilesAsMap(archivedProjDirPath)
      require(isDefinedBinDir(binDirPath).isDefined && isReadableArchiveProjDir(archivedProjDirPath), s"Invalid parameters. Make sure that the bin directory has pg_restore.exe file and you are allowed to read from the project directory.")
      require(isDefinedProjectDir(projFilesAsMap), "The project directory must contain the SQL backup files and the project_properties.json file.")
      udsEM.setFlushMode(FlushModeType.COMMIT)
      if (!udsDbCtx.isInTransaction) {
        localUdsTransaction = udsEM.getTransaction()
        localUdsTransaction.begin()
      }
      logger.debug("restore a project for an existing owner.")
      /* 1.1- Restore the project for an existing user */
      val userOpt = Option(udsEM.find(classOf[UserAccount], ownerId))
      require(userOpt.isDefined, s"Undefined user with id= #$ownerId. Please restore the project with an existing user account.")
      /* 1.2- Rename the project,the new name must not be defined twice for the same owner Id(project_name_owner_idx) */
      projectName.foreach { name =>
        require(isDefinedProject(udsEM, ownerId, name), s"The project name= #$name already defined for the owner with id= #$ownerId. Please rename your project.")
      }
      logger.debug("restore a project for an existing owner.")
      /* 2.0- Read project_properties file and get Json object in a Map */
      val projPropFile = projFilesAsMap("projPropFile").get
      val jsValuesMap = readJsonFile(projPropFile.getAbsolutePath)
      require(!jsValuesMap.isEmpty, "The json values must not be empty.")
      try {
        logger.info("Start to restore your project. Please wait until the project will be restored...")
        var parser = new JsonParser()
        var array: JsonObject = null
        projectId = (jsValuesMap("project").as[JsObject] \ "id").as[Long]
        val jsDbVersion = (jsValuesMap("schema_version").as[JsObject] \ "version").as[Int]
        var JsHost, JsMsiDbName, JsLcmsDbName = ""
        var JsPort, dbVersion = 0
        var newProjectId = 0L
        var udsDataset: Option[Dataset] = None
        var dataSetIdMap: Map[Long, Dataset] = Map()
        var dataSetParentIdMap: Map[Dataset, Long] = Map()
        DoJDBCWork.withEzDBC(udsDbCtx) { ezDBC =>
          ezDBC.selectAndProcess(s"SELECT MAX(version_rank) AS version FROM schema_version") { record =>
            dbVersion = record.getInt("version")
          }
        }
        /* 2.1- Check the schema version */
        require((jsDbVersion == dbVersion) && (jsDbVersion >= 8), "The schema version of the databases are different. Make sure that your databases are updated.")
        /* 3.0- Get project properties */
        val udsProject = new Project(userOpt.get)
        val restoreProjProps =
          Try {
            if (projectName.isDefined && !projectName.get.trim.isEmpty) { udsProject.setName(projectName.get) }
            else {
              val jsProjectName = (jsValuesMap("project").as[JsObject] \ "name").as[String]
              require(isDefinedProject(udsEM, ownerId, jsProjectName), s"The project name= #$jsProjectName from json file is already defined for the owner with id= #$ownerId. Please rename your project!")
              udsProject.setName((jsValuesMap("project").as[JsObject] \ "name").as[String])
            }
            udsProject.setDescription((jsValuesMap("project").as[JsObject] \ "description").as[String])
            udsProject.setCreationTimestamp(java.sql.Timestamp.valueOf((jsValuesMap("project").as[JsObject] \ "creation_timestamp").as[String]))
            (jsValuesMap("project").as[JsObject] \ "serialized_properties").asOpt[String].foreach(udsProject.setSerializedProperties(_))
            udsEM.persist(udsProject)
            newProjectId = udsProject.getId()
            val properties = udsProject.getSerializedProperties()
            array = Try { parser.parse(properties).getAsJsonObject() } getOrElse { parser.parse("{}").getAsJsonObject() }
            /* 3.1- load the project properties ... */
            logger.info("Importing project properties from project_properties.json file...")
            logger.debug("import external_db rows.")
            //import  externalDb data 
            for (externaldb <- jsValuesMap("external_db").as[List[JsObject]]) {
              val udsExternalDb = new ExternalDb()
              if ((externaldb \ "name").as[String] contains "msi_db_project_") {
                udsExternalDb.setDbName(s"msi_db_project_$newProjectId")
                JsMsiDbName = (externaldb \ "name").as[String]
              } else {
                udsExternalDb.setDbName(s"lcms_db_project_$newProjectId")
                JsLcmsDbName = (externaldb \ "name").as[String]
              }
              udsExternalDb.setDbVersion((externaldb \ "version").as[String])
              udsExternalDb.setHost((externaldb \ "host").as[String])
              JsHost = (externaldb \ "host").as[String]
              udsExternalDb.setPort((externaldb \ "port").as[Int])
              JsPort = (externaldb \ "port").as[Int]
              udsExternalDb.setIsBusy((externaldb \ "is_busy").as[Boolean])
              udsExternalDb.setSerializedProperties((externaldb \ "serialized_properties").as[String])
              udsExternalDb.setType(ProlineDatabaseType.valueOf((externaldb \ "type").as[String]))
              udsExternalDb.setConnectionMode(ConnectionMode.valueOf((externaldb \ "connection_mode").as[String]))
              udsExternalDb.setDriverType(DriverType.POSTGRESQL)
              udsExternalDb.addProject(udsProject)
              udsEM.persist(udsExternalDb)
              udsProject.addExternalDatabase(udsExternalDb)
            }
            logger.debug("import data_set rows.")
            // import dataSet data
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
                //udsDatset are trash or root
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

        restoreProjProps match {
          case Success(s) => {
            if (localUdsTransaction != null) {
              logger.debug("commit transaction and save project properties.")
              localUdsTransaction.commit()
              isUdsTxOk = true
              logger.info("Project properties have been imported successfully.")
            }
          }
          case Failure(t) =>
            {
              if ((localUdsTransaction != null) && !isUdsTxOk && udsDbCtx.getDriverType() != DriverType.SQLITE) {
                logger.info("Rollbacking current UDS Db Transaction: ", t.printStackTrace())
                try {
                  localUdsTransaction.rollback()
                } catch {
                  case ex: Exception => logger.error("Error rollbacking UDS Db Transaction", ex)
                }
              }
              logger.error("Error while trying to restore your project.")
            }
        }

        /* 4.0- create externalDb */
        if (isUdsTxOk) {
          val createDatabases =
            Try {
              logger.info(s"Creating msi_db_project_$newProjectId and lcms_db_project_$newProjectId ... ")
              DoJDBCWork.withEzDBC(udsDbCtx) { ezDBC =>
                ezDBC.execute(s"CREATE DATABASE msi_db_project_$newProjectId")
                ezDBC.execute(s"CREATE DATABASE lcms_db_project_$newProjectId")
              }
              isCreationDbsOk = true
            } recover {
              case t: Throwable =>
                {
                  udsDbCtx.rollbackTransaction()
                  logger.error("Error while trying to create the project databases: ", t.printStackTrace())
                  isCreationDbsOk = false
                }
            }
          /* 5.0- execute pg restore commands to back up databases */
          if (isCreationDbsOk) {
            logger.info("Execute pg_restore commands to restore msi_db and lcms_db. Please wait ...")
            val isRestoredDatabases = restoreDatabases(JsHost, JsPort, pgUserName, JsMsiDbName, pgUserName, archivedProjDirPath, binDirPath, projectId, newProjectId)
            isRestoredDatabases match {
              case Success(s) => {
                /* 6.0- update project serialized properties */
                udsDbCtx.tryInTransaction {
                  addProperties(array)
                  udsProject.setSerializedProperties(array.toString())
                  udsEM.merge(udsProject)
                }
                newProjId = newProjectId
                logger.info(s"The Project with id #$projectId has been restored with a new project id #$newProjectId .")
              }
              case Failure(t) => {
                logger.error("Error while trying to restore msi_db_project and lcms_db_project databases!", t.printStackTrace)
              }
            }
          }
        }
      } catch {
        case t: Throwable => logger.error("Error while trying to restore the project.", t.printStackTrace)
      }
    } finally {
      udsEM.setFlushMode(FlushModeType.AUTO)
    }
  }

  /**
   * Check if it's a defined and validated bin directory.
   * @param binDirPath The path of the bin directory.
   * @return <code>Some(File)</code> if it's validated bin directory otherwise None.
   */
  val isDefinedBinDir: String => Option[File] = binDirPath => { if (new File(binDirPath).exists) new File(binDirPath).listFiles().find(file => file.getName.matches("^pg_restore.exe$") && file.canExecute()) else None }

  /**
   * Check if user can read from the archive directory.
   * @param filePath The path of the archive directory.
   * @return <code>true</code> if it's validated archived project directory otherwise false.
   */
  val isReadableArchiveProjDir: String => Boolean = (filePath: String) => (new File(filePath).canRead())

  /**
   * Check if it's a validated and defined project directory.
   * @param projectDirFiles  a map of eventual files from  the project directory.
   * @return <code>true</code> if it's validated archived project directory otherwise false.
   */
  val isDefinedProjectDir: Map[String, Option[File]] => Boolean = projectDirFilesasMap => {
    projectDirFilesasMap.values.forall(_.isDefined)
  }

  /**
   * Get files from project directory.
   * @param filePath The path of the archive project directory.
   * @return <code>Map[String,Option[File]]</code> if it's validated archived project directory.
   */
  val getProjectFilesAsMap: String => Map[String, Option[File]] = archivedProjPath => {
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
  val getFile: (String, String) => Option[File] = (archivedProjPath, pattern) => new File(archivedProjPath).listFiles().find(_.getName.matches(pattern))

  /**
   * Check if the project name is already defined  for a user
   * @param  udsEM The EntityManager.
   * @param ownerId The owner Id.
   * @param name The project name.
   * @param <code>true</code> If the project name already defined for the owner with id ownerId
   */
  val isDefinedProject = (udsEM: EntityManager, ownerId: Long, name: String) => {
    val PROJECT_BY_OWNER_AND_NAME = "Select p from Project p where p.owner.id=:id and p.name=:name"
    udsEM.createQuery(PROJECT_BY_OWNER_AND_NAME)
      .setParameter("id", ownerId)
      .setParameter("name", name).getResultList.isEmpty()
  }

  /**
   *  Read the Json file.
   *  @param file The file path.
   *  return <code>Map[String, JsValue]</code> of JsValue contained in this file.
   */
  def readJsonFile(file: String): Map[String, JsValue] = {
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
      case t: Throwable => logger.error("Error while trying to read project_properties.json file!")
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
  def isValidatedProperty(property: Any): Boolean = property match {
    case Some(property) => true
    case _ => false
  }

  /**
   * Execute command as sequence of strings.
   * @param command the command as sequence of string ,watch out about the white spaces.
   * @return <code>0</code> if the sequence executed successfully.
   *
   */
  def execute(command: => Seq[String]): Int = {
    var process: Option[Process] = None
    var exitCode: Int = 1
    try {
      logger.debug("Executing " + command.mkString(" "))
      process = Some(Process(command).run(ProcessLogger(out => stdout(out), err => stderr(err))))
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

  def restoreDatabases(host: String, port: Integer, user: String, msiDb: String, lcmsDb: String, archivedProjectDirPath: String, binDirPath: String, projectId: Long, newProjectId: Long): Try[Boolean] =
    Try {
      val projectPath = new File(archivedProjectDirPath)
      val pgRestorePath = new File(binDirPath + File.separator + "pg_restore").getCanonicalPath()
      var cmd = Seq(pgRestorePath, "-h", host, "-p", port.toString, "-U", user, "-d", "msi_db_project_" + newProjectId, "-v", projectPath + File.separator + "msi_db_project_" + projectId + ".bak")
      val restoreMsiExitCode = execute(cmd)
      cmd = Seq(pgRestorePath, "-h", host, "-p", port.toString, "-U", user, "-d", "lcms_db_project_" + newProjectId, "-v", projectPath + File.separator + "lcms_db_project_" + projectId + ".bak")
      val restoreLcmsExitCode = execute(cmd)
      Seq(restoreMsiExitCode, restoreLcmsExitCode).forall(_.==(0))
    }

  /**
   * add properties to project serialized properties
   * @param array JsonObject
   *
   */
  def addProperties(array: JsonObject) {
    val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    array.addProperty("restore_date", sdf.format(new Date()).toString())
    array.addProperty("is_active", true)
  }

  /**
   * logger debug
   * @param err
   */
  def stdout(out: String) {
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
