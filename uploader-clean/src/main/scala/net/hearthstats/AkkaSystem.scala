package net.hearthstats

import akka.actor.ActorSystem

/**
 * Shares the actor system across all class which use it.
 */
trait AkkaSystem {
  val actorSystem = ActorSystem("companion")
  implicit val executor = scala.concurrent.ExecutionContext.Implicits.global

}