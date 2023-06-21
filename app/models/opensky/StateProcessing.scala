package models.opensky

import scala.collection.immutable.HashMap
import scala.collection.mutable

import java.time.Instant

import play.api.{Configuration, Logger}

final class StateProcessing(delayTime: Int) {
  type Icao24 = String
  type Country = String

  val logger: Logger = Logger(this.getClass())
  val delay: Int = delayTime

  private val listOfIcao24: mutable.ArrayBuffer[Icao24] = mutable.ArrayBuffer.empty[Icao24]
  private val listOfStates = mutable.ArrayBuffer.empty[State]
  private var flyAltitude = mutable.HashMap.empty[Long, List[StateOfFly]]
  private val countryOrigin: mutable.HashMap[Country, Int] = mutable.HashMap.empty[Country, Int]
  private val planeAboveNetherlands: mutable.HashMap[Icao24, Long] = mutable.HashMap.empty[Icao24, Long]

  private def insertIntoTopCountries(state: State): Unit = {
    val isFound = listOfIcao24.find(code => code == state.icao24) // we check for unique planes
    isFound match {
      case None =>
        listOfIcao24.addOne(state.icao24)
        countryOrigin.get(state.originCountry) match {
          case None        => countryOrigin.addOne((state.originCountry, 1))
          case Some(value) => countryOrigin.update(state.originCountry, value + 1)
        }
      case Some(_) => // we do not do anything
    }
  }

  def getCountries(): Map[String, Int] = {
    countryOrigin.toMap
  }

  def isAboveNetherlands(state: State): Boolean = {
    (state.longitude, state.latitude) match {
      case (Some(long), Some(lat)) => Bounds_WGS84.isAboveNetherlands(long, lat)
      case (_, _)                  => false
    }
  }

  private def isAboveNetherlandsFor1Hour(state: State) = {
    val newTime = if (state.timePosition.isDefined) {
      state.timePosition.get
    } else {
      state.lastContact
    }

    if (isAboveNetherlands(state)) {
      planeAboveNetherlands.get(state.icao24) match {
        case None    => planeAboveNetherlands.addOne((state.icao24, newTime))
        case Some(_) => planeAboveNetherlands.update(state.icao24, newTime)
      }
      val since = Instant.now().getEpochSecond - delay
      val toRemove = planeAboveNetherlands.view.filter(pred => pred._2 < since).keys
      planeAboveNetherlands.subtractAll(toRemove)
    }
  }

  /** Code to return all planes that are above the Netherlands for last hour
    *
    * @return
    *   map of ICAO24 and the last time they were in that place
    */
  def aboveNetherlands(): Map[Icao24, Long] = {
    logger.info(s"aboveNetherlands")
    val since = Instant.now().getEpochSecond - delay
    val toRemove = planeAboveNetherlands.view.filter(pred => pred._2 < since).keys
    planeAboveNetherlands.subtractAll(toRemove)
    planeAboveNetherlands.toMap
  }

  def addToState(state: State) = {
    listOfStates += state
  }

  def resetStates() = {
    listOfStates.clear()
  }

  def getLoadedStates() = {
    logger.info(s"length of state: ${listOfStates.length}")
    listOfStates.toList
  }

  def statesLoaded(): Unit = {
    val stateflyList = listOfStates.map { state =>
      val baroAltitude = state.baroAltitude.getOrElse(0.0)
      val time = state.timePosition.getOrElse(state.lastContact)
      val verticalRate = state.verticalRate.getOrElse(0.0)
      val slicelevel = (baroAltitude / 1000).toLong

      val nextBaroAltitude = baroAltitude + verticalRate
      val nextSlicelevel = (nextBaroAltitude / 1000).toLong

      val flyStatus = if (slicelevel != nextSlicelevel) {
        FlyStatus.WARNING
      } else {
        FlyStatus.NORMAL
      }

      StateOfFly(
        state.icao24,
        state.callsign,
        state.originCountry,
        time,
        state.latitude.getOrElse(0),
        slicelevel,
        baroAltitude,
        verticalRate,
        flyStatus
      )
    }.toList

    flyAltitude.clear()
    flyAltitude.addAll(stateflyList.groupBy(_.baroAltitudeSlice))
  }

  def getSlices() = flyAltitude.toMap

  def processState(state: State) = {
    logger.info(s"state: ${state}")
    addToState(state)
    insertIntoTopCountries(state)
    isAboveNetherlandsFor1Hour(state)
  }
}