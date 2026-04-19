# GUI Asset System — Implementation Plan

## Goal

Introduce a single, easily editable configuration file that maps **cards** (and later events,
players, etc.) to **icons**, **colors**, and **sounds**. The GUI reads this config at startup and
uses it everywhere assets are needed. Future asset types (animations, fonts, particle effects) slot
into the same config without touching rendering logic.

---

## Current State

| Concern             | How it works today                                                    |
| ------------------- | --------------------------------------------------------------------- |
| Card display        | Text buttons (`CardTextFormatter` → `MainController.renderHand`)      |
| Styling             | `frantic-theme.css` — generic CSS, no per-card colors                 |
| Icons               | 4 SVG files in `src/main/resources/icons/` (unnamed, unmapped)        |
| Sounds              | 4 MP3 files in `src/main/resources/sounds/` (unnamed, unmapped)       |
| Config              | None — everything is hard-coded                                       |

Card model facts relevant to asset mapping:

- **CardType**: `COLOR`, `BLACK`, `SPECIAL_SINGLE`, `SPECIAL_FOUR`, `FUCK_YOU`, `EVENT`
- **CardColor**: `RED`, `GREEN`, `BLUE`, `YELLOW`, `BLACK`
- **SpecialEffect**: `SECOND_CHANCE`, `SKIP`, `GIFT`, `EXCHANGE`, `FANTASTIC`,
  `FANTASTIC_FOUR`, `EQUALITY`, `COUNTERATTACK`, `NICE_TRY`

---

## Step 1 — Rename and organise asset files

Rename the generic numbered files to descriptive names that match what they actually represent.
Keep all icons under `icons/`, sounds under `sounds/`, future animations under `animations/`.

Example target layout:

```text
src/main/resources/
├── icons/
│   ├── card_skip.svg          (was 1.svg)
│   ├── card_gift.svg          (was 2.svg)
│   ├── card_fantastic.svg     (was 3.svg)
│   └── card_exchange.svg      (was 4.svg)
├── sounds/
│   ├── card_play.mp3          (was 1.mp3)
│   ├── card_draw.mp3          (was 2.mp3)
│   ├── game_start.mp3         (was 3.mp3)
│   └── game_end.mp3           (was 4.mp3)
└── animations/                (empty — reserved)
```

**Why rename now:** The JSON config in Step 2 references these paths by name. Descriptive names
make the config self-documenting.

---

## Step 2 — Create `asset-config.json`

Create `src/main/resources/config/asset-config.json`.
This is the **single file an artist or designer edits** to reassign any asset without touching Java.

### Schema

```json
{
  "version": 1,
  "cards": {
    "by_effect": {
      "SKIP":          { "icon": "icons/card_skip.svg",      "sound": "card_play" },
      "GIFT":          { "icon": "icons/card_gift.svg",      "sound": "card_play" },
      "EXCHANGE":      { "icon": "icons/card_exchange.svg",  "sound": "card_play" },
      "FANTASTIC":     { "icon": "icons/card_fantastic.svg", "sound": "card_play" },
      "FANTASTIC_FOUR":{ "icon": "icons/card_fantastic.svg", "sound": "card_play" },
      "SECOND_CHANCE": { "icon": null,                       "sound": "card_play" },
      "EQUALITY":      { "icon": null,                       "sound": "card_play" },
      "COUNTERATTACK": { "icon": null,                       "sound": "card_play" },
      "NICE_TRY":      { "icon": null,                       "sound": "card_play" }
    },
    "by_type": {
      "COLOR":         { "icon": null, "sound": "card_play" },
      "BLACK":         { "icon": null, "sound": "card_play" },
      "FUCK_YOU":      { "icon": null, "sound": "card_play" },
      "EVENT":         { "icon": null, "sound": "card_play" }
    },
    "by_color": {
      "RED":    { "background": "#e05050", "text": "#ffffff" },
      "GREEN":  { "background": "#4caf74", "text": "#ffffff" },
      "BLUE":   { "background": "#4a90d9", "text": "#ffffff" },
      "YELLOW": { "background": "#f5c842", "text": "#333333" },
      "BLACK":  { "background": "#2c2c2c", "text": "#ffffff" }
    }
  },
  "sounds": {
    "card_play":  "sounds/card_play.mp3",
    "card_draw":  "sounds/card_draw.mp3",
    "game_start": "sounds/game_start.mp3",
    "game_end":   "sounds/game_end.mp3"
  },
  "events": {
    "GAME_STARTED": { "sound": "game_start" },
    "GAME_ENDED":   { "sound": "game_end"   },
    "CARD_DRAWN":   { "sound": "card_draw"  }
  }
}
```

**Resolution priority for card icons/sounds:**

1. `by_effect` (most specific — a Skip card always uses its icon regardless of color)
2. `by_type` (fallback for typed cards without a matching effect)
3. `null` (no asset; fall back gracefully)

**Resolution priority for card colors:**

1. `by_color` entry for the card's `CardColor`
2. CSS default (cards without a color mapping keep their existing style)

---

## Step 3 — Create `AssetConfig` and `AssetConfigLoader`

**Package:** `ch.unibas.dmi.dbis.cs108.example.client.assets`

### `AssetConfig.java`

Plain Java records/classes that mirror the JSON schema — no logic, just data holders.

```text
AssetConfig
  ├── Map<String, CardAssetEntry>  cardsByEffect
  ├── Map<String, CardAssetEntry>  cardsByType
  ├── Map<String, CardColorEntry>  cardsByColor
  ├── Map<String, String>          soundPaths       (id → resource path)
  └── Map<String, EventAssetEntry> events
```

### `AssetConfigLoader.java`

- Reads `/config/asset-config.json` from the classpath using `getResourceAsStream`
- Parses it with a minimal JSON library (Jackson is already likely on the classpath via the
  server side; alternatively use `org.json` or hand-roll a tiny parser for this flat structure)
- Returns a validated `AssetConfig`; logs warnings for missing resource paths but never throws

---

## Step 4 — Create `AssetRegistry.java`

**Package:** `ch.unibas.dmi.dbis.cs108.example.client.assets`

Single entry point that the GUI uses. Holds one `AssetConfig` and exposes clean lookup methods.

```java
public class AssetRegistry {
    public static AssetRegistry load() { ... }            // loads config from classpath

    // Card asset resolution (priority: effect → type)
    public Optional<Image>  getIcon(Card card)  { ... }
    public Optional<String> getSoundId(Card card) { ... }

    // Color resolution
    public Optional<String> getBackgroundColor(CardColor color) { ... }
    public Optional<String> getTextColor(CardColor color) { ... }

    // Event sounds
    public Optional<String> getSoundId(String eventName) { ... }

    // Sound path lookup
    public Optional<String> getSoundPath(String soundId) { ... }
}
```

Use svgfx for svg images.

---

## Step 5 — Create `SoundManager.java`

**Package:** `ch.unibas.dmi.dbis.cs108.example.client.assets`

```java
public class SoundManager {
    private final AssetRegistry registry;
    private final Map<String, AudioClip> cache = new HashMap<>();

    public SoundManager(AssetRegistry registry) { ... }

    public void play(String soundId) { ... }   // looks up path, loads AudioClip lazily, plays
    public void setMuted(boolean muted) { ... }
}
```

- Uses JavaFX `AudioClip` (short sound effects, low latency)
- Lazy-loads and caches clips on first use
- Use the fallback sound if audio is missing

---

## Step 6 — Create `CardView.java`

**Package:** `ch.unibas.dmi.dbis.cs108.example.client.ui`

Replaces the inline button-creation logic currently in `MainController.renderHand()`.

```text
CardView extends StackPane
  ├── Rectangle (background — color from AssetRegistry)
  ├── ImageView (icon — from AssetRegistry, centered)
  └── Label     (text fallback — CardTextFormatter output, shown when no icon)
```

Constructor: `CardView(Card card, AssetRegistry assets)`

- If an icon is available: show icon + small label at bottom
- If no icon: show full text label (current behavior preserved)
- CSS class `game-card-button` kept for sizing/border/hover styles
- Background color set inline via `-fx-background-color` using the hex from config

---

## Step 7 — Integrate into `MainController` and `GameView`

1. Instantiate `AssetRegistry` once in `FranticFxApp.start()` and pass it to `MainController`.
2. Instantiate `SoundManager` once in `MainController`.
3. Replace the button-creation loop in `renderHand()` with `new CardView(card, assets)`.
4. Wire sound calls:
   - `soundManager.play(registry.getSoundId(card))` when a card is played
   - `soundManager.play(registry.getSoundId("CARD_DRAWN"))` when a card is drawn
   - `soundManager.play(registry.getSoundId("GAME_STARTED"))` on game start
   - `soundManager.play(registry.getSoundId("GAME_ENDED"))` on game end

---

## Step 8 — Add mute toggle to the UI

Add a small mute button (speaker icon) to `GameView`'s toolbar.
It calls `soundManager.setMuted(true/false)`.
State can be persisted to a local preferences file later if desired.

---

## Step 9 — Future extensibility

The config schema is designed to accept new top-level sections without breaking existing code:

| Future asset type  | Where it goes in config                             | New class needed          |
| ------------------ | --------------------------------------------------- | ------------------------- |
| Card animations    | `"animations"` section, keyed by effect/type        | `AnimationManager`        |
| Player avatars     | `"players"` section, keyed by username or rank      | query via `AssetRegistry` |
| Event banners      | already in `"events"` section — add `"banner"` field| extend `EventAssetEntry`  |
| Theme variants     | `"themes"` top-level list                           | `ThemeManager`            |

---

## File Checklist

```text
src/main/resources/
  config/
    asset-config.json                        ← new (Step 2)
  icons/
    card_skip.svg  card_gift.svg  ...        ← renamed (Step 1)
  sounds/
    card_play.mp3  card_draw.mp3  ...        ← renamed (Step 1)

src/main/java/.../client/assets/
  AssetConfig.java                           ← new (Step 3)
  AssetConfigLoader.java                     ← new (Step 3)
  AssetRegistry.java                         ← new (Step 4)
  SoundManager.java                          ← new (Step 5)

src/main/java/.../client/ui/
  CardView.java                              ← new (Step 6)
  MainController.java                        ← modified (Step 7)
  GameView.java                              ← modified (Step 7, Step 8)

src/main/java/.../gui/javafx/
  FranticFxApp.java                          ← modified (Step 7 — wire AssetRegistry)
```

---

## Implementation Order

1. Rename assets (Step 1) — no code changes, easy to do first
2. Write `asset-config.json` (Step 2) — defines the contract everything else follows
3. `AssetConfig` + `AssetConfigLoader` (Step 3) — pure data, no UI dependency, easy to unit-test
4. `AssetRegistry` (Step 4) — depends on loader; resolve SVG strategy here
5. `SoundManager` (Step 5) — independent of registry shape
6. `CardView` (Step 6) — depends on registry
7. Wire into `MainController` / `GameView` / `FranticFxApp` (Steps 7–8)
