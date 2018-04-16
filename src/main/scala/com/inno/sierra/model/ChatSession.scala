package com.inno.sierra.model

import java.sql.Timestamp
import java.util.Date
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.annotations.Column

import scala.collection.mutable

object ChatState extends Enumeration {
    type ChatState = Value
    val Started = Value(1, "Started")
    val CreatingEventInputtingName = Value(2, "CreatingEventInputtingName")
    val CreatingEventInputtingParams = Value(3, "CreatingEventInputtingParams")
    val EditingEvent = Value(4, "EditingEvent")
  }

/**
  * There are many inconviniencies with using Squeryl
  * with enum fields, therefore chatState is stored as Int
  * @param id - id will be assigned automatically by ORM
  * @param csid - chatsession id
  * @param alias - user's alias or "" if it's a group
  * @param isGroup - is this is a group (supergroup/channel) or not
  * @param _chatState - state of the chat
  */
case class ChatSession (
                         var id: Long,
                         var csid: Long,
                         var alias: String,
                         var isGroup: Boolean,
                         @Column("CHATSTATE") var _chatState: Int,
                         var inputEventDatetime: Option[Timestamp] = None,
                         var inputEventName: Option[String] = None,
                         var inputEventDurationInMinutes: Option[Int] = None,
                         var inputCalendarMessageId: Option[Int] = None,
                         var inputTimepickerMessageId: Option[Int] = None,
                         var inputDurationpickerMessageId: Option[Int] = None
                       ) extends KeyedEntity[Long] {

  def chatState: ChatState.ChatState = {
    ChatState.values.find(_.id == _chatState).get
  }
  // scalastyle:off method.name
  def chatState_=(chatStateNew: ChatState.ChatState): Unit = {
    _chatState = chatStateNew.id
  }
  // scalastyle:on method.name

  def save() = DbSchema.update(this)

}


object ChatSession {
  val DEFAULT_STATE = ChatState.Started

  def create(csid: Long, alias: String, isGroup: Boolean,
             chatState: ChatState.ChatState): ChatSession = {
    DbSchema.insert(new ChatSession(0, csid, alias, isGroup, chatState.id))
  }

  /**
    * Returns the set of chat session:
    * @param ids - for the specified id, if Some provided,
    *            for all existing if None.
    * @return - mutable set of chat sessions.
    */
  def getAll(ids: Option[List[Long]]) = {
    DbSchema.getAll[ChatSession](ids)
  }

  def getByChatId(csid: Long) = {
    DbSchema.getChatSessionByChatId(csid)
  }

  def update(cs: ChatSession) = {
    DbSchema.update[ChatSession](cs)
  }

  def exists(id: Long): Boolean = {
    DbSchema.getEntityById[ChatSession](id).isDefined
  }

  def hasIntersections(csid: Long, beginDate: Date, endDate: Date) = {
    val begin = new Timestamp(beginDate.getTime)
    val end = new Timestamp(endDate.getTime)
    DbSchema.hasIntersections(csid, begin, end)
  }

  def addUserToGroup(groupChatId: Long,
                     memberChatId: Long, memberAlias: String) = {
    val group = DbSchema.getChatSessionByChatId(groupChatId)
      .getOrElse(
        ChatSession.create(groupChatId, "", isGroup = true, DEFAULT_STATE)
      )
    val member = DbSchema.getChatSessionByChatId(memberChatId)
      .getOrElse(
        ChatSession.create(
          memberChatId, memberAlias, isGroup = false, DEFAULT_STATE)
      )

    val gm = GroupMembers(group.id, member.id)
    DbSchema.insert[GroupMembers](gm)
  }
}