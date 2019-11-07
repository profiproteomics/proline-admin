package fr.proline.admin.gui.monitor.database

import com.typesafe.scalalogging.LazyLogging

import fr.proline.core.dal.context._
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.orm.uds.UserAccount
import fr.proline.admin.service.user.{ ChangeUserState, ChangeUserGroup, CreateUser, ChangePassword }
import fr.proline.admin.gui.monitor.model.AdapterModel._
import fr.proline.admin.gui.process.UdsRepository
import scala.collection.JavaConverters._
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import fr.profi.util.security._
/**
 * UsersDB perform some operations (add, update, delete and change password) with Proline databases.
 * @author aromdhani
 *
 */
object UsersDB extends LazyLogging {

  lazy val udsDbCtx: DatabaseConnectionContext = UdsRepository.getUdsDbContext()

  /** TODO Remove all users to clean UDS database ?  */
  def clear(): Unit = {

  }
  /** Return a sequence of users to initialize the table view */
  def initialize(): Seq[User] = {
    queryUsersAsView()
  }

  /** Wrap a sequence of usersAcount as sequence of User */
  def queryUsersAsView(): Seq[User] = {
    queryUsers().toBuffer[UserAccount].sortBy(_.getId).map(User(_))
  }

  /** Return current userAccount from UDS database */
  def queryUsers(): Array[UserAccount] = {
    run(
      try {
        logger.debug("load user accounts from UDS database.")
        var udsUsersArray = Array[UserAccount]()
        udsDbCtx.tryInTransaction {
          val udsEM = udsDbCtx.getEntityManager()
          udsEM.clear()
          val UdsUserAccountClass = classOf[UserAccount]
          val jpqlSelectUserAccount = s"FROM ${UdsUserAccountClass.getName}"
          val udsUsers = udsEM.createQuery(jpqlSelectUserAccount, UdsUserAccountClass).getResultList()
          udsUsersArray = udsUsers.asScala.toArray
        }
        udsUsersArray
      } catch {
        case t: Throwable => {
          synchronized {
            logger.error("Cannot load user accounts from UDS db!")
            logger.error(t.getLocalizedMessage())
            throw t
          }
        }
      })
  }

  /** Add a new user */
  def add(login: String, pswd: Option[String] = None, user: Option[Boolean] = None, passwdEncrypted: Option[Boolean] = None) = {
    run {
      val password = if (pswd.isDefined) {
        if (passwdEncrypted.isDefined && (passwdEncrypted.get == true))
          pswd.get
        else
          sha256Hex(pswd.get)
      } else sha256Hex("proline")

      val isGroupUser = if (user.isDefined) user.get else true
      val userCreator = new CreateUser(udsDbCtx, login, password, isGroupUser)
      userCreator.run()
    }
  }

  /** Change user state */
  def changeState(users: Set[User], isActive: Boolean) {
    run(new ChangeUserState(udsDbCtx, users.map(_.id.value), isActive).run())
  }

  /** Change user group */
  def changeGroup(users: Set[User], isInUserGrp: Boolean) {
    run(new ChangeUserGroup(udsDbCtx, users.map(_.id.value), isInUserGrp).run())
  }

  /** Change user password */
  def resetPassword(userId: Long, pswd: Option[String] = None) {
    run {
      new ChangePassword(udsDbCtx,
        userId,
        pswd).run()
    }
  }

  /** Perform database actions and wait for completion */
  private def run[R](actions: => R): R = {
    Await.result(Future { actions }, Duration.Inf)
  }
}