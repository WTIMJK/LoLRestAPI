# Personal Stats - League of Legends API
REST API which retrieves various statistics for users. Built in Java using Spring Boot.
Intermediate data is stored in a local MongoDB datastore. This data is then aggregated and then displated on API call.
If data is requested for a user who is not yet in the database, it will query Riot Games' API for the raw data. This is avoided if possible because of rate limiting only allowing for 100 API calls per 2 minutes, and some users have hundreds of matches, and we need to make at least 2 API calls per match.

### Machine Learning - Match Positions [Top, Mid, Bot, Jungle, Support]
After a set of matches is pulled and inserted into the MongoDB, a Decision Tree Classifier model is run over each game's data to determine the position of each player. This is important for player vs player comparisons in a match. Linear Sum Assignment is used to make sure that each of the 5 positions is assigned exactly once to the 5 players per team. In the databse, each participant in the match is updated with a position.
