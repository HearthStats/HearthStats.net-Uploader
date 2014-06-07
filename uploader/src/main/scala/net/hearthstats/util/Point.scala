package net.hearthstats.util

case class Point(x: Int, y: Int)

object Point {
  def apply(x: Float, y: Float): Point =
    Point(x.toInt, y.toInt)
}