package com.inno.sierra.model

import org.slf4j.LoggerFactory
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.adapters.H2Adapter
import org.squeryl.{Schema, Session, SessionFactory}

import scala.collection.mutable
import scala.collection.mutable.Set

object DbSchema extends Schema {
  // -----Initialize a connection with DB
  //val logger = LoggerFactory.getLogger(getClass)
  val dbConnection = "jdbc:h2:~/sierrabot"
  val dbUsername = "sa"
  val dbPassword = ""

  Class.forName("org.h2.Driver")
  SessionFactory.concreteFactory = Some(() =>
    Session.create(
      java.sql.DriverManager.getConnection(
        dbConnection, dbUsername, dbPassword),
      new H2Adapter)
  )


  // -----Define tables
  val chatSessions = table[ChatSession]
  val events = table[Event]

  on(chatSessions)(s => declare(
    s.csid is(indexed, unique, dbType("bigint")),
    s.alias is(indexed, unique, dbType("varchar(255)")),
    s.chatState is dbType("smallint")
  ))

  on(events)(e => declare(
    e.time is indexed,
    e.name is indexed,
    e.duration is dbType("bigint")
  ))


  DbSchema.init()

  // -----Methods
  def insert(s: ChatSession): ChatSession = {
    transaction {
      chatSessions.insert(s)
    }
  }

  def insert(e: Event): Event = {
    transaction {
      events.insert(e)
    }
  }


    def update(s: ChatSession): Unit = {
      transaction {
        chatSessions.update(s)
      }
    }

  def deleteChatSession(id: Long): Unit = {
    transaction {
      chatSessions.deleteWhere(_.id === id)
    }
  }


  def getAllChatSessions(
                          ids: Option[mutable.Set[Long]]
                        ): mutable.Set[ChatSession] = {

    val result = mutable.Set[ChatSession]()

    if (ids.isEmpty) {
      transaction {
        from(chatSessions)(s => select(s))
          .foreach(s => result += s)
        result
      }
    } else {
      transaction {
        ids.get.foreach(id => {
          from(chatSessions)(s => where(s.id === id).select(s))
            .foreach(s => result += s)
        })
        result
      }
    }
  }

  def init(): Unit = {
    // Recreate DB
    transaction {
      Session.cleanupResources
      DbSchema.drop
      DbSchema.create
    }

    ChatSession.create(0, 101, "ax_yv", ChatState.Start)
    ChatSession.create(0, 102, "happy_marmoset", ChatState.Start)
    ChatSession.create(0, 103, "ilyavy", ChatState.Start)
    ChatSession.create(0, 104, "julioreis22", ChatState.Start)
    ChatSession.create(0, 105, "martincfx", ChatState.Start)

    println(ChatSession.get(None))
  }
}
