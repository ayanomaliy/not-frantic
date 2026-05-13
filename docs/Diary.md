__# Project Diary

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

## What did we do today?

- The game rules were revised, improved, and officially finalized.
- The updated rules were uploaded.
- Currently waiting for the QA slides in order to continue with the next tasks.

## Work Summary

Today, I focused on revising and improving the rule set for our game. The main goal was to create a clearer, more structured, and professional version of the rules so that new players can easily understand how the game works.

### Rule Structure

First, the rules were reorganized into clearly defined sections, including:

- **Game Objective**
- **Scoring System**
- **Card Types**
- **Turn Structure**
- **Special Mechanics**

This new structure helps players quickly find important information and makes the gameplay easier to learn.

### Game Objective

The objective of the game is to get rid of all cards in your hand before the other players. Players must use their cards strategically in order to influence the game and affect their opponents.

The game ends when:

- A player has no cards left, or
- A special event card triggers the end of the game.

The winner is determined by counting the total points of the remaining cards in each player's hand. The player with the lowest number of points wins.

### Card Types

The revised rules explain the different card categories in detail:

#### Normal Cards
- Can be played by matching either the **color** or the **number** of the top card on the discard pile.

#### Black Cards
- Trigger **event cards** that affect all players.

#### Effect Cards
- Provide special abilities that allow players to manipulate the game.
- Examples include:
  - Exchanging hands with another player
  - Forcing opponents to draw cards
  - Changing the direction of play

### Event Cards

Event cards add unpredictable and chaotic elements to the game. When triggered, they create situations that impact every player. Examples include:

- Reshuffling all players’ hands
- Starting a final countdown of rounds
- Random global effects that change the flow of the game

### Conclusion

Overall, the revised rules make the game much easier to understand while preserving its strategic, competitive, and chaotic gameplay style. The improved formatting and structure also make the rules more professional and user-friendly.

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

#### Futrue Plans

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

## What did we do today?

- During the past few days, I worked on our **QA Concept**.
- I started with the **About the Project** section.
- After that, I continued with our **Coding Standards**.
- I also worked on the **Code Review** and **Version Control** sections.
- In the coming days, I will continue working on the QA Concept and add the required measurements.

## Work Summary

Over the past few days, I focused on developing and expanding our **QA Concept** for the project. The goal of this document is to define clear quality assurance processes, coding rules, and development standards to ensure that our project is well-structured, maintainable, and professionally organized.

### About the Project

I began by writing the **About the Project** section. This part introduces the project, explains its purpose, and provides a general overview of what we are developing. It serves as the foundation of the QA document and helps readers understand the context of the project.

### Coding Standards

After that, I continued with the **Coding Standards** section. In this part, I documented the rules and conventions we want to follow while writing code. These standards help keep the code consistent, readable, and easier to maintain. They also improve teamwork, since all developers follow the same structure and style.

### Code Review

Another important section I worked on was **Code Review**. This describes how changes in the code should be checked before being merged into the project. Code reviews help identify bugs, improve code quality, and encourage collaboration between team members.

### Version Control

I also added the **Version Control** section. This explains how we use version control tools such as Git to manage project progress, track changes, and collaborate safely. Proper version control is essential for organized teamwork and reliable development workflows.

## Future Plans

In the coming days, I will continue working on the **QA Concept** and complete the remaining sections. The next focus will be adding the required **measurements and metrics**, which are important for evaluating code quality and technical progress.

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

### QA / Code Coverage (Sevval)

## What did I do today?

- I completed the measurements using **MetricsReloaded**.
- I started working with **JaCoCo** for test coverage analysis.
- The generated report showed **0% coverage**, so I began investigating the issue.
- I identified a possible compatibility problem between **Java 25** and **Gradle**.
- I contacted our tutor for clarification and I am currently waiting for a response.
- Our **QA concept** is finished except for the JaCoCo integration part.

## What did I do today?

Today, I focused on the quality assurance and code analysis section of our project.

### MetricsReloaded

First, I completed the required software measurements using **MetricsReloaded**. These metrics help us analyze the structure and quality of the codebase, for example complexity, maintainability, and other important indicators. This step is important for evaluating the technical quality of our project.

### JaCoCo Test Coverage

After that, I started working with **JaCoCo**, which we use to measure automated test coverage. The goal was to generate a report showing how much of our code is tested by unit tests.

However, after running the tests, the report displayed **0% coverage**, which indicated that something was not working correctly. Because of this, I began troubleshooting the setup and checking possible causes.

### Problem Analysis

After investigating the issue, I found that the most likely reason is a **compatibility conflict between Java 25 and Gradle**. Our current Gradle version appears not to fully support Java 25, which may prevent JaCoCo from running correctly.

A possible solution would be to switch the project to **Java 21 or Java 22**, since these versions are generally more stable and commonly supported by Gradle tools. However, before making such a change, I wanted to confirm whether we are allowed to modify the Java version for the project.

## Work Summary
At the moment, our **QA concept is fully completed except for the JaCoCo coverage part**. Once the compatibility issue is resolved, the final section can be finished quickly.

### Future Pans

To clarify this, I contacted our tutor and am currently waiting for a response. Once I receive feedback, I can continue with the JaCoCo setup and finalize the remaining QA section.

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

### QA / Project Planning (Sevval)

## What did we do today?

- I contacted our tutor because I suspected that our current Java version is causing problems with **JaCoCo**.
- We are currently in contact and working on a solution for the issue.
- The **QA Concept** is almost complete and only the JaCoCo section is still missing.
- Our **QA Report** is finished and will be uploaded soon.
- I also planned to revise the **Project Plan**, since we did not receive the points for it in **MS1**.
- I decided to fully redo the Project Plan tomorrow and complete it.

## What did I do today?

Today, I mainly focused on the quality assurance documentation and project organization.

### JaCoCo / Java Compatibility Issue

Since I believed that our current Java version might not be compatible with **JaCoCo**, I contacted our tutor to clarify the problem. We are now in communication and trying to find the best solution so that the coverage tool can work correctly. Resolving this issue is important because the final missing part of our QA Concept depends on the successful JaCoCo integration.

### QA Concept

The **QA Concept** is nearly finished. At the moment, only the JaCoCo coverage section is still missing. Once the technical issue has been solved, this remaining part can be completed quickly.

### QA Report

In addition, the **QA Report** has already been completed. It is ready for submission and will be uploaded shortly. This means that the majority of our quality assurance documentation is already finalized.

### Project Plan Revision

Another important task was reviewing our **Project Plan**. Since we did not receive the points for it in **MS1**, I decided that it would be better to revise and improve it completely instead of only making small changes.

My plan is to redo the Project Plan tomorrow and finish a stronger, clearer, and more professional version that better meets the requirements.

### Work Summary

Overall, most of the QA work is completed, the report is ready, and the remaining focus is on solving the JaCoCo issue and improving the Project Plan.

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

## What did I do today?

- I completely revised and recreated our **Project Plan**.
- I also created a new **Gantt Chart** to improve the planning and scheduling of the project.
- I uploaded the finished **Project Plan**.
- I uploaded our completed **QA Concept** as well.
- In general, I focused on improving the organization and documentation of our project.

### Project Plan Revision

One of the main tasks was to redo our **Project Plan**. The goal was to create a clearer, more structured, and more professional version that better reflects the current progress of the project and meets the required standards. I reviewed the previous version, improved the content, and reorganized the planning details.

### Gantt Chart Creation

In addition, I created a **Gantt Chart** to visually represent the timeline of the project. This chart helps to organize tasks, deadlines, milestones, and responsibilities more effectively. It also provides a better overview of the project schedule and makes progress tracking easier.

### Uploads and Documentation

After completing the revisions, I uploaded the updated **Project Plan**. I also uploaded our finished **QA Concept**, which means an important part of our quality assurance documentation is now completed and submitted.

### Work Summary

Overall, today’s work helped improve both the planning structure and the project documentation. With the updated project plan, new Gantt chart, and uploaded QA documents, our organizational work is now in a much stronger state.

## Date: April 16, 2026

### Win Endscreen (Sevval, Aiysha)

## What did we do today?

- We created a new view called **GameEndView**.
- The new view displays the final score at the end of the game.
- We tested the feature successfully and confirmed that it works.
- We also discovered that there may be an issue with the **/cheatwin** command.

## Work Summary

Today, we focused on improving the endgame experience by creating a dedicated win endscreen for the game.

### GameEndView Implementation

We developed a new screen called **GameEndView**, which is shown when the game ends. The purpose of this view is to present the final results in a clear and user-friendly way. It currently displays the end score so players can immediately see the outcome of the match once the game is finished.

Creating a separate endscreen improves the overall structure of the game, since the transition from gameplay to results now feels more complete and professional. It also gives us a good foundation for adding more endgame features later.

### Testing

After implementing the new view, we tested it to ensure that it opens correctly and displays the score as intended. The tests were successful, and the GameEndView is functioning properly at the moment.

### /cheatwin Command Issue

During testing, we noticed that the **/cheatwin** command does not seem to work correctly. Since this command is useful for triggering an instant win state during development and testing, we identified it as an issue that needs to be investigated further.

## Future Plans

### High Score System

- Connect the final score with a persistent **High Score** system.
- Store the highest score in a **.txt file** so it remains saved after closing the game.

### Effects and Layout

- Find a better way to display the **resolve effect**.
- Improve the overall **game layout** and visual presentation.

### /cheatwin Debugging

- Investigate what is causing the **/cheatwin** command to fail.
- Fix the issue so it can be used again for testing purposes.

## Date: April 21, 2026

### Win Endscreen (Sevval, Aiysha)

## What did we do today?

- We improved the **win endscreen system**.
- We fixed the **/cheatwin** command so it now works correctly.
- We created a **HighScoreHistory** list for the **High Score** achievement.
- We expanded the achievement system with score tracking features.

## Work Summary

During this work session, we focused on improving the win endscreen system and adding new achievement-related features to the game.

### /cheatwin Command Fix

One of the main tasks was fixing the **/cheatwin** command. Previously, this command did not function correctly or did not trigger the intended result. We reviewed the code, identified the issues, and corrected the logic so that the command now works properly.

When used, **/cheatwin** successfully activates the win condition and immediately displays the win endscreen. This is especially useful for development and testing purposes, since it allows us to quickly verify whether the endscreen, rewards, and related systems are functioning correctly without needing to finish a full game round.

### HighScoreHistory Achievement System

In addition, we worked on the achievement system by creating a **HighScoreHistory** list for the **High Score** achievement. This list is used to store and track players’ previous scores over time.

By saving score history, the system can now compare current results with earlier performances and determine whether a player has reached a new personal best or unlocked the achievement. This creates a stronger sense of progression and gives players additional motivation to improve their results.

### Conclusion

Overall, this session improved both the technical testing tools and the long-term replay value of the game. The fixed cheat command simplifies development testing, while the new score history system makes progression more rewarding for players.

## Future Plans

- Fix and improve our **unit tests**.
- Add **custom sound effects** for special cards, events, and actions.

## Date: April 22, 2026 (Senanur)

### Test Fixing and UI Adaptation

#### What did we do today?
Updated failing tests after UI refactor from draw button to draw pile.

#### Work Summary
- Fixed build failure caused by removed `getDrawButton()`.
- Updated **GameViewTest** to use `getDrawPilePane()`.
- Replaced button checks with pane style checks.
- Updated **MainControllerTest** to simulate mouse click on draw pile.
- Ensured draw action still triggers `drawCard()` correctly.
- Build now passes successfully.

#### Future Plans
- Review remaining tests for UI consistency.
- Improve test robustness for UI changes.
- Increase coverage for user interactions.

## Date: April 22, 2026

### GUI (Sevval, Aiysha)

## What did we do today?

- We improved the graphical user interface (**GUI**) of the game.
- We redesigned and fully integrated the **draw pile** and **discard pile**.
- We removed the old **Draw Card** button because drawing now works directly in the GUI.
- We added a **blur effect** with the message **“It is your turn.”**
- We implemented a custom sound effect for the **“F you”** card.

## Work Summary

During this work phase, we focused intensively on improving the **GUI** of our game and implemented several important upgrades.

Overall, we were able to significantly improve both the **functionality** and the **design** of our game. The draw and discard piles are now fully integrated into the GUI, unnecessary buttons were removed, new visual effects were added, and special sounds make the game feel more immersive and engaging.

### Draw Pile & Discard Pile

One of the main tasks was working on the **draw pile** and the **discard pile**. At the beginning, both piles were only displayed as white placeholders and were not properly integrated into the GUI. This meant that they were neither visually appealing nor functionally connected to the game system.

As a first step, we redesigned the appearance of both piles. We adjusted their **size, position, and overall layout** so that they fit better into the game board and matched the rest of the interface.

After that, we programmed the full functionality of both piles directly into the GUI:

- The **draw pile** now allows players to draw cards directly by clicking on the pile.
- The **discard pile** correctly displays and updates the played cards.

Because of this improvement, we were also able to remove the previous **Draw Card** button, since drawing cards now works directly through the GUI. This makes the gameplay more intuitive and realistic.

### Visual Effects

Besides working on the card piles, we also focused on visual effects to improve the overall player experience.

We implemented a **blur effect** together with the message **“It is your turn.”** Whenever it is a player’s turn:

- The background becomes slightly blurred.
- The message is highlighted on the screen.

This makes it immediately clear whose turn it is and improves the clarity of the game flow. At the same time, it gives the game a more modern and polished look.

### Sound Effects

In addition, we added new sound effects for special game actions.

For the **“F you”** card, we created and implemented a unique sound effect. Whenever this card is played, the special sound is triggered, giving the card more impact and making the gameplay more dynamic and entertaining.

## Future Plans

### Effect Card Notifications

Our future plans are to continue improving the visual effects. For the **effect cards**, we want to implement a blur effect combined with a message display, similar to the **“It is your turn”** notification.

Whenever a special effect card is played:

- The background should blur.
- A fitting message should appear on the screen.
- The card’s action and impact should be clearly shown.

This will make the gameplay more interactive, easier to understand, and visually more appealing.

### Final GUI Polish

In addition, we plan to fully complete and polish the GUI. This includes:

- Refining the layout
- Improving design consistency
- Adjusting remaining elements
- Ensuring all game features are smoothly integrated

Our goal is to create a clean, user-friendly, and professional-looking game experience.  
## Date: April 22, 2026

### Effect Response GUI for Special Cards (Aiysha)

Today, I worked on expanding the JavaFX GUI so that special card effects no longer have to be resolved only through slash commands. I implemented dedicated effect response views for Fantastic, Equality, and Fantastic Four, all using the same visual theme and overlay style on top of the existing `GameView`. I also added the necessary wiring between `MainController`, `FxNetworkClient`, and `ClientProtocolClient` so that the GUI can send structured effect responses directly instead of relying on manual text input.

In addition, I fixed several issues that came up during testing. I corrected how effect requests are targeted so that only the intended player sees the popup. I also added filtering so that players cannot select themselves in the Fantastic Four and Equality GUIs. On the server side, I tightened validation for Fantastic Four so that illegal self-targeting is rejected even if a client tries to send it anyway. Another bug I fixed was that hand updates were sometimes missing after event resolution, which caused the GUI to show stale cards until another action happened. I also improved the handling of Second Chance so that cards in hand and the draw pile can now be clicked directly during effect resolution instead of forcing command-line input.

## Date: April 23, 2026 (Denys)

Documenting what I did a few days before. I created the asset-config file that allows you to assign colors, sounds and icons to cards, events, changes in game state e.t.c. Additonally created AssetConfig, AssetConfigLoader, AssetRegistry, SoundManager that all allow you to load the settings form asset-config into the JavaFX GUI. 

#### Future Plans

Next, I want to continue adding GUI support for the remaining special effects such as Gift, Exchange, Skip, Nice Try, and Counterattack, so that all effect resolution can eventually be done fully through the GUI.

## Date: April 23, 2026

### Testing and Bug fixes (Sevval, Aiysha)
Today we tested the new GUI and the effect response views. We found some bugs regarding the end turn logic, as you were able to end the turn without drawing a card but we quickly fixed it.

## Date: April 23, 2026

### Test Stabilization and Game Logic Refinement (Senanur)

#### What did we do today?
Today, I focused on stabilizing the test suite and aligning existing tests with the current game logic. The goal was to ensure consistency between implementation and expected behavior, especially for turn handling and game rules.

#### Work Summary
- Updated multiple tests to reflect the **current game logic and protocol behavior**.
- Fixed inconsistencies between implementation and outdated test expectations.

- Refined **turn handling logic**:
  - Updated tests to enforce that a player must **perform an action (draw or play)** before ending their turn.
  - Adjusted `TurnEngine` tests accordingly.

- Improved **game rule validation**:
  - Fixed tests related to special rules such as **counterattack** and **"fantastic four"** behavior.
  - Ensured correct handling of the **current acting player** in edge cases (e.g., NICE_TRY scenario).

- Updated **protocol and serialization tests**:
  - Adapted `ProtocolIntegrationTest` to match updated **effect response parsing rules**.
  - Fixed `GameStateSerializer` tests to reflect the **new payload format**.

- Updated **UI-related tests**:
  - Modified `MainController` tests to use the **draw pile interaction** instead of the removed draw button.
  - Adjusted `FxNetworkClient` tests for correctly formatted **top card labels**.

- Cleaned up the test suite:
  - Removed **brittle tests** that depended on UI or assets.
  - Improved overall test reliability and maintainability.

## Date April 24, 2026 (Denys)

Yesterday I rewoked the GUI to visually display the players as if they are sitting at a table. This involves color assigment, a function calculating player seat locations, new graphics (object, areas and svg loaded onto them). Today I merged all this onto main, resolving all the conflicts (naturally, in my favor).

#### Future Plans
- Continue ensuring that all tests reflect the actual game logic and remain stable over time.
- Add additional edge case tests for turn handling and special card effects.
- Strengthen synchronization between server logic and client-side state.


## Date: April 24, 2026

### Final GUI Polish, High Score Integration, and Endgame / Rule Bug Fixes (Aiysha)

#### What did we do today?

Today, I focused on final polishing for the milestone hand-in. The main areas were fixing remaining gameplay edge cases, improving the circular in-game table layout, integrating a persistent global high score display into the endscreen, and making sure the GUI now covers as much gameplay interaction as possible without relying on commands.

#### Work Summary

* Fixed several turn-flow and rule-enforcement edge cases, especially around ending turns, draw/play requirements, and black-card / event interactions.
* Investigated a bug where certain request states after black-card events could leave the discard pile visually unclear and make follow-up plays effectively invalid.
* Confirmed that the discard pile rendering itself updates correctly from `topCardId`, `requestedColor`, and `requestedNumber`, and that the real problem was invalid server-side request state rather than missing GUI refresh logic.
* Reviewed the `WILD_REQUEST` event behavior and clarified that, in the current design, it should be resolved automatically by the server instead of opening a Fantastic-style chooser in the GUI.
* Prepared and discussed a dev-mode extension to force a specific event card for black-card testing, so problematic event interactions such as `WILD_REQUEST` can be reproduced more reliably.
* Reworked the circular table layout for opponent players so the visual seating arrangement looks more natural and no longer clashes as much with the draw pile, discard pile, and the center controls.
* Adjusted opponent placement logic, opponent hand size, spacing, and card scaling so the table view is cleaner and easier to read.
* Improved hover behavior and layering of local hand cards, while also making sure hover effects are disabled where they caused problems, such as in effect-response views and on discard rendering.
* Moved and refined the center game controls so draw pile, discard pile, and End Turn no longer visually interfere as much with player hands.
* Added a persistent global leaderboard to the win endscreen in addition to the local round/game ranking.
* Completed `HighScoreHistory` so stored game results are not only appended to file but can also be read back and aggregated into a leaderboard.
* Extended the `GameEndView` so it now shows both the current game result and previously stored high scores side by side.
* Fixed a mistake during this integration where the endscreen class was temporarily overwritten with the wrong code and restored the correct `GameEndView`.
* Improved the layout of the endscreen so the result panels are centered properly instead of being pushed to the left.
* Reviewed the current milestone achievements and assessed which ones are likely fulfilled based on the present source state and GUI completeness.

## Date: May 1, 2026

Reworked the round lifecycle, since the game was incorrectly ending after the first round. Split the round-end logic into clear phases: compute round scores, check if the game is over, otherwise wait for an explicit next-round trigger. Added proper game termination handling and ensured cumulative scores persist across rounds.

Improved robustness around player disconnects so the game no longer stalls if someone leaves mid-turn. Introduced new message types (NEXT_ROUND, START_NEXT_ROUND) to separate initial game start from subsequent rounds and give players control over when to continue.

Extended the game state with a roundNumber and propagated it through serialization and client state, enabling the UI to reactively display the current round. Updated the initializer to carry over scores and reorder players accordingly.

On the client side, added handling for round transitions, including listeners and a new round-results overlay that shows scores and lets players proceed or exit. Integrated this into the main controller and game view.

Rounded it off with targeted tests covering round transitions, scoring edge cases, state serialization, and a full multi-round flow.

#### Future Plans

* Run one final full end-to-end GUI test before presentation.
* Keep the remaining focus on stability rather than new features.
* If time allows, clean up any last small UI inconsistencies and verify Linux/JAR behavior once more.

## Date: May 6, 2026

### QA Concept(Sevval)

#### What did we do today?

Today, I added the MS 5 metrics to our QA Concept. I will wait for the MS 6 Metrics closer to the end of the project and then finish the QA Concept adding the analyses and the lessons learned.

#### Future Plans

Tomorrow, Aiysha and I will meet again and work together on the project, trying to finish MS 6 tasks + bonuses.

## Date: May 7, 2026

### Game Development Update (Sevval, Aiysha)

## What did we do today?

* We updated and officially uploaded the **game rules**.
* We started looking for a **bug tracker** (to be continued tomorrow).
* We tried to figure out the **CIA task** and its requirements.
* We improved the **GUI design** by removing the background table to make it more attractive.
* We added **card animations** for the milestone system.
* We sent an email to our tutor regarding several **milestone tasks** and are currently waiting for a response.

## Work Summary

During today’s session, we focused on improving both the structure of the project documentation and the visual and functional aspects of the game.

### Game Rules Update

One of the main tasks was updating the official **game rules**. After reviewing and refining them, we uploaded the final version so that the current gameplay mechanics are clearly documented and accessible. This helps ensure consistency across development and testing.

### Bug Tracker Research

We also started researching possible solutions for implementing a **bug tracker**. The goal is to find a suitable system that allows us to efficiently log, organize, and monitor issues during development. This task is not yet completed and will continue tomorrow.

### CIA Task Investigation

Another focus point was trying to understand the requirements of the **CIA task**. We went through the task details and discussed possible interpretations, but further clarification is still needed before implementation can begin.

### GUI Improvements

We made noticeable improvements to the **graphical user interface** by removing the background table. This change made the interface cleaner and more visually appealing. Additionally, we enhanced the overall presentation of the game.

### Card Animations for Milestones

We also implemented **animations for cards related to milestone achievements**. These animations make interactions more dynamic and improve the overall user experience by providing better visual feedback when milestones are reached.

### Communication with Tutor

Finally, we sent an email to our tutor asking for clarification and feedback regarding several **milestone-related tasks**. We are currently waiting for a response, which will also be part of tomorrow’s workflow.

## Conclusion

Today’s work focused heavily on improving structure, visuals, and planning. While some tasks are still in progress (like the bug tracker and CIA task), the project’s interface and documentation have become more polished and organized.

## Future Plans

We will meet tomorrow and try to finish these tasks:

* Find and implement a **bug tracker system**.
* Continue working on and clarifying the **CIA task**.
* Follow up on the **tutor’s email response**.
* Further improve **GUI polish and animations**.

## Date: May 9, 2026

### Resolution-Independent Fullscreen Experiment (Senanur)

## What did I do today?

* Created a separate feature branch for the fullscreen and responsive layout work.
* Started implementing **resolution-independent fullscreen support** for the JavaFX GUI.
* Added an **F11 fullscreen toggle** instead of starting the game directly in fullscreen.
* Improved the connect screen layout and centered the connect panel.
* Adjusted the initial window size so the application opens in a usable windowed mode.
* Tested fullscreen behavior during screen transitions such as:
  * Connect → Lobby,
  * Lobby → Game,
  * Game → Lobby.
* Improved parts of the `GameView` layout for different screen sizes.
* Added dynamic card scaling so cards become smaller on small screens and larger on bigger screens.
* Adjusted the hand card fan layout to reduce overlap and prevent cards from covering the **End Turn** button.
* Tested the layout in both windowed mode and fullscreen mode.
* Decided to stop working on the fullscreen implementation for now because it took more time than expected.

## Work Summary

Today I worked on making the JavaFX GUI more responsive and experimented with resolution-independent fullscreen mode. The first goal was to make the game usable on different screen sizes and allow the user to switch to fullscreen with **F11**.

I improved the connect screen, adjusted the window startup behavior, and worked on the game screen layout. I also added dynamic scaling for the hand cards so that they adapt better when the window size changes. This improved the overall appearance of the game view, especially in fullscreen and smaller window sizes.

However, the fullscreen and fully resolution-independent layout turned out to be more complex than expected. Some parts of the UI still need additional polishing, especially spacing, font scaling, and edge cases during screen transitions. Because this feature was taking too much time, I decided to pause it for now.

## Conclusion

The fullscreen and responsive layout experiment made good progress, especially for the connect screen and game card scaling. However, the implementation is not fully finished yet. I decided not to continue working on this feature for now, so I can focus on other project priorities. If there is enough time later, I may continue improving and finalizing the fullscreen mode.

## Future Plans

* Continue fullscreen polishing if time allows.
* Improve responsive layout behavior in `GameView` and `LobbyView`.
* Test the GUI on different screen sizes and resolutions.
* Improve dynamic font scaling.
* Clean up unused experimental code if the feature is not continued.
* Decide later whether the fullscreen branch should be merged or discarded.

## Date: May 9, 2026

### GUI Polish, Animations, and Visual Identity (Aiysha)

#### What did I do today?

* Improved the JavaFX game GUI to make the table layout cleaner and more modern.
* Reworked the card animations for:

  * drawing cards,
  * playing cards to the discard pile,
  * hidden card transfers between players,
  * opponent draw animations.
* Added smoother scaling and movement transitions using `TranslateTransition`, `ScaleTransition`, and `ParallelTransition`.
* Improved synchronization between animations and state updates to avoid visual glitches during gameplay.
* Refined hand rendering and fan layout positioning for the local player.
* Improved discard pile rendering for:

  * normal cards,
  * color wishes,
  * number wishes.
* Added turn overlay effects and event banner overlays for better game feedback.
* Implemented visual effect dialogs for special cards such as:

  * Fantastic,
  * Fantastic Four,
  * Gift,
  * Exchange,
  * Skip,
  * Nice Try,
  * Counterattack,
  * Equality.
* Began replacing old text-heavy game state indicators (“Current Player”, “Phase”, “Top Card”) with cleaner visual feedback systems.
* Planned a new active-player highlighting system using glowing contours around player icons instead of plain text labels.
* Designed a new futuristic logo style for *Frantic^-1* matching the GUI color palette.
* Created multiple logo concepts including:

  * a main title logo,
  * a simplified icon/logo for card backs,
  * a “^-1” based emblem referencing the “not frantic” joke.
* Refined the logo style to use more dynamic circular motion effects and neon gradients matching the current CSS theme.

#### Future Plans

* Replace remaining debug-like UI labels with cleaner visual indicators.
* Implement animated active-player highlighting around opponent icons.
* Further refine animation timing and easing curves.
* Improve responsiveness and scaling for different screen sizes.
* Integrate the new logo assets into:

  * card backs,
  * menus,
  * loading/game screens.
* Continue polishing the overall visual consistency of the GUI.

## Date: May 9, 2026

### Background Music Production and Audio Settings (Aiysha)

## What did I do today?

Today, I produced a background track for the game in FL Studio. At first, I wanted to create something softer with pretty chords, delay-heavy arpeggios, and a lot of reverb. However, after testing the idea, I realized that this did not fit the playful and chaotic feeling of the game.

Because of that, I researched how companies like Nintendo approach background music in well-known Wii games. I focused on how their tracks often stay light, loopable, and memorable without becoming too distracting during gameplay. I then applied these ideas to my own track.

The track is built around a G minor / modal minor harmonic centre, using a progression similar to Gm7, Fadd9/A, Gm7, and F7. This gives the music a bright but slightly mischievous feeling. The rhythm is in 4/4 at around 124 BPM, with short chord stabs and longer responses, creating a bouncy call-and-response groove. I wanted the music to feel uplifting and energetic, but still simple enough to work as background music during the game.

The track uses a modal minor harmonic colour, short rhythmic chord stabs, and a loop-friendly call-and-response structure to create an uplifting and bouncy background atmosphere without overpowering the gameplay.


I made the sounds myself using the Serum and Nexus synthesizer plugins instead of only using finished loops. I shaped the synth sounds to make the track feel playful, clear, and game-like. The current version is not professionally mixed yet, but it already works as a recognizable background track for the project.

I also integrated music settings so the background music can be made quieter. This was important because even a catchy track can become annoying if it is too loud during longer gameplay sessions.

## Future Plans:
If I have enough time, I want to properly mix the background track further so the instruments, volume balance, and overall sound become cleaner. I also want to work together with Sevval to create more custom sound effects for the game. Our goal is to make the audio, visuals, logos, and overall game identity ourselves, instead of stealing assets or relying on AI-generated material.

## Date: May 9, 2026

### Card Design & Lobby Feature Updates (Sevval, Aiysha)

## What did we do today?

* We added a **Change Name** button in the **lobby**.
* We added **white numbers** on the **black cards** to improve visibility.
* We removed the **small numbers** on the **special cards** because they were confusing.
* We changed the names of our **F U cards** back to its **original name**.

## Work Summary

During today’s work session, we focused on improving the usability, clarity, and overall player experience of the game interface.

### Lobby Name Change Feature

One of the main tasks today was adding a **Change Name** button in the lobby. This new feature allows players to easily update their displayed name before starting or joining a game. This improves flexibility and makes the lobby experience more user-friendly.

### Card Visibility Improvements

We also improved the visibility of the cards by adding **white numbers** on the **black cards**. Previously, the numbers were harder to recognize during gameplay, so this change makes card values much easier to read and improves accessibility for players.

Got it—here’s the corrected version of that section:

### Special Card Design Changes

Another important update was removing the **small numbers** displayed on the **special cards**. These numbers caused confusion during gameplay because, unlike the other cards where the number represents the actual card value, the numbers on the special cards represented the **score** of the card instead. This often led to misunderstandings for players. By removing them, the card design is now clearer, more consistent, and easier to understand.

### Card Name Adjustments

We also changed the names of our **F U cards** back to its **original name**. This helps maintain consistency with the intended game design and makes the cards easier for players to recognize and remember.

### Conclusion

Overall, today’s session focused on improving the user interface and reducing confusion during gameplay. The new lobby feature and the visual improvements to the cards make the game more intuitive and polished.

## Future Plans

* Add a **Settings** button for adjusting **sound**.
* Find and implement a **bug tracker system**.
* Create our own custom sound effects and audio for the game.
* Remove an unnecessary or unbalanced **card** from the game.

## Date: May 10, 2026

### Spectator Mode and Reconnect Improvements (Senanur)

## What did I do today?

* Implemented a new **spectator mode** for active games.
* Added a **Spectate Lobby** button to the GUI.
* Made spectators automatically follow the **current player’s perspective**.
* Allowed spectators to see the current player’s hand while preventing gameplay actions.
* Blocked spectators from:
  * drawing cards,
  * playing cards,
  * ending turns,
  * resolving effects.
* Added server-side spectator handling and cleanup.
* Fixed spectator reconnect after connection loss.
* Fixed rejoining spectator mode after leaving a lobby.
* Updated the game UI so spectators see **“Watching Current Player”** instead of **“Your Hand”**.
* Fixed the lobby player list for spectators.

## Work Summary

Today I focused on implementing and polishing the spectator mode. Spectators can now join a running game from the GUI using the **Spectate Lobby** button. They do not participate as players, but they can watch the game from the perspective of the current player.

I also improved synchronization so spectators receive live game state, hand updates, and player list updates. Additionally, I fixed reconnect-related issues so spectator mode still works after a connection loss or after leaving and joining again.

## Conclusion

Spectator mode is now working through the GUI. It supports current-player perspective, reconnect handling, and prevents spectators from interacting with the game.

## Future Plans

* Improve spectator UI polish.
* Continue testing reconnect and lobby edge cases.

## Date: May 12, 2026

### Reconnect Timeout Handling (Senanur)

## What did I do today?

* Improved the reconnect handling for players who lose connection during an active game.
* Added a **60-second reconnect timeout** on the server side.
* Tracked disconnected players inside the lobby.
* Removed players automatically if they did not reconnect in time.
* Added logic so that:
  * games with more than two players continue,
  * 2-player games end automatically after one player times out.
* Updated the `GameState` so removed players are deleted from the active turn order.
* Fixed the current-player index after player removal.
* Tested the feature with simulated network loss.

## Work Summary

Today I improved the reconnect system for the multiplayer game. When a player disconnects during an active match, the server now gives them 60 seconds to reconnect. If the player reconnects in time, they can continue playing normally.

If the player does not reconnect within the timeout, they are removed from the game. In games with more than two players, the remaining players can continue. In 2-player games, the match ends automatically because the game cannot continue with only one player.

## Conclusion

The reconnect system is now more stable and prevents games from getting stuck when a player disconnects. The feature was tested successfully with both 2-player and 3-player scenarios.

## Date: May 12, 2026

### Sound Design and Audio Implementation (Sevval, Aiysha)

## What did we do today?

* We worked on the **sound design** for the game.
* We recorded and edited sounds for the **F% U card**.
* We recorded and edited sounds for **wrong card plays**.
* We recorded and edited sounds for the **draw pile** and **discard pile**.
* We added our **background music**, which was created earlier by Aiysha.
* We added a **start sound** for the game.
* We removed other **unnecessary sounds** to keep the audio experience cleaner.

## Work Summary

During today’s work session, we focused entirely on improving the audio experience of the game. Our main goal was to create custom sounds that make gameplay feel more interactive, immersive, and polished.

### Recording and Editing Sound Effects

One of the main tasks today was recording and editing several **custom sound effects** for important game actions. We created sounds for the **F% U card**, for situations where a player places the **wrong card**, and for interactions with both the **draw pile** and the **discard pile**.

After recording the sounds, we edited them to improve their quality and ensure they fit well into the overall atmosphere of the game. This helps create stronger feedback for player actions and makes gameplay feel more dynamic.

### Background Music and Start Sound

We also integrated our **background music**, which Aiysha had already produced in FL Studio a few days ago. Adding the background music gives the game more personality and helps create a more engaging playing experience.

In addition, we added a **start sound** that plays when the game begins. This creates a stronger first impression and makes the game feel more complete and professional.

### Audio Cleanup

Another important task was reviewing the existing sounds in the game and removing **unnecessary audio effects** that did not add much value. This cleanup helps keep the sound design focused and prevents the game from feeling overloaded with audio feedback.

### Conclusion

Overall, today’s session significantly improved the game’s audio design. By creating our own custom sound effects, adding background music, and cleaning up unnecessary sounds, the game now feels much more immersive and polished.

## Future Plans

* Finish editing sounds for the **special cards**.
* Add and finalize sounds for the **black cards**.
* Complete the remaining audio implementation so the **game’s sound design is fully finished**.

## Date: May 12, 2026

### Logo Sketching, Manual SVG Asset Creation and GUI Integration (Aiysha)

## What did I do today?

Today, I worked on the visual identity of the project and turned my own logo sketches from Clip Studio Paint into usable SVG assets. I also integrated these assets into the JavaFX GUI and solved several technical problems related to loading complex SVG files.

* I designed and refined the **card backside** for the game.

* I considered visual design principles such as:

  * symmetry,
  * contrast,
  * colour balance,
  * readability,
  * visual meaning,
  * and consistency with the game’s overall style.

* I made sure that the contrast between the **card background** and the **logo elements** is high enough for readability, so the design remains visible even at smaller sizes.

* I chose a strong orange-to-purple gradient because it fits the chaotic, energetic style of *Frantic^-1*.

* I created a blank SVG version of the card backside with:

  * transparent background,
  * rounded corners,
  * gradient fill,
  * subtle highlight,
  * and clean border styling.

* I also worked on the larger **Frantic^-1 logo** based on my own sketch.

* I tested how the blank card stack, motion streaks, wordmark, contour lines, and sparkle elements work together.

* I adjusted the colourful contour so it fits the composition better and does not overlap the cards too much.

* I then created a separate **circular Frantic symbol** based on my own Clip Studio Paint sketch.

* I manually built the SVG shape step by step, including the circular segments, inner swirl, motion streaks, black outlines, and “-1” symbol.

* This was very tedious because the SVGs had to be created manually instead of simply exporting a finished vector file.

* I repeatedly adjusted paths, gradients, outlines, and proportions so the result looked closer to my original sketch.

* After creating the SVG assets, I integrated the new **card backside logo** into the JavaFX game view.

* I replaced the old placeholder / PNG-based backside symbol with the new SVG-based logo asset.

* I tested several approaches for displaying complex SVGs inside JavaFX:

  * loading the SVG directly with JavaFX `Image`,
  * using the existing lightweight SVG loading library,
  * rendering it through a `WebView`,
  * and finally rendering it with **Apache Batik**.

* I found out that the direct JavaFX and lightweight SVG approaches were not reliable enough for this logo because the SVG contains complex gradients, filters, reusable shapes, and a large view box.

* I also tested a `WebView` solution. While it could display the SVG, it introduced unwanted layout and rendering problems such as clipping and a white background rectangle behind the logo.

* I then switched to **Apache Batik**, which renders the SVG into an in-memory PNG and then displays it as a normal JavaFX `ImageView`.

* I debugged why Batik initially failed to render the SVG and found that the problem was the unsupported SVG filter primitive `feDropShadow`.

* I replaced `feDropShadow` with a Batik-compatible SVG 1.1 shadow filter using:

  * `feGaussianBlur`,
  * `feOffset`,
  * `feFlood`,
  * `feComposite`,
  * and `feMerge`.

* I also updated SVG references from modern `href` syntax to the more Batik-compatible `xlink:href` syntax.

* I adjusted the logo rendering so that the SVG is rendered at a high resolution for sharpness, but displayed at a smaller size inside the actual card.

* I noticed that the colourful logo became hard to read when displayed very small on the card backside, so I created a much darker version of the circular logo.

* I manually darkened the gradients while keeping subtle colour variation, so the logo now looks almost black but still retains the original visual identity and slight colour depth.

* I integrated this darker version into the larger **Frantic^-1 game logo**, replacing the previous brighter mini-logo on the front card.

* I made the full game logo more Batik-compatible by moving definitions into safer locations and using `xlink:href` consistently.

* I then integrated the large `logo_font.svg` into the **ConnectView**, replacing the old text label `Frantic^-1`.

* I created a reusable SVG rendering helper so complex SVGs can be loaded as JavaFX images using Batik.

* I adjusted the ConnectView layout so the logo appears cleanly above the connection form.

* I also started integrating the game logo into the **GameView** by replacing the unnecessary `"Game Table"` label with a small version of the logo.

* I planned the placement carefully so the logo stays in the top-left header area and does not overlap the circular table, opponent cards, player markers, or other game elements.

## What worked well?

The final SVG integration works much better than the earlier attempts. Rendering the SVGs with Batik is more stable than relying on direct SVG loading or WebView. The card backside now uses the custom logo instead of a placeholder, and the ConnectView looks much more polished with the real project logo.

## What was difficult?

The most difficult part was making complex SVGs work reliably in JavaFX. Some SVG features that work in browsers do not work properly with Batik or JavaFX. I had to debug rendering errors, replace incompatible filters, adjust SVG syntax, and repeatedly test how the assets looked at very small sizes.

## What are the next steps?

Next, I want to create SVG icons for the missing card effects, especially for all special cards that currently still use fallback symbols or are missing proper visual assets. These icons should match the style of the existing card design, so the whole game looks more consistent.

I also want to continue polishing the GUI visually. This includes improving spacing, proportions, logo placement, card readability, panel layout, and the general presentation of the game screens. The goal is to make the interface feel more polished, coherent, and visually appealing for the final demo.


## Date: May 13, 2026

### Card Icon and Logo Polish (Aiysha)

## What did I do today?


I also created and refined several SVG icons for special cards, including Skip, Gift, Exchange, Fantastic, Fantastic Four, Second Chance, Equality, and Nice Try.


## Date: May 13, 2026

### Card Icon and Logo Polish (Aiysha, Sevval)

## What did I do today?

Today we worked on improving the Frantic^-1 GUI and website. We adjusted the player layout around the table, improved spacing, moved UI elements like the hand label and piles, and refined the table background. We also worked on making the website card visuals match the in-game JavaFX cards more closely. Later, we fixed several test issues after removing Counterattack from the game and updated failing tests such as `DeckFactoryTest`, `CircularTablePaneTest`, `HandFanPaneTest`, and `FullRoundSimulationTest`. We also debugged why the Fuck You card icon was not loading and found that the asset path in `asset-config.json` needed to point to `icons/fuck_you.svg`. Additionally, we recorded gameplay videos and updated the README so that it matches the current state of the project.

We've also decided that more sounds are not neccessary as we played the game.

## Future plans

We plan to fix a server-side bug where `LEAVE` only removes a player from the lobby session list, but not from the active `GameState`. Because of this, a player can leave one running game, join another lobby, and still remain inside the old game state. We want to handle intentional leaving during an active game differently from normal lobby leaving, so the player is properly removed from the running game state as well.

We also plan to cut the videos into a nice trailer and gameplay video and then upload thos onto the website.



## Date: May 13, 2026

### Lobby Switching and Reconnect Bug Fix

## What did we do today?

Today, we investigated a server-side bug where players could leave a running lobby, join another lobby, and still remain inside the old active `GameState`. This caused problems when starting a new game, especially with the reconnect-on-connection-loss system.

We analyzed the server logs and found that `LEAVE` only removed players from the lobby session list, but not from the running game state. We then adjusted the logic so intentional lobby leaving is handled differently from real connection loss. Players who intentionally leave a running game are now removed from the old `GameState`, while automatic reconnect still remains available for actual disconnects.

After testing again, we confirmed that the server now removes leaving players correctly and that new lobbies can start with a clean player list. We also identified a remaining GUI/client-state issue: after `GAME_END`, the client could still think that the game view was already shown. We planned a fix by resetting the local game state and `gameViewShown` after leaving a lobby or receiving `GAME_END`, so the GUI can correctly open a new game view when another lobby starts.
