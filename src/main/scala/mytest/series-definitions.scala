package mytest


case class MultiNumberDataSeries(label: String, data: Seq[Seq[AnyVal]])

case class MultiLongDataSeries(label: String, data: Seq[Seq[Long]])

case class MultiDecimalDataSeries(label: String, data: Seq[(Long, BigDecimal)])

