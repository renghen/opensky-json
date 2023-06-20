package model.opensky

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.immutable.HashMap
import scala.collection.mutable

import javax.inject.{Inject, Singleton}
import java.time.Instant
import com.google.inject.ImplementedBy

import play.api.{Configuration, Logger}

@ImplementedBy(classOf[StateProcessingImpl])
abstract class StateProcessing {
  val delay: Int
  type Icao24 = String
  type Country = String

  val logger: Logger = Logger(this.getClass())

  private val listOfIcao24 = mutable.ArrayBuffer.empty[Icao24]
  private val listOfStates = mutable.ArrayBuffer.empty[State] // debug purposes only, to be removed later
  private val countryOrigin: mutable.HashMap[String, Int] = mutable.HashMap.empty[Country, Int]
  private val planeAboveNetherlands: mutable.HashMap[String, Long] = mutable.HashMap.empty[Icao24, Long]

  def toCountries(state: State): Unit = {
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

  def aboveNetherlands() = {
    planeAboveNetherlands.toMap
  }

  def addToState(state: State) = {
    listOfStates += state
  }

  def resetStates() = listOfStates.clear()

  def getLoadedStates() = {
    logger.info(s"length of state: ${listOfStates.length}")
    listOfStates.toList
  }

  def processState(state: State) = {
    // logger.info(s"state: ${state.icao24}")
    addToState(state)
    toCountries(state)
    isAboveNetherlandsFor1Hour(state)
  }
}

@Singleton
class StateProcessingImpl @Inject() (configuration: Configuration)(implicit ec: ExecutionContext)
    extends StateProcessing {
  final val delay: Int = configuration.getOptional[Int]("opensky.top.time").getOrElse(3600)

  override val logger: Logger = Logger(this.getClass())
}
