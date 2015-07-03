package actors

import actors.PennyAuction.ItemId
import actors.WSConnection.{ Bid, StatusRequest }
import actors.Wallet.{ UserName, WalletStatus }
import actors.distributed.{ AuctionRegion, SystemAlias, Topic, WalletRegion }
import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Subscribe
import akka.event.LoggingReceive

class WSConnection(user: UserName, itemId: ItemId, out: ActorRef) extends Actor
    with AuctionRegion with WalletRegion with SystemAlias with ActorLogging {

  override def preStart() = {
    val subscriber = DistributedPubSubExtension(context.system).mediator
    subscriber ! Subscribe(Topic.wallet(user), self)
    subscriber ! Subscribe(Topic.itemAuction(itemId), self)
  }

  def receive = LoggingReceive {
    case "STATUS" =>
      auctionRegion ! StatusRequest(itemId.value)
      walletRegion ! StatusRequest(user.value)
    case "BID" =>
      auctionRegion ! Bid(itemId, user)
    case status: WalletStatus =>
      context.watch(sender())
      out ! status.toJson
    case auction: AuctionStatus =>
      context.watch(sender())
      out ! auction.toJson

    case Terminated(ref) =>
      log.warning(s"killed $ref")
      context.stop(self)
  }
}

object WSConnection {
  case class Bid(item: ItemId, userName: UserName)
  def props(user: UserName, itemId: ItemId, out: ActorRef) = Props(new WSConnection(user, itemId, out))
  case class StatusRequest(name: String)
}
