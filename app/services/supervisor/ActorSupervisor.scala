package services.supervisor

import java.util.UUID

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorRef, OneForOneStrategy, SupervisorStrategy}
import models._
import models.user.User
import org.joda.time.LocalDateTime
import services.result.CachedResultActor
import util.{ApplicationContext, DateUtils, Logging}

object ActorSupervisor {
  case class SocketRecord(userId: UUID, name: String, actorRef: ActorRef, started: LocalDateTime)

  protected val sockets = collection.mutable.HashMap.empty[UUID, SocketRecord]
}

class ActorSupervisor(val ctx: ApplicationContext) extends Actor with Logging {
  import services.supervisor.ActorSupervisor._

  override def preStart() = {
    context.actorOf(CachedResultActor.props(), "result-cleanup")
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _ => Stop
  }

  override def receive = {
    case ss: SocketStarted => handleSocketStarted(ss.user, ss.socketId, ss.conn)
    case ss: SocketStopped => handleSocketStopped(ss.socketId)

    case GetSystemStatus => handleGetSystemStatus()
    case ct: SendSocketTrace => handleSendSocketTrace(ct)
    case ct: SendClientTrace => handleSendClientTrace(ct)

    case im: InternalMessage => log.warn(s"Unhandled internal message [${im.getClass.getSimpleName}] received.")
    case x => log.warn(s"ActorSupervisor encountered unknown message: $x")
  }

  private[this] def handleGetSystemStatus() = {
    val connectionStatuses = ActorSupervisor.sockets.toList.sortBy(_._2.name).map(x => x._1 -> x._2.name)
    sender() ! SystemStatus(connectionStatuses)
  }

  private[this] def handleSendSocketTrace(ct: SendSocketTrace) = ActorSupervisor.sockets.find(_._1 == ct.id) match {
    case Some(c) => c._2.actorRef forward ct
    case None => sender() ! ServerError("Unknown Socket", ct.id.toString)
  }

  private[this] def handleSendClientTrace(ct: SendClientTrace) = ActorSupervisor.sockets.find(_._1 == ct.id) match {
    case Some(c) => c._2.actorRef forward ct
    case None => sender() ! ServerError("Unknown Client Socket", ct.id.toString)
  }

  protected[this] def handleSocketStarted(user: User, socketId: UUID, socket: ActorRef) = {
    log.debug(s"Socket [$socketId] registered to [${user.username}] with path [${socket.path}].")
    ActorSupervisor.sockets(socketId) = SocketRecord(user.id, user.username, socket, DateUtils.now)
  }

  protected[this] def handleSocketStopped(id: UUID) = {
    ActorSupervisor.sockets.remove(id) match {
      case Some(sock) => log.debug(s"Connection [$id] [${sock.actorRef.path}] stopped.")
      case None => log.warn(s"Socket [$id] stopped but is not registered.")
    }
  }
}
