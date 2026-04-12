# Protocol Documentation - Group 17

## 1. Introduction

This document defines the current client-server communication protocol implemented in the Java project.

The protocol supports:

- player name changes
- global, lobby, and whisper chat
- broadcast messages
- lobby creation, joining, leaving, and listing
- player list requests
- game start requests
- turn actions such as play, draw, and end turn
- special effect resolution
- public game-state updates
- hand updates
- round-end and game-end summaries
- dev-mode activation by scenario
- heartbeat (`PING` / `PONG`)

The protocol is line-based over TCP and uses UTF-8 text messages. 

---

## 2. Transport and Message Format

### 2.1 General Format

- Each protocol message is sent as one text line.
- Main wire format: `TYPE|payload`
- `TYPE` is parsed case-insensitively.
- The parser splits only at the first `|`.
- Some payloads themselves contain additional subfields separated by `|`, `,`, or `:` depending on the message type. :contentReference[oaicite:2]{index=2}

Examples:

```text
NAME|Alice
GLOBALCHAT|Hello everyone
PLAYERS|
PLAY_CARD|81
DEV|default
````

### 2.2 Empty and Non-Empty Payload Rules

Some message types require an empty payload, for example:

```text
START|
DRAW_CARD|
END_TURN|
PLAYERS|
PING|
```

Other message types require or allow content, for example:

```text
NAME|Alice
CREATE|Lobby1
JOIN|Lobby1
PLAY_CARD|81
DEV|default
```

The parser validates whether a payload is allowed for the chosen message type.

---

## 3. Supported Message Types

The following message types are recognized by the current protocol:

### 3.1 General / chat / lobby

* `NAME`
* `GLOBALCHAT`
* `LOBBYCHAT`
* `WHISPERCHAT`
* `PLAYERS`
* `ALLPLAYERS`
* `START`
* `QUIT`
* `LEAVE`
* `CREATE`
* `LOBBIES`
* `JOIN`
* `PING`
* `PONG`
* `INFO`
* `ERROR`
* `GAME`
* `BROADCAST`
* `CHEATWIN`
* `DEV`

### 3.2 Game action / game state

* `PLAY_CARD`
* `DRAW_CARD`
* `END_TURN`
* `EFFECT_RESPONSE`
* `GET_HAND`
* `GET_GAME_STATE`
* `GET_ROUND_END`
* `GET_GAME_END`
* `GAME_STATE`
* `HAND_UPDATE`
* `EFFECT_REQUEST`
* `ROUND_END`
* `GAME_END`

### 3.3 Internal fallback

* `UNKNOWN`

These types are defined in the current `Message.Type` enum and are used by both CLI and GUI clients through the shared parser.

---

## 4. Client-to-Server Commands

This section documents the messages a client can actively send.

---

### 4.1 `NAME|<newName>`

Purpose: set or change the player name.

Example:

```text
NAME|Alice
```

Typical successful response:

```text
INFO|Your name has been set to Alice
```

Possible outcomes include:

* direct success
* automatic renaming if the desired name is already taken
* error if the name is invalid

Slash alias:

```text
/name Alice
```

---

### 4.2 `GLOBALCHAT|<text>`

Purpose: send a message to the global chat.

Example:

```text
GLOBALCHAT|Hello everyone
```

Typical server fan-out:

```text
GLOBALCHAT|Alice|Hello everyone
```

Slash aliases:

```text
/chat Hello everyone
/g Hello everyone
/global Hello everyone
```

---

### 4.3 `LOBBYCHAT|<text>`

Purpose: send a message to the current lobby chat only.

Example:

```text
LOBBYCHAT|Ready to start?
```

Typical server fan-out:

```text
LOBBYCHAT|Alice|Ready to start?
```

Slash aliases:

```text
/l Ready to start?
/lobby Ready to start?
```

---

### 4.4 `WHISPERCHAT|<player>|<text>`

Purpose: send a private whisper message to one player.

Example:

```text
WHISPERCHAT|Bob|Can you join my lobby?
```

The receiving and sending client may see formatted direction metadata.

Slash aliases:

```text
/w Bob Can you join my lobby?
/whisper Bob Can you join my lobby?
/msg Bob Can you join my lobby?
/tell Bob Can you join my lobby?
```

---

### 4.5 `BROADCAST|<text>`

Purpose: send a server-wide broadcast-style info message.

Example:

```text
BROADCAST|Server restart in 5 minutes
```

Typical server fan-out:

```text
BROADCAST|Alice|Server restart in 5 minutes
```

Slash alias:

```text
/broadcast Server restart in 5 minutes
```

---

### 4.6 `PLAYERS|`

Purpose: request the list of players in the current lobby.

Example:

```text
PLAYERS|
```

Typical response:

```text
PLAYERS|Alice,Bob,Charlie
```

Slash alias:

```text
/players
```

---

### 4.7 `ALLPLAYERS|`

Purpose: request the list of all connected players on the server.

Example:

```text
ALLPLAYERS|
```

Typical response:

```text
ALLPLAYERS|Alice,Bob,Charlie,Dana
```

Slash alias:

```text
/allplayers
```

---

### 4.8 `LOBBIES|`

Purpose: request the list of available lobbies.

Example:

```text
LOBBIES|
```

Typical response format:

```text
LOBBIES|Lobby1:WAITING:2:5,Lobby2:PLAYING:4:5
```

Slash alias:

```text
/lobbies
```

---

### 4.9 `CREATE|<lobbyId>`

Purpose: create a lobby and join it.

Example:

```text
CREATE|MyLobby
```

Typical response:

```text
INFO|Lobby created and joined: MyLobby
```

Slash alias:

```text
/create MyLobby
```

---

### 4.10 `JOIN|<lobbyId>`

Purpose: join an existing lobby.

Example:

```text
JOIN|MyLobby
```

Typical response:

```text
INFO|Joined lobby: MyLobby
```

Slash alias:

```text
/join MyLobby
```

---

### 4.11 `LEAVE|`

Purpose: leave the current lobby.

Example:

```text
LEAVE|
```

Typical response:

```text
INFO|You left lobby: MyLobby
```

Slash alias:

```text
/leave
```

---

### 4.12 `START|`

Purpose: request that the current lobby starts a game.

Example:

```text
START|
```

Typical result:

* game initialization
* public `GAME_STATE|...` broadcast
* hand broadcasts
* possible error if the lobby is invalid or the request is illegal

Slash alias:

```text
/start
```

---

### 4.13 `QUIT|`

Purpose: disconnect from the server.

Example:

```text
QUIT|
```

Typical result:

* session disconnect
* lobby/player cleanup
* client-side shutdown flow

Slash alias:

```text
/quit
```

---

### 4.14 `PING|` and `PONG|`

Purpose: heartbeat.

Examples:

```text
PING|
PONG|
```

Behavior:

* clients respond to incoming `PING|` with `PONG|`
* the client also runs its own heartbeat loop
* the server sends periodic `PING|` and times out dead connections

These are not intended for manual gameplay usage.

Legacy compatibility accepted by the parser:

```text
SYS|PING
SYS|PONG
```

---

### 4.15 `PLAY_CARD|<cardId>`

Purpose: play one card from the current hand.

Example:

```text
PLAY_CARD|81
```

Typical results:

* `GAME|CARD_PLAYED:<player>:<cardId>`
* possible `GAME|EFFECT_TRIGGERED:...`
* `GAME_STATE|...`
* `HAND_UPDATE|...`
* possible error if the move is illegal

Slash aliases:

```text
/play 81
/card 81
```

---

### 4.16 `DRAW_CARD|`

Purpose: draw one card from the draw pile.

Example:

```text
DRAW_CARD|
```

Typical result:

```text
GAME|CARD_DRAWN:Alice:42
```

Slash aliases:

```text
/draw
/pickup
```

---

### 4.17 `END_TURN|`

Purpose: end the current turn.

Example:

```text
END_TURN|
```

Typical result:

```text
GAME|TURN_ADVANCED:Bob
```

Slash aliases:

```text
/end
/endturn
```

---

### 4.18 `GET_HAND|`

Purpose: request the current private hand from the server.

Example:

```text
GET_HAND|
```

Typical response:

```text
HAND_UPDATE|81,82,5
```

Slash alias:

```text
/hand
```

---

### 4.19 `GET_GAME_STATE|`

Purpose: request the current public game state.

Example:

```text
GET_GAME_STATE|
```

Typical response:

```text
GAME_STATE|phase:AWAITING_PLAY,currentPlayer:Alice,discardTop:30,drawPileSize:103,players:Alice:3:0,Bob:5:0
```

Slash alias:

```text
/gamestate
```

---

### 4.20 `GET_ROUND_END|`

Purpose: request the most recent round-end summary.

Example:

```text
GET_ROUND_END|
```

Typical response:

```text
ROUND_END|Alice:0:0,Bob:39:39
```

Slash alias:

```text
/roundend
```

---

### 4.21 `GET_GAME_END|`

Purpose: request the game-end summary.

Example:

```text
GET_GAME_END|
```

Typical response:

```text
GAME_END|winner:Alice,scores:Alice:0,Bob:156
```

Slash alias:

```text
/gameend
```

---

### 4.22 `EFFECT_RESPONSE|...`

Purpose: resolve a pending special effect by supplying the necessary arguments.

This is a family of payloads rather than one single fixed syntax.

#### Skip

```text
EFFECT_RESPONSE|SKIP|Bob
```

Slash alias:

```text
/skip Bob
```

#### Counterattack (color only)

```text
EFFECT_RESPONSE|COUNTERATTACK||BLUE
```

Slash alias:

```text
/counter blue
```

#### Counterattack (redirect target and color)

```text
EFFECT_RESPONSE|COUNTERATTACK|Bob|BLUE
```

Slash alias:

```text
/counter blue Bob
```

#### Nice Try

```text
EFFECT_RESPONSE|NICE_TRY|Bob
```

Slash alias:

```text
/nicetry Bob
```

#### Gift

```text
EFFECT_RESPONSE|GIFT|Bob|81
EFFECT_RESPONSE|GIFT|Bob|81,82
```

Slash alias:

```text
/gift Bob 81
/gift Bob 81 82
```

#### Exchange

```text
EFFECT_RESPONSE|EXCHANGE|Bob|81,82
```

Slash alias:

```text
/exchange Bob 81 82
```

#### Fantastic

Color request:

```text
EFFECT_RESPONSE|FANTASTIC|BLUE
```

Number request:

```text
EFFECT_RESPONSE|FANTASTIC||7
```

Slash aliases:

```text
/fantastic blue
/fantastic 7
```

#### Fantastic Four

Color request with four recipients:

```text
EFFECT_RESPONSE|FANTASTIC_FOUR|BLUE||Alice,Bob,Charlie,Dana
```

Number request with four recipients:

```text
EFFECT_RESPONSE|FANTASTIC_FOUR||7|Alice,Alice,Bob,Charlie
```

Slash aliases:

```text
/fantasticfour blue Alice Bob Charlie Dana
/fantasticfour 7 Alice Alice Bob Charlie
```

#### Equality

```text
EFFECT_RESPONSE|EQUALITY|Bob|BLUE
```

Slash alias:

```text
/equality Bob blue
```

#### Second Chance

Play another card:

```text
EFFECT_RESPONSE|SECOND_CHANCE|82
```

Draw instead:

```text
EFFECT_RESPONSE|SECOND_CHANCE|
```

Slash aliases:

```text
/secondchance 82
/secondchance draw
```

These effect forms are currently supported by the shared parser and by the typed protocol client.

---

### 4.23 `DEV|<scenarioName>`

Purpose: enable dev mode for the current lobby and select a scenario file to apply when the next game starts.

Example:

```text
DEV|default
```

Slash alias:

```text
/dev default
```

Disable dev mode:

```text
/dev off
```

Scenario files are loaded from:

```text
/devmode/<scenarioName>.properties
```

Example file:

```text
/devmode/default.properties
```

Typical success response:

```text
INFO|Dev mode enabled for lobby TestLobby with scenario: default
```

---

### 4.24 `CHEATWIN|`

Purpose: immediately force the game to end for testing or debug purposes.

Example:

```text
CHEATWIN|
```

This is a no-payload control command.

---

## 5. Server-to-Client Messages

This section documents the most important messages sent by the server to clients.

---

### 5.1 `INFO|<text>`

Purpose: human-readable informational message.

Examples:

```text
INFO|WELCOME
INFO|Your name has been set to Alice
INFO|Lobby created and joined: MyLobby
INFO|Joined lobby: MyLobby
INFO|You left lobby: MyLobby
```

---

### 5.2 `ERROR|<text>`

Purpose: human-readable error message.

Examples:

```text
ERROR|Unknown command.
ERROR|Not your turn
ERROR|Could not apply dev mode scenario 'default': ...
```

---

### 5.3 `GAME|<detail>`

Purpose: game-event log message.

Examples:

```text
GAME|CARD_PLAYED:Alice:81
GAME|CARD_DRAWN:Alice:42
GAME|TURN_ADVANCED:Bob
GAME|EFFECT_TRIGGERED:SECOND_CHANCE
GAME|ROUND_ENDED:player_empty_hand
GAME|ERROR:Cannot play card in phase RESOLVING_EFFECT
```

The payload format is event-specific and is intended mainly as a human-readable event stream.

---

### 5.4 `GAME_STATE|<statePayload>`

Purpose: send the current public game state.

Example:

```text
GAME_STATE|phase:AWAITING_PLAY,currentPlayer:Alice,discardTop:30,drawPileSize:103,players:Alice:3:0,Bob:5:0
```

Current commonly used keys include:

* `phase`
* `currentPlayer`
* `discardTop`
* `drawPileSize`
* `players`

The `players:` section encodes repeated entries as:

```text
<name>:<handSize>:<totalScore>
```

---

### 5.5 `HAND_UPDATE|<cardIdList>`

Purpose: send the private current hand of one player.

Example:

```text
HAND_UPDATE|81,82,5
```

An empty hand is represented as:

```text
HAND_UPDATE|
```

---

### 5.6 `EFFECT_REQUEST|<effectData>`

Purpose: tell the client that a special effect now needs user input.

Example:

```text
EFFECT_REQUEST|SECOND_CHANCE:Alice
```

The exact format depends on the requested effect. It is intended to prompt the client to answer with a matching `EFFECT_RESPONSE|...`.

---

### 5.7 `ROUND_END|<scoreSummary>`

Purpose: provide round-end scoring information.

Example:

```text
ROUND_END|Alice:0:0,Bob:39:39
```

Current repeated entry format:

```text
<playerName>:<roundScore>:<totalScore>
```

---

### 5.8 `GAME_END|<summary>`

Purpose: provide final game-over summary.

Example:

```text
GAME_END|winner:Alice,scores:Alice:0,Bob:156
```

The exact payload may evolve, but the message type is already part of the current protocol.

---

### 5.9 `PLAYERS|...`, `ALLPLAYERS|...`, `LOBBIES|...`

Purpose: list responses.

Examples:

```text
PLAYERS|Alice,Bob
ALLPLAYERS|Alice,Bob,Charlie
LOBBIES|Lobby1:WAITING:2:5,Lobby2:PLAYING:4:5
```

The GUI currently reformats lobby entries for display, but the wire format remains the raw serialized form from the server.

---

### 5.10 `GLOBALCHAT|...`, `LOBBYCHAT|...`, `WHISPERCHAT|...`, `BROADCAST|...`

Purpose: chat and broadcast fan-out.

Examples:

```text
GLOBALCHAT|Alice|Hello everyone
LOBBYCHAT|Alice|Ready?
WHISPERCHAT|FROM|Bob|Join my lobby
WHISPERCHAT|TO|Bob|Okay
BROADCAST|Alice|Server restart in 5 minutes
```



---

## 6. Slash Command Compatibility

The shared parser accepts a set of slash commands that are converted into structured protocol messages. This works in both the terminal client and the GUI command input.

### 6.1 General commands

```text
/name <username>
/chat <text>
/g <text>
/global <text>
/l <text>
/lobby <text>
/w <player> <text>
/whisper <player> <text>
/msg <player> <text>
/tell <player> <text>
/broadcast <text>
/players
/allplayers
/lobbies
/create <lobbyId>
/join <lobbyId>
/leave
/start
/quit
/hand
/gamestate
/roundend
/gameend
/play <cardId>
/card <cardId>
/draw
/pickup
/end
/endturn
/dev <scenario>
```

### 6.2 Effect helper commands

```text
/skip <player>
/counter <color>
/counter <color> <player>
/nicetry <player>
/gift <player> <cardId1> [cardId2]
/exchange <player> <cardId1> <cardId2>
/fantastic <color>
/fantastic <number>
/fantasticfour <color> <player1> <player2> <player3> <player4>
/fantasticfour <number> <player1> <player2> <player3> <player4>
/equality <player> <color>
/secondchance <cardId>
/secondchance draw
```

Legacy heartbeat compatibility also exists:

```text
SYS|PING
SYS|PONG
```

---

## 7. Startup and Heartbeat Notes

### 7.1 Startup

On a fresh connection, the server sends a normal protocol info message:

```text
INFO|WELCOME
```

It is no longer plain raw text outside the protocol.

### 7.2 Heartbeat timing

Current heartbeat behavior:

* server heartbeat interval: `5000 ms`
* server timeout threshold: `15000 ms`
* clients also run a heartbeat loop and answer `PING` with `PONG`

---

## 8. Error Handling

If input is invalid, malformed, or violates game/protocol rules, the server responds with an error message or rejects the action.

Typical examples:

```text
ERROR|Unknown command.
ERROR|Invalid command.
ERROR|Not your turn
ERROR|Card 45 cannot be played on 30
ERROR|You must be in a lobby to configure dev mode.
ERROR|Usage: /dev <scenarioName> or /dev off
ERROR|Could not apply dev mode scenario 'default': ...
```

In some cases, game-rule failures may also be surfaced as `GAME|ERROR:...` event messages rather than top-level `ERROR|...`, depending on where the rule check happens.

---

## 9. Sequence Example

A typical modern communication flow might look like this:

```text
Server -> Client: INFO|WELCOME

Client -> Server: NAME|Alice
Server -> Client: INFO|Your name has been set to Alice

Client -> Server: CREATE|TestLobby
Server -> Client: INFO|Lobby created and joined: TestLobby
Server -> All in lobby: PLAYERS|Alice

Client -> Server: DEV|default
Server -> All in lobby: INFO|Dev mode enabled for lobby TestLobby with scenario: default

Client -> Server: START|
Server -> All in lobby: GAME_STATE|phase:AWAITING_PLAY,currentPlayer:Alice,discardTop:30,drawPileSize:103,players:Alice:3:0,Bob:3:0
Server -> Alice: HAND_UPDATE|81,82,5

Client -> Server: PLAY_CARD|81
Server -> All in lobby: GAME|CARD_PLAYED:Alice:81
Server -> All in lobby: GAME|EFFECT_TRIGGERED:SECOND_CHANCE
Server -> All in lobby: EFFECT_REQUEST|SECOND_CHANCE:Alice

Client -> Server: EFFECT_RESPONSE|SECOND_CHANCE|82
Server -> All in lobby: GAME|CARD_PLAYED:Alice:82
Server -> All in lobby: GAME|EFFECT_TRIGGERED:SKIP
Server -> All in lobby: EFFECT_REQUEST|SKIP:Alice

Client -> Server: EFFECT_RESPONSE|SKIP|Bob
Server -> All in lobby: GAME|TURN_ADVANCED:Alice
Server -> All in lobby: GAME_STATE|phase:AWAITING_PLAY,currentPlayer:Alice,discardTop:82,drawPileSize:102,players:Alice:2:0,Bob:3:0
```

This example combines current startup behavior, dev mode, game start, a playable card, an effect request, and an effect response.

---

## 10. Notes on Extensibility

The protocol remains extensible because it is based on line-oriented `TYPE|payload` messages and a shared parser used by both the GUI and CLI clients.

Future additions can introduce:

* more structured `GAME|...` payloads
* richer event-card interactions
* more GUI-driven effect dialogs
* authentication or session extensions
* protocol versioning if needed

The current message enum and parser design already allow adding new types without changing the basic wire format.

```

A couple of especially important differences from your old document are these: `CHAT` should be removed in favor of `GLOBALCHAT`, `LOBBYCHAT`, and `WHISPERCHAT`; the startup note should now say `INFO|WELCOME` instead of raw plain text; and the document needs to include the full game/action set and `DEV|<scenario>` support. Those are the biggest blockers for the “up to date” achievement. 

```
