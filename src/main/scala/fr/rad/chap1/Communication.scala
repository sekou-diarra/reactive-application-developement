package fr.rad.chap1

import java.util.{Currency, Locale}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import fr.rad.chap1.Guidebook.Inquiry
import fr.rad.chap1.Tourist.{Guidance, Start}


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

