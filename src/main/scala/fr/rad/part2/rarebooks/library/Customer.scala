package fr.rad.part2.rarebooks.library

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.util.Random

object Customer {
  import RareBooksProtocol._

  def props(rareBooks: ActorRef, odds: Int, tolerance:Int): Props =
    Props(new Customer(rareBooks, odds, tolerance))

  case class CustomerModel (
    odds: Int,
    tolerance: Int,
    found: Int,
    notFound: Int
  )

  case class State(model :CustomerModel, timeInMillis: Long) {
    def update(m: Msg):State = m match {
      case BookFound(b,d) =>
        copy(model.copy(model.found + b.size),d)

      case BookNotFound(_, d) =>
        copy(model.copy(notFound = model.notFound + 1), d)

      case Credit(d) =>
        copy(model.copy(notFound = 0), d)
    }
  }


}

class Customer(rareBooks: ActorRef, odds: Int, tolerance:Int) extends Actor with ActorLogging{

  import Customer._
  import RareBooksProtocol._

  private var state = State(CustomerModel(odds, tolerance, 0, 0), -1L)

  def pickTopic(): Topic =
    if (Random.nextInt(100) < state.model.odds){
      viableTopics(Random.nextInt(viableTopics.size))
    } else {
      Unknown
    }



  def requestBookInfo() = rareBooks ! FindBookByTopic(Set(pickTopic()))

  requestBookInfo()


  override def receive: Receive = {
    case m:Msg => m match {
      case f: BookFound =>
        state = state.update(f)
        log.info(s"find my book ${f.books.foreach(println)}")
        requestBookInfo()

      case f: BookNotFound
        if state.model.notFound < state.model.tolerance => state = state.update(f)
        requestBookInfo()

      case f:BookNotFound => state= state.update(f)
        sender ! Complain

      case c:Credit =>
        state = state.update(c)
        requestBookInfo()
    }
  }
}
