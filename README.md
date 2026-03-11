# Frantic^-1

**Frantic^-1** is a multiplayer card game developed as part of the **Programmierprojekt** lecture at the **University of Basel**.

The project is developed by **The Devs^-1**.

## About the Project

Frantic^-1 is an interactive card game where players try to get rid of all cards in their hand while sabotaging each other through special effects, hand manipulation, and table-wide events. The game combines simple matching rules with chaotic action cards and global event mechanics.

This repository contains the current prototype implementation of the game, including:
- a TCP server
- a TCP client
- login and nickname handling
- chat functionality
- basic lobby interaction

## Context

This game is being developed for the **Programmierprojekt** course at the **University of Basel**.

## Team

**The Devs^-1**


## How to Run

Make sure you are in the **project root directory** (the folder containing `build.gradle`).

### Requirements

Before running the project, make sure you have:

- **Java 25**
- **Gradle** available through the wrapper (`gradlew` / `gradlew.bat`)


### Start the server
.\gradlew server --args="<port>"
or just .\gradlew server to automatically run on port 5555


or not use gradle:


### Start the client
.\gradlew client --args="localhost:5555 Alice"

or leave out the username to have a suggested username assigned to you via 

.\gradlew client --args="localhost:5555" 

or connect to the default port (port 5555) and localhost using 

.\gradlew client


or not use gradle:

java -jar build\libs\not-frantic.jar

java -cp build\libs\not-frantic.jar ch.unibas.dmi.dbis.cs108.example.client.ClientMain


java -cp build\libs\not-frantic.jar ch.unibas.dmi.dbis.cs108.example.client.ClientMain localhost:5555


java -cp build\libs\not-frantic.jar ch.unibas.dmi.dbis.cs108.example.client.ClientMain localhost:5555 Ayano