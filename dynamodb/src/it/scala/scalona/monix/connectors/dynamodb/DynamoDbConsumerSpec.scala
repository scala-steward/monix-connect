package scalona.monix.connectors.dynamodb

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.{ Consumer, Observable }
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers, WordSpecLike }
import software.amazon.awssdk.services.dynamodb.model._
import org.scalatest.concurrent.ScalaFutures._
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

class DynamoDbConsumerSpec extends WordSpecLike with Matchers with DynamoDbFixture with BeforeAndAfterAll {

  /*val key = AttributeValue.builder().s("key1").build()
  val value = AttributeValue.builder().n("1").build()
  val keyMap = Map("keyCol" -> key, "valCol" -> value)
  val getItemRequest: GetItemRequest =
    GetItemRequest.builder().tableName("tableName").key(keyMap.asJava).attributesToGet("data").build()
   */

  implicit val client: DynamoDbAsyncClient = DynamoDbClient()
  s"${DynamoDbConsumer}.build() " should {

    s"create a reactive $Consumer" that {

      s"receives `CreateTableRequests` and returns `CreateTableResponses`" in {
        //given
        val consumer: Consumer[CreateTableRequest, Observable[Task[CreateTableResponse]]] =
          DynamoDbConsumer().build[CreateTableRequest, CreateTableResponse]
        val request1 =
          createTableRequest(tableName = citiesTableName, schema = cityKeySchema, attributeDefinition = cityAttrDef)
        val request2 =
          createTableRequest(tableName = "citiesTableName", schema = cityKeySchema, attributeDefinition = cityAttrDef)

        //when
        val stream: Observable[Observable[Task[CreateTableResponse]]] =
          Observable
            .fromIterable(Iterable(request1))
            .consumeWithF[Observable, Observable[Task[CreateTableResponse]]](consumer)

        //then
        val createTableResponse = {
          stream
            .foreach(resp => println(resp.fo.tableDescription()))
        }
        Consumer
        /*createTableResponse shouldBe a[CreateTableResponse]
        createTableResponse.tableDescription().hasKeySchema shouldBe true
        createTableResponse.tableDescription().hasAttributeDefinitions shouldBe true
        createTableResponse.tableDescription().hasGlobalSecondaryIndexes shouldBe false
        createTableResponse.tableDescription().hasReplicas shouldBe false
        createTableResponse.tableDescription().tableName() shouldEqual citiesTableName
        createTableResponse.tableDescription().keySchema() should contain theSameElementsAs cityKeySchema
        createTableResponse.tableDescription().attributeDefinitions() should contain theSameElementsAs cityAttrDef*/
      }

      s"receives `PutItemRequest` and returns `PutItemResponse` " in {
        createCitiesTable()
        val consumer: Consumer[PutItemRequest, Task[PutItemResponse]] =
          DynamoDbConsumerF().build[PutItemRequest, PutItemResponse]
        val request: PutItemRequest = putItemRequest(citiesTableName, citiesMappAttr)

        //when
        val stream =
          Observable.fromIterable(Iterable(request)).consumeWithF[Observable, Task[PutItemResponse]](consumer)

        //then
        val response: PutItemResponse = {
          stream
            .consumeWith(Consumer.head)
            .runSyncUnsafe()
            .runSyncUnsafe()
        }

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
