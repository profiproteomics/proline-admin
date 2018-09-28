package fr.proline.admin.gui.wizard.service

import scalafx.concurrent.Task
import java.util.concurrent.atomic.AtomicBoolean
import javafx.{ concurrent => jfxc }
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.service.user.{ ArchiveProject => ArchiveUserProject }
/**
 * Task to archive project
 * @author aromdhani
 *
 */
class ArchiveProject(projectId: Long, binPath: String, archivePath: String) {
  var shouldThrow = new AtomicBoolean(false)
  /** Create archive project task */
  object Worker extends Task(new jfxc.Task[Unit] {
    protected def call(): Unit = {
      //start task to archive project 
      val udsDbContext = UdsRepository.getDataStoreConnFactory()
      ArchiveUserProject(udsDbContext, projectId, binPath, archivePath)
    }
  })
}