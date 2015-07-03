package actors.distributed

import actors.PennyAuction.ItemId
import actors.Wallet.UserName
import akka.actor.Actor
import akka.contrib.pattern.DistributedPubSubExtension

trait PublishChannel {
  self: Actor =>
  val channel = DistributedPubSubExtension(context.system).mediator
}
object Topic {
  def wallet(userName: UserName) = s"wallet-${userName.value}"
  def itemAuction(id: ItemId) = s"item-${id.value}"
}
