package com.inno.sierra.model

import org.squeryl.KeyedEntity
import org.squeryl.dsl.CompositeKey2

/**
  * Represents the relation between chatsession and
  * the event assigned to it.
  * @param eventId  event id
  * @param chatSessionId  chatsession id
  */
case class ChatSessionEvents (
                               var eventId: Long,
                               var chatSessionId: Long)
  extends KeyedEntity[CompositeKey2[Long,Long]] {

  def id = CompositeKey2(chatSessionId, eventId)
}
