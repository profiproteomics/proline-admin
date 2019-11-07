package fr.proline.admin.gui.process

import com.sun.management.OperatingSystemMXBean

import java.lang.management.ManagementFactory

object PhysicalMemory {

  /* Access Physical memory size */

  // From http://stackoverflow.com/questions/950754/how-do-i-find-the-physical-memory-size-in-java
  // Cross-platform

  val os = ManagementFactory.getOperatingSystemMXBean().asInstanceOf[OperatingSystemMXBean]
  
  val totalMemorySize: Long = os.getTotalPhysicalMemorySize() //in bytes
  
  def freeMemorySize: Long = os.getFreePhysicalMemorySize()

//  println("physicalMemorySize: " + physicalMemorySize)

}