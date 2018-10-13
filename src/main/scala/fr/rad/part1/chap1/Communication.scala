package fr.rad.part1.chap1

import java.util.{Currency, Locale}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import fr.rad.part1.chap1.Guidebook.Inquiry
import fr.rad.part1.chap1.Tourist.{Guidance, Start}
import akka.routing.FromConfig
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object Guidebook {
  case class Inquiry(code:String)
  def props = Props(new Guidebook)
}

object Tourist {
  case class Guidance(code: String, description: String)
  case class Start(codes: Seq[String])
  def  props(guidebook: ActorRef)= Props(new Tourist(guidebook))
}


class Tourist(guidebook: ActorRef) extends Actor{
  override def receive: Receive ={
    case Start(codes)=>
      codes.foreach(guidebook ! Inquiry(_))
    case Guidance (code, description) =>
      println(s"$code: $description")
  }
}

class Guidebook extends Actor{

  def describe(locale:Locale) =
    s"""In ${locale.getDisplayCountry},
       |${locale.getDisplayLanguage} is spoken and the currency
       |is the ${Currency.getInstance(locale).getDisplayName}
     """.stripMargin

  override def receive: Receive = {
    case Inquiry(code) => println(s"Actor ${self.path.name} " +
      s"responding to inquiry about $code")
      Locale.getAvailableLocales.filter{_.getCountry == code}.foreach{
        locale => sender() ! Guidance(code, describe(locale))
      }
  }
}

object Main extends App {

  implicit val system = ActorSystem("GuideSystem")

  val  guidebook: ActorRef = system.actorOf(Guidebook.props, "guidebook")

  val tourist: ActorRef = system.actorOf(Tourist.props(guidebook), "tourist")

  tourist ! Start(Locale.getISOCountries)

}

object TouristMain extends App {
  val system: ActorSystem = ActorSystem("TouristSystem")

  val path = "akka.tcp://BookSystem@127.0.0.1:2553/user/guidebook"

  implicit val timeout :Timeout = Timeout(5, SECONDS)

  system.actorSelection(path).resolveOne().onComplete{
    case Success(guidebook) => val tourProps = Tourist.props(guidebook)
      val tourist: ActorRef = system.actorOf(tourProps)

      tourist ! Start(Locale.getISOCountries)

    case Failure(exception) => println(exception)
  }
}

object GuidebookMain extends App {
  val system: ActorSystem = ActorSystem("BookSystem")

  val guideProps: Props = Props[Guidebook]

  val routerProps: Props = FromConfig.props(guideProps)

  val guidebook: ActorRef = system.actorOf(routerProps, "guidebook")
}

