package com.c12e.cortex.apps.movierecommender;

import co.cask.cdap.api.Resources;
import co.cask.cdap.api.spark.AbstractSpark;
import co.cask.cdap.api.spark.Spark;
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel;
import org.apache.spark.mllib.recommendation.Rating;
import org.apache.spark.rdd.RDD;

import com.c12e.cortex.apps.domain.UserMovieRating;

/**
 * A Spark program that creates a MatrixFactorizationModel from UserMovieRating and recommend movies
 * to user by calculating UserRating through MatrixFactorizationModel.predict(RDD)
 */
public class SparkALSModelBuilderSpec extends AbstractSpark {
	
   private static final int DEFAULT_VIRTUAL_CORES = 1;
   private static final int DEFAULT_MEMORY_MB = 1024;
  
  @Override
  public void configure() {
    setName("SparkALSModelBuilder");
    setDescription("Spark ml job that computes movie recommendations");
    //injected Spark ML implementation 
    setMainClass(SparkALSModelBuilder.class);
    //constructs a Resources instance that represents 1 virtual core and 512MB of memory.
    setDriverResources(new Resources(DEFAULT_MEMORY_MB, DEFAULT_VIRTUAL_CORES));
    setExecutorResources(new Resources(DEFAULT_MEMORY_MB, DEFAULT_VIRTUAL_CORES));
  }
}
