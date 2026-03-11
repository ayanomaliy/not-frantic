# Protocol Documentation - Group 14

## 1. Introduction

This document defines the current client-server communication protocol implemented in the Java project.

The protocol supports:

- player name changes
- chat messages
- player list requests
- game start requests
- disconnect requests
- heartbeat (ping/pong)
- server information, game messages, and error responses

The protocol is line-based over TCP and uses UTF-8 text messages.

## 2. Command Definitions and Syntax

### 2.1 Transport and Message Format

- Each protocol message is sent as one text line.
- Main wire format: `TYPE|payload`
- `TYPE` is case-insensitive when parsing (`name`, `NAME`, `Name` are accepted).
- `payload` may be empty, depending on command.
- The parser splits only at the first `|`.

Examples:

- `NAME|Alice`
- `CHAT|Hello everyone`
- `PLAYERS|`
- `PING|`

### 2.2 Supported Message Types

The following message types are recognized:

- `NAME`
- `CHAT`
- `PLAYERS`
- `START`
- `QUIT`
- `PING`
- `PONG`
- `INFO`
- `ERROR`
- `GAME`
- `UNKNOWN` (internal fallback after parse)

### 2.3 Client-to-Server Commands

1. `NAME|<newName>`

    Purpose: set or change the player name.

    Validation rules on server:

    - name must not be empty
    - max length: 20 characters
    - must not contain `|` or `,`
    - if same as current name (case-insensitive), request is rejected
    - if name is taken, server auto-generates unique name: `Name(2)`, `Name(3)`, ...

    Server responses:

    - `INFO|Your name has been set to ...`
    - `INFO|Your name is now ...`
    - `INFO|Your requested name was taken. ...`
    - `ERROR|...` on invalid requests

2. `CHAT|<text>`

    Purpose: send a chat message.

    Validation rules on server:

    - message must not be empty
    - max length: 200 characters
    - must not contain newline (`\n`) or carriage return (`\r`)

    Broadcast format:

    - `CHAT|<playerName>|<text>`

3. `PLAYERS|`

    Purpose: request list of connected players.

    Rules:

    - payload must be empty

    Server response:

    - `PLAYERS|name1,name2,name3`

4. `START|`

    Purpose: request game start.

    Rules:

    - payload must be empty
    - game must not already be started
    - at least 2 players required

    Server responses:

    - on success: multiple `GAME|...` messages
    - on failure: `ERROR|Game already started.` or `ERROR|Need at least 2 players to start.`

5. `QUIT|`

    Purpose: disconnect from server.

    Rules:

    - payload must be empty

    Server response:

    - `INFO|Goodbye.` (then session closes)

6. `PING|` and `PONG|`

    Purpose: heartbeat.

    Behavior:

    - client periodically sends `PING|`
    - server answers with `PONG|`
    - client also answers server `PING|` with `PONG|`

### 2.4 Server-Only Message Types

The client must not send these as normal application commands:

- `INFO`
- `ERROR`
- `GAME`

If a client sends them, server responds with:

- `ERROR|Client may not send server-only message types.`

### 2.5 Legacy Compatibility

The parser still accepts legacy commands:

- `/name <username>` -> `NAME`
- `/chat <message>` -> `CHAT`
- `/players` -> `PLAYERS`
- `/start` -> `START`
- `/quit` -> `QUIT`

Legacy heartbeat accepted by parser:

- `SYS|PING`
- `SYS|PONG`

### 2.6 Startup/Compatibility Notes

On connection, the server currently sends two plain lines for compatibility:

- `WELCOME`
- `Set your name with: /name YourName`

These are not `TYPE|payload` protocol messages.

## 3. Error Handling

If input is invalid or violates command rules, the server returns an error message.

Typical error responses:

- `ERROR|Unknown command.`
- `ERROR|Name cannot be empty.`
- `ERROR|Name is too long. Maximum 20 characters.`
- `ERROR|Name contains invalid characters.`
- `ERROR|You already have this name.`
- `ERROR|Chat message cannot be empty.`
- `ERROR|Chat message is too long. Maximum 200 characters.`
- `ERROR|Chat message contains invalid characters.`
- `ERROR|PLAYERS must not contain content.`
- `ERROR|START must not contain content.`
- `ERROR|QUIT must not contain content.`
- `ERROR|Client may not send server-only message types.`

If parsing fails completely (null/blank input), message is ignored or treated as invalid command depending on context.

## 4. Sequence Example

A typical communication flow might look like this:

```text
Server -> Client: WELCOME
Server -> Client: Set your name with: /name YourName

Client -> Server: NAME|Alice
Server -> Client: INFO|Your name has been set to Alice
Server -> All:    INFO|Alice joined the lobby.

Client -> Server: CHAT|Hello all
Server -> All:    CHAT|Alice|Hello all

Client -> Server: PLAYERS|
Server -> Client: PLAYERS|Alice,Bob

Client -> Server: START|
Server -> All:    GAME|Starting prototype game...
Server -> All:    GAME|Connected players: 2
Server -> All:    GAME|This is where actual game initialization will go.

Client -> Server: QUIT|
Server -> Client: INFO|Goodbye.
```

## Extension Possibilities

The protocol can be extended in later milestones, for example:

- dedicated movement/game action commands (currently not implemented)
- richer `GAME|...` payload format (structured fields)
- authentication/session tokens
- versioned protocol negotiation
- optional encryption/signature layer

The existing `TYPE|payload` format allows adding new message types while staying backward-compatible.
