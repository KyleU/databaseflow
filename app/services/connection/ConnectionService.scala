package services.connection

import java.util.UUID

import akka.actor.{ ActorRef, Props }
import models._
import models.queries.query.SavedQueryQueries
import models.user.User
import services.database.MasterDatabase
import services.schema.SchemaService
import utils.metrics.InstrumentedActor
import utils.{ Config, Logging }

object ConnectionService {
  def props(id: Option[UUID], supervisor: ActorRef, connectionId: UUID, user: User, out: ActorRef, sourceAddress: String) = {
    Props(new ConnectionService(id.getOrElse(UUID.randomUUID), supervisor, connectionId, user, out, sourceAddress))
  }
}

class ConnectionService(
    val id: UUID = UUID.randomUUID,
    val supervisor: ActorRef,
    val connectionId: UUID,
    val user: User,
    val out: ActorRef,
    val sourceAddress: String
) extends InstrumentedActor with TraceHelper with DetailHelper with QueryHelper with PlanHelper with SqlHelper with Logging {

  protected[this] var currentUsername = user.username
  protected[this] var userPreferences = user.preferences
  protected[this] val db = MasterDatabase.databaseFor(connectionId)

  protected[this] val savedQueries = MasterDatabase.db.query(SavedQueryQueries.getByOwner(user.id))
  protected[this] val schema = SchemaService.getSchema(connectionId, db)

  protected[this] var pendingDebugChannel: Option[ActorRef] = None

  override def preStart() = {
    supervisor ! ConnectionStarted(user, id, self)
    out ! InitialState(user.id, currentUsername, userPreferences, savedQueries, schema)
  }

  override def receiveRequest = {
    // Incoming basic messages
    case mr: MalformedRequest => timeReceive(mr) { log.error(s"MalformedRequest:  [${mr.reason}]: [${mr.content}].") }
    case p: Ping => timeReceive(p) { out ! Pong(p.timestamp) }
    case GetVersion => timeReceive(GetVersion) { out ! VersionResponse(Config.version) }
    case dr: DebugInfo => timeReceive(dr) { handleDebugInfo(dr.data) }

    case sq: SubmitQuery => timeReceive(sq) { handleSubmitQuery(sq.queryId, sq.sql, sq.action.getOrElse("run")) }
    case st: ShowTableData => timeReceive(st) { handleShowTableData(st.queryId, st.name) }

    case gtd: GetTableDetail => timeReceive(gtd) { handleGetTableDetail(gtd.name) }
    case gvd: GetViewDetail => timeReceive(gvd) { handleGetViewDetail(gvd.name) }
    case gpd: GetProcedureDetail => timeReceive(gpd) { handleGetProcedureDetail(gpd.name) }

    case im: InternalMessage => handleInternalMessage(im)
    case rm: ResponseMessage => out ! rm
    case x => throw new IllegalArgumentException(s"Unhandled message [${x.getClass.getSimpleName}].")
  }

  override def postStop() = {
    supervisor ! ConnectionStopped(id)
  }

  protected[this] def handleSubmitQuery(queryId: UUID, sql: String, action: String) = action match {
    case "run" => handleRunQuery(queryId, sql)
    case "explain" => handleExplainQuery(queryId, sql)
    case "analyze" => handleAnalyzeQuery(queryId, sql)
    case _ => throw new IllegalArgumentException(action)
  }

  private[this] def handleInternalMessage(im: InternalMessage) = im match {
    case ct: SendConnectionTrace => timeReceive(ct) { handleConnectionTrace() }
    case ct: SendClientTrace => timeReceive(ct) { handleClientTrace() }
    case x => throw new IllegalArgumentException(s"Unhandled internal message [${x.getClass.getSimpleName}].")
  }
}
