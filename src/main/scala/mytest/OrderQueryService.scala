package mytest

import scala.collection.JavaConversions._
import org.elasticsearch.search.aggregations.Aggregation
import org.elasticsearch.search.aggregations.bucket.{SingleBucketAggregation, MultiBucketsAggregation}
import org.elasticsearch.search.aggregations.bucket.histogram.{DateHistogram, Histogram, InternalHistogram}
import org.elasticsearch.search.aggregations.bucket.terms.{Terms, StringTerms}
import org.elasticsearch.search.aggregations.metrics.stats.{Stats, InternalStats}


/**
 * Provides query services for Orders
 */
object OrderQueryService {

  def executeQuery(indexingMetadata: IndexingMetadata, chartMetadata: ChartMetadata): Seq[MultiNumberDataSeries] = {
    val esResponse = indexingMetadata.client.java.prepareSearch(indexingMetadata.indexName).setSource(chartMetadata.query.query).execute().actionGet()
    esResponse.getAggregations.foreach(aggregation =>
      learnAboutAnAggregation(s"${aggregation.getName}", aggregation)
    )
    esResponse.getAggregations.toSeq.flatMap(aggregation => processAggregation(aggregation))
  }

  def processAggregation(aggregation: Aggregation): Seq[MultiNumberDataSeries] = {
    aggregation match {
      case d: DateHistogram  => d.getBuckets.map(b => MultiNumberDataSeries(b.getKey, Seq(Seq(b.getDocCount)))).toSeq
      case h: Histogram      => h.getBuckets.map(b => MultiNumberDataSeries(b.getKey, Seq(Seq(b.getDocCount)))).toSeq
      case t: Terms          => t.getBuckets.map(b => MultiNumberDataSeries(b.getKey, Seq(Seq(b.getDocCount)))).toSeq
      case _                 => {println(s"Yikes! processAggregation for '${aggregation.getName}' couldn't process due ot an unknown Aggregation Type: ${aggregation.getClass}"); Seq(MultiNumberDataSeries("Yikes", Seq()))}
    }
  }

  def processSingleBucketAggregation(aggregation: SingleBucketAggregation) = {
  }

  def processMultiBucketAggregation(aggregation: MultiBucketsAggregation) = {
  }


  /*
    Debugging Methods
  */

  def learnAboutAnAggregation(outputPrefix: String, aggregation: Aggregation): Unit = {
    val myOutputPrefix = s"$outputPrefix (${DataGenerator.rng.nextInt}}) -> ${aggregation.getName}"
    println(s"$myOutputPrefix: aggregation class = ${aggregation.getClass}")
    aggregation match {
      case singleBucketAggregation: SingleBucketAggregation  => learnAboutSingleBucketAggregation(myOutputPrefix, singleBucketAggregation)
      case multiBucketsAggregation: MultiBucketsAggregation  => learnAboutMultiBucketAggregation(myOutputPrefix, multiBucketsAggregation)
      case statsAggregation: Stats                           => Nil // println(s"$myOutputPrefix: Stats '${statsAggregation.getName}'")
      case _                                                 => println(s"$myOutputPrefix: learnAboutAnAggregation Can't Unknown Aggregation Type: ${aggregation.getClass}")
    }
  }

  def learnAboutSingleBucketAggregation(outputPrefix: String, aggregation: SingleBucketAggregation) = {
    val myOutputPrefix = s"$outputPrefix (${DataGenerator.rng.nextInt}})"
    println(s"$myOutputPrefix: contains ${aggregation.getDocCount} documents and ${aggregation.getAggregations.asList.size} sub aggregations")
    aggregation.getAggregations.foreach(subAggregation => learnAboutAnAggregation(myOutputPrefix, subAggregation))
  }

  def learnAboutMultiBucketAggregation(outputPrefix: String, aggregation: MultiBucketsAggregation) = {
    val myOutputPrefix = s"$outputPrefix (${DataGenerator.rng.nextInt}})"
    println(s"$myOutputPrefix: contains ${aggregation.getBuckets.size()} buckets")
    aggregation.getBuckets.foreach(bucket => {
      //println(s"$myOutputPrefix: bucket '${bucket.getKey}' (${bucket.getClass}) contains ${bucket.getDocCount} documents and ${bucket.getAggregations.asList.size} sub aggregations")
      bucket.getAggregations.foreach(subAggregation => learnAboutAnAggregation(myOutputPrefix, subAggregation))
    })
  }

}
