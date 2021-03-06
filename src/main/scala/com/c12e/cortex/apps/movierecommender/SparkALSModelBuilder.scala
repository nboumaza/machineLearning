package com.c12e.cortex.apps.movierecommender

import co.cask.cdap.api.common.Bytes
import co.cask.cdap.api.spark.{SparkExecutionContext, SparkMain}
import org.apache.spark.SparkContext
import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.apache.spark.rdd.RDD
import org.slf4j.{Logger, LoggerFactory}
import com.c12e.cortex.apps.domain.UserMovieRating;

import scala.util.control.Exception._

/**
 * Spark Program which makes recommendation for movies to users
 */
class SparkALSModelBuilder extends SparkMain {
  import SparkALSModelBuilder._

  case class Params(
                     numIterations: Int = 10,
                     lambda: Double = 1.0,
                     rank: Int = 10,
                     numUserBlocks: Int = -1,
                     numProductBlocks: Int = -1,
                     implicitPrefs: Boolean = false)

  override def run(implicit sec: SparkExecutionContext) {
    val sc = new SparkContext
    LOG.info("Running with arguments {}", sec.getRuntimeArguments.get("args"))
    val params = parseArguments(sec, Params())
    LOG.info("Processing ratings data with parameters {}", params)

    val userMovieRatings: RDD[String] = sc.fromStream("ratingsStream")

    val umrRDD = userMovieRatings.map { e =>
      val userMovieRating = e.split("::")
      new UserMovieRating(userMovieRating(0).toInt, userMovieRating(1).toInt, userMovieRating(2).toInt)
    }.cache()
    val scores = umrRDD.collect()

    val ratingData = userMovieRatings.map { curUserMovieRating =>
      val userMovieRating = curUserMovieRating.toString.split("::")
      if (params.implicitPrefs) {
        /*
        * MovieLens ratings are on a scale of 1-5:
        * To map ratings to confidence scores, we subtract 2.5
        * 5 -> 2.5
        */
        Rating(userMovieRating(0).toInt, userMovieRating(1).toInt, userMovieRating(2).toDouble - 2.5)
      } else {
        Rating(userMovieRating(0).toInt, userMovieRating(1).toInt, userMovieRating(2).toDouble)
      }
    }.cache()

    val parallelizedScores = sc.parallelize(scores)

    val scoresRDD = parallelizedScores
      .keyBy(x => Bytes.add(Bytes.toBytes(x.getUserID), Bytes.toBytes(x.getMovieID)))
      .saveAsDataset("ratings")

    val moviesDataset: RDD[(Array[Byte], String)] = sc.fromDataset("movies")

    val numRatings = ratingData.count()
    val numUsers = ratingData.map(_.user).distinct().count()
    val numRatedMovies = ratingData.map(_.product).distinct().count()

    val numMovies = moviesDataset.count()

    LOG.info(s"Got $numRatings ratings from $numUsers users on $numRatedMovies movies out of $numMovies")


    LOG.info("Calculating model")

    val model = new ALS()
      .setRank(params.rank)
      .setIterations(params.numIterations)
      .setLambda(params.lambda)
      .setImplicitPrefs(params.implicitPrefs)
      .setUserBlocks(params.numUserBlocks)
      .setProductBlocks(params.numProductBlocks)
      .run(ratingData)

    LOG.debug("Creating predictions")

    val userRatedMovies = ratingData.map(x => (x.user, x.product)).groupByKey().map(x => (x._1, x._2.toSet))

    val movies = moviesDataset.map(x => (Bytes.toInt(x._1), x._2)).collect().toMap
    val notRatedMovies = userRatedMovies.map(x => (x._1, movies.keys.filter(!x._2.contains(_)).toSeq)).collect()

    for (curUser <- notRatedMovies) {
      val nr = sc.parallelize(curUser._2)
      sc.parallelize(model.predict(nr.map((curUser._1, _)))
        .collect().sortBy(-_.rating).take(20))
        .keyBy(x => Bytes.add(Bytes.toBytes(x.user), Bytes.toBytes(x.product)))
        .map(x => (x._1, new UserMovieRating(x._2.user, x._2.product, x._2.rating.toInt)))
        .saveAsDataset("recommendations")
    }

    LOG.debug("Stored predictions in dataset.")
  }

  /** Parse runtime arguments - use defaults if none are specified **/
  def parseArguments(sec: SparkExecutionContext, defaultParams: Params): Params = {
    val arguments: String = sec.getRuntimeArguments.get("args")
    val args: Array[String] = if (arguments == null) Array() else arguments.split("\\s")


    val numIterations = getInt(args, 0).getOrElse(defaultParams.numIterations)
    val lambda = getDouble(args, 1).getOrElse(defaultParams.lambda)
    val rank = getInt(args, 2).getOrElse(defaultParams.rank)
    val numUserBlocks = getInt(args, 3).getOrElse(defaultParams.numUserBlocks)
    val numProductBlocks = getInt(args, 4).getOrElse(defaultParams.numProductBlocks)
    val implicitPrefs = getBoolean(args, 5).getOrElse(defaultParams.implicitPrefs)

    Params(numIterations, lambda, rank, numUserBlocks, numProductBlocks, implicitPrefs)
  }

  def getInt(args: Array[String], idx: Int): Option[Int] = catching(classOf[Exception]).opt(args(idx).toInt)

  def getDouble(args: Array[String], idx: Int): Option[Double] = catching(classOf[Exception]).opt(args(idx).toDouble)

  def getBoolean(args: Array[String], idx: Int): Option[Boolean] = catching(classOf[Exception]).opt(args(idx).toBoolean)
}

/**
 * companion object
 */
object SparkALSModelBuilder {
  val LOG: Logger = LoggerFactory.getLogger(classOf[SparkALSModelBuilder])
}
