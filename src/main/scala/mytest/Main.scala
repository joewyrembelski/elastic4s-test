package mytest

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.common.settings.ImmutableSettings
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.collection.JavaConversions._

object Main extends App {

//  val client = ElasticClient.local
 val settings = ImmutableSettings.settingsBuilder().put("cluster.name", "elasticsearch_jtp").build()
 val client = ElasticClient.remote(settings, ("localhost", 9300))
 // val client = ElasticClient.remote("localhost", 9300)

  // client.sync.execute {
  //   index into "music/bands" fields(
  //     "name" -> "coldplay",
  //     "singer" -> "chris martin",
  //     "drummer" -> "will champion",
  //     "guitar" -> "johnny buckland"
  //     )
  // }
  // client.sync.execute {
  //   index into "music/artists" fields(
  //     "name" -> "kate bush",
  //     "singer" -> "kate bush"
  //     )
  // }
  // client.sync.execute {
  //   index into "music/bands" fields(
  //     "name" -> "jethro tull",
  //     "singer" -> "ian anderson",
  //     "guitar" -> "martin barre",
  //     "keyboards" -> "johnny smith"
  //     ) id 45
  // }

  // println("done indexing...")

  val q = """{ "query" : { "match": { "drummer" : "will champion" } } }"""

  // both of these work, but the first has all documents in the query result
  val q_agg = """{ "aggs": { "bucket_o_fish": { "terms": { "field" : "eventType" } } } }"""
//  val q_agg = """{ "filter" : { "type": { "value": "event" } }, "aggs": { "bucket": { "terms": { "field" : "eventType" } } } }"""

  val idx = "music"
  val jResp = client.java.prepareSearch(idx).setSource(q).execute().actionGet()
  println(s"java response 1: $jResp")

  val jResp_aggs = client.java.prepareSearch("gesto").setSource(q_agg).execute().actionGet()
  println(s"java response 2: $jResp_aggs")
  val aggs = jResp_aggs.getAggregations.asMap()
  val keys = aggs.keySet()
  println(s" response aggregation keys: $keys")
  aggs.foreach({ case (key, value) =>
    println(s"key: $key  value_class: ${value.getClass}")
    value match {
      case st: org.elasticsearch.search.aggregations.bucket.terms.StringTerms => println("ooh, StringTerms")
      case _ => println("hmm, something else")
    }
    value match {
      case mb: org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation =>
        println("ooh multi bucket aggregation")
        val buckets = mb.getBuckets
        println(s" there are ${buckets.size()} buckets")
        buckets.foreach(b => println(s"  bucket ${b.getKey} contains ${b.getDocCount} documents and ${b.getAggregations.asList().size()} sub buckets"))
      case _ => println("hmm, not a multi bucket aggregation...")
    }
  })

  // println("done with refresh, now searching...")
//  val srch1 = search in "music" -> "bands" query matches ("drummer", "will champion")
//
//  val resp = Await.result(client.execute(srch1), 10.seconds)
//
//  println(s"response: $resp")

  println("Done")

}
