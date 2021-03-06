package com.c12e.cortex.apps.movierecommender;

import co.cask.cdap.test.ApplicationManager;
import co.cask.cdap.test.ServiceManager;
import co.cask.cdap.test.SparkManager;
import co.cask.cdap.test.StreamManager;
import co.cask.cdap.test.TestBase;
import co.cask.cdap.test.TestConfiguration;
import co.cask.common.http.HttpRequest;
import co.cask.common.http.HttpRequests;
import co.cask.common.http.HttpResponse;

import com.c12e.cortex.apps.api.rest.MovieRecommendationService;
import com.c12e.cortex.apps.api.rest.MovieStoreService;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Test for {@link MovieRecommendationApp}
 */
public class MovieRecommendationAppTest extends TestBase {

  private static final Gson GSON = new Gson();
  private static final Logger LOG = LoggerFactory.getLogger(MovieRecommendationAppTest.class);

  @ClassRule
  public static final TestConfiguration TEST_CONFIG = new TestConfiguration("explore.enabled", false);

  @Test
  public void testRecommendation() throws Exception {

    ApplicationManager appManager = deployApplication(MovieRecommendationApp.class);

    // Send movies data through service
    sendMovieData(appManager);

    // Inject ratings data
    sendRatingsData();
    
    // Start the Spark Program
    SparkManager sparkManager = appManager.getSparkManager(SparkALSModelBuilder.class.getSimpleName()).start();
    sparkManager.waitForFinish(60, TimeUnit.SECONDS);

    verifyRecommenderService(appManager);
  }

  /**
   * Verify that the movie recommendations were generated for the users
   *
   * @param appManager {@link ApplicationManager} for the deployed application
   * @throws InterruptedException
   * @throws IOException
   */
  private void verifyRecommenderService(ApplicationManager appManager) throws InterruptedException, IOException {
    ServiceManager serviceManager = appManager.getServiceManager(MovieRecommendationApp.RECOMMENDATION_SERVICE).start();
    serviceManager.waitForStatus(true);

    // Verify that recommendation are generated
    String response =
      doGet(new URL(serviceManager.getServiceURL(), MovieRecommendationService.RECOMMEND + "/1"));

    Map<String, String[]> responseMap =
      GSON.fromJson(response, new TypeToken<Map<String, String[]>>() { }.getType());

    Assert.assertTrue(responseMap.containsKey("rated") && responseMap.get("rated").length > 0);
    Assert.assertTrue(responseMap.containsKey("recommended") && responseMap.get("recommended").length > 0);
  }

  private String doGet(URL url) throws IOException {
    HttpResponse response = HttpRequests.execute(HttpRequest.get(url).build());
    Assert.assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
    return response.getResponseBodyAsString();
  }

  /**
   * Sends raw movies data to {@link MovieRecommendationApp#MOVIE_STORE_SERVICE}
   */
  private void sendMovieData(ApplicationManager applicationManager) throws Exception {
    String moviesData = "0::Movie0\n1::Movie1\n2::Movie2\n3::Movie3\n";
    ServiceManager serviceManager =
      applicationManager.getServiceManager(MovieRecommendationApp.MOVIE_STORE_SERVICE).start();
    serviceManager.waitForStatus(true);

    URL url = new URL(serviceManager.getServiceURL(), MovieStoreService.STORE_MOVIES);
    HttpRequest request = HttpRequest.post(url).withBody(moviesData, Charsets.UTF_8).build();
    HttpResponse response = HttpRequests.execute(request);
    Assert.assertEquals(200, response.getResponseCode());
    LOG.debug("Sent movies data");
  }

  /**
   * Send some raw ratings to the Stream
   */
  private void sendRatingsData() throws IOException {
    StreamManager streamManager = getStreamManager(MovieRecommendationApp.RATINGS_STREAM);
    streamManager.send("0::0::3");
    streamManager.send("0::1::4");
    streamManager.send("1::1::4");
    streamManager.send("1::2::4");
    LOG.debug("Sent ratings data");
  }
}
