package fr.rad.part2.rarebooks.library

import akka.actor.{Actor, ActorLogging, Props, Stash}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import fr.rad.part2.rarebooks.library.RareBooks.{Close, Open, Report}
import fr.rad.part2.rarebooks.library.RareBooksProtocol.Msg

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration, MILLISECONDS => Millis}


object RareBooks {

  def props: Props = Props(new RareBooks)

  case object Close

  case object Open

  case object Report
}


class RareBooks extends Actor with ActorLogging with Stash {

  private val openDuration: FiniteDuration = Duration(context.system.settings.config.getDuration("rare-books.open-duration", Millis), Millis)
  private val closeDuration: FiniteDuration = Duration(context.system.settings.config.getDuration("rare-books.close-duration", Millis), Millis)
  private val findBookDuration: FiniteDuration = Duration(context.system.settings.config
    .getDuration("rare-books.librarian.find-book-duration", Millis), Millis)
  private val nbrOfLibrarians: Int = context.system.settings.config.getInt("rare-books.nbr-of-librarians")


  private val router: Router = createLibrarian()

  var requestToday: Int = 0
  var totalRequests: Int = 0


  context.system.scheduler.scheduleOnce(openDuration, self, Close)


  override def receive: Receive = open


  private def open: Receive = {
    case m: Msg =>
      router.route(m, sender)
      requestToday = List(requestToday, 1).foldLeft(requestToday)(_ + _)
//      librarian forward m

    case Close =>
      context.system.scheduler.scheduleOnce(closeDuration, self, Open)
      context.become(closed)

      self ! Report
  }

  private def closed: Receive = {

    case Report =>
      requestToday = List(requestToday, 1).foldLeft(requestToday)(_ + _)

    case Open =>
      context.system.scheduler.scheduleOnce(openDuration, self, Close)

      unstashAll()

      context.become(open)

    case _ =>
      stash()

  }

  protected def createLibrarian(): Router = {
    var cnt: Int = 0
    val routees : Vector[ActorRefRoutee] = Vector.fill(nbrOfLibrarians) {
      val r = context.actorOf(Librarian.props(findBookDuration), s"librarian-$cnt")
      cnt += 1
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }
}
