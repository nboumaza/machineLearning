
# Application Summary
Collaborative filtering sample application that leverages Apache Spark MLlib library.
* The recommendation engine is based on ALS (Alternating Least Square).
* The ``ratings`` and ``movies`` non curated data can be fetched from: `MovieLens Dataset <http://grouplens.org/datasets/movielens/>`

* movies data row structure: ``<movieId>::<title>::<genre1>[|<genre2>...]``
* user/movies rating data row structure: ``<userId>::<movieId>::<rating>::<timestamp>``

# Solution Overview
The following diagram describes the main components of the MovieRecommender:

![Alt text](resources/img/MovieRecommender.png?raw=true "UML Diagram")

* A ``ratingsStream`` for ingesting movie ``ratings`` data 
* A ``MovieStoreService`` to store ``movies`` in a ``Dataset``
* A ``MovieRecommendationService`` that recommends movies for a particular user (userId: [1, 35])
* A ``SparkALSModelBuilder`` program that builds a recommendation model using the ALS algorithm and recommends
  movies for all the users included in the dataset 


The ``SparkALSModelBuilder`` Spark program contains the core logic for building the movie
recommendations. It uses the ALS (Alternating Least Squares) algorithm from Apache Spark's MLlib
to train the prediction model.

a. ``SparkALSModelBuilder`` reads the ``ratingsStream`` stream and uses it to train the prediction
model.  

b. ``SparkALSModelBuilder`` computes an RDD of not-rated movies using the ``movies`` dataset and the ``ratings`` dataset. 

c. ``SparkALSModelBuilder`` uses the prediction model to predict a score for each not-rated 
movie and stores the top 20 highest scored movies for each user in the ``recommendations`` dataset.

![Alt text](resources/img/SparkALS.png?raw=true "UML Diagram")



# UML Class Diagram 
The following represents the UML class diagram of the java source base package 
![Alt text](resources/uml/class-diag.png?raw=true "UML Diagram")
	

# Prerequisites 

* Java 7+  (http://www.oracle.com/technetwork/java/javase/downloads)
* Apache Maven (http://maven.apache.org)
* Download and install CORTEX (http://cortex-docs.c1.io/downloads/)

# Building the Artifact 

Download or clone the source then use the following command to build 
	
	mvn clean install

# Deploying the Artifact
 
Note that the steps are the same for deploying localy or to EMR_CORTEX environment.
These instructions use the command line intefrace of CORTEX
 

1. If you haven't already started a standalone CORTEX instance, start it with the command:

  	$ $CORTEX_HOME/bin/cortex sdk start
  	
2. Start the command line interface 
 
 	$ $CORTEX_HOME/bin/cortex cli 

3.Deploy the Application to a CDAP instance defined by its host (defaults to localhost):

  $ cdap cli load artifact target/MovieRecommender-<version>.jar
  $ cdap cli create app MovieRecommender MovieRecommender <version> user
  
4. Start the Application Services:

  $ cdap cli start service MovieRecommender.MovieStoreService
  $ cdap cli start service MovieRecommender.MovieRecommendationService
  
5. Make sure that the Services are running:

  $ cdap cli get service status MovieRecommender.MovieStoreService
  $ cdap cli get service status MovieRecommender.MovieRecommendationService
  
6. Ingest ``ratings`` and ``movies`` data:

  $ $BASEDIR/bin/ingest-data.sh --host [host]


7. Run the ``SparkALSModelBuilder`` Spark Program::

  $ cdap cli start spark MovieRecommender.SparkALSModelBuilder

The Spark program may take a couple of minutes to complete. You can check if it is complete by its
status (once done, it becomes STOPPED)::

  $ cdap cli get spark status MovieRecommender.SparkALSModelBuilder
  
8. Once the Spark program is complete, you can query the ``MovieRecommendationService`` for recommendations:

  $ cdap cli call service MovieRecommender.MovieRecommendationService GET 'recommend/16'
  
This will return a JSON response of rated and recommended movies:
```
{
  "rated": [
    "Legends of the Fall (1994)",
    "Arsenic and Old Lace (1944)",
    "Truman Show, The (1998)",
    "Doctor Dolittle (1998)",
    "Bug's Life, A (1998)",
    "Desperately Seeking Susan (1985)",
    "Jack Frost (1998)",
    "Prince of Egypt, The (1998)",
    "She's All That (1999)",
    "Baby Geniuses (1999)",
    "Mod Squad, The (1999)",
    "10 Things I Hate About You (1999)",
    "Never Been Kissed (1999)",
    "Love Letter, The (1999)",
    "Superman IV: The Quest for Peace (1987)",
    "Austin Powers: The Spy Who Shagged Me (1999)",
    "Arachnophobia (1990)",
    "Wild Wild West (1999)",
    "Lake Placid (1999)",
    "Inspector Gadget (1999)",
    "Deep Blue Sea (1999)",
    "Mystery Men (1999)",
    "Runaway Bride (1999)",
    "Iron Giant, The (1999)",
    "Bowfinger (1999)",
    "Mickey Blue Eyes (1999)",
    "Dudley Do-Right (1999)",
    "I Saw What You Did (1965)",
    "Drive Me Crazy (1999)",
    "Best Man, The (1999)",
    "Who Framed Roger Rabbit? (1988)",
    "Bachelor, The (1999)",
    "Creepshow (1982)",
    "Galaxy Quest (1999)",
    "Bell, Book and Candle (1958)"
  ],
  "recommended": [
    "Father of the Bride Part II (1995)",
    "If Lucy Fell (1996)",
    "Walk in the Clouds, A (1995)",
    "Mr. Wonderful (1993)",
    "Program, The (1993)",
    "Vie est belle, La (Life is Rosey) (1987)",
    "Beautiful Thing (1996)",
    "Paris Is Burning (1990)",
    "Killer, The (Die xue shuang xiong) (1989)",
    "St. Elmo's Fire (1985)",
    "About Last Night... (1986)",
    "Velvet Goldmine (1998)",
    "Varsity Blues (1999)",
    "Children of Paradise (Les enfants du paradis) (1945)",
    "All About My Mother (Todo Sobre Mi Madre) (1999)",
    "Topsy-Turvy (1999)",
    "White Men Can't Jump (1992)",
    "Flamingo Kid, The (1984)",
    "Hoosiers (1986)",
    "Diner (1982)",
    "Dreamscape (1984)"
  ]
}
```

To stop the application, execute:

  $ cdap cli stop service MovieRecommender.MovieStoreService
  $ cdap cli stop service MovieRecommender.MovieRecommendationService

	