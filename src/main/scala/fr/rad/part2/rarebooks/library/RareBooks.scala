package fr.rad.part2.rarebooks.library

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import fr.rad.part2.rarebooks.library.RareBooks.{Close, Open, Report}
import fr.rad.part2.rarebooks.library.RareBooksProtocol.Msg
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{ MILLISECONDS => Millis, FiniteDuration, Duration }


object RareBooks {
  case object Close
  case object Open
  case object Report

  def props: Props = Props(new RareBooks)
}


class RareBooks extends Actor with ActorLogging with Stash{

  private val openDuration: FiniteDuration = Duration(context.system.settings.config.getDuration("rare-books.open-duration", Millis),Millis)
  private val closeDuration: FiniteDuration =Duration(context.system.settings.config.getDuration("rare-books.close-duration", Millis),Millis)
  private val findBookDuration: FiniteDuration =Duration(context.system.settings.config
    .getDuration("rare-books.librarian.find-book-duration", Millis),Millis)

  private val librarian = createLibrarian()

  var requestToday: Int = 0
  var totalRequests: Int=0


  context.system.scheduler.scheduleOnce(openDuration,self,Close)


  override def receive: Receive = open

  private def open: Receive = {
    case m: Msg =>
     requestToday= List(requestToday,1).foldLeft(requestToday)(_+_)
      librarian forward m

    case Close =>
      context.system.scheduler.scheduleOnce(closeDuration, self,Open)
      context.become(closed)

      self ! Report
  }


  private def closed: Receive = {

    case Report =>
     requestToday= List(requestToday,1).foldLeft(requestToday)(_+_)

    case Open =>
      context.system.scheduler.scheduleOnce(openDuration,self, Close)

      unstashAll()

      context.become(open)

    case _ =>
      stash()

  }

  protected def createLibrarian():ActorRef = {
    context.actorOf(Librarian.props(findBookDuration), "librarian")
  }
}
