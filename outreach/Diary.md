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

## Date: March 3, 2026 (Everyone)

### Research Discussion and Project Decision

#### What did we do today?
Today, we met to share the research we had done individually. After discussing our findings, we decided which game we will develop for our project.

#### Meeting Summary
- Each team member briefly presented their research.
- We discussed different ideas and compared them.
- Finally, we agreed on the game we will create.
- After making this decision, we started preparing our presentation and organized the main points together.

---

## Date: March 3, 2026 (Aiysha)

### Project Skeleton and Core Networking Setup

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


## Date: March 10, 2026 (Aiysha)

### Disconnect Handling and Automatic Nickname Assignment

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

