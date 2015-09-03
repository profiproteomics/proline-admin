package fr.proline.admin.gui.process.config.postgres

import fr.profi.util.scala.ByteUtils.BytesAmountWrapper
import fr.proline.admin.gui.process.PhysicalMemory

import PostgresOptimizableConfigParam._

/**
 * *************************** *
 * Optimize postgres.conf file *
 * *************************** *
 */
object PostgresConfigOptimizer {
  
//  /* Access Physical memory size */
//  private val physicalMemorySize: Long = {
//    // From http://stackoverflow.com/questions/950754/how-do-i-find-the-physical-memory-size-in-java
//    // Cross-platform
//
//    val os = ManagementFactory.getOperatingSystemMXBean().asInstanceOf[OperatingSystemMXBean]
//    os.getTotalPhysicalMemorySize() //in bytes
//  }
  val physicalMemorySize = PhysicalMemory.totalMemorySize
  
  /* Infer optimized values for some parameters */
  val sharedBuffersOptimizedValue = math.min( physicalMemorySize / 4, 4 GB)
  val sharedBuffersMaxValue = physicalMemorySize / 2
  
  val checkpointSegmentsOptimizedValue = sharedBuffersOptimizedValue / (16 MB)
  //val checkpointSegmentsOptimizedValue = sharedBuffersOptimizedValue / 16
  val checkpointSegmentsMaxValue = 2 * checkpointSegmentsOptimizedValue
  
  val tempBuffersOptimizedValue = physicalMemorySize / 32
  val tempBuffersMaxValue = tempBuffersOptimizedValue * 4
  
  val maintenanceWorkMemOptimizedValue = physicalMemorySize / 16
  val maintenanceWorkMemMaxValue = maintenanceWorkMemOptimizedValue * 2
  
  val effectiveCacheSizeOptimizedValue = physicalMemorySize / 2
  val effectiveCacheSizeMaxValue = physicalMemorySize * 3 / 4

  // Compute all suggested values
  /*def optimizedValueByConfigParam(): Map[PostgresOptimizableConfigParam.Value, AnyVal ] = Map(
    SHARED_BUFFERS -> sharedBuffersOptimizedValue,
    CHECKPOINT_SEGMENTS -> checkpointSegmentsOptimizedValue
  )*/

}