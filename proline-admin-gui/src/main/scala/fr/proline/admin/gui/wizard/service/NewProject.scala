package fr.proline.admin.gui.wizard.service

import com.typesafe.scalalogging.LazyLogging
import scalafx.concurrent.Task
import java.util.concurrent.atomic.AtomicBoolean
import javafx.{ concurrent => jfxc }

import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.service.user.CreateProject
import fr.proline.admin.service.db.CreateProjectDBs

/**
 * Task to create new project
 * @aromdhani
 *
 */

class NewProject(val newProjectName: String, val newProjectDesc: String, val ownerId: Long) extends LazyLogging {
  var shouldThrow = new AtomicBoolean(false)
  object Worker extends Task(new jfxc.Task[Unit] {
    protected def call(): Unit = {
      /* Create project */
      val udsDbContext = UdsRepository.getUdsDbContext()
      val prolineConf = SetupProline.getUpdatedConfig
      val projectCreator = new CreateProject(udsDbContext, newProjectName, newProjectDesc, ownerId)
      projectCreator.doWork()
      val projectId = projectCreator.projectId
      /* Create project databases */
      if (projectId > 0L) {
        new CreateProjectDBs(udsDbContext, prolineConf, projectId).doWork()
      } else {
        logger.error("Invalid Project Id: ", projectId)
      }
    }
  })

}