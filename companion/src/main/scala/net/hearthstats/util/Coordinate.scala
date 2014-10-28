package net.hearthstats.util

case class Coordinate(x: Int, y: Int)

object Coordinate {
  def apply(x: Float, y: Float): Coordinate =
    Coordinate(x.toInt, y.toInt)
}