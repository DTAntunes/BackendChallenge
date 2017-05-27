## Initial Setup

Obtain the testing.properties and production.properties files.

Run setup.sh. You're done!

## Project Layout

The project is a standard maven project. Within the code/src/main/java directory, the base package contains the main class which sets up the server.

Configuration info is pulled from a configuration class in the util package to allow swapping things out for testing/production (for example using DynamoDB local for testing, printing exceptions to console rather than attempting to log them etc.).

Currently all endpoints are implemented within the login package, though the data retrieval classes should likely be moved to a more suitable package. The endpoints use tho login.responseModels classes to model the responses.

Responses (should) conform to the JSON API specification, for which they mostly use the ResourceObject class in the util package and are rendered by the JsonRenderer (using Gson).

## Client Documentation

Client documentation description possible response codes and bodies is available (both as yaml and generated html2) as a swagger doc in the docs folder. Endpoints have an implied 500 response, which Spark (the server framework) generates when an uncaught exception is generated.
