package com.pipeline

import org.apache.spark.sql.{SparkSession, Dataset, functions => F}
import com.pipeline.models._ 
import java.sql.Timestamp
import java.time.Instant

object SilverTransformer{

  def main(args: Array[String]): Unit = {
    // 1. Unitialize spark session 
    val spark = SparkSession.builder().appName("SilverTransformer").master("local[*]").getOrCreate()

    import spark.implicits._ 

    // Read Raw Data (bronze Layer) 
    val rawPath = "data/bronze/transactions.json"
    val rawDs: Dataset[RawTransaction] = spark.read.json(rawPath).as[RawTransaction]

    // ADD THESE TWO LINES FOR DEBUGGING:
    //println(s"📥 Total Raw Records Found: ${rawDs.count()}")
    //rawDs.show(5)

    // Transformation Logic 
    val taxRate = 0.15

    val silverDs: Dataset[CleanTransaction] = rawDs
      .filter(_.amount > 0) // Data Quality Check: filter out garbage data
      .map { r =>
        CleanTransaction(
          transactionId = r.id.toUpperCase,
          totalAmount = r.amount + (r.amount * taxRate),
          tax = r.amount * taxRate,
          processAt = Timestamp.from(Instant.now())
        )
      }

    // Add partition columns (year + month) before writing
    val silverWithPartition = silverDs.toDF()
      .withColumn("year",  F.year(F.col("processAt")))
      .withColumn("month", F.month(F.col("processAt")))

    // Write Silver Layer (Parquete format for performance)
    silverWithPartition.write
      .mode("overwrite")
      .partitionBy("year", "month")
      .parquet("data/silver/transaction_cleaned")

    println("Pipeline task complete successfully")
    spark.stop()
  }
}
