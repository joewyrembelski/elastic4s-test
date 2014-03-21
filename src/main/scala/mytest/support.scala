package mytest

import scala.math._
import scala.util.Random
import org.joda.time.{DateTime, Period}
import java.util.UUID


object DataGenerator {

  val rng = new Random(new DateTime().getMillis)

  def periodKeyToObject(periodKey: String) =
    periodKey match {
      case "minute" => Period.minutes(1)
      case "hour"   => Period.hours(1)
      case "day"    => Period.days(1)
      case "week"   => Period.weeks(1)
      case "month"  => Period.months(1)
      case "random" => Period.seconds(generateDataValue(1, 600))
    }

  def dateRange(from: DateTime, to: DateTime, period: String) =
    Iterator.iterate(from)(_.plus(periodKeyToObject(period))).takeWhile(!_.isAfter(to)).toSeq

  def generateDataValue(min: Int, max: Int) =
    abs(rng.nextInt % max) + min

  def generateDoubleDataValue(min: Int, max: Int) =
    generateDataValue(min, max) + Math.round(abs(rng.nextDouble()) * 100.0) / 100.0

  def randomIdSet() =
    (0 to generateDataValue(1, 10)).map(
      r => UUID.randomUUID
    ).toSeq

}
