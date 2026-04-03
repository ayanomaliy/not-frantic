# Frantic^-1

**Frantic^-1** is a multiplayer card game developed as part of the **Programmierprojekt** lecture at the **University of Basel**.

The project is developed by **The Devs^-1**.

## About the Project

Frantic^-1 is an interactive card game where players try to get rid of all cards in their hand while sabotaging each other with special effects, hand manipulation, and table-wide events, combining simple matching rules with chaotic action cards and global event mechanics.

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

or use the jar to run:

java -jar build\libs\not-frantic.jar server

java -jar build\libs\not-frantic.jar server 5555

### Run Client with GUI
.\gradlew run --args="gui"

or 

java -jar build\libs\not-frantic.jar gui



### Start the client
.\gradlew client --args="localhost:5555 Alice"

or leave out the username to have a suggested username assigned to you via 

.\gradlew client --args="localhost:5555" 

or connect to the default port (port 5555) and localhost using 

.\gradlew client

or use the jar to run:

java -jar build\libs\not-frantic.jar client localhost:5555

java -jar build\libs\not-frantic.jar client localhost:5555 Alice

### Figure out Cards Cheat-Sheet

1. 0-71: normal color cards (two copies of 1-9 in RED, GREEN, BLUE, YELLOW blocks)
2. 72-80: BLACK 1-9
3. 81-100: single-color specials
4. 101-123: four-color specials
5. 124: FUCK_YOU

### Quick Protocol Command list

1. PLAY_CARD|<cardId>
2. DRAW_CARD|
3. END_TURN|
4. EFFECT_RESPONSE 
- EFFECT_RESPONSE|SKIP|<targetPlayer>
- EFFECT_RESPONSE|COUNTERATTACK|<targetPlayer>
- EFFECT_RESPONSE|NICE_TRY|<targetPlayer>
- EFFECT_RESPONSE|GIFT|<targetPlayer>|<cardId1>[,<cardId2>]
- EFFECT_RESPONSE|EXCHANGE|<targetPlayer>|<cardId1>,<cardId2>
- EFFECT_RESPONSE|FANTASTIC|<COLOR> or EFFECT_RESPONSE|
- FANTASTIC|<COLOR>|<number>
- EFFECT_RESPONSE|FANTASTIC_FOUR|<COLOR> or EFFECT_RESPONSE|- FANTASTIC_FOUR|<COLOR>|<number>
- EFFECT_RESPONSE|EQUALITY|<targetPlayer>|<COLOR>
- EFFECT_RESPONSE|SECOND_CHANCE|<cardId> (or empty card part for draw penalty)