/*
 * Copyright (c) 2020-2021 by The Monix Connect Project Developers.
 * See the project homepage at: https://monix.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package monix.connect.mongodb

import cats.effect.Resource
import com.mongodb.client.model.{Filters, Updates}
import com.mongodb.reactivestreams.client.MongoClients
import monix.connect.mongodb.client.{CollectionCodecRef, CollectionOperator, CollectionRef, MongoConnection}
import monix.connect.mongodb.domain.{Tuple2F, Tuple3F, Tuple4F, Tuple5F, Tuple6F, Tuple7F, Tuple8F}
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.mongodb.scala.bson.codecs.Macros.createCodecProvider
import org.scalacheck.Gen
import org.scalatest.{Assertion, BeforeAndAfterEach}
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class MongoConnectionSuite extends AnyFlatSpecLike with Fixture with Matchers with BeforeAndAfterEach {

  override def beforeEach() = {
    super.beforeEach()
    MongoDb.dropDatabase(db).runSyncUnsafe()
    MongoDb.dropCollection(db, employeesColName).runSyncUnsafe()
    MongoDb.dropCollection(db, companiesColName).runSyncUnsafe()
    MongoDb.dropCollection(db, investorsColName).runSyncUnsafe()
  }

  "A single collection" should "be created given the url endpoint" in {
    //given
    val collectionName = Gen.identifier.sample.get
    val investor = genInvestor.sample.get
    val connection = MongoConnection
      .create1(
        mongoEndpoint,
        CollectionCodecRef(
          dbName,
          collectionName,
          classOf[Investor],
          createCodecProvider[Employee](),
          createCodecProvider[Company](),
          createCodecProvider[Investor]())
      )

    //when
    val r = connection.use {
      case CollectionOperator(_, source, single, _) =>
        single.insertOne(investor).flatMap(_ => source.find(Filters.eq("name", investor.name)).headL)
    }.runSyncUnsafe()

    //then
    r shouldBe investor
  }

  it should "be created given the mongo client settings" in new MongoConnectionFixture {
    //given
    val collectionName = Gen.identifier.sample.get
    val employee = genEmployee.sample.get

    val connection = MongoConnection.create1(
      mongoClientSettings,
      CollectionCodecRef(dbName, collectionName, classOf[Employee], createCodecProvider[Employee]()))

    //when
    val r = connection.use {
      case CollectionOperator(_, source, single, _) =>
        single.insertOne(employee).flatMap(_ => source.find(Filters.eq("name", employee.name)).headL)
    }.runSyncUnsafe()

    //then
    r shouldBe employee
  }

  it should "be created unsafely given a mongo client" in {
    //given
    val collectionName = Gen.identifier.sample.get
    val employee = genEmployee.sample.get
    val col = CollectionCodecRef(dbName, collectionName, classOf[Employee], createCodecProvider[Employee]())
    val connection = MongoConnection.createUnsafe1(MongoClients.create(mongoEndpoint), col)

    //when
    val r = connection.flatMap {
      case CollectionOperator(_, source, single, _) =>
        single.insertOne(employee).flatMap(_ => source.find(Filters.eq("name", employee.name)).headL)
    }.runSyncUnsafe()

    //then
    r shouldBe employee
  }

  "Two collections" should "be created given the url endpoint" in new MongoConnectionFixture {
    def makeResource(col1: CollectionRef[Employee], col2: CollectionRef[Company]) =
      MongoConnection.create2(mongoEndpoint, (col1, col2))
    createConnectionTest2(makeResource)
  }

  it should "be created given the mongo client settings" in new MongoConnectionFixture {
    def makeResource(col1: CollectionRef[Employee], col2: CollectionRef[Company]) =
      MongoConnection.create2(mongoClientSettings, (col1, col2))
    createConnectionTest2(makeResource)
  }

  it should "be created unsafely given the mongo client" in new MongoConnectionFixture {
    def makeResource(col1: CollectionRef[Employee], col2: CollectionRef[Company]) =
      Resource.liftF(MongoConnection.createUnsafe2(MongoClients.create(mongoEndpoint), (col1, col2)))
    createConnectionTest2(makeResource)
  }

  "Three collections" should "be created given the url endpoint" in new MongoConnectionFixture {
    def makeResource(col1: CollectionRef[Company], col2: CollectionRef[Employee], col3: CollectionRef[Investor]) =
      MongoConnection.create3(mongoEndpoint, (col1, col2, col3))
    abstractCreateConnectionTest3(makeResource)
  }

  it should "be created given the mongo client settings" in new MongoConnectionFixture {
    def makeResource(col1: CollectionRef[Company], col2: CollectionRef[Employee], col3: CollectionRef[Investor]) =
      MongoConnection.create3(mongoClientSettings, (col1, col2, col3))
    abstractCreateConnectionTest3(makeResource)
  }

  it should "be created unsafely given a mongo client" in new MongoConnectionFixture {
    def makeResource(col1: CollectionRef[Company], col2: CollectionRef[Employee], col3: CollectionRef[Investor]) =
      Resource.liftF(MongoConnection.createUnsafe3(MongoClients.create(mongoEndpoint), (col1, col2, col3)))
    abstractCreateConnectionTest3(makeResource)
  }

  "Four collections" should "be created given the url endpoint" in new MongoConnectionFixture {
    val makeResource = (collections: Tuple4F[CollectionRef, Employee, Employee, Employee, Company]) =>
      MongoConnection.create4(mongoEndpoint, (collections._1, collections._2, collections._3, collections._4))
    abstractCreateConnectionTest4(makeResource)
  }

  it should "be created given the mongo client settings" in new MongoConnectionFixture {
    val makeResource = (collections: Tuple4F[CollectionRef, Employee, Employee, Employee, Company]) =>
      MongoConnection.create4(mongoClientSettings, (collections._1, collections._2, collections._3, collections._4))
    abstractCreateConnectionTest4(makeResource)
  }

  it should "be created unsafely given a mongo client" in new MongoConnectionFixture {
    val makeResource = (collections: Tuple4F[CollectionRef, Employee, Employee, Employee, Company]) =>
      Resource.liftF(
        MongoConnection.createUnsafe4(
          MongoClients.create(mongoEndpoint),
          (collections._1, collections._2, collections._3, collections._4))
      )
    abstractCreateConnectionTest4(makeResource)
  }

  "Five collections" should "be created given the url endpoint" in new MongoConnectionFixture {
    val makeResource = (collections: Tuple5F[CollectionRef, Employee, Employee, Employee, Employee, Company]) =>
      MongoConnection
        .create5(mongoEndpoint, (collections._1, collections._2, collections._3, collections._4, collections._5))
    abstractCreateConnectionTest5(makeResource)
  }

  it should "be created given the mongo client settings" in new MongoConnectionFixture {
    val makeResource = (collections: Tuple5F[CollectionRef, Employee, Employee, Employee, Employee, Company]) =>
      MongoConnection
        .create5(mongoClientSettings, (collections._1, collections._2, collections._3, collections._4, collections._5))
    abstractCreateConnectionTest5(makeResource)
  }

  it should "be created unsafely given a mongo client" in new MongoConnectionFixture {
    val makeResource = (collections: Tuple5F[CollectionRef, Employee, Employee, Employee, Employee, Company]) =>
      Resource.liftF(
        MongoConnection.createUnsafe5(
          MongoClients.create(mongoEndpoint),
          (collections._1, collections._2, collections._3, collections._4, collections._5)
        )
      )
    abstractCreateConnectionTest5(makeResource)
  }

  "Six collections" should "be created given the url endpoint" in new MongoConnectionFixture {
    val makeResource =
      (collections: Tuple6F[CollectionRef, Employee, Employee, Employee, Employee, Employee, Company]) => {
        val (c1, c2, c3, c4, c5, c6) = collections
        MongoConnection
          .create6(mongoEndpoint, (c1, c2, c3, c4, c5, c6))
      }
    abstractCreateConnectionTest6(makeResource)
  }

  it should "be created given the mongo client settings" in new MongoConnectionFixture {
    val makeResource =
      (collections: Tuple6F[CollectionRef, Employee, Employee, Employee, Employee, Employee, Company]) => {
        val (c1, c2, c3, c4, c5, c6) = collections
        MongoConnection
          .create6(mongoClientSettings, (c1, c2, c3, c4, c5, c6))
      }
    abstractCreateConnectionTest6(makeResource)
  }

  it should "be created unsafely given a mongo client" in new MongoConnectionFixture {
    val makeResource =
      (collections: Tuple6F[CollectionRef, Employee, Employee, Employee, Employee, Employee, Company]) => {
        val (c1, c2, c3, c4, c5, c6) = collections
        Resource.liftF(
          MongoConnection
          .createUnsafe6(MongoClients.create(mongoEndpoint), (c1, c2, c3, c4, c5, c6))
        )
      }
    abstractCreateConnectionTest6(makeResource)
  }

  "Seven collections" should "be created given the url endpoint" in new MongoConnectionFixture {
    val makeResource =
      (collections: Tuple7F[CollectionRef, Employee, Employee, Employee, Employee, Employee, Employee, Company]) => {
        val (c1, c2, c3, c4, c5, c6, c7) = collections
        MongoConnection
          .create7(mongoEndpoint, (c1, c2, c3, c4, c5, c6, c7))
      }
    abstractCreateConnectionTest7(makeResource)
  }

  it should "be created given the mongo client settings" in new MongoConnectionFixture {
    val makeResource =
      (collections: Tuple7F[CollectionRef, Employee, Employee, Employee, Employee, Employee, Employee, Company]) => {
        val (c1, c2, c3, c4, c5, c6, c7) = collections
        MongoConnection
          .create7(mongoClientSettings, (c1, c2, c3, c4, c5, c6, c7))
      }
    abstractCreateConnectionTest7(makeResource)
  }

  it should "be created unsafely given a mongo client" in new MongoConnectionFixture {
    val makeResource =
      (collections: Tuple7F[CollectionRef, Employee, Employee, Employee, Employee, Employee, Employee, Company]) => {
        val (c1, c2, c3, c4, c5, c6, c7) = collections
        Resource.liftF(
          MongoConnection
          .createUnsafe7(MongoClients.create(mongoEndpoint), (c1, c2, c3, c4, c5, c6, c7))
        )
      }
    abstractCreateConnectionTest7(makeResource)
  }

  "Eight collections" should "be created given the url endpoint" in new MongoConnectionFixture {
    val makeResource =
      (collections: Tuple8F[CollectionRef, Employee, Employee, Employee, Employee, Employee, Employee, Employee, Company]) => {
        val (c1, c2, c3, c4, c5, c6, c7, c8) = collections
        MongoConnection
          .create8(mongoEndpoint, (c1, c2, c3, c4, c5, c6, c7, c8))
      }
    abstractCreateConnectionTest8(makeResource)
  }

  it should "be created given the mongo client settings" in new MongoConnectionFixture {
    val makeResource =
      (collections: Tuple8F[CollectionRef, Employee, Employee, Employee, Employee, Employee, Employee, Employee, Company]) => {
        val (c1, c2, c3, c4, c5, c6, c7, c8) = collections
        MongoConnection
          .create8(mongoClientSettings, (c1, c2, c3, c4, c5, c6, c7, c8))
      }
    abstractCreateConnectionTest8(makeResource)
  }

  it should "be created unsafely given a mongo client" in new MongoConnectionFixture {
    val makeResource =
      (collections: Tuple8F[CollectionRef, Employee, Employee, Employee, Employee, Employee, Employee, Employee, Company]) => {
        val (c1, c2, c3, c4, c5, c6, c7, c8) = collections
        Resource.liftF(
          MongoConnection
          .createUnsafe8(MongoClients.create(mongoEndpoint), (c1, c2, c3, c4, c5, c6, c7, c8))
        )
      }
    abstractCreateConnectionTest8(makeResource)
  }

  trait MongoConnectionFixture {

    protected[this] def createConnectionTest2(
      makeResource: (
        CollectionRef[Employee],
        CollectionRef[Company]) => Resource[Task, Tuple2F[CollectionOperator, Employee, Company]]): Assertion = {
      //given
      val employee = genEmployee.sample.get
      val company = genCompany.sample.get
      val connection = makeResource(employeesCol, companiesCol)

      //when
      val (r1, r2) = connection.use {
        case (
            CollectionOperator(employeeDb, employeeSource, employeeSingle, employeeSink),
            CollectionOperator(companyDb, companySource, companySingle, companySink)) =>
          val r1 = employeeSingle
            .insertOne(employee)
            .flatMap(_ => employeeSource.find(Filters.eq("name", employee.name)).headL)
          val r2 = companySingle
            .insertOne(company)
            .flatMap(_ => companySource.find(Filters.eq("name", company.name)).headL)
          Task.parZip2(r1, r2)
      }.runSyncUnsafe()

      //then
      r1 shouldBe employee
      r2 shouldBe company
    }

    protected[this] def abstractCreateConnectionTest3(
      makeResource: (
        CollectionRef[Company],
        CollectionRef[Employee],
        CollectionRef[Investor]) => Resource[Task, Tuple3F[CollectionOperator, Company, Employee, Investor]])
      : Assertion = {
      //given
      val employees = List(Employee("Caroline", 21, "Barcelona", "OldCompany"))
      val company = Company("OldCompany", employees, 0)
      val investor1 = Investor("MyInvestor1", 10001, List(company))
      val investor2 = Investor("MyInvestor2", 20001, List(company))
      MongoSingle.insertMany(investorsMongoCol, List(investor1, investor2)).runSyncUnsafe()
      MongoSingle.insertMany(employeesMongoCol, employees).runSyncUnsafe()
      MongoSingle.insertOne(companiesMongoCol, company).runSyncUnsafe()
      //and
      val companiesCol = CollectionCodecRef(
        dbName,
        companiesColName,
        classOf[Company],
        createCodecProvider[Company](),
        createCodecProvider[Employee]())
      val employeesCol =
        CollectionCodecRef(dbName, employeesColName, classOf[Employee], createCodecProvider[Employee]())
      val investorsCol = CollectionCodecRef(
        dbName,
        investorsColName,
        classOf[Investor],
        createCodecProvider[Investor](),
        createCodecProvider[Company](),
        createCodecProvider[Employee]())

      val connection = makeResource(companiesCol, employeesCol, investorsCol)

      //when
      connection.use {
        case (
            CollectionOperator(_, companySource, companySingle, companySink),
            CollectionOperator(_, employeeSource, employeeSingle, employeeSink),
            CollectionOperator(_, investorSource, investorSingle, _)) =>
          for {
            _ <- companySingle
              .insertOne(Company("NewCompany", employees = List.empty, investment = 0))
              .delayResult(1.second)
            _ <- {
              employeeSource
                .find(Filters.eq("companyName", "OldCompany")) //read employees from old company
                .bufferTimedAndCounted(2.seconds, 15)
                .map { employees =>
                  // pushes them into the new one
                  (Filters.eq("name", "NewCompany"), Updates.pushEach("employees", employees.asJava))
                }
                .consumeWith(companySink.updateOne())
            }
            //aggregates all the
            investment <- investorSource.find(Filters.in("companies.name", "OldCompany")).map(_.funds).sumL
            updateResult <- companySingle.updateMany(
              Filters.eq("name", "NewCompany"),
              Updates.set("investment", investment))
            newCompany <- MongoSource.find(companiesMongoCol, Filters.eq("name", "NewCompany")).headL
          } yield {
            //then
            updateResult.wasAcknowledged shouldBe true
            updateResult.matchedCount shouldBe 1

            //and
            newCompany.employees should contain theSameElementsAs employees
            newCompany.investment shouldBe investor1.funds + investor2.funds
          }
      }.runSyncUnsafe()

    }

    protected[this] def abstractCreateConnectionTest4(
      makeResource: Tuple4F[CollectionRef, Employee, Employee, Employee, Company] => Resource[
        Task,
        Tuple4F[CollectionOperator, Employee, Employee, Employee, Company]]
    ): Assertion = {
      //given
      val company = genCompany.sample.get
      val (employee1, employee2, employee3) = (genEmployee.sample.get, genEmployee.sample.get, genEmployee.sample.get)
      val connection = makeResource((employeesCol, employeesCol, employeesCol, companiesCol))

      //when
      connection.use {
        case (employees1, employees2, employees3, companies) =>
          for {
            r1 <- employees1.single
              .insertOne(employee1)
              .flatMap(_ => employees1.source.find(Filters.eq("name", employee1.name)).headL)
            r2 <- employees2.single
              .insertOne(employee2)
              .flatMap(_ => employees2.source.find(Filters.eq("name", employee2.name)).headL)
            r3 <- employees3.single
              .insertOne(employee3)
              .flatMap(_ => employees3.source.find(Filters.eq("name", employee3.name)).headL)
            r4 <- companies.single
              .insertOne(company)
              .flatMap(_ => companies.source.find(Filters.eq("name", company.name)).headL)
          } yield {
            //then
            r1 shouldBe employee1
            r2 shouldBe employee2
            r3 shouldBe employee3
            r4 shouldBe company
          }
      }.runSyncUnsafe()

    }
  }

  protected[this] def abstractCreateConnectionTest5(
    makeResource: Tuple5F[CollectionRef, Employee, Employee, Employee, Employee, Company] => Resource[
      Task,
      Tuple5F[CollectionOperator, Employee, Employee, Employee, Employee, Company]]): Assertion = {
    //given
    val company = genCompany.sample.get
    val (employee1, employee2, employee3, employee4) =
      (genEmployee.sample.get, genEmployee.sample.get, genEmployee.sample.get, genEmployee.sample.get)
    val connection = makeResource((employeesCol, employeesCol, employeesCol, employeesCol, companiesCol))

    //when
    connection.use {
      case (employees1, employees2, employees3, employees4, companies) =>
        for {
          r1 <- employees1.single
            .insertOne(employee1)
            .flatMap(_ => employees1.source.find(Filters.eq("name", employee1.name)).headL)
          r2 <- employees2.single
            .insertOne(employee2)
            .flatMap(_ => employees2.source.find(Filters.eq("name", employee2.name)).headL)
          r3 <- employees3.single
            .insertOne(employee3)
            .flatMap(_ => employees3.source.find(Filters.eq("name", employee3.name)).headL)
          r4 <- employees4.single
            .insertOne(employee4)
            .flatMap(_ => employees4.source.find(Filters.eq("name", employee4.name)).headL)
          r5 <- companies.single
            .insertOne(company)
            .flatMap(_ => companies.source.find(Filters.eq("name", company.name)).headL)
        } yield {
          //then
          r1 shouldBe employee1
          r2 shouldBe employee2
          r3 shouldBe employee3
          r4 shouldBe employee4
          r5 shouldBe company
        }
    }.runSyncUnsafe()

  }

  protected[this] def abstractCreateConnectionTest6(
    makeResource: Tuple6F[CollectionRef, Employee, Employee, Employee, Employee, Employee, Company] => Resource[
      Task,
      Tuple6F[CollectionOperator, Employee, Employee, Employee, Employee, Employee, Company]]): Assertion = {
    //given
    val company = genCompany.sample.get
    val (employee1, employee2, employee3, employee4, employee5) =
      (
        genEmployee.sample.get,
        genEmployee.sample.get,
        genEmployee.sample.get,
        genEmployee.sample.get,
        genEmployee.sample.get)
    val connection = makeResource((employeesCol, employeesCol, employeesCol, employeesCol, employeesCol, companiesCol))

    //when
    connection.use {
      case (employees1, employees2, employees3, employees4, employees5, companies) =>
        for {
          r1 <- employees1.single
            .insertOne(employee1)
            .flatMap(_ => employees1.source.find(Filters.eq("name", employee1.name)).headL)
          r2 <- employees2.single
            .insertOne(employee2)
            .flatMap(_ => employees2.source.find(Filters.eq("name", employee2.name)).headL)
          r3 <- employees3.single
            .insertOne(employee3)
            .flatMap(_ => employees3.source.find(Filters.eq("name", employee3.name)).headL)
          r4 <- employees4.single
            .insertOne(employee4)
            .flatMap(_ => employees4.source.find(Filters.eq("name", employee4.name)).headL)
          r5 <- employees5.single
            .insertOne(employee5)
            .flatMap(_ => employees5.source.find(Filters.eq("name", employee5.name)).headL)
          r6 <- companies.single
            .insertOne(company)
            .flatMap(_ => companies.source.find(Filters.eq("name", company.name)).headL)
        } yield {
          //then
          r1 shouldBe employee1
          r2 shouldBe employee2
          r3 shouldBe employee3
          r4 shouldBe employee4
          r5 shouldBe employee5
          r6 shouldBe company
        }
    }.runSyncUnsafe()


  }

  protected[this] def abstractCreateConnectionTest7(makeResource: Tuple7F[
    CollectionRef,
    Employee,
    Employee,
    Employee,
    Employee,
    Employee,
    Employee,
    Company] => Resource[
    Task,
    Tuple7F[CollectionOperator, Employee, Employee, Employee, Employee, Employee, Employee, Company]]): Assertion = {
    //given
    val company = genCompany.sample.get
    val (employee1, employee2, employee3, employee4, employee5, employee6) =
      (
        genEmployee.sample.get,
        genEmployee.sample.get,
        genEmployee.sample.get,
        genEmployee.sample.get,
        genEmployee.sample.get,
        genEmployee.sample.get)
    val connection =
      makeResource((employeesCol, employeesCol, employeesCol, employeesCol, employeesCol, employeesCol, companiesCol))

    //when
    connection.use {
      case (employees1, employees2, employees3, employees4, employees5, employees6, companies) =>
        for {
          r1 <- employees1.single
            .insertOne(employee1)
            .flatMap(_ => employees1.source.find(Filters.eq("name", employee1.name)).headL)
          r2 <- employees2.single
            .insertOne(employee2)
            .flatMap(_ => employees2.source.find(Filters.eq("name", employee2.name)).headL)
          r3 <- employees3.single
            .insertOne(employee3)
            .flatMap(_ => employees3.source.find(Filters.eq("name", employee3.name)).headL)
          r4 <- employees4.single
            .insertOne(employee4)
            .flatMap(_ => employees4.source.find(Filters.eq("name", employee4.name)).headL)
          r5 <- employees5.single
            .insertOne(employee5)
            .flatMap(_ => employees5.source.find(Filters.eq("name", employee5.name)).headL)
          r6 <- employees6.single
            .insertOne(employee6)
            .flatMap(_ => employees6.source.find(Filters.eq("name", employee6.name)).headL)
          r7 <- companies.single
            .insertOne(company)
            .flatMap(_ => companies.source.find(Filters.eq("name", company.name)).headL)
        } yield {
          //then
          r1 shouldBe employee1
          r2 shouldBe employee2
          r3 shouldBe employee3
          r4 shouldBe employee4
          r5 shouldBe employee5
          r6 shouldBe employee6
          r7 shouldBe company
        }
    }.runSyncUnsafe()

  }

  protected[this] def abstractCreateConnectionTest8(
    makeResource: Tuple8F[
      CollectionRef,
      Employee,
      Employee,
      Employee,
      Employee,
      Employee,
      Employee,
      Employee,
      Company] => Resource[
      Task,
      Tuple8F[CollectionOperator, Employee, Employee, Employee, Employee, Employee, Employee, Employee, Company]])
    : Assertion = {
    //given
    val company = genCompany.sample.get
    val (employee1, employee2, employee3, employee4, employee5, employee6, employee7) =
      (
        genEmployee.sample.get,
        genEmployee.sample.get,
        genEmployee.sample.get,
        genEmployee.sample.get,
        genEmployee.sample.get,
        genEmployee.sample.get,
        genEmployee.sample.get)
    val connection = makeResource((
      employeesCol,
      employeesCol,
      employeesCol,
      employeesCol,
      employeesCol,
      employeesCol,
      employeesCol,
      companiesCol))

    //when
    connection.use {
      case (employees1, employees2, employees3, employees4, employees5, employees6, employees7, companies) =>
        for {
          r1 <- employees1.single
            .insertOne(employee1)
            .flatMap(_ => employees1.source.find(Filters.eq("name", employee1.name)).headL)
          r2 <- employees2.single
            .insertOne(employee2)
            .flatMap(_ => employees2.source.find(Filters.eq("name", employee2.name)).headL)
          r3 <- employees3.single
            .insertOne(employee3)
            .flatMap(_ => employees3.source.find(Filters.eq("name", employee3.name)).headL)
          r4 <- employees4.single
            .insertOne(employee4)
            .flatMap(_ => employees4.source.find(Filters.eq("name", employee4.name)).headL)
          r5 <- employees5.single
            .insertOne(employee5)
            .flatMap(_ => employees5.source.find(Filters.eq("name", employee5.name)).headL)
          r6 <- employees6.single
            .insertOne(employee6)
            .flatMap(_ => employees6.source.find(Filters.eq("name", employee6.name)).headL)
          r7 <- employees7.single
            .insertOne(employee7)
            .flatMap(_ => employees7.source.find(Filters.eq("name", employee7.name)).headL)
          r8 <- companies.single
            .insertOne(company)
            .flatMap(_ => companies.source.find(Filters.eq("name", company.name)).headL)
        } yield {
          //then
          r1 shouldBe employee1
          r2 shouldBe employee2
          r3 shouldBe employee3
          r4 shouldBe employee4
          r5 shouldBe employee5
          r6 shouldBe employee6
          r7 shouldBe employee7
          r8 shouldBe company
        }
    }.runSyncUnsafe()

  }
}
