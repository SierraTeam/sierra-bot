package com.inno.sierra.model

import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._

object ChatState extends Enumeration {
    type ChatState = Value
    val Start = Value(1, "Start")
    val CreateEvent = Value(2, "CreateEvent")
    val EditEvent = Value(3, "EditEvent")
  }

/*ChatSession has a private constructor, use "apply"
function from "ChatSession" object to create an instance.*/
case class ChatSession (
                         csid: Long,
                         var chatState: ChatState.ChatState,
                       ) extends BaseEntity {

  // Required by Squeryl because of enumeration field
  def this() = this(0, ChatState.Start)

  private var temp: Any = ""

  private def this(
                    id: Long,
                    chatState: ChatState.ChatState,
                    t: Any) {
    this(id, chatState)
    temp = t
  }
}

object ChatSession {
  def apply(): ChatSession =
    new ChatSession(0, ChatState.Start)
}