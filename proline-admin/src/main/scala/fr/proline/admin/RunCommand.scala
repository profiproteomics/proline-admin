package fr.proline.admin

import com.beust.jcommander.{ JCommander, MissingCommandException, Parameter, ParameterException, Parameters }
import com.typesafe.scalalogging.LazyLogging

import fr.profi.util.ThreadLogger
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.service.db.maintenance.DumpDatabase
import fr.proline.admin.service.db.migration.UpgradeAllDatabases
import fr.proline.admin.service.user.{ CreateProject, CreateUser, DisableUser, DeleteProject, ArchiveProject, UnarchiveProject, RestoreProject, ResetPassword }
import fr.proline.repository.UncachedDataStoreConnectorFactory

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

    @Parameter(names = Array("--drop", "-d"), description = "Drop entire project databases (MSI and LCMS)", required = false)
    var dropDatabases: Boolean = false

  }

  /** create user command */
  @Parameters(commandNames = Array("create_user"), commandDescription = "Create new user account", separators = "=")
  private object CreateUserCommand extends JCommandReflection {
    @Parameter(names = Array("--login", "-l"), description = "The user account login", required = true)
    var userLogin: String = ""

    @Parameter(names = Array("--password", "-p"), description = "The user password. Default could be used.", required = false)
    var userPassword: String = ""

    @Parameter(names = Array("--administrator", "-a"), description = "Add user to administrator group. user is in user group per default.", required = false)
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

  /** dump ps db  */
  @Parameters(commandNames = Array("dump_ps_db"), commandDescription = "Dump PSdb content into an XML file", separators = "=")
  private object DumpPsDbCommand extends JCommandReflection {
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

  /** archive project command */
  @Parameters(commandNames = Array("archive_project"), commandDescription = "archive project", separators = "=")
  private object ArchiveProjectCommand extends JCommandReflection {

    @Parameter(names = Array("--project_id", "-pid"), description = "The project id to archive", required = true)
    var projectId: Int = 0

    @Parameter(names = Array("--postgreSQL_bin_directory", "-db"), description = "The path of the directory bin of PostreSQL", required = true)
    var BinDirectoryPath: String = ""

    @Parameter(names = Array("--project_directory_path", "-dd"), description = "The path of the directory where the project will be stored", required = true)
    var projectDirectoryPath: String = ""
  }

  /** unarchive project command */
  @Parameters(commandNames = Array("unarchive_project"), commandDescription = "unarchive project", separators = "=")
  private object UnarchiveProjectCommand extends JCommandReflection {

    @Parameter(names = Array("--project_id", "-pid"), description = "The project id to unarchive", required = true)
    var projectId: Int = 0
  }

  /** restore project command */
  @Parameters(commandNames = Array("restore_project"), commandDescription = "restore project", separators = "=")
  private object RestoreProjectCommand extends JCommandReflection {

    @Parameter(names = Array("--project_id", "-pid"), description = "The project id to restore ", required = true)
    var projectId: Int = 0

    @Parameter(names = Array("--owner_id", "-oid"), description = "The user account id of the project owner", required = true)
    var ownerId: Int = 0

    @Parameter(names = Array("--postgreSQL_bin_directory", "-db"), description = "The path of the directory bin of PostreSQL", required = true)
    var BinDirectoryPath: String = ""

    @Parameter(names = Array("--project_directory_path", "-dd"), description = "The path of the directory from where the project will be restored", required = true)
    var projectDirectoryPath: String = ""
  }

  /** reset  command */
  @Parameters(commandNames = Array("reset_password"), commandDescription = "reset password", separators = "=")
  private object ResetPasswordCommand extends JCommandReflection {

    @Parameter(names = Array("--user_id", "-uid"), description = "The user account id ", required = true)
    var userId: Int = 0
    @Parameter(names = Array("--new_password", "-np"), description = "The new password", required = false)
    var password: String = ""
  }

  /** disable user command */

  @Parameters(commandNames = Array("disable_user"), commandDescription = "disable user", separators = "=")
  private object DisableUserCommand extends JCommandReflection {

    @Parameter(names = Array("--user_id", "-uid"), description = "The user account id ", required = true)
    var userId: Int = 0
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

  override def main(args: Array[String]): Unit = {
    Thread.currentThread.setUncaughtExceptionHandler(new ThreadLogger(logger.underlying.getName()))

    // Instantiate a JCommander object and affect some commands
    val jCmd = new JCommander()
    jCmd.addCommand(SetupProlineCommand)
    jCmd.addCommand(CreateProjectCommand)
    jCmd.addCommand(CreateUserCommand)
    jCmd.addCommand(ResetPasswordCommand)
    jCmd.addCommand(DumpMsiDbCommand)
    jCmd.addCommand(DumpPsDbCommand)
    jCmd.addCommand(DumpUdsDbCommand)
    jCmd.addCommand(ExportDbUnitDTDsCommand)
    jCmd.addCommand(ExportMsiDbStatsCommand)
    jCmd.addCommand(UpgradeDatabasesCommand)
    jCmd.addCommand(DeleteProjectCommand)
    jCmd.addCommand(ArchiveProjectCommand)
    //jCmd.addCommand(UnarchiveProjectCommand)
    //jCmd.addCommand(RestoreProjectCommand)
    jCmd.addCommand(DisableUserCommand)

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
          //create default Admin user
          logger.info("Creating default admin user")
          CreateUser("admin",Option("proline"),Option(true))
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
          this.logger.info(s"Project with id=${DeleteProjectCommand.projectId} will be deleted !")
          val projectId = DeleteProject(dsConnectorFactory, DeleteProjectCommand.projectId, DeleteProjectCommand.dropDatabases)
        }
        case ArchiveProjectCommand.Parameters.firstName => {
          import fr.proline.admin.service.user.ArchiveProject

          ArchiveProject(dsConnectorFactory, ArchiveProjectCommand.projectId, ArchiveProjectCommand.BinDirectoryPath, ArchiveProjectCommand.projectDirectoryPath)
        }
        case UnarchiveProjectCommand.Parameters.firstName => {
          import fr.proline.admin.service.user.UnarchiveProject

          UnarchiveProject(dsConnectorFactory, UnarchiveProjectCommand.projectId)
        }
        case RestoreProjectCommand.Parameters.firstName => {
          import fr.proline.admin.service.user.RestoreProject

          RestoreProject(dsConnectorFactory, RestoreProjectCommand.projectId, RestoreProjectCommand.ownerId, RestoreProjectCommand.BinDirectoryPath, RestoreProjectCommand.projectDirectoryPath)
        }
        case CreateUserCommand.Parameters.firstName => {
          import fr.proline.admin.service.user.CreateUser
          val pswd = if (CreateUserCommand.userPassword.isEmpty()) None else Some(CreateUserCommand.userPassword)
          CreateUser(CreateUserCommand.userLogin, pswd, Option(CreateUserCommand.groupUser))
        }

        case ResetPasswordCommand.Parameters.firstName => {

          val pswd = if (ResetPasswordCommand.password.isEmpty()) None else Some(ResetPasswordCommand.password)
          ResetPassword(ResetPasswordCommand.userId, pswd)
        }
        case DumpMsiDbCommand.Parameters.firstName => {
          import fr.proline.admin.service.db.maintenance.DumpDatabase
          val msiDbConnector = dsConnectorFactory.getMsiDbConnector(DumpMsiDbCommand.projectId)
          DumpDatabase(msiDbConnector, DumpMsiDbCommand.filePath)
        }
        case DisableUserCommand.Parameters.firstName => {
          DisableUser(DisableUserCommand.userId)
        }
        case DumpPsDbCommand.Parameters.firstName => {
          import fr.proline.admin.service.db.maintenance.DumpDatabase
          DumpDatabase(dsConnectorFactory.getPsDbConnector, DumpPsDbCommand.filePath)
        }
        case DumpUdsDbCommand.Parameters.firstName => {
          import fr.proline.admin.service.db.maintenance.DumpDatabase
          DumpDatabase(dsConnectorFactory.getUdsDbConnector, DumpUdsDbCommand.filePath)
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

}