package net.hearthstats.util

import akka.actor.ActorRef
import akka.actor.ActorDSL._
import akka.actor.ActorSystem
import akka.actor.Actor

trait ActorObservable {
  implicit val system = ActorSystem("companion")
  var observers: List[ActorRef] = Nil

  def addObserver(o: ActorRef): Unit =
    observers ::= o

  def removeObserver(o: ActorRef): Unit =
    observers = observers.diff(Seq(o))

  /**
   * Creates an actor which will receive the messages and pass them to this function.
   */
  def addReceive(o: Actor.Receive, name: Option[String] = None): Unit = {
    addObserver(actor(name.getOrElse(null))(new Act {
      become(o)
    }))
  }

  def notify(msg: Any): Unit =
    for (o <- observers) o ! msg
}