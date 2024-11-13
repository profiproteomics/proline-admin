package fr.proline.admin

import com.beust.jcommander.{ JCommander, MissingCommandException, Parameter, ParameterException, Parameters }
import com.typesafe.scalalogging.LazyLogging
import scala.collection.Set
import fr.profi.util.ThreadLogger
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.service.db.maintenance.DumpDatabase
import fr.proline.admin.service.db.migration.UpgradeAllDatabases
import fr.proline.admin.service.user._
import fr.proline.repository.UncachedDataStoreConnectorFactory

/**
 * Warning : main deprecated. see App :
 * It should also be noted that the `main` method should not be overridden:
 *  the whole class body becomes the “main method”.
 */
object RunCommand extends App with LazyLogging {

  trait JCommandReflection {
    lazy private val _parametersAnnotation = this.getClass().getAnnotation(classOf[Parameters])
    object Parameters {
      lazy val names = _parametersAnnotation.commandNames()
      lazy val firstName = names(0)
      lazy val description = _parametersAnnotation.commandDescription()
    }
  }
  /** set up proline command */

  @Parameters(commandNames = Array("setup"), commandDescription = "Set Up the Proline databases", separators = "=")
  private object SetupProlineCommand extends JCommandReflection

  /** create project command */
  @Parameters(commandNames = Array("create_project"), commandDescription = "Create new project", separators = "=")
  private object CreateProjectCommand extends JCommandReflection {

    @Parameter(names = Array("--owner_id", "-oid"), description = "The user account id of the project owner", required = true)
    var ownerId: Int = 0

    @Parameter(names = Array("--name", "-n"), description = "The project name", required = true)
    var projectName: String = ""

    @Parameter(names = Array("--description", "-desc"), description = "The project description", required = false)
    var projectDescription: String = ""
  }

  /** delete project command */
  @Parameters(commandNames = Array("delete_project"), commandDescription = "Delete project", separators = "=")
  private object DeleteProjectCommand extends JCommandReflection {

    @Parameter(names = Array("--project_id", "-pid"), description = "The project id to delete", required = true)
    var projectId: Int = 0

    @Parameter(names = Array("--drop", "-d"), description = "Delete project and drop entire project databases(MSI and LCMS)", required = false)
    var dropDatabases: Boolean = false
  }

  /** create user command */
  @Parameters(commandNames = Array("create_user"), commandDescription = "Create new user account", separators = "=")
  private object CreateUserCommand extends JCommandReflection {
    @Parameter(names = Array("--login", "-l"), description = "The user account login", required = true)
    var userLogin: String = ""

    @Parameter(names = Array("--password", "-p"), description = "The user password. Default could be used.", required = false)
    var userPassword: String = ""

    @Parameter(names = Array("--administrator", "-a"), description = "Add user to administrator group. User is in user group per default.", required = false)
    var groupUser: Boolean = true
  }

  /** change user group command */
  @Parameters(commandNames = Array("change_user_group"), commandDescription = "Change user group", separators = "=")
  private object ChangeUserGroupCommand extends JCommandReflection {
    @Parameter(names = Array("--user_id", "-uid"), description = "The user id.", required = true)
    var userId: Long = 0
    @Parameter(names = Array("--administrator", "-a"), description = "Add user to administrator group. User is in user group per default.", required = false)
    var groupUser: Boolean = true
  }

  /** dump msi db command */
  @Parameters(commandNames = Array("dump_msi_db"), commandDescription = "Dump MSIdb content into an XML file", separators = "=")
  private object DumpMsiDbCommand extends JCommandReflection {

    @Parameter(names = Array("--project_id", "-p"), description = "The id of the project corresponding to this MSIdb", required = true)
    var projectId: Int = 0

    @Parameter(names = Array("--file_path", "-f"), description = "The path of the XML file to be generated", required = true)
    var filePath: String = ""
  }

  /** dump lcms db  command */
  @Parameters(commandNames = Array("dump_lcms_db"), commandDescription = "Dump LCMSdb content into an XML file", separators = "=")
  private object DumpLcmsDbCommand extends JCommandReflection {
    @Parameter(names = Array("--project_id", "-p"), description = "The id of the project corresponding to this MSIdb", required = true)
    var projectId: Int = 0

    @Parameter(names = Array("--file_path", "-f"), description = "The path of the XML file to be generated", required = true)
    var filePath: String = ""
  }

  /** dump uds db  command */
  @Parameters(commandNames = Array("dump_uds_db"), commandDescription = "Dump UDSdb content into an XML file", separators = "=")
  private object DumpUdsDbCommand extends JCommandReflection {
    @Parameter(names = Array("--file_path", "-f"), description = "The path of the XML file to be generated", required = true)
    var filePath: String = ""
  }

  /** export dbunits command */
  @Parameters(commandNames = Array("export_dbunit_dtds"), commandDescription = "Export DBUnit DTD files", separators = "=")
  private object ExportDbUnitDTDsCommand extends JCommandReflection {
    @Parameter(names = Array("--dir_path", "-d"), description = "The path to the export directory", required = true)
    var dirPath: String = ""
  }
  /** export msidb stats command */
  @Parameters(commandNames = Array("export_msidb_stats"), commandDescription = "Export some statitics about MSIdb", separators = "=")
  private object ExportMsiDbStatsCommand extends JCommandReflection {
    @Parameter(names = Array("--dir_path", "-d"), description = "The path to the export directory", required = true)
    var dirPath: String = ""
  }

  /** create missing dbs command */
  @Parameters(commandNames = Array("create_missing_dbs"), commandDescription = "Create missing MSI and LCMS databases", separators = "=")
  private object CreateMissingDbsCommand extends JCommandReflection

  /** upgarde all dbs command */
  @Parameters(commandNames = Array("upgrade_dbs"), commandDescription = "Upgrade all databases to the latest format", separators = "=")
  private object UpgradeDatabasesCommand extends JCommandReflection

  /** Check dbs version and get available updates command */
  @Parameters(commandNames = Array("check_for_updates"), commandDescription = "Check Proline databases version and search for available updates", separators = "=")
  private object CheckDbsCommand extends JCommandReflection

  /** delete proline obsolete databases command */
  @Parameters(commandNames = Array("delete_obsolete_dbs"), commandDescription = "Delete obsolete Proline databases", separators = "=")
  private object DeleteObsoleteDatabasesCommand extends JCommandReflection

  /** archive project command */
  @Parameters(commandNames = Array("archive_project"), commandDescription = "Archive project", separators = "=")
  private object ArchiveProjectCommand extends JCommandReflection {

    @Parameter(names = Array("--project_id", "-pid"), description = "The project id to archive", required = true)
    var projectId: Int = 0

    @Parameter(names = Array("--pg_bin_directory", "-bd"), description = "The path of the bin directory of PostreSQL", required = true)
    var binDirPath: String = ""

    @Parameter(names = Array("--archive_directory_path", "-ad"), description = "The path of archive directory where the project will be archived", required = true)
    var archiveProjectDirPath: String = ""
  }

  /** change project state command */
  @Parameters(commandNames = Array("change_project_state"), commandDescription = "Change project state", separators = "=")
  private object ChangeProjectStateCommand extends JCommandReflection {

    @Parameter(names = Array("--project_id", "-pid"), description = "The project id to activate or to disable", required = true)
    var projectId: Long = 0

    @Parameter(names = Array("--disable", "-d"), description = "Disable project. The project is activated per default.", required = false)
    var isActive: Boolean = true
  }
  /** Change project owner command */
  @Parameters(commandNames = Array("change_project_owner"), commandDescription = "Change project owner", separators = "=")
  private object ChangeProjectOwnerCommand extends JCommandReflection {

    @Parameter(names = Array("--project_id", "-pid"), description = "The project id to change its owner", required = true)
    var projectId: Int = 0
    @Parameter(names = Array("--owner_id", "-oid"), description = "The user account id of the new project owner", required = true)
    var userId: Int = 0
  }
  /** restore project command */
  @Parameters(commandNames = Array("restore_project"), commandDescription = "Restore project", separators = "=")
  private object RestoreProjectCommand extends JCommandReflection {

    @Parameter(names = Array("--name", "-n"), description = "Rename project", required = false)
    var name: String = ""

    @Parameter(names = Array("--owner_id", "-oid"), description = "The user account id of the project owner", required = true)
    var ownerId: Int = 0

    @Parameter(names = Array("--pg_bin_directory", "-bd"), description = "The path of the bin directory of PostreSQL", required = true)
    var BinDirectoryPath: String = ""

    @Parameter(names = Array("--project_directory_path", "-pd"), description = "The path of the project directory from where the project will be restored", required = true)
    var projectDirectoryPath: String = ""
  }

  /** reset  command */
  @Parameters(commandNames = Array("change_password"), commandDescription = "Change user password", separators = "=")
  private object ChangePasswordCommand extends JCommandReflection {

    @Parameter(names = Array("--user_id", "-uid"), description = "The user account id ", required = true)
    var userId: Int = 0
    @Parameter(names = Array("--new_password", "-np"), description = "The new password", required = false)
    var password: String = ""
  }

  /** change user state command */

  @Parameters(commandNames = Array("change_user_state"), commandDescription = "Change user state", separators = "=")
  private object ChangeUserStateCommand extends JCommandReflection {

    @Parameter(names = Array("--user_id", "-uid"), description = "The user account id to activate or to disable", required = true)
    var userId: Long = 0
    @Parameter(names = Array("--Disable", "-d"), description = "Disable user. The user is activated per default.", required = false)
    var isActive: Boolean = true
  }

  var hasDsConnectorFactory = false
  lazy val dsConnectorFactory: UncachedDataStoreConnectorFactory = {

    // Instantiate a database manager
    val dsConnectorFactory = UncachedDataStoreConnectorFactory.getInstance()
    if (!dsConnectorFactory.isInitialized) {
      dsConnectorFactory.initialize(SetupProline.config.udsDBConfig.toNewConnector)
    }
    hasDsConnectorFactory = true
    dsConnectorFactory
  }

    Thread.currentThread.setUncaughtExceptionHandler(new ThreadLogger(logger.underlying.getName()))

    // Instantiate a JCommander object and affect some commands
    val jCmd = new JCommander()
    jCmd.addCommand(SetupProlineCommand)
    jCmd.addCommand(UpgradeDatabasesCommand)
    jCmd.addCommand(CheckDbsCommand)
    jCmd.addCommand(CreateUserCommand)
    jCmd.addCommand(CreateProjectCommand)
    jCmd.addCommand(DeleteProjectCommand)
    jCmd.addCommand(ChangeProjectStateCommand)
    jCmd.addCommand(ChangeProjectOwnerCommand)
    jCmd.addCommand(ChangePasswordCommand)
    jCmd.addCommand(ChangeUserGroupCommand)
    jCmd.addCommand(ChangeUserStateCommand)
    jCmd.addCommand(DumpMsiDbCommand)
    jCmd.addCommand(DumpUdsDbCommand)
    jCmd.addCommand(DumpLcmsDbCommand)
    //jCmd.addCommand(DeleteObsoleteDatabasesCommand)
    jCmd.addCommand(ExportDbUnitDTDsCommand)
    jCmd.addCommand(ExportMsiDbStatsCommand)
    jCmd.addCommand(ArchiveProjectCommand)
    jCmd.addCommand(RestoreProjectCommand)

    // Try to parse the command line
    var parsedCommand = ""
    try {
      jCmd.parse(args: _*)

      parsedCommand = jCmd.getParsedCommand()
      println("Running '" + parsedCommand + "' command...")

      // Execute parsed command
      parsedCommand match {
        case SetupProlineCommand.Parameters.firstName => {
          // Run the setup proline service
          SetupProline()
        }
        case CreateProjectCommand.Parameters.firstName => {

          import fr.proline.admin.service.user.CreateProject

          val projectId = CreateProject(
            CreateProjectCommand.projectName,
            CreateProjectCommand.projectDescription,
            CreateProjectCommand.ownerId)
          this.logger.info(s"Project with id=$projectId has been created !")
        }

        case DeleteProjectCommand.Parameters.firstName => {
          import fr.proline.admin.service.user.DeleteProject
          DeleteProject(
            Set(DeleteProjectCommand.projectId),
            DeleteProjectCommand.dropDatabases)
        }
        case ChangeProjectOwnerCommand.Parameters.firstName => {
          ChangeProjectOwner(
            ChangeProjectOwnerCommand.projectId,
            ChangeProjectOwnerCommand.userId)
        }
        
        case ArchiveProjectCommand.Parameters.firstName => {
          import fr.proline.admin.service.user.ArchiveProject
          ArchiveProject(
            SetupProline.config.udsDBConfig.userName,
            ArchiveProjectCommand.projectId,
            ArchiveProjectCommand.binDirPath,
            ArchiveProjectCommand.archiveProjectDirPath)
        }
        case ChangeProjectStateCommand.Parameters.firstName => {
          ChangeProjectState(
            Set(ChangeProjectStateCommand.projectId),
            ChangeProjectStateCommand.isActive)
        }
        case RestoreProjectCommand.Parameters.firstName => {
          import fr.proline.admin.service.user.RestoreProject
          RestoreProject(
            SetupProline.config.udsDBConfig.userName,
            RestoreProjectCommand.ownerId,
            RestoreProjectCommand.BinDirectoryPath,
            RestoreProjectCommand.projectDirectoryPath,
            Option(RestoreProjectCommand.name))
        }
        case CreateUserCommand.Parameters.firstName => {
          val pswd = if (CreateUserCommand.userPassword.isEmpty()) None else Some(CreateUserCommand.userPassword)
          CreateUser(CreateUserCommand.userLogin, pswd, Option(CreateUserCommand.groupUser), Option(false))
        }
        case ChangeUserGroupCommand.Parameters.firstName => {
          ChangeUserGroup(Set(ChangeUserGroupCommand.userId),
            ChangeUserGroupCommand.groupUser)
        }
        case ChangePasswordCommand.Parameters.firstName => {
          val pswd = if (ChangePasswordCommand.password.isEmpty()) None else Some(ChangePasswordCommand.password)
          ChangePassword(ChangePasswordCommand.userId, pswd)
        }
        case DumpMsiDbCommand.Parameters.firstName => {
          import fr.proline.admin.service.db.maintenance.DumpDatabase
          val msiDbConnector = dsConnectorFactory.getMsiDbConnector(DumpMsiDbCommand.projectId)
          DumpDatabase(msiDbConnector, DumpMsiDbCommand.filePath)
        }
        case ChangeUserStateCommand.Parameters.firstName => {
          ChangeUserState(Set(ChangeUserStateCommand.userId),
            ChangeUserStateCommand.isActive)
        }
        case DumpUdsDbCommand.Parameters.firstName => {
          import fr.proline.admin.service.db.maintenance.DumpDatabase
          DumpDatabase(dsConnectorFactory.getUdsDbConnector, DumpUdsDbCommand.filePath)
        }
        case DumpLcmsDbCommand.Parameters.firstName => {
          import fr.proline.admin.service.db.maintenance.DumpDatabase
          DumpDatabase(dsConnectorFactory.getLcMsDbConnector(DumpLcmsDbCommand.projectId), DumpLcmsDbCommand.filePath)
        }
        case DeleteObsoleteDatabasesCommand.Parameters.firstName => {
          if (dsConnectorFactory.isInitialized) {
            DeleteObsoleteDbs(dsConnectorFactory)
          } else {
            logger.error("Unable to initialize DataStoreConnectorFactory instance")
          }

        }
        case ExportDbUnitDTDsCommand.Parameters.firstName => {
          import fr.proline.admin.service.db.maintenance.ExportDbUnitDTDs
          ExportDbUnitDTDs(dsConnectorFactory, ExportDbUnitDTDsCommand.dirPath)
        }
        case ExportMsiDbStatsCommand.Parameters.firstName => {
          import fr.proline.admin.service.db.maintenance.ExportMsiDbStats
          ExportMsiDbStats(dsConnectorFactory, ExportMsiDbStatsCommand.dirPath)
        }
        case CreateMissingDbsCommand.Parameters.firstName => {
          helper.sql.createMissingDatabases(SetupProline.config.udsDBConfig, dsConnectorFactory)
        }
        case UpgradeDatabasesCommand.Parameters.firstName => {
          if (dsConnectorFactory.isInitialized) {
            logger.info("Upgrading all Proline Databases...")

            UpgradeAllDatabases(dsConnectorFactory)
          } else {
            logger.error("Unable to initialize DataStoreConnectorFactory instance")
          }
        }
        case CheckDbsCommand.Parameters.firstName => {
          if (dsConnectorFactory.isInitialized) {
            logger.info("Checking Proline databases version and search for available updates...")
            CheckForUpdates(dsConnectorFactory)

          } else {
            logger.error("Unable to initialize DataStoreConnectorFactory instance")
          }
        }
        case _ => {
          throw new MissingCommandException("unknown command '" + jCmd.getParsedCommand() + "'")
        }
      }

    } catch {

      case pEx: ParameterException => {
        println()
        logger.warn("Invalid command or parameter", pEx)
        jCmd.usage()
      }

      case ex: Exception => {
        println()
        logger.error("Execution of command '" + parsedCommand + "' failed", ex)
      }

    } finally {
      if (hasDsConnectorFactory) dsConnectorFactory.closeAll()
    }

}