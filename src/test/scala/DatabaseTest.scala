
import java.util.Date

import com.inno.sierra.model.{ChatSession, ChatState, DbSchema, Event}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

class DatabaseTest extends FlatSpec with MockFactory with Matchers {

  behavior of "The database"

  it should "insert and retrieve chat sessions by DBschema" in {

    DbSchema.init()

    val chatSessionsInserted = List(ChatSession.create(101, "test1", isGroup = false, ChatState.Started),
      ChatSession.create(1010, "test2", isGroup = false, ChatState.Started))
    val chatSessionsRetrieved = DbSchema.getAll[ChatSession](None)
    assert(chatSessionsInserted.forall(chatSessionsRetrieved.contains(_)) &&
      chatSessionsInserted.size == chatSessionsRetrieved.size)
  }

  it should "insert and retrieve chat sessions by chatsession companion object" in {

    DbSchema.init()

    val chatSessionsInserted = List(ChatSession.create(101, "test1", isGroup = false, ChatState.Started),
      ChatSession.create(1010, "test2", isGroup = false, ChatState.Started))
    val chatSessionsRetrieved = ChatSession.getAll(None)
    assert(chatSessionsInserted.forall(chatSessionsRetrieved.contains(_)) &&
      chatSessionsInserted.size == chatSessionsRetrieved.size)
  }

  it should "insert and retrieve events by event companion object" in {
    DbSchema.init()
    ChatSession.create(1, "test1", isGroup = false, ChatState.Started)
    ChatSession.create(2, "test2", isGroup = false, ChatState.Started)

    val eventsInserted = List(Event.create(1, new Date(), "test1", new Date()),
      Event.create(2, new Date(), "test1", new Date()))
    val eventsRetrieved = Event.get(None)
    assert(eventsInserted.forall(eventsRetrieved.contains(_)) &&
      eventsInserted.size == eventsRetrieved.size)
  }

}
