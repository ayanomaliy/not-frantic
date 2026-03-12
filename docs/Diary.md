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

## Date: March 9, 2026 (Senanur)

### Ping/Pong System and Nickname Features

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

### Excercise session

#### What did we do today?

- We tested our server and clients

#### Work Summary

Aiysha was able to run the server while the rest was able to join. We were able to see the ping pongs,who joined the lobby and to send messages.

## Date: March 12, 2026

### "Cleaning" (Sevval)

#### What did we do today?

- Since QA task was postponed, I didn't start to work on it as planned.
- I went through all our uploads, adjusted some things, made sure there are no more typos/errors and checked that everything for the milestone is ready.
- 