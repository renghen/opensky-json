package models.opensky

import play.api.{Configuration, Logger}

import akka.Done
import akka.actor.{Actor, ActorSystem, Props}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.Http
import akka.stream.scaladsl.{Sink, Source}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject
import scala.util.Success
import scala.util.Failure

class FetchTimeAndStateFRP {
  
}
