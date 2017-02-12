package com.c12e.cortex.apps.domain;

import java.io.Serializable;

/**
 * Represents user's ratings for different movies
 */
public class UserMovieRating implements Serializable {

  private static final long serialVersionUID = -1258765752695193629L;

  private final int userID;
  private final int movieID;
  private final int rating;

  public UserMovieRating(int userID, int movieID, int rating) {
    this.userID = userID;
    this.movieID = movieID;
    this.rating = rating;
  }

  public int getUserID() {
    return userID;
  }

  public int getMovieID() {
    return movieID;
  }

  public int getRating() {
    return rating;
  }
}
