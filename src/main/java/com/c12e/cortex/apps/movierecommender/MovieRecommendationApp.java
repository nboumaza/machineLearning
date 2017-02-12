package com.c12e.cortex.apps.movierecommender;

import co.cask.cdap.api.app.AbstractApplication;
import co.cask.cdap.api.data.schema.UnsupportedTypeException;
import co.cask.cdap.api.data.stream.Stream;
import co.cask.cdap.api.dataset.lib.ObjectStores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.c12e.cortex.apps.api.rest.MovieRecommendationService;
import com.c12e.cortex.apps.api.rest.MovieStoreService;
import com.c12e.cortex.apps.domain.UserMovieRating;

@SuppressWarnings("rawtypes")
/**
 * Movie Recommendation Application
 */
public class MovieRecommendationApp extends AbstractApplication {

  private static final Logger LOG = LoggerFactory.getLogger(MovieRecommendationApp.class);
  public static final String RECOMMENDATION_SERVICE = "MovieRecommendationService";
  public static final String MOVIE_STORE_SERVICE = "MovieStoreService";
  public static final String RATINGS_STREAM = "ratingsStream";

  @Override
  public void configure() {
    //app name and description
    setName("MovieRecommender");
    setDescription("Movie Recommendation Application");
    
    //stream to ingest ratings data
    addStream(new Stream(RATINGS_STREAM));
    
    //spark for processing ratings data
    addSpark(new SparkALSModelBuilderSpec());
    
    //movie recommendation service
    addService(RECOMMENDATION_SERVICE, new MovieRecommendationService());
    
    //movie store service
    addService(MOVIE_STORE_SERVICE, new MovieStoreService());

    //persistence stores
    try {
      ObjectStores.createObjectStore(getConfigurer(), "ratings", UserMovieRating.class);
      ObjectStores.createObjectStore(getConfigurer(), "recommendations", UserMovieRating.class);
      ObjectStores.createObjectStore(getConfigurer(), "movies", String.class);
    } catch (UnsupportedTypeException e) {
    	LOG.error(e.getMessage());
        throw new RuntimeException(e);
    }
  }
}
