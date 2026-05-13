
# Frantic^-1

**Frantic^-1** is a multiplayer card game developed as part of the **Programmierprojekt** lecture at the **University of Basel**.

The project is developed by **The Devs^-1**.

## About the Project

Frantic^-1 is an interactive multiplayer card game where players try to get rid of all cards in their hand while sabotaging each other with special effects, hand manipulation, and black-card-triggered event mechanics.

This repository currently contains:

- a TCP server
- a terminal client
- a JavaFX GUI client
- nickname and login handling
- global, lobby, and whisper chat
- lobby creation and joining
- turn-based game logic
- slash-command parsing for both CLI and GUI command input
- lobby-scoped dev mode scenarios

## Context

This game is being developed for the **Programmierprojekt** course at the **University of Basel**.

## Team

**The Devs^-1**

---

## Requirements

Make sure you have:

- **Java 25**
- the included **Gradle wrapper** (`gradlew` / `gradlew.bat`)

Run all commands from the project root directory, the folder containing `build.gradle`.

---

## How to Run

### Start the server

Using Gradle:

```bash
.\gradlew server
````

or with an explicit port:

```bash
.\gradlew server --args="5555"
```

Using the built JAR:

```bash
java -jar build\libs\not-frantic.jar server
java -jar build\libs\not-frantic.jar server 5555
```

---

### Start the GUI client

The GUI client is now started with `client`.

If no address is given, the connect screen opens with default values:

* host: `localhost`
* port: `5555`
* username: system username

Using the built JAR:

```bash
java -jar build\libs\not-frantic.jar client
java -jar build\libs\not-frantic.jar client localhost:5555
java -jar build\libs\not-frantic.jar client localhost:5555 Alice
```

What this does:

* `client` opens the **GUI**
* if `host:port` is provided, those values are prefilled in the GUI connect screen
* if a username is also provided, it is prefilled as well

---

### Start the terminal client

The terminal client is now started with `cli`.

Using Gradle:

```bash
.\gradlew client
```

or with explicit arguments:

```bash
.\gradlew client --args="localhost:5555 Alice"
```

Using the built JAR:

```bash
java -jar build\libs\not-frantic.jar cli
java -jar build\libs\not-frantic.jar cli localhost:5555
java -jar build\libs\not-frantic.jar cli localhost:5555 Alice
```

CLI defaults:

* if no address is given, it uses `localhost:5555`
* if no username is given, it uses the system username, or `Player` as fallback

---

## Launcher Summary

The executable JAR supports these top-level modes:

```text
java -jar not-frantic.jar server [port]
java -jar not-frantic.jar client [host:port] [username]
java -jar not-frantic.jar cli [host:port] [username]
```

Meaning:

* `server` = server
* `client` = JavaFX GUI client
* `cli` = terminal client

---

## GUI Notes

The GUI contains:

* a **Connect View**
* a **Lobby View**
* a **Game View**

The GUI command input uses the same shared command parsing as the terminal client, so slash commands such as `/join`, `/start`, `/play`, `/secondchance`, and `/dev` work there as well. The game view also includes:

* current player display
* current phase display
* top discard display
* hand rendering as clickable card buttons
* game/info log
* integrated chat with mode switching
* command input for manual slash commands

---

## Heartbeats / Connection Handling

Both client and server handle heartbeats automatically.

* clients send `PING`
* servers reply with `PONG`
* the server also monitors client heartbeat timeout
* heartbeat messages are not meant to be entered manually

---

## Card ID Cheat Sheet

### Main deck

1. `0–71` → normal color cards

    * 4 colors × values 1–9 × 2 copies

2. `72–80` → black cards

    * values 1–9

3. `81–100` → single-color special cards

4. `101–120` → four-color special cards

5. `121` → F%&/ U

### Event deck

* event cards use IDs `0–19` inside the separate event deck

---

## Quick Command Reference

The client accepts both raw protocol messages and slash aliases.

### Name / chat / lobby commands

#### Set name

```text
NAME|<username>
```

Slash:

```text
/name <username>
```

#### Global chat

```text
GLOBALCHAT|<text>
```

Slash:

```text
/chat <text>
/g <text>
/global <text>
```

#### Lobby chat

```text
LOBBYCHAT|<text>
```

Slash:

```text
/l <text>
/lobby <text>
```

#### Whisper chat

```text
WHISPERCHAT|<player>|<text>
```

Slash:

```text
/w <player> <text>
/whisper <player> <text>
/msg <player> <text>
/tell <player> <text>
```

#### Broadcast

```text
BROADCAST|<text>
```

Slash:

```text
/broadcast <text>
```

---

### Info / listing commands

#### Get current hand

```text
GET_HAND|
```

Slash:

```text
/hand
```

#### Get current public game state

```text
GET_GAME_STATE|
```

Slash:

```text
/gamestate
```

#### Get latest round-end summary

```text
GET_ROUND_END|
```

Slash:

```text
/roundend
```

#### Get game-end summary

```text
GET_GAME_END|
```

Slash:

```text
/gameend
```

#### List players in current lobby

```text
PLAYERS|
```

Slash:

```text
/players
```

#### List all connected players

```text
ALLPLAYERS|
```

Slash:

```text
/allplayers
```

#### List lobbies

```text
LOBBIES|
```

Slash:

```text
/lobbies
```

---

### Lobby / session commands

#### Start game

```text
START|
```

Slash:

```text
/start
```

#### Quit client

```text
QUIT|
```

Slash:

```text
/quit
```

#### Leave lobby

```text
LEAVE|
```

Slash:

```text
/leave
```

#### Create lobby

```text
CREATE|<lobbyId>
```

Slash:

```text
/create <lobbyId>
```

#### Join lobby

```text
JOIN|<lobbyId>
```

Slash:

```text
/join <lobbyId>
```

---

### Turn commands

#### Play card

```text
PLAY_CARD|<cardId>
```

Slash:

```text
/play <cardId>
/card <cardId>
```

#### Draw one card

```text
DRAW_CARD|
```

Slash:

```text
/draw
/pickup
```

#### End turn

```text
END_TURN|
```

Slash:

```text
/end
/endturn
```

---

## Effect Response Commands

These user-facing commands are parsed into `EFFECT_RESPONSE|...` payloads.

### Skip

```text
EFFECT_RESPONSE|SKIP|<player>
```

Slash:

```text
/skip <player>
```

### Nice Try

```text
EFFECT_RESPONSE|NICE_TRY|<player>
```

Slash:

```text
/nicetry <player>
```

### Gift

One card:

```text
EFFECT_RESPONSE|GIFT|<player>|<cardId1>
```

Two cards:

```text
EFFECT_RESPONSE|GIFT|<player>|<cardId1>,<cardId2>
```

Slash:

```text
/gift <player> <cardId1> [cardId2]
```

### Exchange

```text
EFFECT_RESPONSE|EXCHANGE|<player>|<cardId1>,<cardId2>
```

Slash:

```text
/exchange <player> <cardId1> <cardId2>
```

### Fantastic

Color request:

```text
EFFECT_RESPONSE|FANTASTIC|<COLOR>
```

Slash:

```text
/fantastic <color>
```

Number request:

```text
EFFECT_RESPONSE|FANTASTIC||<number>
```

Slash:

```text
/fantastic <number>
```

### Fantastic Four

Color request plus four recipients:

```text
EFFECT_RESPONSE|FANTASTIC_FOUR|<COLOR>||<player1>,<player2>,<player3>,<player4>
```

Number request plus four recipients:

```text
EFFECT_RESPONSE|FANTASTIC_FOUR||<number>|<player1>,<player2>,<player3>,<player4>
```

Slash:

```text
/fantasticfour <color> <player1> <player2> <player3> <player4>
/fantasticfour <number> <player1> <player2> <player3> <player4>
```

Examples:

```text
/fantasticfour blue Alice Bob Charlie David
/fantasticfour 7 Alice Alice Bob Charlie
```

### Equality

```text
EFFECT_RESPONSE|EQUALITY|<player>|<COLOR>
```

Slash:

```text
/equality <player> <color>
```

### Second Chance

Play another card:

```text
EFFECT_RESPONSE|SECOND_CHANCE|<cardId>
```

Slash:

```text
/secondchance <cardId>
```

Take the draw option:

```text
EFFECT_RESPONSE|SECOND_CHANCE|
```

Slash:

```text
/secondchance draw
```

---

## Dev Mode

The project now supports a lobby-scoped dev mode.

### Enable dev mode

```text
DEV|<scenarioName>
```

Slash:

```text
/dev <scenarioName>
```

Example:

```text
/dev default
```

This enables dev mode for the current lobby and stores the named scenario to be applied when the next game starts.

### Disable dev mode

Slash:

```text
/dev off
```

### Dev mode scenario files

Scenario files are loaded from:

```text
src/main/resources/devmode/<scenarioName>.properties
```

Example:

```text
src/main/resources/devmode/default.properties
```

### What dev mode can configure

A scenario can override parts of a freshly initialized round, including:

* `topCard`
* `currentPlayer`
* `phase`
* player hands
* requested color
* requested number
* pending effects
* pending effect target

### Player references in dev mode

Dev mode supports generic player-slot references, so the config does not need real usernames.

Examples:

```properties
currentPlayer=player1
pendingEffectTarget=player2

player1.hand=81,82,5
player2.hand=10,11,12
```

This makes the same scenario reusable even if the actual lobby players are called `p1`, `Alice`, or something else.

### Example dev config

```properties
# Top card on discard pile after setup
topCard=30

# Whose turn / state
currentPlayer=player1
phase=AWAITING_PLAY

# Optional requests
# requestedColor=BLUE
# requestedNumber=5

# Optional pending effects
# pendingEffects=SECOND_CHANCE
# pendingEffectTarget=player1

# Player hands
player1.hand=81,82,5
player2.hand=10,11,12
player3.hand=20,21,22
```

### Notes about dev mode

* dev mode is applied after normal round initialization
* it loads one named scenario file
* it can be triggered from both the terminal client and the GUI command field
* `/dev` by itself is not enough; use `/dev <scenario>` or `/dev off`

---

## Notes on Round and Game Flow

* A player wins the **round** by ending with zero cards
* Scores are then calculated from all remaining hands
* If the cumulative score threshold is reached, the **game** ends
* Otherwise, the server starts the next round with a freshly initialized and shuffled deck

---

## Example Usage

### Start server

```bash
java -jar build\libs\not-frantic.jar server 5555
```

### Open GUI with defaults

```bash
java -jar build\libs\not-frantic.jar client
```

### Open GUI with prefilled address and username

```bash
java -jar build\libs\not-frantic.jar client localhost:5555 Alice
```

### Start terminal client

```bash
java -jar build\libs\not-frantic.jar cli localhost:5555 Bob
```

### Typical lobby flow

```text
/create TestLobby
/join TestLobby
/players
/start
```

### Typical dev-mode flow

```text
/create TestLobby
/dev default
/start
```

---

## Current Status

Implemented features include:

* server/client networking
* shared protocol parsing between CLI and GUI
* GUI connect, lobby, and game views
* player lists and lobby lists
* global, lobby, and whisper chat
* command execution in CLI and GUI
* turn actions such as play, draw, end turn
* special effect resolution
* round-end and next-round flow
* dev mode scenario loading

```

A couple of details in your old note are now outdated: the old `/devmode on`, `/devmode off`, and `/devsetup ...` flow has effectively been replaced by `DEV|<scenario>` / `/dev <scenario>` plus `/dev off`, and the top-level launcher mode `gui` has been replaced by `client`, while the old `client` terminal entry moved to `cli`. The GUI also now pre-fills the connect screen from command-line arguments instead of requiring manual entry every time. :contentReference[oaicite:1]{index=1}

If you want, I can turn this into a shorter “polished README” version with less protocol detail and a separate `COMMANDS.md` section.
```
