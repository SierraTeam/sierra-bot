package com.inno.sierra.model

import org.slf4j.LoggerFactory
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.adapters.H2Adapter
import org.squeryl.{Schema, Session, SessionFactory}
import scala.collection.mutable.Set

object DbSchema extends Schema {
  //val logger = LoggerFactory.getLogger(getClass)
  val dbConnection = "jdbc:h2:~/sierrabot"
  val dbUsername = "sa"
  val dbPassword = ""

  val chatSessions = table[ChatSession]
  Class.forName("org.h2.Driver")

  SessionFactory.concreteFactory = Some(() =>
    Session.create(
      java.sql.DriverManager.getConnection(
            dbConnection, dbUsername, dbPassword),
      new H2Adapter)
    )

/*  on(chatSessions)(s => declare(
    s.id is(primaryKey)
  ))*/

  def insert(s: ChatSession): ChatSession = {
    transaction {
      chatSessions.insert(s)
    }
  }

  def update(s: ChatSession): ChatSession = {
    transaction {
      chatSessions.insertOrUpdate(s)
    }
  }

  def deleteChatSession(id: Long): Unit = {
    transaction {
      chatSessions.deleteWhere(_.id === id)
    }
  }


  def getAllChatSessions() = {
    val result = Set[ChatSession]()
    transaction {
      from(chatSessions)(s => select(s))
        .foreach(s => result += s)
      result
    }
  }

  def main(args: Array[String]): Unit = {
    transaction {
      Session.cleanupResources
      DbSchema.drop

      DbSchema.create

      println(insert(ChatSession(9, ChatState.Start)))
      println(insert(ChatSession(3, ChatState.Start)))
      val cs = insert(ChatSession(2, ChatState.Start))
      println(cs)
      cs.chatState = ChatState.CreateEvent
      val cs2 = update(cs)
      println(cs2)
      deleteChatSession(1)
      println(getAllChatSessions())
    }
  }
}
