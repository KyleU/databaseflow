package services.user

import java.util.UUID

import akka.actor.ActorRef
import models.GetQueryHistory
import models.user.User

object QueryHistoryService {
  def handleGetQueryHistory(connectionId: UUID, user: Option[User], gqh: GetQueryHistory, out: ActorRef) = {

  }
}