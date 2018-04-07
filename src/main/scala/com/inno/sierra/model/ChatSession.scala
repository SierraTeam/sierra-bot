package com.inno.sierra.model

import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._

import scala.collection.mutable

object ChatState extends Enumeration {
    type ChatState = Value
    val Start = Value(1, "Start")
    val CreateEvent = Value(2, "CreateEvent")
    val EditEvent = Value(3, "EditEvent")
  }

/**
  * There are many inconviniencies with using Squeryl
  * with enum fields, therefore chatState is stored as Int
  * @param id - id will be assigned automatically by ORM
  * @param csid - chatsession id
  * @param alias - user's alias or "" if it's a group
  * @param chatState - state of the chat
  */
case class ChatSession (
                         var id: Long,
                         var csid: Long,
                         var alias: String,
                         var chatState: Int
                       ) extends KeyedEntity[Long] {

}


object ChatSession {
  def create(csid: Long, alias: String,
             chatState: ChatState.ChatState): ChatSession = {
    val state = chatState match {
      case ChatState.Start => 1
      case ChatState.CreateEvent => 2
      case ChatState.EditEvent => 3
    }

    DbSchema.insert(new ChatSession(0, csid, alias, state))
  }

  /**
    * Returns the set of chat session:
    * @param ids - for the specified id, if Some provided,
    *            for all existing if None.
    * @return - mutable set of chat sessions.
    */
  def get(ids: Option[mutable.Set[Long]]): mutable.Set[ChatSession] = {
    DbSchema.getAllChatSessions(ids)
  }

  def exists(id: Long): Boolean = {
    DbSchema.existsChatSession(id)
  }
}