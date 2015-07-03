package actors.distributed

import akka.actor.{ Actor, ActorRef, ActorSystem }
import akka.contrib.pattern.ClusterSharding

trait WalletRegion {
  def system: ActorSystem
  lazy val walletRegion: ActorRef = ClusterSharding(system).shardRegion(WalletShard.name)
}
trait AuctionRegion {
  def system: ActorSystem
  lazy val auctionRegion: ActorRef = ClusterSharding(system).shardRegion(ItemShard.name)
}

trait SystemAlias {
  self: Actor =>
  val system = context.system
}
