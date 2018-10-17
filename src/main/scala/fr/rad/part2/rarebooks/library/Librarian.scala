package fr.rad.part2.rarebooks.library

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}

import scala.concurrent.duration.FiniteDuration

object Librarian {

  import Catalog._
  import RareBooksProtocol._

  def props(findBookDuration: FiniteDuration, maxComplainCount: Int): Props = Props(new Librarian(findBookDuration,
    maxComplainCount))

  private def findByIsbn(fb: FindBookByIsbn) =
    optToEither[String](fb.isbn, findBookByIsbn)

  private def findByAuthor(fb: FindBookByAuthor) =
    optToEither[String](fb.author, findBookByAuthor)

  private def optToEither[T](v: T, f: T => Option[List[BookCard]]): Either[BookNotFound, BookFound] =
    f(v) match {
      case b: Some[List[BookCard]] => Right(BookFound(b.get))
      case _ => Left(BookNotFound(s"Book(s) not found base on $v"))
    }

  private def findByTitle(fb: FindBookByTitle) = optToEither[String](fb.title, findBookByTitle)

  private def findByTopic(fb: FindBookByTopic) =
    optToEither[Set[Topic]](fb.topic, findBookByTopic)

  final case class Done(e: Either[BookNotFound, BookFound], customer: ActorRef)

  final case class ComplainException(c: Complain, customer: ActorRef) extends IllegalStateException("Too many " +
    "complaints")
}

class Librarian(findBookDuration: FiniteDuration, maxComplaintCount: Int) extends Actor with ActorLogging with Stash {

  import Librarian._
  import RareBooksProtocol._

  private var complainCount: Int = 0

  override def receive: Receive = ready


  private def ready: Receive = {
    case m: Msg => m match {
      case c: Complain if complainCount == maxComplaintCount =>
        throw ComplainException(c, sender())
      case c:Complain =>
        sender ! Credit()
        log.info(s"Credit issued to customer $sender()")

      case f: FindBookByIsbn =>
        research(Done(findByIsbn(f), sender()))
      case f: FindBookByAuthor =>
        research(Done(findByAuthor(f), sender()))
      case f: FindBookByTitle =>
        research(Done(findByTitle(f), sender()))
      case f: FindBookByTopic =>
        research(Done(findByTopic(f), sender()))
    }
  }

  private def research(d: Done): Unit = {
    context.system.scheduler.scheduleOnce(findBookDuration, self, d)
    context.become(busy)
  }

  private def busy: Receive = {
    case Done(e, s) =>
      process(e, s)
      unstashAll()
      context.unbecome()

    case _ =>
      stash()
  }

  private def process(r: Either[BookNotFound, BookFound], sender: ActorRef): Unit = {
    r fold(
      f => {
        sender ! f
        log.info(f.toString)
      },
      s => {
        sender ! s

      }
    )
  }
}
