package mytest

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mapping.FieldType._
import java.util.UUID
import org.elasticsearch.common.settings.ImmutableSettings
import org.joda.time.DateTime


class IndexingMetadata(val indexName: String, val client: ElasticClient)
class GestoWorld(val memberIds: Seq[UUID], val locationIds: Seq[UUID])

object Main extends App {
  println("App starting")

  if (args.length < 2) {
    println("Usage: Requires two arguments: ")
    println("  arg0: Number of months to generate")
    println("  arg1: Number of passes over that month")
    System.exit(0)
  }

  val numMonths = args(0).toInt
  val numPasses = args(1).toInt

  val settings = ImmutableSettings.settingsBuilder().put("cluster.name", "gesto_es").build()
  val client   = ElasticClient.remote(settings, ("localhost", 9300))

  val indexingMetadata = new IndexingMetadata("gesto", client)
  val world = new GestoWorld(DataGenerator.randomIdSet(), DataGenerator.randomIdSet())


  if (numMonths > 0) {

    /*
      Step #1: Clear index and install mappings
    */
    if (client.sync.exists(indexingMetadata.indexName).isExists) {
      client.sync.execute {
        deleteIndex(indexingMetadata.indexName)
      }
    }

    client.sync.execute {
      create index indexingMetadata.indexName mappings (
        "order" as(
          "id" typed StringType index "not_analyzed",
          "memberId" typed StringType index "not_analyzed",
          "referenceNumber" typed StringType index "not_analyzed",
          "captureDate" typed DateType,
          "locationId" typed StringType index "not_analyzed",
          "status" typed LongType,
          "detail" inner(
            "amount" typed DoubleType,
            "detail" typed ObjectType
            )
          )
        )
    }


    /*
      Step #2: Index Orders
    */
    val rangeEnd = new DateTime()

    // TODO: Docs suggest we can use the fields but it doesn't seem to work with the nested objects
    //       https://github.com/sksamuel/elastic4s/blob/master/guide/index.md
    //IndexOrdersWithFields.indexOrders(indexingMetadata, world, DataGenerator.dateRange(rangeEnd.minusMonths(3), rangeEnd, "random"))

    println("\n\nIndexing orders")
    1 to numPasses foreach (i =>
      IndexOrdersByString.indexOrders(indexingMetadata, world, DataGenerator.dateRange(rangeEnd.minusMonths(numMonths), rangeEnd, "random"))
    )
  }

  /*
    Step #3: Run Queries
  */

  def executeQuery(queryName: String, query: String) = {
    println(s"\n\nExecuting: $queryName -> $query")
    val response = OrderQueryService.executeQuery(indexingMetadata, queryName, query)
    //println("Results: " + response)
    response
  }

  /*
      Orders By Status

      GET gesto/order/_search
      {
          "aggregations" : {
              "orders-by-status" : {
                  "histogram" : {
                      "field" : "status",
                      "interval" : 1
                  }
              }
          }
      }
  */
  executeQuery("Orders By Status", "{\"aggregations\":{\"Status\":{\"histogram\":{\"field\":\"status\",\"interval\":1}}}}")

  /*
      Orders By Location

      {
          "aggregations" : {
              "orders-by-location": {
                  "terms" : {
                      "field" : "locationId"
                  }
              }
          }
      }
  */
  executeQuery("Orders By Location", "{\"aggregations\":{\"Location\":{\"terms\":{\"field\":\"locationId\"}}}}")

  /*
      Orders Over Time

      {
          "query" : {
              "term" : { "status" : "2" }
          },
          "aggregations" : {
              "orders-by-week" : {
                  "date_histogram" : {
                      "field" : "captureDate",
                      "interval" : "week",
                      "time_zone" : "-07:00"
                  },
                  "aggregations" : {
                      "order-amount-stats" : {
                          "stats" : {
                              "field" : "detail.amount"
                          }
                      }
                  }
              }
          }
      }
  */
  executeQuery("Orders Over Time", "{\"query\":{\"term\":{\"status\":\"2\"}},\"aggregations\":{\"Week\":{\"date_histogram\":{\"field\":\"captureDate\",\"interval\":\"week\",\"time_zone\":\"-07:00\"},\"aggregations\":{\"Order Amount\":{\"stats\":{\"field\":\"detail.amount\"}}}}}}")

  /*
      Orders Over Time by location

      {
          "query" : {
              "term" : { "status" : "2" }
          },
          "aggregations" : {
              "orders-by-week" : {
                  "date_histogram" : {
                      "field" : "captureDate",
                      "interval" : "week",
                      "time_zone" : "-07:00"
                  },
                  "aggregations" : {
                      "orders-by-location": {
                          "terms" : {
                              "field" : "locationId"
                          },
                          "aggregations" : {
                              "order-stats": {
                                  "stats" : {
                                      "field" : "detail.amount"
                                  }
                              }
                          }
                      }
                  }
              }
          }
      }
  */
  executeQuery("Orders Over Time by Location", "{\"query\":{\"term\":{\"status\":\"2\"}},\"aggregations\":{\"Week\":{\"date_histogram\":{\"field\":\"captureDate\",\"interval\":\"week\",\"time_zone\":\"-07:00\"},\"aggregations\":{\"Location\":{\"terms\":{\"field\":\"locationId\"},\"aggregations\":{\"Order Amount\":{\"stats\":{\"field\":\"detail.amount\"}}}}}}}}")

  /*
      Orders Over Time by location

      {
          "query" : {
              "term" : { "status" : "2" }
          },
          "aggregations" : {
              "Location": {
                  "terms" : {
                      "field" : "locationId"
                  },
                  "aggregations" : {
                      "Week" : {
                          "date_histogram" : {
                              "field" : "captureDate",
                              "interval" : "week",
                              "time_zone" : "-07:00"
                          },
                          "aggregations" : {
                              "order-stats": {
                                  "stats" : {
                                      "field" : "detail.amount"
                                  }
                              }
                          }
                      }
                  }
              }
          }
      }
  */
  executeQuery("Orders by Location Over Time", "{\"query\":{\"term\":{\"status\":\"2\"}},\"aggregations\":{\"Location\":{\"terms\":{\"field\":\"locationId\"},\"aggregations\":{\"Week\":{\"date_histogram\":{\"field\":\"captureDate\",\"interval\":\"week\",\"time_zone\":\"-07:00\"},\"aggregations\":{\"order-stats\":{\"stats\":{\"field\":\"detail.amount\"}}}}}}}}")

}
