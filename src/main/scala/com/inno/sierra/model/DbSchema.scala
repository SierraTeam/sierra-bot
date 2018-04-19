package com.inno.sierra.model

import java.sql.Timestamp
import java.util.Date

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.adapters.{H2Adapter, PostgreSqlAdapter}
import org.squeryl.{Schema, Session, SessionFactory}

import scala.collection.mutable.ListBuffer
import scala.reflect.runtime.universe._


object DbSchema extends Schema with LazyLogging {
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


  // -----Define tables-----
  val chatSessions = table[ChatSession]
  val events = table[Event]
  val csEvents = manyToManyRelation(chatSessions, events).
    via[ChatSessionEvents](
    (cs, e, cse) => (cse.eventId === e.id, cs.id === cse.chatSessionId)
  )
  val groupMembers = manyToManyRelation(chatSessions, chatSessions).
    via[GroupMembers](
    (gr, mem, grm) => (grm.groupId === gr.id, grm.memberId === mem.id)
  )

  on(chatSessions)(s => declare(
    s.csid is(indexed, unique, dbType("bigint")),
    s.alias is(indexed, unique, dbType("varchar(255)"))
  ))

  on(events)(e => declare(
    e.beginDate is(indexed, dbType("timestamp")),
    e.name is indexed,
    e.endDate is(indexed, dbType("timestamp"))
  ))


  // -----Methods-----
  /**
    * Returns the entity by its id (in the database).
    *
    * @param id id of the entity
    * @tparam T type of the entity
    * @return Option[T]
    */
  def getEntityById[T: TypeTag](id: Long): Option[T] = {
    val result = typeOf[T] match {
      case t if t =:= typeOf[ChatSession] =>
        transaction(from(chatSessions)(s => where(s.id === id).select(s)).headOption)
    }
    result.asInstanceOf[Option[T]]
  }

  /**
    * Returns the list of entities by the provided list of ids.
    * If no ids provided then all the entities are returned.
    *
    * @param ids optional list of ids
    * @tparam T type of the entity
    * @return List[T]
    */
  def getAll[T: TypeTag](ids: Option[List[Long]]): List[T] = {
    val result =
      if (ids.isEmpty) {
        typeOf[T] match {
          case t if t =:= typeOf[ChatSession] =>
            transaction(from(chatSessions)(s => select(s)).toList)
          case t if t =:= typeOf[Event] =>
            transaction(from(events)(s => select(s)).toList)
        }
      } else {
        ids.get.map(id => getEntityById[T](id).getOrElse(null))
      }

    result.filter(_ != null).asInstanceOf[List[T]]
  }

  /**
    * Inserts the entity into the database.
    *
    * @param entity entity
    * @tparam T type of the entity
    * @return the inserted entity
    */
  def insert[T](entity: T): T = {
    val result = entity match {
      case cs: ChatSession => transaction(chatSessions.insert(cs))
      case e: Event => transaction(events.insert(e))
      case cse: ChatSessionEvents => transaction(csEvents.insert(cse))
      case gm: GroupMembers => transaction(groupMembers.insert(gm))
      case _ => new IllegalArgumentException(
        "the type " + entity.getClass + "is not known")
    }
    result.asInstanceOf[T]
  }

  /**
    * Updates the entity in the database.
    *
    * @param entity entity
    * @tparam T type of the entity
    */
  def update[T](entity: T): Unit = {
    entity match {
      case cs: ChatSession => transaction(chatSessions.update(cs))
      case e: Event => transaction(events.update(e))
      //case cse: ChatSessionEvents => transaction(csEvents.update(cse))
      //case gm: GroupMembers => transaction(groupMembers.update(gm))
      case _ => new IllegalArgumentException(
        "the type " + entity.getClass + "is not known")
    }
  }

  /**
    * Deletes the entity by its id.
    *
    * @param id id of the entity
    * @tparam T type of the entity
    */
  def delete[T: TypeTag](id: Long): Unit = typeOf[T] match {
    case t if t =:= typeOf[ChatSession] =>
      transaction(chatSessions.deleteWhere(_.id === id))
    case t if t =:= typeOf[Event] =>
      transaction(events.deleteWhere(_.id === id))
  }


  def getAllEventsTillDate(date: Date, isNotified: Boolean = false): List[Event] = {
    val stamp = new Timestamp(date.getTime)
    transaction {
      from(events)(e =>
        where(e.isNotified === false and e.beginDate.lt(stamp))
          .select(e)).toList
    }
  }

  def getAllUpcomingEventsForUser(csid: Long) = {
    val beginDate = new Timestamp(new Date getTime)
    val result = ListBuffer[Event]()
    transaction {
      from(events, csEvents, chatSessions)((e, cse, cs) =>
        where(
          cse.eventId === e.id and cse.chatSessionId === cs.id and cs.csid === csid and
            e.beginDate.gt(beginDate)
        ).select(e))
        .foreach(e => result += e)
      logger.debug(result.toString)
      result
    }
  }

  def getChatSessionByChatId(chatId: Long) = {
    transaction {
      from(chatSessions)(cs => where(cs.csid === chatId).select(cs)).headOption
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

    /*ChatSession.create(103478185, "ilyavy", false, ChatState.Start)
    Event.create(103478185, new Date((new Date()).getTime + 300000),
      "Test delayed", new Date((new Date()).getTime + 600000))*/

    /*ChatSession.create(101, "ax_yv", ChatState.Start)
    ChatSession.create(102, "happy_marmoset", ChatState.Start)
    ChatSession.create(104, "julioreis22", ChatState.Start)
    ChatSession.create(105, "martincfx", ChatState.Start)*/

    logger.debug("Chat sessions: " + ChatSession.getAll(None))
    logger.debug("Events: " + Event.get(None))
  }

}
