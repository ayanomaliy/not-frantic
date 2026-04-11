# Initialization

1. Generate **main deck** of 125 cards

- 72 **color cards** (4 colors x values 1-9 x 2 copies each)
- 9 **black cards** (value 1-9)
- 43 **special cards**
    - 20 single-color special cards
    - 23 four-color special cards
- 1 **fuck you card**

2. Generate **events card deck** of 20 cards

3. Randomize order of cards

- Randomize **main deck** card order
- Randomize **event card deck** card order

4. Distpribute 7 cards to each player

5. Set:

- **draw pile**, face down, created from **main deck**
- **discard pile**, face up, empty. Filled with played cards. Only last card in stack remains active.
- **event pile**, face down, created from **event card deck**

6. Assing **dealer** role to server

7. Generate max score value (150 - (3 * number of players))

8. Create player order.

- if first round: use alphabetical order of names
- if second or more round: order based on amount of points from first round
- play direction: always anti-clockwise

8. **dealer** flips top card from **draw pile** to **discard pile**

# Turn Structure

1. Assing effects of card in **discard pile** to player

2. Player attempts to play valid card:
- Same color
- Same number
- Same symbol
- Matches requiested number/color
- Required by event

3. If player cannot/chooses not to play:

- Draw one card

4. End turn, next player, repeat cycle.

# Card types

1. **Color cards**

- Values 1-9
- One of 4 colors (red, green blue, yellow)
- Play rulues:
    - Number on Number
    - Color on Color
    - Respect Requested color/number
- Scoring value: value of card

2. **Black cards**

- Values 1-9
- Black color
- Play rules:
    - Number on Number
    - Cannot be played on black cards
    - Black is not a color
Scoring value: value of card * 2

3. **Event cards**

- Triggered by **black cards**
- Must be resolved before continuing
- Events may:
    - Affect multiple players
    - Last entire round
    - End round emmidiately
- Not asinged to specific player, the events card triggers a global game **event**
- Scoring value: Does not exist, **events card** cannot be assigned to a specific player

4. Single-color **special cards**

- Play rules:
    - Color on Color
    - Symbol on Symbol
    - Via color request
- Effects:
    - Second chance
        - Must play another card
        - If impossible -> draw 1
    - Skip
        - Choose player -> the chosen player skips next turn
    - Gift
        - Give 2 cards to another player
        - If only 1 card -> give that card
    - Exchange
        - Swap 2 cards with another player. The cards of the exchangee are not visible eto the exchanger.
- Scoring value: 10 points

5. Four-color **special cards**

- Play rules:
    - On any card during turn
    - Sometimes outside of turn
- Effects:
    - Fantastic
        - Request color or number
    - Fantastic four
        - Distribute 4 cards among players
        - Request color and/or number
    - Equality
        - Target player draws until they have the same hand size as you
        - Then request color
    - Counterattack
        - Can be played out of turn
        - Cancels incoming attack (effect)
        - Applies attack (effect) on new target
    - Nice Try
        - Can be player when someone runs out of cards
        - Forces the plater who ran out of cards to draw 3 new cards
- Scoring value: 20 points

6. **Fuck you card**

- Play rules:
    - Playable only if player has exactly 10 cards
- No effects
- Scoring value: 69 points

# Special interaction

- Special effects must always be fully resolved before next turn
- Chain reactions allowed: counterattacks can stack
- Round cannot end until: all active effect are fully resolved

# Round end conditions

- A player has 0 cards
- A player must draw, but **draw pile** is empty

# Scoring

- Score: Sum of all scoring value of the hand held
- Adds to total score
- Player order for next round decided by score (highest to lowest)

# Game end

- Game ends when a player reaches or exceed the max score
- Winner: player with lowest score

# Key edge cases

- If a player can play after drawing, must be allowed to play
- Effects can interrupt order
- Counterattack can occur outside of turn
- Round end delayed until effects are resolved
- Black cards always trigger events
- **Nice try** can revive finished players