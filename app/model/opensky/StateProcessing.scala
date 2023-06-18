package model.opensky

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.immutable.HashMap
import scala.collection.mutable

import javax.inject.{Inject, Singleton}
import java.time.Instant

@Singleton
class StateProcessing @Inject() (implicit ec: ExecutionContext) {
  final val delay: Int = 3600
  val countries: mutable.HashMap[String, Int] = mutable.HashMap.empty[String, Int]
  val planeAboveNetherlands: mutable.HashMap[String, Long] = mutable.HashMap.empty[String, Long]

  def toCountries(state: State): Unit = {
    countries.get(state.originCountry) match {
      case None        => countries.addOne((state.originCountry, 1))
      case Some(value) => countries.update(state.originCountry, value + 1)
    }
  }

  def isAboveNetherlands(state: State): Boolean = {
    (state.longitude, state.latitude) match {
      case (Some(long), Some(lat)) => Bounds_WGS84.isAboveNetherlands(long, lat)
      case (_, _)                  => false
    }
  }

  def isAboveNetherlandsFor1Hour(state: State) = {
    val newTime = if (state.timePosition.isDefined) {
      state.timePosition.get
    } else {
      state.lastContact
    }

    if (isAboveNetherlands(state)) {
      planeAboveNetherlands.get(state.icao24) match {
        case None    => planeAboveNetherlands.addOne((state.originCountry, newTime))
        case Some(_) => planeAboveNetherlands.update(state.originCountry, newTime)
      }
      val oneHourAgo = Instant.now().getEpochSecond - delay
      val toRemove = planeAboveNetherlands.view.filter(pred => pred._2 > oneHourAgo).keys
      planeAboveNetherlands.subtractAll(toRemove)
    }
  }

  def processState(state: State) = {
    toCountries(state)
    isAboveNetherlandsFor1Hour(state)
  }
}
