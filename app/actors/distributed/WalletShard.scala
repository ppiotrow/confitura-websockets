package actors.distributed

import actors.WSConnection.StatusRequest
import actors.Wallet.{ CreateWallet, Charge }
import akka.contrib.pattern.ShardRegion

object WalletShard {
  val name = "wallet-shard"
  val idExtractor: ShardRegion.IdExtractor = {
    case c @ Charge(userName, _) => (userName.value, c)
    case c @ CreateWallet(userName) => (userName.value, c)
    case s @ StatusRequest(userName) => (userName, s)
  }
  val resolver: ShardRegion.ShardResolver = {
    case Charge(userName, _) => (userName.hashCode % 100).toString
    case CreateWallet(userName) => (userName.hashCode % 100).toString
    case StatusRequest(userName) => (userName.hashCode % 100).toString
  }
}

