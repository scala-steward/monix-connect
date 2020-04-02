package scalona.monix.connectors.dynamodb

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.{Consumer, Observable}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers, WordSpecLike}
import software.amazon.awssdk.services.dynamodb.model._
import org.scalatest.concurrent.ScalaFutures._
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

class DynamoDbConsumerSpec extends WordSpecLike with Matchers with ScalaFutures with DynamoDbFixture with BeforeAndAfterAll {

  /*val key = AttributeValue.builder().s("key1").build()
  val value = AttributeValue.builder().n("1").build()
  val keyMap = Map("keyCol" -> key, "valCol" -> value)
  val getItemRequest: GetItemRequest =
    GetItemRequest.builder().tableName("tableName").key(keyMap.asJava).attributesToGet("data").build()
   */

  implicit val client: DynamoDbAsyncClient = DynamoDbClient()
  s"${DynamoDb}.build() " should {

    s"create a reactive $Consumer" that {

      s"receives `CreateTableRequests` and returns `CreateTableResponses`" in {
        //given
        val consumer: Consumer[CreateTableRequest, Task[CreateTableResponse]] =
          DynamoDb.consumer[CreateTableRequest, CreateTableResponse]
        val request = createTableRequest(tableName = citiesTableName, schema = cityKeySchema, attributeDefinition = cityAttrDef)

        //when
        val f: Task[CreateTableResponse] =
          Observable.fromIterable(Iterable(request)).consumeWith(consumer).runSyncUnsafe()

        //then

        whenReady(f.runToFuture) { response: CreateTableResponse =>
          response shouldBe a[CreateTableResponse]
          response.tableDescription().hasKeySchema shouldBe true
          response.tableDescription().hasAttributeDefinitions shouldBe true
          response.tableDescription().hasGlobalSecondaryIndexes shouldBe false
          response.tableDescription().hasReplicas shouldBe false
          response.tableDescription().tableName() shouldEqual citiesTableName
          response.tableDescription().keySchema() should contain theSameElementsAs cityKeySchema
          response.tableDescription().attributeDefinitions() should contain theSameElementsAs cityAttrDef
        }
      }

      s"receives `PutItemRequest` and returns `PutItemResponse` " in {
        createCitiesTable()
        val consumer: Consumer[PutItemRequest, Task[PutItemResponse]] =
          DynamoDb.consumer[PutItemRequest, PutItemResponse]
        val request: PutItemRequest = putItemRequest(citiesTableName, citiesMappAttr)

        //when
        val stream: Task[Task[PutItemResponse]] =
          Observable.fromIterable(Iterable(request)).consumeWith(consumer)

        //then


      }
    }
  }


  override def beforeAll(): Unit = {
    super.beforeAll()
    deleteTable(citiesTableName)
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }
}
