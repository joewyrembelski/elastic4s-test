package mytest

import java.util.UUID


case class MultiNumberDataSeries(label: String, data: Seq[Seq[AnyVal]])

case class ChartQuery(id: UUID, executor: String, query: String)

case class ChartMetadata(id: UUID, chartType: String, title: String, query: ChartQuery)

case class Chart(metadata: ChartMetadata, options: Option[String], series: Seq[MultiNumberDataSeries])

