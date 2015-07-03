package controllers

import java.util.UUID
import javax.inject._

import actors.PennyAuction.{ Auction, CreateAuction, ItemId }
import actors.WSConnection
import actors.Wallet.{ UserName, CreateWallet }
import actors.distributed.{ AuctionRegion, WalletRegion }
import akka.actor.ActorSystem
import play.api.Play.current
import play.api.mvc._

@Singleton
class Application @Inject() (val system: ActorSystem) extends Controller with AuctionRegion with WalletRegion {

  def ws(id: String, userName: String) = WebSocket.acceptWithActor[String, String] { request =>
    out =>
      WSConnection.props(UserName(userName), ItemId(id), out)
  }

  def create = Action(BodyParsers.parse.json) { request =>
    val auctionResult = request.body.validate[Auction]
    auctionResult.fold(
      _ => BadRequest,
      auction => {
        val id = ItemId(UUID.randomUUID().toString)
        auctionRegion ! CreateAuction(id, auction)
        Ok(id.value)
      })
  }

  def register(userName: String) = Action {
    walletRegion ! CreateWallet(UserName(userName))
    Ok
  }

  def show(id: String, userName: String) = Action { implicit request =>
    Ok(views.html.index(id, userName))
  }
}

