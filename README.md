Word Rest Web Service Application
=====================================

Requirements
------------

Create a restful web service that receives words and puts them in a data store.  We request that the data store be a flat file.

The web service will have four endpoints to access information about the words:
* Note: 5 Endpoints Described

1) A list of all words stored in the file.

2) The ranking, based on number of occurrences, that the word is.  i.e. if “can” “soup” and “Campbell”
   all exist in the data store, where “can” was entered twice, and “soup” and “Campbell” were each entered once,
   the ranking of “can” would be 1, whereas “soup” and “Campbell” would each be 2
   
3) The number of occurrences for a specific word.

4) The web service will have one insert endpoint, which inserts a specific word into the data store,
   and increments the number of times it has appeared.
   
5) The web service will also have an “update” endpoint, which can be used to override the counting behavior,
   and force the number of times a word has appeared to match the input of this endpoint.
   i.e. if “can” were entered twice, we could update the count to be “1”, using the update endpoint.
   
   
Please highlight the following in a brief discussion:

1) any scalability concerns with this solution
  a. suggested ways of moving it from one web server to several web servers
     i. suggested technologies that would make the above easier
     
2) highlight any data consistency concerns and decisions that were made when you designed your system

3) why requests were either designed synchronously or asynchronously



Answers
-------

1)

* As load is increased on the web services we will have points of contention at the app server level.
   * We can move to using a load balancer in front of multiple application servers. For instance we can have multiple
   instances of tomcat running these webservers with
      * Can use Jenkins to help build, test and deploy the restful web services to the tomcat instances. By using scripting
      * or Jenkins plugins we can stop the servers, deploy and restart servers.

* Another point of contention will be in the access of the data store, in this case the flat file.
   * One solution would be to move to a different datastore like a rdbms or nosql database with we can then cluster
   * We could also implement paging to reduce the amount of data that is retrieved with one call
   * If we must stick with flat files we could start by creating a file for each letter in the alphabet with only words
     in each file that start with that letter.
      * This could also help in sending back chunks of data in an asynchronous service call.
      * This is a rudimentary MapReduce scenario.

2)
* This simple application does not currently check if the word being inserted is a valid word.
   * We would need to build a service to check if a word is valid against an interal or external dictionary type service.
   * We would also need to gather the requirement on if slang words are valid.
* There is also no localization built into the application if we want to support different languages. Would need to discover if we need to support different languages.
* There is also no check to discover if the word was spelled incorrectly. We may just require the correct spelling and and return no results. If there is not results we could also return a suggestion block for words that are close matches.

3)
* Right now all requests are designed synchronously. The next version will need to create asynchronous services for the
  list endpoints if we do not implement paging. The list endpoints will take longer periods of time as our datastore fills up.
   * As a poc we can implement synchronously with the goal to have those implement paging and to later add the asynchronous services in the next iteration.

Current Issues
--------------
* Test harnes is throwing exception related to inject Spring beans

Remember To
--------------
* Run $:play idea after you change sbt dependencies. They will not show up in your IDE util the project files have been rewritten.
  * Intellij initially looks in the repository/local even though new dependencies are located in respository/cache

TODO
----
* Fix issue with test harness throwing exception 
* Create Result object for update and insert
* Pass in file path to WordsServiceFlatFile so we can test with different file 

Play Notes
---------
Pros
* Easy to get up and running quickly creating rest web services
* Controllers are automatically reloaded when developing, no need to restart server.

Cons
* Issue with Ide and dependencies not syncing properly
* If using Java will need to use 3rd party api to do DI


Scala Notes
-----------
* Recommend using the Scala version of Play.
* Play is written in scala and the syntax in the routing file is scala.
* Can integrate spring or go with DI pattern like Cake.
* Write a function parameter to check if the file has been read
* Write a function parameter to do the file try catch block and closing of IO
* Write a function parameter to do divide and conquer find - Can use in update and insert

