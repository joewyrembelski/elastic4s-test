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
  println(s"Starting with System Time: '${new DateTime()}'")
  val ProgramArgument = """([a-zA-Z]+)=(.+)""".r
  var mode            = "not-specified"
  var query           = ""
  var orderDate       = new DateTime()

  args.map(a => {
    println(s"Argument: '$a'")
    a match {
      case ProgramArgument(k, v) =>
        k match {
          case "mode"      => mode = v
          case "query"     => query = v
          case "orderDate" => orderDate = new DateTime(v)
        }
    }
  })

  println("Starting with options: ")
  println("\tMode: " + mode)
  mode match {
    case "single" => println("\tOrder Date: " + orderDate)
    case "query"  => println("\tQuery: " + query)
    case "reset"  => Unit
    case "multi"  => Unit
  }

  val settings = ImmutableSettings.settingsBuilder().put("cluster.name", "gesto_es").build()
  val client   = ElasticClient.remote(settings, ("localhost", 9300))

  val indexingMetadata = new IndexingMetadata("gesto", client)
  val world = new GestoWorld(DataGenerator.randomIdSet(), DataGenerator.randomIdSet())

  mode match {
    case "reset" =>
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

    case "single" =>
      println("\n\nIndexing orders")
      IndexOrdersByString.indexOrder(indexingMetadata, world, orderDate)

    case "multi" =>
      def numMonths = args(0).toInt
      def numPasses = args(1).toInt
      val rangeEnd = new DateTime()

      println("\n\nIndexing orders")
      1 to numPasses foreach (i =>
        IndexOrdersByString.indexOrders(indexingMetadata, world, DataGenerator.dateRange(rangeEnd.minusMonths(numMonths), rangeEnd, "random"))
      )

    case "query" =>
      executeQuery(
        ChartMetadata(UUID.randomUUID(), "line", "Some Chart Name",
          ChartQuery(UUID.randomUUID(), "elasticsearch", query)
        ), None
      )
  }

  def executeQuery(chartMetadata: ChartMetadata, options: Option[String]) = {
    println(s"\n\nExecuting: ${chartMetadata.title} -> ${chartMetadata.query}")
    val data = OrderQueryService.executeQuery(indexingMetadata, chartMetadata)
    val chart = Chart(chartMetadata, options, data)
    println(chart)
  }

}
