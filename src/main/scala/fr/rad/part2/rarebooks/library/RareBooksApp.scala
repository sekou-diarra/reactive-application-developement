package fr.rad.part2.rarebooks.library





import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn

object RareBooksApp {

  def main(args: Array[String]): Unit = {
    val system: ActorSystem = ActorSystem("rare-book-system")
    val rareBookApp: RareBooksApp = new RareBooksApp(system)
    rareBookApp.run()
  }


}


class RareBooksApp(system: ActorSystem) extends Console {

  private val log = Logging(system, getClass.getName)
  private val rareBooks = createRareBooks()

  protected def createRareBooks(): ActorRef = {
    system.actorOf(RareBooks.props, "rare-books")
  }

  def run(): Unit = {
    log.warning(f"{} running%nEnter commands [`q` = quit, `2c` = 2 customers, etc.]:", getClass.getSimpleName)
    commandLoop()
    Await.ready(system.whenTerminated, Duration.Inf)
  }

  @tailrec
  private def commandLoop(): Unit =
    Command(StdIn.readLine()) match {
      case Command.Customer(count, odds, tolerance) =>
        createCustomer(count, odds, tolerance)
        commandLoop()
      case Command.Quit =>
        system.terminate()
      case Command.Unknown(command) =>
        log.warning(s"Unknown command $command")
        commandLoop()
    }


  protected def createCustomer(count: Int, odds: Int, tolerance: Int): Unit =
    for (_ <- 1 to count)
      system.actorOf(Customer.props(rareBooks, odds, tolerance))

}

