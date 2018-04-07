package com.inno.sierra.model

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.adapters.H2Adapter
import org.squeryl.{Schema, Session, SessionFactory}
import scala.collection.mutable
import java.util.Date

object DbSchema extends Schema {
  private val conf = ConfigFactory.load()
  // -----Initialize a connection with DB
  //val logger = LoggerFactory.getLogger(getClass)

  Class.forName("org.h2.Driver")
  SessionFactory.concreteFactory = Some(() =>
    Session.create(
      java.sql.DriverManager.getConnection(
        conf.getString("db.connection"),
        conf.getString("db.username"),
        conf.getString("db.password")),
      new H2Adapter)
  )


  // -----Define tables
  val chatSessions = table[ChatSession]
  val events = table[Event]
  val csEvents = manyToManyRelation(chatSessions, events).
    via[ChatSessionEvents](
    (cs, e, cse) => (cse.eventId === e.id, cs.id === cse.chatSessionId)
  )

  on(chatSessions)(s => declare(
    s.csid is(indexed, unique, dbType("bigint")),
    s.alias is(indexed, unique, dbType("varchar(255)")),
    s.chatState is dbType("smallint")
  ))

  on(events)(e => declare(
    e.beginDate is (indexed, dbType("timestamp")),
    e.name is indexed,
    e.endDate is (indexed, dbType("timestamp"))
  ))

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

  def insert(cse: ChatSessionEvents): ChatSessionEvents = {
    transaction {
      csEvents.insert(cse)
    }
  }

  def update(e: Event): Unit = {
    transaction {
      events.update(e)
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

  def existsChatSession (csid: Long): Boolean = {
    var result = mutable.Set[ChatSession]()

    transaction {
      from(chatSessions)(cs => where(cs.csid === csid).select(cs))
        .foreach(cs => result += cs)
      result
    }
    !result.isEmpty
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

  def getAllEvents(ids: Option[mutable.Set[Long]]): mutable.Set[Event] = {
    val result = mutable.Set[Event]()

    if (ids.isEmpty) {
      transaction {
        from(events)(e => select(e))
          .foreach(e => result += e)
        result
      }
    } else {
      transaction {
        ids.get.foreach(id => {
          from(events)(e => where(e.id === id).select(e))
            .foreach(e => result += e)
        })
        result
      }
    }
  }

  def getAllEventsTillDate(date: Date): mutable.Set[Event] = {
    val result = mutable.Set[Event]()

    transaction {
      from(events)(e => select(e))
        .foreach(e => {
          if (e.beginDate.before(date)) {
            result += e
          }
        })
      result
    }
  }

  def init(): Unit = {
    // Recreate DB
    transaction {
      Session.cleanupResources
      DbSchema.drop
      DbSchema.create
      //test values for notifications
      Event.create(3, new Date((new Date()).getTime + 180000), "Test delayed", new Date((new Date()).getTime + 240000))
    }

    println("db is initialized")

    /*ChatSession.create(101, "ax_yv", ChatState.Start)
    ChatSession.create(102, "happy_marmoset", ChatState.Start)
    ChatSession.create(103, "ilyavy", ChatState.Start)
    ChatSession.create(104, "julioreis22", ChatState.Start)
    ChatSession.create(105, "martincfx", ChatState.Start)*/

    /*val e = Event.create(0, new Date(), "meeting", 60)
    Event.assignEventTo(e.id, 1)
    Event.assignEventTo(e.id, 2)*/

    println(ChatSession.get(None))
    println(Event.get(None))

  }
}
