package com.inno.sierra.model

import java.sql.Timestamp
import java.util.Date
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
  * @param isGroup - is this is a group (supergroup/channel) or not
  * @param chatState - state of the chat
  */
case class ChatSession (
                         var id: Long,
                         var csid: Long,
                         var alias: String,
                         var isGroup: Boolean,
                         var chatState: Int
                       ) extends KeyedEntity[Long] {

  def _chatState(chatState: ChatState.ChatState) = {
    val state = chatState match {
      case ChatState.Start => 1
      case ChatState.CreateEvent => 2
      case ChatState.EditEvent => 3
    }
  }
}


object ChatSession {
  val DEFAULT_STATE = ChatState.Start

  def create(csid: Long, alias: String, isGroup: Boolean,
             chatState: ChatState.ChatState): ChatSession = {
    val state = chatState match {
      case ChatState.Start => 1
      case ChatState.CreateEvent => 2
      case ChatState.EditEvent => 3
    }

    DbSchema.insert(new ChatSession(0, csid, alias, isGroup, state))
  }

  /**
    * Returns the set of chat session:
    * @param ids - for the specified id, if Some provided,
    *            for all existing if None.
    * @return - mutable set of chat sessions.
    */
  def get(ids: Option[mutable.Set[Long]]) = {
    DbSchema.getAllChatSessions(ids)
  }

  def getByChatSessionId(csid: Long) = {
    DbSchema.getChatSessionByChatSessionId(csid)
  }

  def update(cs: ChatSession) = {
    DbSchema.update[ChatSession](cs)
  }

  def exists(id: Long): Boolean = {
    DbSchema.existsChatSession(id)
  }

  def hasIntersections(csid: Long, beginDate: Date, endDate: Date) = {
    val begin = new Timestamp(beginDate.getTime)
    val end = new Timestamp(endDate.getTime)
    DbSchema.hasIntersections(csid, begin, end)
  }

  def addUserToGroup(groupChatId: Long,
                     memberChatId: Long, memberAlias: String) = {
    val group = DbSchema.getChatSessionIdByChatId(groupChatId)
      .getOrElse(
        ChatSession.create(groupChatId, "", isGroup = true, DEFAULT_STATE)
      )
    val member = DbSchema.getChatSessionIdByChatId(memberChatId)
      .getOrElse(
        ChatSession.create(
          memberChatId, memberAlias, isGroup = false, DEFAULT_STATE)
      )

    val gm = GroupMembers(group.id, member.id)
    DbSchema.insert[GroupMembers](gm)
  }
}