package com.c12e.cortex.apps.movierecommender;

import co.cask.cdap.api.app.AbstractApplication;
import co.cask.cdap.api.data.schema.UnsupportedTypeException;
import co.cask.cdap.api.data.stream.Stream;
import co.cask.cdap.api.dataset.lib.ObjectStores;

/**
 * Application that provides movie recommendations to users.
 */
@SuppressWarnings("rawtypes")
public class MovieRecommenderApp extends AbstractApplication {

  public static final String RECOMMENDATION_SERVICE = "MovieRecommenderService";
  public static final String DICTIONARY_SERVICE = "MovieDictionaryService";
  public static final String RATINGS_STREAM = "ratingsStream";

  @Override
  public void configure() {
    setName("MovieRecommender");
    setDescription("Movie Recommendation Application");
    addStream(new Stream(RATINGS_STREAM));
    addSpark(new RecommendationBuilderSpecification());
    addService(RECOMMENDATION_SERVICE, new MovieRecommenderServiceHandler());
    addService(DICTIONARY_SERVICE, new MovieDictionaryServiceHandler());

    try {
      ObjectStores.createObjectStore(getConfigurer(), "ratings", UserScore.class);
      ObjectStores.createObjectStore(getConfigurer(), "recommendations", UserScore.class);
      ObjectStores.createObjectStore(getConfigurer(), "movies", String.class);
    } catch (UnsupportedTypeException e) {
      // This exception is thrown by ObjectStore if its parameter type cannot be
      // (de)serialized (for example, if it is an interface and not a class, then there is
      // no auto-magic way deserialize an object.) In this case that will not happen
      // because String is an actual class.
      throw new RuntimeException(e);
    }
  }
}
