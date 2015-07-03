package actors.distributed

import actors.PennyAuction.CreateAuction
import actors.WSConnection.{ StatusRequest, Bid }
import akka.contrib.pattern.ShardRegion

object ItemShard {
  val name = "item-shard"
  val idExtractor: ShardRegion.IdExtractor = {
    case b @ Bid(id, _) => (id.value, b)
    case c @ CreateAuction(id, _) => (id.value, c)
    case s @ StatusRequest(id) => (id, s)
  }

  val resolver: ShardRegion.ShardResolver = {
    case Bid(id, _) => (id.value.hashCode % 100).toString
    case CreateAuction(id, _) => (id.value.hashCode % 100).toString
    case StatusRequest(id) => (id.hashCode % 100).toString
  }
}
