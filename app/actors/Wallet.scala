package actors

import actors.WSConnection.StatusRequest
import actors.distributed.{ PublishChannel, SystemAlias, Topic }
import akka.actor.{ ActorLogging, Props }
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import akka.persistence.PersistentActor

class Wallet extends PersistentActor with PublishChannel with SystemAlias with ActorLogging {
  import Wallet._

  var balance = 0

  def receiveCommand = {
    case create @ CreateWallet(userName) => persist(create) { _ =>
      log.info(s"Wallet created for $userName")
      balance = initialBalance
      publish(userName, WalletStatus(balance))
    }
    case charge @ Charge(userName, amount) => persist(charge) { _ =>
      log.info("Wallet charged")
      balance -= amount
      publish(userName, WalletStatus(balance))
    }
    case StatusRequest(_) => sender ! WalletStatus(balance)
  }

  override def receiveRecover = {
    case CreateWallet(_) =>
      balance = initialBalance
    case Charge(_, amount) =>
      balance -= amount
  }

  def publish(userName: UserName, msg: Any) = channel ! Publish(Topic.wallet(userName), msg)

  override def persistenceId: String = self.path.parent.name + "-" + self.path.name
}

object Wallet {
  type Currency = Int
  case class UserName(value: String)

  case class CreateWallet(userName: UserName)
  case class WalletStatus(balance: Currency) {
    def toJson = s"""{"statusType": "wallet", "balance": "$balance"}"""
  }
  case class Charge(userName: UserName, amount: Currency)

  def props = Props[Wallet]
  val initialBalance = 100 // might be send from external accounting service

}

