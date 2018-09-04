package fr.rad.chap1

import java.util.Locale

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.routing.FromConfig
import fr.rad.chap1.Tourist.Start

object TouristRouter extends App{

  val system: ActorSystem = ActorSystem("TouristSystem")

  val guidebook: ActorRef = system.actorOf(FromConfig.props(), "balancer")

  val tourProps: Props = Props(classOf[Tourist], guidebook)

  val tourist:ActorRef = system.actorOf(tourProps)
  tourist ! Start(Locale.getISOCountries)
}
