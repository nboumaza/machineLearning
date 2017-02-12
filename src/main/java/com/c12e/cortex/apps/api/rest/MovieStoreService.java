package com.c12e.cortex.apps.api.rest;

import co.cask.cdap.api.annotation.UseDataSet;
import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.api.dataset.lib.ObjectStore;
import co.cask.cdap.api.service.http.AbstractHttpServiceHandler;
import co.cask.cdap.api.service.http.HttpServiceRequest;
import co.cask.cdap.api.service.http.HttpServiceResponder;
import com.google.common.base.Charsets;

import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.util.regex.Pattern;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 * 
 * Exposes HTTP API to update movie store
 */
public class MovieStoreService extends AbstractHttpServiceHandler {
  private static final Pattern NEWLINE_DELIMITER = Pattern.compile("[\\r\\n]+");
  private static final Pattern FIELDS_DELIMITER = Pattern.compile("::");
  public static final String STORE_MOVIES = "storemovies";

  @UseDataSet("movies")
  private ObjectStore<String> movies;

  @Path(STORE_MOVIES)
  @POST
  /**
   * parses and stores movie data
   * @param request HTTP request
   * @param responder HTTP response
   */
  public void uploadHandler(HttpServiceRequest request, HttpServiceResponder responder) {
    ByteBuffer requestContents = request.getContent();
    if (requestContents == null) {
      responder.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "Movies information is empty.");
      return;
    }

    String moviesData = Charsets.UTF_8.decode(requestContents).toString();

    if (parseAndStoreMovies(moviesData)) {
      responder.sendStatus(HttpURLConnection.HTTP_OK);
    } else {
      responder.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "Malformed movies information.");
    }
  }

  private boolean parseAndStoreMovies(String moviesData) {
    String[] lines = NEWLINE_DELIMITER.split(moviesData.trim());

    for (String movie : lines) {
      String[] movieInfo = FIELDS_DELIMITER.split(movie.trim());
      if (!movieInfo[0].isEmpty() && !movieInfo[1].isEmpty()) {
        movies.write(Bytes.toBytes(Integer.parseInt(movieInfo[0])), movieInfo[1]);
      } else {
        return false;
      }
    }
    return true;
  }
}
