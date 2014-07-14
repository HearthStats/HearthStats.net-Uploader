package net.hearthstats.modules

import net.hearthstats.log.Log
import net.hearthstats.config.TempConfig
import net.hearthstats.video.VideoEncoder

/**
 * Checks if video module is present in the classpath, either uses it or a dummy implementation.
 */
abstract class ModuleFactory[M](
  name: String,
  use: => Boolean,
  dummyImpl: => M,
  moduleClass: String,
  constructorArgs: => Seq[AnyRef] = Nil) {

  var status: Status = INITIAL

  /**
   * Invokes the constructor by using the arguments from ModuleFactory (constructorArgs)
   * then from newInstance(args) concatenated :
   * arguments = constructorArgs ++ args
   */
  def newInstance(args: Seq[AnyRef] = Nil): M =
    if (FAILURE == status || !use) dummyImpl
    else {
      try {
        val arguments = constructorArgs ++ args
        val constructor = Class.forName(moduleClass).getConstructor(arguments.map(_.getClass).toArray[Class[_]]: _*)
        val module = constructor.newInstance(arguments.toArray: _*).asInstanceOf[M]
        if (INITIAL == status) {
          Log.info(s"module $name loaded")
          status = SUCCESS
        }
        module
      } catch {
        case e: Exception =>
          if (INITIAL == status) {
            Log.warn(s"module $name could not be loaded : " + e.getMessage, e)
            status = FAILURE
          }
          status = FAILURE
          dummyImpl
      }
    }

}
sealed trait Status
case object INITIAL extends Status
case object FAILURE extends Status
case object SUCCESS extends Status