package com.c12e.cortex.apps.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Collect rated and recommended movies.
 */
public class Recommendation implements Serializable {
  private static final long serialVersionUID = -1258787639695193629L;

  private List<String> rated = new ArrayList<String>();
  private List<String> recommended = new ArrayList<String>();

  public List<String> getRated() {
    return rated;
  }

  public List<String> getRecommended() {
    return recommended;
  }

  public void addRated(String movie) {
    rated.add(movie);
  }

  public void addRecommended(String movie) {
    recommended.add(movie);
  }
}
