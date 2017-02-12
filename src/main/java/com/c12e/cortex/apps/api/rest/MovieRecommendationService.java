package com.c12e.cortex.apps.api.rest;

import co.cask.cdap.api.annotation.UseDataSet;
import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.api.dataset.lib.CloseableIterator;
import co.cask.cdap.api.dataset.lib.KeyValue;
import co.cask.cdap.api.dataset.lib.ObjectStore;
import co.cask.cdap.api.service.http.AbstractHttpServiceHandler;
import co.cask.cdap.api.service.http.HttpServiceRequest;
import co.cask.cdap.api.service.http.HttpServiceResponder;

import java.net.HttpURLConnection;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.c12e.cortex.apps.domain.Recommendation;
import com.c12e.cortex.apps.domain.UserMovieRating;

/**
 * Exposes HTTP API to retrieve recommended movies.
 * 
 */
public class MovieRecommendationService extends AbstractHttpServiceHandler {
  public static final String RECOMMEND = "recommend";
  
  @UseDataSet("recommendations")
  private ObjectStore<UserMovieRating> recommendations;

  @UseDataSet("ratings")
  private ObjectStore<UserMovieRating> ratings;

  @UseDataSet("movies")
  private ObjectStore<String> movies;

  @Path("/" + RECOMMEND + "/{userId}")
  @GET
  public void recommend(HttpServiceRequest request, HttpServiceResponder responder, @PathParam("userId") int userId) {
    byte[] userID = Bytes.toBytes(userId);

    CloseableIterator<KeyValue<byte[], UserMovieRating>> userRatings =
      ratings.scan(userID, Bytes.stopKeyForPrefix(userID));
    try {
      if (!userRatings.hasNext()) {
        responder.sendError(HttpURLConnection.HTTP_NOT_FOUND,
                            String.format("No ratings found for user %s.", userId));
        return;
      }

      CloseableIterator<KeyValue<byte[], UserMovieRating>> userPredictions =
        recommendations.scan(userID, Bytes.stopKeyForPrefix(userID));
      try {
        if (!userPredictions.hasNext()) {
          responder.sendError(HttpURLConnection.HTTP_NOT_FOUND,
                              String.format("No recommendations found for user %s.", userId));
          return;
        }

        responder.sendJson(getRecommendation(movies, userRatings, userPredictions));
      } finally {
        userPredictions.close();
      }
    } finally {
      userRatings.close();
    }
  }

  /**
   * Prepares a response of watched and recommended movies in the following format:
   * {"rated":["ratedMovie1","ratedMovie2"],"recommended":["recommendedMovie1","recommendedMovie2"]}
   *
   * @param store dataset with stored movies
   * @param userRatings user given rating to movies
   * @param userPredictions movie recommendation to user with predicted rating
   *
   * @return Recommendation of watched and recommended movies
   */
  private Recommendation getRecommendation(ObjectStore<String> store,
                                           CloseableIterator<KeyValue<byte[], UserMovieRating>> userRatings,
                                           CloseableIterator<KeyValue<byte[], UserMovieRating>> userPredictions) {
    Recommendation recommendations = new Recommendation();

    while (userRatings.hasNext()) {
      recommendations.addRated(store.read(Bytes.toBytes(userRatings.next().getValue().getMovieID())));
    }

    while (userPredictions.hasNext()) {
      recommendations.addRecommended(store.read(Bytes.toBytes(userPredictions.next().getValue().getMovieID())));
    }

    return recommendations;
  }
}
