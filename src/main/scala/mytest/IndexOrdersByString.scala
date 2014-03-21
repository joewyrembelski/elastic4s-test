package mytest

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.DocumentSource
import java.util.UUID


/**
 * Uses a String to describe an order
 */
object IndexOrdersByString {

  def indexOrders(indexingMetadata: IndexingMetadata, world: GestoWorld, range: Seq[DateTime]) = {
    range.foreach(p => {
      indexingMetadata.client.sync.execute {
        index into indexingMetadata.indexName -> "order" doc createDocMap(world, p)
      }
    })
  }

  def createDocMap(world: GestoWorld, p: DateTime) = {
    val dtf = ISODateTimeFormat.dateTime()
    val orderJson =
      s"""
        | {
        |   "id":              "${UUID.randomUUID()}",
        |   "memberId":        "${world.memberIds(DataGenerator.generateDataValue(0, world.memberIds.size))}",
        |   "referenceNumber": "${UUID.randomUUID()}",
        |   "captureDate":     "${p.toString(dtf)}",
        |   "locationId":      "${world.locationIds(DataGenerator.generateDataValue(0, world.locationIds.size))}",
        |   "status":           ${DataGenerator.generateDataValue(0, 4)},
        |   "detail": {
        |     "amount":   ${DataGenerator.generateDoubleDataValue(5, 49)},
        |     "detail":   { "itemname": "sandwich" }
        |   }
        | }
      """.stripMargin

    class OrderDocumentSource(orderJson: String) extends DocumentSource {
      def json = orderJson
    }

    new OrderDocumentSource(orderJson)
  }

}
