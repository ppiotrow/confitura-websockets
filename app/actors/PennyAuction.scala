package actors

import java.time.LocalTime
import java.time.temporal.ChronoUnit

import actors.PennyAuction.Auction
import actors.WSConnection.{ Bid, StatusRequest }
import actors.Wallet.{ Charge, UserName }
import actors.distributed.{ PublishChannel, SystemAlias, Topic, WalletRegion }
import akka.actor.{ ActorLogging, Props, ReceiveTimeout }
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import akka.persistence.PersistentActor
import play.api.libs.json._

import scala.concurrent.duration._

class PennyAuction extends PersistentActor with PublishChannel with ActorLogging with WalletRegion with SystemAlias {
  import PennyAuction._

  private var state = AuctionStatus.zero

  def uninitialised: Receive = {
    case ca: CreateAuction => persist(ca) { event =>
      createAuction(event)
    }
  }

  def react(itemId: ItemId): Receive = {
    case bid: Bid => persist(bid) { _ =>
      bidAuction(bid)
      publish(itemId, state.updateTimeout(bidTimeout))
      walletRegion ! Charge(bid.userName, 1)
    }
    case StatusRequest(_) =>
      sender ! state.updateTimeout(bidTimeout)
    case ReceiveTimeout => persist(ReceiveTimeout) { _ =>
      finishAuction()
      publish(itemId, state)
    }
  }

  def finished: Receive = {
    case StatusRequest(_) => sender ! state
  }

  def createAuction(event: CreateAuction): Unit = {
    log.info(s"create auction $event")
    context.setReceiveTimeout(bidTimeout)
    state = state.init(event.auction).updateTimeout(bidTimeout)
    context.become(react(event.itemId))
  }

  def finishAuction(): Unit = {
    context.setReceiveTimeout(Duration.Undefined)
    state = state.finish
    context.become(finished)
  }

  def bidAuction(event: Bid): Unit = {
    context.setReceiveTimeout(bidTimeout)
    state = state.bidBy(event.userName)
  }

  override def receiveRecover = {
    case event: CreateAuction =>
      createAuction(event)
    case ReceiveTimeout =>
      finishAuction()
    case event: Bid =>
      bidAuction(event)
  }

  override def receiveCommand = uninitialised

  override def persistenceId = self.path.parent.name + "-" + self.path.name

  def publish(id: ItemId, msg: AuctionStatus) = channel ! Publish(Topic.itemAuction(id), msg.updateTimeout(bidTimeout))

}

object PennyAuction {
  case class ItemId(value: String) extends AnyVal
  case class Auction(title: String, img: String)
  object Auction {
    implicit val reads = Json.reads[Auction]
  }
  case class CreateAuction(itemId: ItemId, auction: Auction)
  def props = Props[PennyAuction]
  val bidTimeout = 100.seconds
}

case class AuctionStatus(title: String, img: String, winner: Option[UserName], price: Price, finished: Boolean, remainMillis: Long, lastBid: LocalTime) {

  def init(auction: Auction) = copy(title = auction.title, img = auction.img)
  def updateTimeout(bidDuration: FiniteDuration) = copy(remainMillis = bidDuration.toMillis - ChronoUnit.MILLIS.between(lastBid, LocalTime.now()))
  def bidBy(user: UserName) = copy(winner = Some(user), price = price.increment, lastBid = LocalTime.now())
  def finish = copy(finished = true, remainMillis = 0L)

  def toJson = {
    val win = winner.map(_.value).getOrElse("")
    s"""{"statusType": "auction", "winner": "$win", "title" : "$title", "img":"$img",
         |"price": "${price.value}","isFinished": $finished, "millis": $remainMillis}""".stripMargin
  }
}
object AuctionStatus {
  def zero = AuctionStatus("", "", None, Price.initial, finished = false, 0, LocalTime.now())
}

case class Price(value: BigDecimal) {
  def increment = copy(value = value + Price.penny)
}
object Price {
  val initial = Price(BigDecimal("0.00"))
  val penny = BigDecimal("0.01")
}
