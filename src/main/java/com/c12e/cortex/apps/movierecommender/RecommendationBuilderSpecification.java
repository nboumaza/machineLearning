package com.c12e.cortex.apps.movierecommender;

import co.cask.cdap.api.Resources;
import co.cask.cdap.api.spark.AbstractSpark;
import co.cask.cdap.api.spark.Spark;
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel;
import org.apache.spark.mllib.recommendation.Rating;
import org.apache.spark.rdd.RDD;

/**
 * A {@link Spark} program that creates a {@link MatrixFactorizationModel} from {@link UserScore} and recommend movies
 * to user by calculating {@link Rating} through {@link MatrixFactorizationModel#predict(RDD)}
 */
public class RecommendationBuilderSpecification extends AbstractSpark {
  @Override
  public void configure() {
    setName("RecommendationBuilder");
    setDescription("Spark program that computes movie recommendations.");
    setMainClass(RecommendationBuilder.class);
    setDriverResources(new Resources(1024));
    setExecutorResources(new Resources(1024));
  }
}
