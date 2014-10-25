package net.hearthstats.modules

import java.util.ServiceLoader

import scala.collection.JavaConversions.asScalaIterator
import scala.util.control.NonFatal

import grizzled.slf4j.Logging

/**
 * Checks if module is defined in the SPI, either uses it or a dummy implementation.
 */
abstract class ModuleFactory[M](
  name: String,
  moduleInterface: Class[M],
  dummyClass: Class[_ <: M]) extends Logging {

  var status: Status = INITIAL

  def newInstance(useDummy: Boolean = false): M =
    if (FAILURE == status || useDummy) dummyImpl
    else {
      try {
        val inst = moduleImpl
        if (INITIAL == status) {
          info(s"module $name loaded")
          status = SUCCESS
        }
        inst
      } catch {
        case NonFatal(e) =>
          if (INITIAL == status) {
            warn(s"module $name could not be loaded : " + e.getMessage, e)
            status = FAILURE
          }
          status = FAILURE
          dummyImpl
      }
    }

  lazy val moduleImpl: M = {
    info(s"Instantiation $moduleInterface from SPI")
    ServiceLoader.load(moduleInterface).iterator.toList.head
  }

  lazy val dummyImpl: M = {
    info(s"Dummy instantiation for $moduleInterface :$dummyClass")
    dummyClass.newInstance
  }

}
sealed trait Status
case object INITIAL extends Status
case object FAILURE extends Status
case object SUCCESS extends Status