package net.hearthstats.util

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

import akka.actor.ActorSystem

object SchedulerDemo extends App {
  val system = ActorSystem("TestSystem")
  system.scheduler.schedule(50.milliseconds, 50.milliseconds)(println(System.currentTimeMillis))

}