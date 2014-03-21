package mytest

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import com.sksamuel.elastic4s.ElasticDsl._
import java.util.UUID


/**
 * Uses the DSL to describe an order
 */
object IndexOrdersWithFields {

  def indexOrders(indexingMetadata: IndexingMetadata, world: GestoWorld, range: Seq[DateTime]) = {
    val dtf = ISODateTimeFormat.dateTime()

    range.foreach(p => {
      indexingMetadata.client.sync.execute {
        index into indexingMetadata.indexName -> "order" fields(
          "id"              -> UUID.randomUUID(),
          "memberId"        -> world.memberIds(DataGenerator.generateDataValue(0, world.memberIds.size)),
          "referenceNumber" -> UUID.randomUUID(),
          "captureDate"     -> p.toString(dtf),
          "locationId"      -> world.locationIds(DataGenerator.generateDataValue(0, world.locationIds.size)),
          "status"          -> DataGenerator.generateDataValue(0, 2),
          "detail"    -> Map(
            "amount"  -> DataGenerator.generateDataValue(1, 15),
            "detail"  -> "{ \"itemname\" : \"drink\" }"
          )
        )
      }
    })
  }

}
