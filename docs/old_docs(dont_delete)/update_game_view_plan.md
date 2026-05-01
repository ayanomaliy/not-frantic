# Non-Technical plan

1.  Load the new assets. New Assets: table.svg, card_backside.svg, player.svg; card_backside background color (#ffffff); player color (take from a list of commons colors: red, green, blue, yellow, purple, pink e.t.c.)
2.  Have cards displayed in a "fan" formation around a central node.
3.  Assign Player Hand Fan node to be as the bottom middle part of the GUI (excluding sidebar) 
4.  Have colors assigned to each player.
5.  Have a region assigned to a table with a few central nodes for loading the game table info (Draw Pile, Discard Pile, Current Player, Phase, Top Card, End Turn Button)
6.  Have nodes on the table assigned to every additional player using the following rules:
	1. Bottom 120° reserved for the main player. This means that if the top of the circle is at 0°, 120°- 240° are reserved for the main player.
	2.  For the remaining 240° apply the following calculation: interval = 240°/(number_of_additional_players + 2). number_of_additional_players excludes main player for whom the gui is loaded.  Divide the 240° into interval of calculated size and assign each edge between the intervals to a player excluding the 2 outermost edges. For example if the top of the cirlce is 0° and there are 2 addional platers, they will the assigned to 40° and 320°
7.  Convert the player svg asset into a javafx shape and assing each player their corresponding color.
8.  Have the players visualised on their nodes
9.  Figure out the rotation for the fan for each additional player. Use the following formula: fan_degree = player_degree + 180°. So if a player is located at 40° their card fan will be rotated to 220° (facing inside the table circle)
10.  Have the cards of all other players visible
11.  Have the cards of all other players covered up with a card backside
12.  Have a command PEEK (/peek) that makes the cards of other players visible

# Technical Plan

Implementation is split into 8 phases ordered by dependency and testability. Phases 1–4 are pure logic or isolated components with no layout risk. Phase 5 is the largest structural change. Phases 6–7 wire everything together. Phase 8 is deferred because it requires server-side work.

---

## Phase 1 — Data layer: parse player order and hand sizes from GAME\_STATE

**Files:** `ClientState.java`, `FxNetworkClient.java`

The server already sends per-player data in every `GAME_STATE` message in the form `players:Alice:5:12,Bob:3:8` (name:handSize:totalScore, comma-separated). `applyGameStatePayload()` currently ignores this section. We need to parse it and expose it as observable state so the view can react.

Steps:

1. Add an inner record to `ClientState`:
   `record PlayerInfo(String name, int handSize, String color) {}`
2. Add `ObservableList<PlayerInfo> playerInfoList = FXCollections.observableArrayList()` to `ClientState` with a getter.
3. In `FxNetworkClient.applyGameStatePayload()`, after the existing `prefix.split(",")` loop, parse the `players:` section:
   - Split the substring after `"players:"` on `,` to get per-player tokens.
   - Each token is `name:handSize:totalScore` — split on `:` to extract name and hand size.
   - Collect the ordered names, call `PlayerColorAssigner.assign(names, localUsername)` (Phase 2), then rebuild `state.getPlayerInfoList()` with `PlayerInfo` records.
4. Keep the update atomic: call `Platform.runLater` (already the case in `applyGameStatePayload`).

**Test:** Unit-test `applyGameStatePayload` with a mock payload string and assert `state.getPlayerInfoList()` is populated in the correct order with correct hand sizes.

---

## Phase 2 — Player color assigner

**File:** new `client/ui/PlayerColorAssigner.java`

A stateless utility that maps player names to colors so each player always gets the same color within a session.

Steps:

1. Define a static ordered list of colors: `["red", "green", "blue", "yellow", "purple", "pink", "orange", "teal"]`.
2. Implement `static Map<String, String> assign(List<String> orderedNames, String localUsername)`:
   - Reserve `"red"` for `localUsername` (index 0 of the color list).
   - Assign remaining colors to other players in the order they appear in `orderedNames`, skipping `"red"`.
3. Add player color CSS rules to `frantic-theme.css`:

   ```css
   .player-color-red    { -fx-fill: #e74c3c; }
   .player-color-green  { -fx-fill: #2ecc71; }
   .player-color-blue   { -fx-fill: #3498db; }
   .player-color-yellow { -fx-fill: #f1c40f; }
   .player-color-purple { -fx-fill: #9b59b6; }
   .player-color-pink   { -fx-fill: #e91e8c; }
   .player-color-orange { -fx-fill: #e67e22; }
   .player-color-teal   { -fx-fill: #1abc9c; }
   ```

**Test:** Unit-test `assign()` with 1, 4, and 8 players, verifying the local player is always `"red"` and no two players share a color.

---

## Phase 3 — CardBacksideView

**File:** new `client/ui/CardBacksideView.java`

A card-sized pane showing the backside asset. Same fixed size as `CardView` (90×130 px).

Steps:

1. `CardBacksideView extends StackPane`:
   - Load `card_backside.svg` from `/icons/card_backside.svg` via `getClass().getResource(...)` and wrap in an `ImageView` (or use `SvgImageView` from the existing svgfx library if it supports a resource path directly).
   - Set background color `#ffffff` via inline style or a new CSS class `card-backside`.
   - Apply `getStyleClass().add("card-view")` for the shared border/radius styling.
   - `setPrefSize(90, 130); setMinSize(90, 130); setMaxSize(90, 130);`
2. Add `.card-backside { -fx-background-color: #ffffff; }` to `frantic-theme.css`.

**Test:** Instantiate in a JavaFX test scene inside a `FlowPane` and verify the SVG renders with a white background.

---

## Phase 4 — OtherPlayerView

**File:** new `client/ui/OtherPlayerView.java`

A self-contained node representing one opponent: their colored player icon, name label, and a compact fan of face-down cards. Designed to be placed and rotated as a unit on the circular table.

Steps:

1. `OtherPlayerView extends Group`:
   - Constructor: `OtherPlayerView(String playerName, int handSize, String colorClass)`.
   - Load `player.svg` into an `ImageView`. Apply a `ColorInput`+`Blend` JavaFX effect to tint it with the player's color (avoids needing multiple SVG files). Alternatively set a `ColorAdjust` hue shift derived from the color class — pick whichever produces cleaner results at runtime.
   - Create a `Label` for the player name, styled with `.field-label`.
   - Build a card fan: N instances of `CardBacksideView` arranged in a small overlapping row. For a compact look, offset each card 18 px horizontally and rotate each ±4° relative to its neighbour (total spread ≈ ±(N/2)*4°). Wrap them in a `Pane` positioned below the icon.
   - Layout the icon, label, and card fan vertically inside the `Group`.
2. Expose `setHandSize(int n)` — rebuilds the card fan children.
3. Expose `revealCards(List<Integer> cardIds)` and `hideCards()` for the PEEK feature (Phase 8). `revealCards` replaces `CardBacksideView` instances with `CardView` instances for the given IDs.
4. The fan `Pane` inside the group should be independently rotatable via `setFanRotation(double degrees)` so cards face inward while the name label stays readable. Achieve this by wrapping only the card fan `Pane` in a nested `Group` and calling `setRotate()` on that inner group.

**Test:** Create 3 `OtherPlayerView` instances with different colors and hand sizes in an isolated test scene. Verify icon tinting, card count, and `setFanRotation` visually.

---

## Phase 5 — CircularTablePane: redesign the center area

**Files:** new `client/ui/CircularTablePane.java`, `GameView.java`

This is the largest structural change. The current `centerTablePane` (a `StackPane` containing a `VBox` with piles and labels) is replaced with a circular layout where player slots are placed on a ring and center info sits in the middle.

Steps:

1. Create `CircularTablePane extends Pane`:
   - `private final double radius` — distance from center to player slot anchors (e.g., 280 px).
   - `private final double cx, cy` — pane center (half of preferred size, e.g., 600×600 pane → cx=cy=300).
   - Background: load `table.svg` as an `ImageView` sized to fill the pane, or fall back to a large `Circle` with CSS fill if the asset is unavailable.
   - Center cluster: a `VBox` containing `drawPilePane`, `discardPilePane`, `statusRow`, and `endTurnButton` — the same nodes currently in `GameView.buildCenterArea()`. Position this `VBox` by calling `setLayoutX(cx - vbox.prefWidth/2)` and `setLayoutY(cy - vbox.prefHeight/2)` after layout (use `layoutBoundsProperty` listener or `Platform.runLater`).
   - Player slot storage: `Map<String, OtherPlayerView> playerSlots = new LinkedHashMap<>()`.

2. Implement `void setPlayerSlots(List<PlayerInfo> others, String localUsername)`:
   - Clears existing player slot nodes from the pane.
   - Calculates angles for each entry in `others` (see formula below).
   - For each player, creates an `OtherPlayerView`, positions it at the calculated `(x, y)`, calls `setFanRotation(angle + 180)`, and adds it to the pane.

3. Angle calculation (from non-technical plan):
   - 0° = top (North). Using screen coordinates: `x = cx + radius * sin(toRadians(angle))`, `y = cy - radius * cos(toRadians(angle))`.
   - The 240° arc runs from 320° (clockwise from top) wrapping through 0° to 120°. In degrees starting from top going clockwise: reserved bottom arc is 120°–240°; available arc is 240°–360° (≡ 0°–360° minus 120°–240°).
   - `interval = 240.0 / (numOthers + 2)`.
   - Player i (1-indexed) is placed at: `angle = (320 + i * interval) % 360`.
   - Example: 2 players → interval = 80° → angles 40° and 320° ✓.

4. In `GameView`:
   - Add `private CircularTablePane circularTablePane` field.
   - In `buildCenterArea()`, construct `CircularTablePane`, pass it the draw/discard pane references and the labels, replace `centerTablePane.getChildren().add(centerWrapper)` with `centerTablePane.getChildren().add(circularTablePane)`.
   - Expose `getCircularTablePane()` getter.

**Test:** Instantiate with 0, 1, 2, 3, and 5 player infos and verify positions visually. Assert that with 2 players the angles are 40° and 320°.

---

## Phase 6 — Main player fan hand

**Files:** `GameView.java`, `MainController.java`

Replace the `FlowPane playerHandPane` with a custom fan layout pane so the local player's cards arc upward from the bottom center of the game area, matching the visual language of the circular table.

Steps:

1. Add `private Pane handFanPane` to `GameView` alongside the existing `playerHandPane` field.
2. In `buildBottomHandArea()`, construct `handFanPane` instead of `playerHandPane`. Set its preferred height to ~180 px and `prefWidthProperty().bind(widthProperty().multiply(0.60))`.
3. Card positioning formula for N cards in `MainController.renderHand()`:
   - Fan spread: 60° total (adjustable constant).
   - Arc radius: 500 px (cards appear to curve from a point far below the pane).
   - For card at index i: `angle = -spread/2 + i * (spread / max(N-1, 1))` in degrees.
   - `x = cx + arcRadius * sin(toRadians(angle))` — cx is half of pane width.
   - `y = paneHeight + arcRadius * (1 - cos(toRadians(angle)))` + vertical offset so cards peek above the bottom.
   - Apply `cardView.setRotate(angle)` so each card tilts to match its arc position.
   - Set `cardView.setLayoutX(x - cardWidth/2)` and `cardView.setLayoutY(y - cardHeight)`.
4. Update `MainController.renderHand()` to clear and rebuild `handFanPane.getChildren()` using the formula above. The click handler on each `CardView` is unchanged.
5. Update `GameView.getPlayerHandPane()` return type or add `getHandFanPane()` — update all call sites in `MainController`.

**Test:** Render with 1, 5, 10, and 15 cards and verify no overlap or clipping. Verify clicking a card still fires the play action.

---

## Phase 7 — Wire everything in MainController

**Files:** `MainController.java`

Connect `ClientState.playerInfoList` changes to the `CircularTablePane` so the table updates whenever a new GAME\_STATE arrives.

Steps:

1. In `MainController.showGameView()`, after obtaining the `GameView`:
   - Add a `ListChangeListener` on `state.getPlayerInfoList()` that calls `refreshOtherPlayers(gameView)`.
   - Call `refreshOtherPlayers(gameView)` once immediately in case the list is already populated.
2. Implement `private void refreshOtherPlayers(GameView view)`:
   - Filter `state.getPlayerInfoList()` to exclude the local user (by matching `state.getUsername()`).
   - Call `view.getCircularTablePane().setPlayerSlots(others, state.getUsername())`.
3. Remove (or hide) the `playersList` `ListView` from the sidebar now that the circular table shows all players — or keep it as a compact reference; that is a UX decision to make during implementation.

**Test:** Mock `ClientState` with changing player lists and assert `CircularTablePane.setPlayerSlots` is called with the correct filtered list.

---

## Phase 8 — PEEK command (/peek)

**Files:** `MainController.java`, server-side (new message type), `FxNetworkClient.java`, `ClientState.java`

PEEK requires revealing other players' actual card IDs, which the server currently does not send to non-owning clients. Two options:

**Option A — Server extension (recommended for real multiplayer):**

1. Add a `PEEK` client→server command. Server responds with `PEEK_RESPONSE|playerName:id1,id2,...` for all other players.
2. In `FxNetworkClient`, handle `PEEK_RESPONSE`: parse per-player card lists, store in `ClientState` as `Map<String, List<Integer>> peekedHands`.
3. In `MainController`, handle the `/peek` command typed in `commandInput` by calling `network.send("PEEK|")`. Toggle a `boolean peekMode` in `ClientState`.
4. When `peekedHands` changes, call `otherPlayerView.revealCards(ids)` (already scaffolded in Phase 4). Calling `/peek` again (or on next GAME\_STATE) calls `otherPlayerView.hideCards()`.

**Option B — Dev mode only (no server changes):**

1. In `MainController`, handle `/peek` locally: iterate `CircularTablePane` player slots and call `revealCards()` using placeholder or known card IDs from the dev properties file. This only works when the game is run in dev mode with a known deck.

Implement Option B first as a proof of concept, then upgrade to Option A once the server side is ready.

**Test (Option B):** Run in dev mode, type `/peek`, verify `OtherPlayerView` instances switch from backsides to face-up `CardView` nodes.

---

## Implementation order summary

| Phase | What                  | Risk     | Can test without prior phases  |
| ----- | --------------------- | -------- | ------------------------------ |
| 1     | Parse player data     | Low      | Yes (unit test)                |
| 2     | Color assigner + CSS  | Low      | Yes (unit test)                |
| 3     | CardBacksideView      | Low      | Yes (visual)                   |
| 4     | OtherPlayerView       | Low–Med  | Yes (isolated scene)           |
| 5     | CircularTablePane     | High     | After Phase 4                  |
| 6     | Fan hand              | Med      | After Phase 3                  |
| 7     | Wire MainController   | Med      | After Phases 1+5               |
| 8     | PEEK                  | Med–High | After Phase 4                  |
