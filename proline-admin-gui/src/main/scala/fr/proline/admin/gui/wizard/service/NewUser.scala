package fr.proline.admin.gui.wizard.service

import scalafx.concurrent.Task
import java.util.concurrent.atomic.AtomicBoolean
import javafx.{ concurrent => jfxc }

import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.service.user.CreateUser
import fr.profi.util.security.sha256Hex

/**
 * Task to create new  user
 * @aromdhani
 *
 */

class NewUser(val login: String, val pwField: Option[String], isAdmin: Boolean) {
  var shouldThrow = new AtomicBoolean(false)
  //Create user task 
  object Worker extends Task(new jfxc.Task[Unit] {
    protected def call(): Unit = {
      val udsDbContext = UdsRepository.getUdsDbContext()
      var userCreator: CreateUser = null
      /* Create user */
      val pswd = if (pwField.isDefined) pwField.get else "proline" //TODO: define in config!
      val encryptedPswd = sha256Hex(pswd)
      if (isAdmin) { userCreator = new CreateUser(udsDbContext, login, encryptedPswd, false) }
      else { userCreator = new CreateUser(udsDbContext, login, encryptedPswd, true) }
      if (userCreator != null) {
        userCreator.run()
      }
    }
  })
}