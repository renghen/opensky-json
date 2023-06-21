package models.opensky

final case class Bounds_WGS84(top: Double, left: Double, bottom: Double, right: Double)

object Bounds_WGS84 {

  final val netherlandsBounds = Bounds_WGS84(3.2, 50.75, 7.22, 53.7)
  val isAboveNetherlands = isAbove(_, _, netherlandsBounds)

  def isAbove(long: Double, lat: Double, bounds: Bounds_WGS84): Boolean = {
    (lat >= bounds.left) && (lat <= bounds.right) && (long >= bounds.top) && (long <= bounds.bottom)
  }
}
