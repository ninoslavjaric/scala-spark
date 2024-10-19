package org.nino

import org.apache.spark.sql.{DataFrame, SparkSession, Dataset}
import org.schema.PlayerStat
import org.apache.spark.sql.functions.{col, struct, sum, count}

object MultiHeaderCSVParser {
    def main(args: Array[String]): Unit = {
        val spark = SparkSession.builder
            .appName("Multi-Header CSV Parsing")
            .master("local[*]")
            .getOrCreate()

        val rawDf = spark.read
            .option("header", "false") // Read without treating first line as header
            .csv("stats.csv")
        
        rawDf.createOrReplaceTempView("player_stats")


        import spark.implicits._
        val subRawDf: DataFrame = spark.sql("select * from player_stats offset 3")
            .withColumnRenamed("_c0", "league")
            .withColumnRenamed("_c1", "season")
            .withColumnRenamed("_c2", "game")
            .withColumnRenamed("_c3", "team")
            .withColumnRenamed("_c4", "player")
            .withColumnRenamed("_c5", "jersey_number")
            .withColumnRenamed("_c6", "nation")
            .withColumnRenamed("_c7", "pos")
            .withColumnRenamed("_c8", "age")
            .withColumnRenamed("_c9", "min")
            .withColumnRenamed("_c10", "performance_Gls")
            .withColumnRenamed("_c11", "performance_Ast")
            .withColumnRenamed("_c12", "performance_PK")
            .withColumnRenamed("_c13", "performance_PKatt")
            .withColumnRenamed("_c14", "performance_Sh")
            .withColumnRenamed("_c15", "performance_SoT")
            .withColumnRenamed("_c16", "performance_CrdY")
            .withColumnRenamed("_c17", "performance_CrdR")
            .withColumnRenamed("_c18", "performance_Touches")
            .withColumnRenamed("_c19", "performance_Tkl")
            .withColumnRenamed("_c20", "performance_Int")
            .withColumnRenamed("_c21", "performance_Blocks")
            .withColumnRenamed("_c22", "expectation_xG")
            .withColumnRenamed("_c23", "expectation_npxG")
            .withColumnRenamed("_c24", "expectation_xAG")
            .withColumnRenamed("_c25", "sca_SCA")
            .withColumnRenamed("_c26", "sca_GCA")
            .withColumnRenamed("_c27", "passes_Cmp")
            .withColumnRenamed("_c28", "passes_Att")
            .withColumnRenamed("_c29", "passes_Cmp%")
            .withColumnRenamed("_c30", "passes_PrgP")
            .withColumnRenamed("_c31", "carries_Carries")
            .withColumnRenamed("_c32", "carries_PrgC")
            .withColumnRenamed("_c33", "take_ons_Att")
            .withColumnRenamed("_c34", "take_ons_Succ")
            .withColumnRenamed("_c35", "game_id")
            

        val ds: Dataset[PlayerStat] = subRawDf.select(
            col("league"),
            col("season"),
            col("player"),
            struct(
                col("performance_Gls").cast("int").alias("goals"),
                col("performance_Ast").cast("int").alias("assists"),
                col("performance_Touches").cast("int").alias("touches"),
                col("performance_Tkl").cast("int").alias("tackles"),
                col("performance_Int").cast("int").alias("intersections"),
                col("performance_Blocks").cast("int").alias("blocks")
            ).alias("performance"),
            struct(
                col("sca_SCA").cast("int").alias("subsequentContributionAction"),
                col("sca_GCA").cast("int").alias("goalContributionAction")
            ).alias("sca"),
            col("game_id").alias("game")
        ).as[PlayerStat]
        // .orderBy(col("player").asc)
        // .orderBy(col("season").asc)
        // .orderBy(col("performance.tackles").desc)
        // .orderBy(col("performance.intersections").desc)
        // .orderBy(col("performance.blocks").desc)

        val da = ds.groupBy(col("player"), col("season")).agg(
            count("*").alias("playsCounter"),
            sum("performance.tackles").alias("tackles"),
            sum("performance.intersections").alias("intersections"),
            sum("performance.blocks").alias("blocks"),
        )
        .orderBy(col("player").desc, col("season").asc)
        
        da.show()
    }

}