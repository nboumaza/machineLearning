MovieRecommender
================

Movie recommendation application for CDAP_.

Overview
--------
The MovieRecommender application recommends movies to users using collaborative filtering.

* The ``ratings`` and ``movies`` data is taken from the `MovieLens Dataset <http://grouplens.org/datasets/movielens/>`_.
* The recommendation engine is based on the ALS (Alternating Least Square) implementation in Apache Spark MLlib library.

Implementation Details
----------------------

The MovieRecommender application is composed of the components:

* ``Streams`` for ingesting ``ratings`` data into the system
* A ``Service`` to store ``movies`` in a ``Dataset``
* A ``Service`` that recommends movies for a particular user
* A ``Spark`` Program which builds a recommendation model using the ALS algorithm and recommends
  movies for all the users

|(App)|


The ``RecommendationBuilder`` Spark program contains the core logic for building the movie
recommendations. It uses the ALS (Alternating Least Squares) algorithm from Apache Spark's MLlib
to train the prediction model.

|(RecommendationBuilder)| 

First, ``RecommendationBuilder`` reads the ``ratingsStream`` and uses it to train the prediction
model.  Then, it computes an RDD of not-rated movies using the ``movies`` dataset and the
``ratings`` dataset. It uses the prediction model to predict a score for each not-rated movie and
stores the top 20 highest scored movies for each user in the ``recommendations`` dataset.


Installation & Usage
====================
*Pre-Requisite*: Download and install CDAP_.

From the project root, build ``MovieRecommender`` with `Apache Maven <http://maven.apache.org/>`_ ::

  $ MAVEN_OPTS="-Xmx512m" mvn clean package

Note that the remaining commands assume that the ``cdap`` script is available on your PATH.
If this is not the case, please add it::

  $ export PATH=$PATH:<cdap-home>/bin

If you haven't already started a standalone CDAP installation, start it with the command::

  $ cdap sdk start


Deploy the Application to a CDAP instance defined by its host (defaults to localhost)::

  $ cdap cli load artifact target/MovieRecommender-<version>.jar
  $ cdap cli create app MovieRecommender MovieRecommender <version> user
  
Start the Application Services::

  $ cdap cli start service MovieRecommender.MovieDictionaryService
  $ cdap cli start service MovieRecommender.MovieRecommenderService
  
Make sure that the Services are running (note that the
``RecommendationBuilder`` Spark program will be started later)::

  $ cdap cli get service status MovieRecommender.MovieDictionaryService
  $ cdap cli get service status MovieRecommender.MovieRecommenderService
  
Ingest ``ratings`` and ``movies`` data::

  $ bin/ingest-data.sh --host [host]


Run the ``RecommendationBuilder`` Spark Program::

  $ cdap cli start spark MovieRecommender.RecommendationBuilder

The Spark program may take a couple of minutes to complete. You can check if it is complete by its
status (once done, it becomes STOPPED)::

  $ cdap cli get spark status MovieRecommender.RecommendationBuilder
  
Once the Spark program is complete, you can query the ``MovieRecommenderService`` for recommendations::

  $ cdap cli call service MovieRecommender.MovieRecommenderService GET 'recommend/1'
  
This will return a JSON response of rated and recommended movies::

  +=========================================================================================================================+
  | status | headers                         | body size | body                                                             |
  +=========================================================================================================================+
  | 200    | Content-Length : 1943           | 1943      | {"rated":["Toy Story (1995)","Pocahontas (1995)","Apollo 13 (199 |
  |        | Content-Type : application/json |           | 5)","Star Wars: Episode IV - A New Hope (1977)","Schindler\u0027 |
  |        | Connection : keep-alive         |           | s List (1993)","Secret Garden, The (1993)","Aladdin (1992)","Sno |
  |        |                                 |           | w White and the Seven Dwarfs (1937)","Beauty and the Beast (1991 |
  |        |                                 |           | ...                                                              |
  +=========================================================================================================================+

To stop the application, execute::

  $ cdap cli stop service MovieRecommender.MovieDictionaryService
  $ cdap cli stop service MovieRecommender.MovieRecommenderService

