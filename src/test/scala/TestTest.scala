
import com.inno.sierra.model.DbSchema
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

class TestTest extends FlatSpec with MockFactory with Matchers {

  behavior of "The database"

  it should "print set" in {
    assert(DbSchema.init() === "Set(ChatSession(1,101,ax_yv,1), ChatSession(5,105,martincfx,1)," +
      " ChatSession(2,102,happy_marmoset,1)," +
      " ChatSession(3,103,ilyavy,1)," +
      " ChatSession(4,104,julioreis22,1))")
  }

}
