package com.inno.sierra.model

import java.sql.Timestamp
import java.util.{Calendar, Date}

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.adapters.{H2Adapter, PostgreSqlAdapter}
import org.squeryl.{Schema, Session, SessionFactory}

import scala.collection.mutable.ListBuffer
import scala.reflect.runtime.universe._


/**
  * The intermidiary in the communications with the database.
  * Uses Squeryl ORM.
  */
object DbSchema extends Schema with LazyLogging {
  private val conf = ConfigFactory.load()

  /**
    * Loads the configuration of the database.
    * Can work with H2 database.
    */
  private val driver = conf.getString("db.driver")
  private val adapter = driver match {
    case "h2" =>
      Class.forName("org.h2.Driver")
      new H2Adapter

    case "postgresql" => // TODO: not tested, maybe doesn't work
      Class.forName("org.postgresql.Driver")
      new PostgreSqlAdapter

    case _ =>
      throw new NotImplementedError("Unsupported database driver")
  }

  /**
    * The necessary database's session initialisation.
    * Without it transaction function will not work.
    */
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
  private val chatSessions = table[ChatSession]
  private val events = table[Event]
  private val csEvents = manyToManyRelation(chatSessions, events).
    via[ChatSessionEvents](
    (cs, e, cse) => (cse.eventId === e.id, cs.id === cse.chatSessionId)
  )
  private val groupMembers = manyToManyRelation(chatSessions, chatSessions).
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
      case t if t =:= typeOf[Event] =>
        transaction(from(events)(s => where(s.id === id).select(s)).headOption)
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
    * @param entity entity
    * @tparam T type of the entity
    * @return the inserted entity
    * @throws IllegalArgumentException if the type is not supported
    */
  def insert[T](entity: T): T = {
    val result = entity match {
      case cs: ChatSession => transaction(chatSessions.insert(cs))
      case e: Event => transaction(events.insert(e))
      case cse: ChatSessionEvents => transaction(csEvents.insert(cse))
      case gm: GroupMembers => transaction(groupMembers.insert(gm))
      case _ => throw new IllegalArgumentException(
        "the type " + entity.getClass + " is not known")
    }
    result.asInstanceOf[T]
  }

  /**
    * Updates the entity in the database.
    * @param entity entity
    * @tparam T type of the entity
    */
  def update[T](entity: T): Unit = {
    entity match {
      case cs: ChatSession => transaction(chatSessions.update(cs))
      case e: Event => transaction(events.update(e))
      //case cse: ChatSessionEvents => transaction(csEvents.update(cse))
      //case gm: GroupMembers => transaction(groupMembers.update(gm))
      case _ => throw new IllegalArgumentException(
        "the type " + entity.getClass + " is unknown")
    }
  }

  /**
    * Deletes the entity by its id.
    * @param id id of the entity. In case of ChatSessionEvents use eventId
    * @tparam T type of the entity
    * @throws IllegalArgumentException if the type is not supported
    */
  def delete[T: TypeTag](id: Long): Unit = typeOf[T] match {
    case t if t =:= typeOf[ChatSession] =>
      transaction(chatSessions.deleteWhere(_.id === id))
    case t if t =:= typeOf[Event] =>
      transaction(events.deleteWhere(_.id === id))
    case t if t =:= typeOf[ChatSessionEvents] =>
      transaction(csEvents.deleteWhere(_.eventId === id)) //BY EVENT ID
    case _ => throw new IllegalArgumentException(
      "the type " + typeOf[T] + " is unknown")
  }

  def delete(gm: GroupMembers): Unit = {
      transaction(groupMembers.deleteWhere(g =>
        g.groupId === gm.groupId and g.memberId === gm.memberId))
  }

  /**
    * Returns members of the groupby its chatsession id.
    * @param csid chatsession id
    * @return the list of chatsessions
    */
  def getMembers(csid: Long): List[ChatSession] = {
    val group = getChatSessionByChatId(csid).getOrElse(
      return List[ChatSession]()
    )
    transaction {
      from(groupMembers, chatSessions)((gm, cs) =>
        where(
          gm.memberId === cs.id and gm.groupId === group.id
        ).select(cs)).toList
    }
  }

  /**
    * Returns all the events to the specified date.
    * @param date the date to which the events returned
    * @param isNotified only unnotified events are returned
    * @return
    */
  def getAllEventsTillDate(date: Date, isNotified: Boolean = false): List[Event] = {
    val stamp = new Timestamp(date.getTime)
    transaction {
      from(events)(e =>
        where(e.isNotified === false and e.beginDate.lt(stamp))
          .select(e)).toList
    }
  }

  /**
    * Returns all the upcoming events for the user
    * specified by chatsession id.
    * @param csid chatsession id
    * @return list of events
    */
  def getAllUpcomingEventsForUser(csid: Long): List[Event] = {
    val beginDate = new Timestamp(new Date().getTime)
    transaction {
      from(events, csEvents, chatSessions)((e, cse, cs) =>
        where(
          cse.eventId === e.id and cse.chatSessionId === cs.id and cs.csid === csid and
            e.beginDate.gt(beginDate)
        ).select(e)).toList
    }
  }

  /**
    * Returns the events for the specified chatsession id
    * (does not matter personal or group) for the specified day.
    * @param csid chatsession id
    * @param day  date of the day of interest
    * @return
    */
  def getAllEventsForDay(csid: Long, day: Date): List[Event] = {
    val c = Calendar.getInstance()
    c.setTimeInMillis(day.getTime)
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    val beginDate = new Timestamp(c.getTimeInMillis)

    c.set(Calendar.HOUR_OF_DAY, 23)
    c.set(Calendar.MINUTE, 59)
    c.set(Calendar.SECOND, 59)
    c.set(Calendar.MILLISECOND, 0)
    val endDate = new Timestamp(c.getTimeInMillis)

    getEventsWithLimits(csid, beginDate, endDate)
  }

  /**
    * Returns the events which intersect with
    * the dates specified for the specified chatsession.
    * @param csid chatsession id
    * @param beginDate  starting date
    * @param endDate  finishing date
    * @return list of events
    */
  def getEventsWithLimits(csid: Long,
                          beginDate: Timestamp,
                          endDate: Timestamp): List[Event] = {
    transaction {
      from(events, csEvents, chatSessions)((e, cse, cs) =>
        where(
          cse.eventId === e.id and cse.chatSessionId === cs.id and cs.csid === csid and
            ((e.beginDate.lte(beginDate) and e.endDate.gte(beginDate)) or
              (e.beginDate.lte(endDate) and e.endDate.gte(endDate)) or
              (e.beginDate.gte(beginDate) and e.endDate.lte(endDate))
              )
        ).select(e)).toList
    }
  }

  /**
    * Returns chatsession by chatsession id
    * @param csid chatsession id
    * @return Option of Chatsession
    */
  def getChatSessionByChatId(csid: Long): Option[ChatSession] = {
    transaction {
      from(chatSessions)(cs => where(cs.csid === csid).select(cs)).headOption
    }
  }

  /**
    * Returns the chatsession to which the event specified
    * by its id is assigned.
    * @param eventId  event's id
    * @return ChatSession
    */
  def getChatSessionByEventId(eventId: Long): ChatSession = {
    transaction {
      val chatId = from(csEvents)(
        s => where(s.eventId === eventId).select(s)).head.chatSessionId
      from(chatSessions)(cs => where(cs.id === chatId).select(cs)).head
    }
  }

  /**
    * Initializes the database.
    */
  def init(): Unit = {
    // Recreate DB
    transaction {
      Session.cleanupResources
      DbSchema.drop
      DbSchema.create
    }
    logger.debug("db is initialized")
  }
}
