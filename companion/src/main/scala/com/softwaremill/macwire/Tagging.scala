package com.softwaremill.macwire

// from https://github.com/adamw/macwire/issues/9
// until next version is released
trait Tagging {
  type Tag[U] = { type Tag = U }
  type @@[T, U] = T with Tag[U]
  type Tagged[T, U] = T with Tag[U]
  implicit class Tagging[T](t: T) {
    def taggedWith[U]: T @@ U = t.asInstanceOf[T @@ U]
  }
}

object Tagging extends Tagging