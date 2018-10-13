package fr.rad.part1.chap3

import akka.actor._
import akka.event.Logging

class ImmutableHello(name: String) extends Actor with ActorLogging {
  override def receive: Receive = {
    case msg: Hello =>
     log.info(s"Received hello: $msg")
      sender() ! Hello("Greetings Hello")

    case msg: GoodBye =>
      log.info(s"Received goodbye: $msg")
      sender() ! GoodBye("Greetings Hello")

    case ukm => log.info(s"Received unknow message: $ukm")
  }

}
final case class Hello(name:String)
final case class GoodBye(name:String)

object ImmutableHello{



 private def apply(name: String): ImmutableHello = new ImmutableHello(name)

  def props(name:String) = Props(apply(name))
}

object HelloApp extends App{

  implicit val system = ActorSystem("system")

  val immutableActor =system.actorOf(ImmutableHello.props("Sekou"))

immutableActor ! Hello("Sek")
}

class GreetingsActor extends Actor {
  val log = Logging(context.system, this)

  override def receive = hello

  def hello:Receive = {
    case msg: Hello => log.info(s"Received.....")
      sender() ! Hello("Greetings Hello")
      context.become(goodbye)
    case _ => log.info("receveid unknow message")
  }

 private val goodbye:Receive = {
    case msg: GoodBye => log.info(s"Received goodbye: $msg")
      sender() ! Hello("Greetings Goodbye")
      context.become(hello)
    case _ => log.info("received unknow....")
  }
}
