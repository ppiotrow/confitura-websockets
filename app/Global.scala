import actors.distributed.{ ItemShard, WalletShard }
import actors.{ PennyAuction, Wallet }
import akka.actor._
import akka.contrib.pattern.ClusterSharding
import akka.persistence.journal.leveldb.{ SharedLeveldbJournal, SharedLeveldbStore }
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import play.api.{ Application, GlobalSettings }
import scala.concurrent.duration._
import akka.pattern.ask

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    ClusterSharding(app.actorSystem).start(
      typeName = ItemShard.name,
      entryProps = Some(PennyAuction.props),
      idExtractor = ItemShard.idExtractor,
      shardResolver = ItemShard.resolver)

    ClusterSharding(app.actorSystem).start(
      typeName = WalletShard.name,
      entryProps = Some(Wallet.props),
      idExtractor = WalletShard.idExtractor,
      shardResolver = WalletShard.resolver)

    startupSharedJournal(app.actorSystem,
      startStore = ConfigFactory.load.getInt("akka.remote.netty.tcp.port") == 2550,
      path = ActorPath.fromString("akka.tcp://application@127.0.0.1:2550/user/store"))
  }

  def startupSharedJournal(system: ActorSystem, startStore: Boolean, path: ActorPath): Unit = {
    if (startStore)
      system.actorOf(Props[SharedLeveldbStore], "store")
    import system.dispatcher
    implicit val timeout = Timeout(15.seconds)
    val f = system.actorSelection(path) ? Identify(None)
    f.onSuccess {
      case ActorIdentity(_, Some(ref)) => SharedLeveldbJournal.setStore(ref, system)
      case _ =>
        system.log.error("Shared journal not started at {}", path)
        system.shutdown()
    }
    f.onFailure {
      case _ =>
        system.log.error("Lookup of shared journal at {} timed out", path)
        system.shutdown()
    }
  }
}
