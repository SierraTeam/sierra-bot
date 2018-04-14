package com.inno.sierra.model

import java.sql.Timestamp

import com.typesafe.config.ConfigFactory
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.adapters.{H2Adapter, PostgreSqlAdapter}
import org.squeryl.{Schema, Session, SessionFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import java.util.Date

import com.typesafe.scalalogging.LazyLogging

object DbSchema extends Schema with LazyLogging{
  private val conf = ConfigFactory.load()
  // -----Initialize a connection with DB

  val driver = conf.getString("db.driver")
  val adapter = driver match {
    case "h2" => {
      Class.forName("org.h2.Driver")
      new H2Adapter
    }
    case "postgresql" => {
      Class.forName("org.postgresql.Driver")
      new PostgreSqlAdapter
    }
    case _ => {
      throw new NotImplementedError("Unsupported database driver")
    }
  }

  SessionFactory.concreteFactory = Some(() =>
    Session.create(
      java.sql.DriverManager.getConnection(
        conf.getString("db.connection"),
        conf.getString("db.username"),
        conf.getString("db.password")
      ),
      adapter
    )
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

  def getAllEventsTillDate(date: Date, isNotified: Boolean = false): mutable.Set[Event] = {
    val stamp = new Timestamp(date.getTime)
    val result = mutable.Set[Event]()

    transaction {
      from(events)(e => where(e.isNotified === false and e.beginDate.lt(stamp)).select(e))
        .foreach(e => result += e)
    }
    result
  }

  def getChatSessionIdByChatId(chatId: Long) = {
    transaction {
      from(chatSessions)(cs => where(cs.csid === chatId).select(cs)).head
    }
  }

  def getChatSessionByEventId(eventId: Long) = {
    transaction {
      val chatId = from(csEvents)(
        s => where(s.eventId === eventId).select(s)).head.chatSessionId
      from(chatSessions)(cs => where(cs.id === chatId).select(cs)).head
    }
  }

  def hasIntersections(csid: Long, beginDate: Timestamp, endDate: Timestamp) = {
    val result = ListBuffer[Event]()
    transaction {
      from(events, csEvents, chatSessions)((e, cse, cs) =>
        where(
          cse.eventId === e.id and cse.chatSessionId === cs.id and cs.csid === csid and
          ((e.beginDate.lt(beginDate) and e.endDate.gt(beginDate)) or
            (e.beginDate.lt(endDate) and e.endDate.gt(endDate)) or
            (e.beginDate.gt(beginDate) and e.endDate.lt(endDate))
          )
        ).select(e))
        .foreach(e => result += e)
      logger.debug(result.toString)
      result
    }

  }

  /*or
  (e.beginDate.lt(endDate) and e.endDate.gt(endDate))*/

  /**
    * Initializes the database and fills it with test data.
    */
  def init(): Unit = {
    // Recreate DB
    transaction {
      Session.cleanupResources
      DbSchema.drop
      DbSchema.create
    }
    logger.debug("db is initialized")

    /*ChatSession.create(103478185, "ilyavy", ChatState.Start)
    Event.create(103478185, new Date((new Date()).getTime + 300000),
      "Test delayed", new Date((new Date()).getTime + 600000))*/

    /*ChatSession.create(101, "ax_yv", ChatState.Start)
    ChatSession.create(102, "happy_marmoset", ChatState.Start)
    ChatSession.create(104, "julioreis22", ChatState.Start)
    ChatSession.create(105, "martincfx", ChatState.Start)*/

    logger.debug("Chat sessions: " + ChatSession.get(None))
    logger.debug("Events: " + Event.get(None))
  }
}
