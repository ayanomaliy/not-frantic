# Project Diary

## Date: February 19, 2026

### First Project Meeting and Idea Discussion (Everyone)

#### What did we do today?

Today, we had our first project meeting. We spent time chatting and getting to know each other’s strengths, which was important for task distribution.

#### Meeting Summary

- Everyone shared different project ideas.
- We discussed the examples provided in class and talked about them together.
- By the end of the meeting, we came up with a few possible project ideas.

#### Future Plans

- We scheduled our next meeting for **Tuesday, March 3 at 20:30**.
- During this meeting, we will share the results of our research and decide on the final project.
- We will also prepare the presentation for the **first milestone** together.

---

## Date: March 3, 2026

### Research Discussion and Project Decision (Everyone)

#### What did we do today?

Today, we met to share the research we had done individually. After discussing our findings, we decided which game we will develop for our project.

#### Meeting Summary

- Each team member briefly presented their research.
- We discussed different ideas and compared them.
- Finally, we agreed on the game we will create.
- After making this decision, we started preparing our presentation and organized the main points together.

---

## Date: March 3, 2026

### Project Skeleton and Core Networking Setup (Aiysha)

#### What did we do today?

Today, we started implementing the basic skeleton of our project. The focus was on creating the first core classes and setting up the server-client structure so that communication could already work in a simple form.

#### Work Summary

- We created the first important project classes for the basic structure.
- We implemented the server and client entry points.
- We set up the first TCP connection between server and client.
- We added the basic command structure for features such as login, chat, and logout.
- We also began implementing nickname handling so that players can identify themselves.

#### Future Plans

- We want to improve the command handling and make the protocol more robust.
- We will continue expanding chat and lobby functionality.
- We also plan to refine nickname handling and make sure login/logout are handled cleanly.

---

## Date: March 9, 2026 

### Ping/Pong System and Nickname Features (Senanur)

#### What did we do today?
Today, I implemented several protocol features required for Milestone 2. The focus was on improving connection reliability and adding nickname management functionality.

#### Work Summary
- Implemented the Ping system, where the server regularly sends ping messages to connected clients.
- Clients process the ping messages and respond appropriately so that connection losses can be detected.
- Implemented the Pong mechanism, where the client sends periodic pong responses back to the server.
- This allows the server to monitor whether clients are still connected.

- Added unique nickname handling on the server.
    - If a player joins with a nickname that already exists, the server automatically assigns a modified name by adding a suffix

- Implemented the feature, where the client suggests a nickname based on the system username.

- Implemented the feature, which allows players to change their nickname during runtime.

#### Future Plans
- Test ping/pong behavior with multiple clients to ensure connection losses are detected reliably.
- Test nickname uniqueness with several clients joining simultaneously.
- Continue improving the protocol and expand the lobby/chat functionality.


## Date: March 10, 2026

### Disconnect Handling and Automatic Nickname Assignment (Aiysha)

#### What did we do today?

Today, we improved the connection handling in our client-server structure. The main focus was to make disconnect/logout behavior more reliable and to improve the nickname flow when a client connects.

#### Work Summary

- We fixed the disconnect handling so that cleanup now goes through one central disconnect method.
- We prevented duplicate unregister/cleanup behavior when a client leaves.
- We corrected the session cleanup flow in `ClientSession`.
- We also improved the nickname behavior on the client side.
- Instead of only suggesting the system username, the client now automatically sends it to the server when connecting.
- This makes the initial login process more convenient while still allowing users to change their nickname later with `/name`.

#### Future Plans

- We want to continue testing login, logout, and nickname uniqueness with multiple clients.
- We also plan to improve protocol consistency and extend the chat/lobby functionality further.

## Date: March 11, 2026

### Protocol Document (Denys)

#### What did we do to-day?

- Clearly defined the communication protocol by defining commands, syntax, message formats, and error handling. Added clear explanations with examples. Defines legacy commands for backwards-compatibility, and possilbe future additions for forwards-compatibility.

## Date: March 11, 2026

### Message Cleanup, Emoji Testing, and JAR Launcher (Aiysha)

#### What did we do today?

Today, I focused on improving the client-server communication structure and cleaning up the protocol handling. The goal was to make message encoding, decoding, and validation more consistent and easier to justify for the milestone requirements.

#### Work Summary

- I cleaned up the message handling so protocol-related logic is now more centralized and consistent.
- I improved the client so that incoming messages are parsed and interpreted instead of only being printed as raw protocol lines.
- I adjusted validation so malformed or structurally invalid messages can be rejected more clearly.
- I also removed some older ad-hoc handling to make the message flow cleaner and easier to understand.

I also investigated whether emojis could be sent correctly through the chat, since this is relevant for robust encoding. For this, I added temporary debugging output and checked the message flow step by step. The result was that the emojis were already corrupted before they even entered the actual protocol layer. This means the issue is not mainly caused by the socket encoding itself, but by terminal input handling on Windows. A full fix for emoji input through the console was not achieved, but the source of the problem was narrowed down.

In addition, I created a new `AppMain` launcher class so that the executable `not-frantic.jar` can be used to start either the server or the client. This makes the packaged application more practical, because one JAR file can now serve as the unified entry point for both modes.

## Date: March 11, 2026

### Game Rules (Sevval)

#### What did we do today?

- The Game rules have been revised, officially established and uploaded. 
- Waiting for QA slides to work on.

#### Work Summary

I revised and improved the rule set for our game. The goal was to create a clearer and more structured set of rules so that players can easily understand how the game works.
First, the rules were reorganized into clear sections, including the game objective, scoring system, card types turn structure and special mechanics. This structure helps players quickly find important infos and understand the gameplay.
The objective of the game is for players to get rid of all the cards in their hand using their different cards strategically to affect other players. The game ends when player has no cards left or when a special event ends the game. The winner is determined by counting the points of the cards remaining in each players hand.
The rules describe different types of cards. Normal cards are played by matching color or number, while black cards trigger event cards that affect all players. Effect cards introduce special abilities that allow players to manipulate the game, such as exchanging hands or forcing others to draw cards.
Event cards ass unpredictable elements to the game. When triggered, they create situations that affect all players, such as reshuffling all hands or starting a final countdown of rounds.
Overall, the revised rules make the game easier to understand while keeping its strategic and chaotic gameplay.

#### Future Plans

I asked our tutor for the QA slides, as soon as we receive them I will start working on it.
Tomorrow I will go through our docs and see if we lack on milestones, if there are typos or any other errors to make sure its ready to hand in.

## Date: March 11, 2026

### Excercise session (Everyone)

#### What did we do today?

- We tested our server and clients

#### Work Summary

Aiysha was able to run the server while the rest was able to join. We were able to see the ping pongs,who joined the lobby and to send messages.

## Date: March 12, 2026

### "Cleaning" (Sevval)

#### What did we do today?

- Since QA task was postponed, I didn't start to work on it as planned.
- I went through all our uploads, adjusted some things, made sure there are no more typos/errors and checked that everything for the milestone is ready.


## Date: April 1, 2026

### JavaFX GUI Integration, Chat Functionality, Networking Debugging, and Command Input (Aiysha)

#### What did we do today?

Today, I focused on building the JavaFX GUI for the client and integrating it with the existing networking and protocol system.

#### Work Summary

- I implemented a JavaFX-based GUI structure consisting of:
  - a **ConnectView** for entering host, port, and username
  - a **LobbyView** for displaying players, chat, and game/info messages
  - a **MainController** to manage scene switching and user interaction
- I connected the GUI to the existing client logic via a new `FxNetworkClient`, ensuring that the GUI uses the same protocol and message system as the terminal client.

- I implemented full **chat functionality in the GUI**:
  - Incoming chat messages are displayed in real time using observable lists.
  - Outgoing messages are sent through the same protocol (`Message.Type.CHAT`) as in the terminal client.
  - The GUI updates automatically via JavaFX bindings and `Platform.runLater`.

- I ensured that **player list updates are reactive**:
  - The server now broadcasts updated player lists when players join, leave, or rename.
  - The GUI listens for `PLAYERS` messages and updates the list automatically.

- I debugged a critical issue where the **client instantly disconnected when connecting over the network**:
  - The problem was caused by the heartbeat system using an outdated `lastServerPongTime`.
  - Because it was not reset on connect, the client immediately assumed the connection timed out.
  - Fix: Reset the heartbeat timestamp during connection initialization.
  - After the fix, remote clients can connect reliably over LAN.

- I added a **command input field to the GUI**, allowing users to enter commands just like in the terminal client:
  - Commands such as `/name`, `/players`, `/start`, and `/quit` are parsed using `Message.parse(...)`.
  - The behavior now mirrors the terminal client exactly, including validation and error handling.
  - Special cases like heartbeat commands are blocked from manual input.
  - The `/quit` command properly disconnects the client and returns to the connect screen.

- I ensured that the GUI remains consistent with the architecture of the reference repositories:
  - Clear separation between UI, controller, state, and networking
  - Reuse of the same message protocol across both GUI and CLI clients

#### Next Steps

- Improve the **visual design of the GUI** (layout, spacing, styling)
- Make the GUI more intuitive and user-friendly
- Prepare the GUI structure so that **future game logic can be easily integrated**, especially:
  - rendering hands, cards, and the game board
  - handling turn-based interactions
  - visualizing game events and effects
- Possibly refactor UI components further to keep them modular and extensible

## Date: April 2, 2026 

### Multi-Lobby Support Implementation (Senanur)

#### What did we do today?
Today, I implemented support for multiple lobbies in the server. The goal was to allow players to create, join, and interact within separate game spaces instead of being in a single global lobby.

#### Work Summary
- Implemented **multi-lobby architecture** on the server side.
- Added a `Lobby` model to represent individual lobbies.
- Updated `ServerService` to manage multiple lobbies simultaneously.
- Enabled players to **create new lobbies and join existing ones**.
- Ensured that each lobby maintains its own set of players.
- Adjusted message handling so that actions are processed **within the correct lobby context** instead of globally.
- Improved internal structure to better support future features like game start per lobby.

#### Future Plans
- Test multi-lobby behavior with multiple clients to ensure proper isolation between lobbies.
- Add validation (e.g., prevent joining non-existing lobbies).
- Extend lobby functionality with features such as player lists and game start conditions.

## Date: April 2, 2026

### GUI Lobby System, Player Lists, and Multi-Channel Chat (Aiysha)

#### What did we do today?

Today, I worked mainly on the GUI and the communication features around lobbies and chat. The goal was to make the client feel more usable without relying only on terminal commands.

#### Work Summary

* I added GUI support for creating and joining lobbies, so lobbies can now be managed directly from the interface.
* I implemented lobby listing in the GUI and connected it to the server-side lobby data.
* I extended the player listing so that the client can now distinguish between players in the current lobby and all connected players on the server.
* I reworked the left GUI panel so lobby players and global players are shown in separate proper list panels instead of being mixed into one list.

I also expanded the chat system beyond the original single chat channel.

* I introduced separate global chat, lobby chat, and whisper chat message types in the protocol.
* I made global chat available independently of lobby membership.
* I added lobby chat so messages can be restricted to the current lobby.
* I implemented whisper chat for direct private messages between individual players.
* I updated the GUI chat area so it can switch between global, lobby, and whisper chat views.

In addition, I fixed several smaller synchronization issues between GUI and server state.

* I fixed disconnect handling so old chat and lobby data no longer remain visible after reconnecting.
* I fixed stale player list updates by making sure global player lists are refreshed after renames and connection changes.
* I also improved the handling of lobby/player list refreshes so GUI state now matches the server state more reliably.

## Date: April 5, 2026

### QA Concept (Sevval)

#### What did we do today?

The past days, I was working on our QA Concept, started with the "about the project" continued with our Coding Standards and moved on with Code Review and Versions Control.
The coming days, I will keep working on our QA Concept and add our measurements.

## Date: April 6, 2026 

### Game State Integration and UI Improvements (Senanur)

#### What did we do today?
Today, I focused on integrating the game state into the client and improving the game UI. The goal was to establish a basic structure for game flow and ensure that card data can be properly displayed in the interface.

#### Work Summary
- Added a **GameState** object to the `MainController` to manage the local game state.
- Implemented initialization of the local game state when the game starts.
- Added logic to **start and manage turns** within the game.
- Created a **game screen skeleton** as the foundation for the in-game UI.
- Updated and improved the **game view layout**.
- Implemented a helper method `cardToText` to convert `Card` objects into a readable format for UI display.
- Ensured that card data can be correctly visualized in the interface.

#### Future Plans
- Connect the game state more tightly with server updates (synchronization).
- Improve turn logic and enforce rules such as turn-based actions.
- Enhance UI with interactive elements (clickable cards, animations).
- Continue refining the overall game flow and user experience.

## Date: April 7, 2026

### Client Restructuring and GameView Rework (Aiysha)

#### What did we do today?

Today, I focused on restructuring the client architecture and improving the game UI. The main goal was to remove redundant client logic between CLI and GUI, unify the networking flow, and rework the `GameView` so it fits the rest of the application visually and starts displaying actual game data instead of only placeholders.

#### Work Summary

* Restructured the client logic to remove redundancy between the **CLI client** and the **GUI client**.
* Unified the client flow so communication now goes through **`ClientProtocolClient`**, **`NetworkClientCore`**, and **`ClientMain`** instead of having two mostly separate client implementations.
* Cleaned up the architecture so the shared client/network logic is reused more consistently.
* Reworked Sena’s **`GameView`** so it now uses the shared **CSS styling** like the other views.
* Improved the **GameView layout and presentation** so it is closer to the actual in-game interface.
* Started replacing placeholder-style display logic so the **game view reflects the game state more directly** instead of showing only static placeholders.

#### Future Plans

* Improve the layout of the **player list** and **Game Info** area, since it currently feels too cramped.
* Add a **Settings** option with features such as **dark mode** and later settings like **music volume**.
* Implement proper **effect resolution in the GUI**, since it currently still has to be done through the CLI.
* Investigate and fix the **effect resolution bug** where the player sometimes gets kicked while resolving an effect.
* Continue refining the overall in-game UI so it becomes clearer and more comfortable to use.

## Date: April 8, 2026

### QA (Sevval)

#### What did we do today?

Today I did the measurements with MetricsReloaded then started to work with Jacoco but the test gave me 0% so i tried to figure out where the problem is. I figured our Java 25 is not compatible with our Grandle, so I would need to change our Java version to 21-22 since Grandle doesn't work with Java 25, but I also don't know if we are allowed to change our Java version, so I messaged our tutor and now im waiting for a response.
Our QA concept is finished besides the Jacoco part. 

## Date: April 8, 2026 (Senanur)

### Lobby Constraints and Improved Lobby Display

#### What did we do today?
Today, I focused on improving lobby management by enforcing player limits and enhancing how lobby information is displayed on the client.

#### Work Summary
- Enforced a **maximum of 5 players per lobby** to prevent overfilling.
- Added a validation to require **at least 2 players to start a game**.
- Updated the server logic to reject invalid start attempts with appropriate feedback messages.
- Improved lobby handling to ensure that only valid game conditions are allowed.

- Implemented **formatted lobby list parsing** on the client side.
- Extracted and displayed important lobby information such as:
  - Lobby status (e.g., WAITING / PLAYING)
  - Current player count
- Improved the readability of the lobby list for better user experience.

#### Future Plans
- Prevent joining lobbies that are already full or currently playing.
- Add visual indicators (e.g., disabled buttons) in the UI for invalid actions.
- Extend lobby status handling (e.g., FINISHED state).

## Date: April 10, 2026 (Senanur)

### Lobby Join Restrictions and Status Handling

#### What did we do today?
Today, I worked on improving the lobby system by enforcing stricter join rules and refining how lobby states are handled and displayed in the UI.

#### Work Summary
- Prevented players from joining lobbies that are already in **PLAYING** state.
- Added restrictions to block joining lobbies that are **full (5/5 players)**.
- Ensured that invalid join attempts are properly handled and do not proceed silently.

- Improved lobby state handling by distinguishing between different states such as:
  - WAITING
  - PLAYING
  - FINISHED

- Began refining how lobby information is displayed in the UI:
  - Ensured correct parsing and display of lobby status and player count.
  - Prepared the structure for further UI improvements related to lobby visibility.

#### Future Plans
- Fully implement and display the **FINISHED state** for completed games.
- Prevent joining finished lobbies entirely.
- Improve UI feedback (e.g., disable join buttons instead of only blocking on server side).
- Continue refining synchronization between server state and UI.

## Date: April 11, 2026

### QA (Sevval)

#### What did we do today?
Since I thought our Java version doesn't work with javacoco, I messaged our tutor and we are in touch trying to solve this problem. 
QA Concept is missing just the javacoco part and I'll revise the project plan soon since we didn't get the points for it on MS1.
Our QA report is finished and will be uploaded. 
For our Project plan I decided to redo it tomorrow and finish it.
Here is the updated diary entry with the work from this chat added:

## Date: April 12, 2026 (Aiysha)

### Bug Fixing, GUI Styling Cleanup, and Manual Writing

#### What did we do today?

Today, I worked on cleaning up several parts of the project that were still rough around the edges. The focus was on fixing gameplay-related bugs, improving the visual consistency of the GUI, writing a proper manual so someone can receive the JAR and understand how to run and play the game, and debugging several edge cases in card validation and server-side effect handling.

#### Work Summary

* I fixed and investigated several gameplay bugs, especially around effect resolution and dev mode scenarios.
* I improved the dev mode setup so scenario configuration works more reliably and is less dependent on exact player names.
* I worked on the launcher/client flow so the GUI client starts through the new `client` mode and can open with host, port, and username already prefilled.
* I checked and adjusted the GUI scroll pane styling so the scroll areas no longer show mismatched default JavaFX backgrounds and instead follow the CSS theme properly.
* I cleaned up documentation-related inconsistencies, especially around the current protocol and command behavior.
* I wrote and refined the game manual so it now explains how to start the server, connect with the GUI, create and join lobbies, start a game, understand turns, resolve card effects, read the game log, and understand scoring.
* I also added clearer explanations for special-card commands, card IDs, round-end output, and how match scoring works.
* I investigated a bug where invalid effect targets such as `/gift 82 81` caused the player to be disconnected because the server treated the invalid target as an uncaught exception instead of a normal game error.
* I adjusted the server-side effect-response handling so invalid effect arguments now return an `ERROR` message to the player instead of terminating the client session.
* I found and fixed a card-validation bug where a `Fantastic` number request incorrectly allowed colored special cards like `Yellow Exchange` to be played even though only cards matching the requested number should be valid.
* I added and planned test scenarios for request-based card validation, including requested color, requested number, same-symbol matching for special cards, black-card edge cases, and invalid Gift/Exchange targets.
* I investigated black-card event handling and clarified that event effects are currently resolved automatically by the server rather than through manual slash commands.
* I found a server flow bug where automatic event resolution, especially `CANCEL_EFFECTS`, could leave the game stuck in `RESOLVING_EFFECT`, which then prevented normal actions like drawing a card.
* I updated the `handlePlayCard` logic so that after automatic event resolution, the game correctly falls back to `AWAITING_PLAY` when nothing is left to resolve, instead of remaining stuck in the wrong phase.
* I also reviewed the general phase follow-up logic after card plays and effect resolution so the turn flow is now more consistent and less likely to get stuck in edge cases.

#### Future Plans

* Continue fixing remaining gameplay bugs, especially edge cases in effect handling, turn transitions, and GUI updates.
* Improve the Game View further so game-state changes are reflected even more clearly without relying on manual refresh commands.
* Keep the external documentation in sync with the current source code, especially the protocol, manual files, and effect-resolution behavior.
* Continue polishing usability so the project feels more complete and easier to understand for someone testing it for the first time.
* Add more targeted tests and dev-mode scenarios for tricky interactions involving black cards, event effects, requested colors/numbers, and chained effect resolution.

## Date: April 12, 2026

### QA (Sevval)

#### What did we do today?
Today I redid our project plan and did a gantt chart aswell. 
I uploaded the project plan and our qa concept.

## Date: April 16, 2026
### Win Endscreen (Sevval, Aiysha)
We created a new view GameEndView to display the end score and tested if it works, which it did. 
We figured out that something might be wrong with the /cheatwin command.

#### Future Plans
* Connect the score with persistent High Score storing. It should store the High Score in a txt file.
* Figure out a way to display the resolve effect and the layout of the game.
* Figure out what is wrong with the /cheatwin command