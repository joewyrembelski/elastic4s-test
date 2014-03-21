package mytest

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.common.settings.ImmutableSettings
import scala.collection.JavaConversions._
import org.joda.time.DateTime
import java.util.UUID
import org.joda.time.format.ISODateTimeFormat


class IndexingMetadata(val indexName: String, val client: ElasticClient)
class GestoWorld(val memberIds: Seq[UUID], val locationIds: Seq[UUID])

object Main extends App {

  val settings = ImmutableSettings.settingsBuilder().put("cluster.name", "gesto_es").build()
  val client   = ElasticClient.remote(settings, ("localhost", 9300))

  val indexingMetadata = new IndexingMetadata("gesto", client)
  val world = new GestoWorld(DataGenerator.randomIdSet(), DataGenerator.randomIdSet())

  // Clear the index
  client.sync.execute { deleteIndex(indexingMetadata.indexName) }

  // Execute scenarios
  val rangeEnd     = new DateTime()

  // TODO: Docs suggest we can use the fields but it doesn't seem to work with the nested objects
  //       https://github.com/sksamuel/elastic4s/blob/master/guide/index.md
  //IndexOrdersWithFields.indexOrders(indexingMetadata, world, DataGenerator.dateRange(rangeEnd.minusMonths(3), rangeEnd, "random"))

  1 to 10 foreach ( i =>
    IndexOrdersByString.indexOrders(indexingMetadata, world, DataGenerator.dateRange(rangeEnd.minusMonths(6), rangeEnd, "random"))
  )
}
