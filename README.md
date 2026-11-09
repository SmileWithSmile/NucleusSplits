# Nucleus Splits

A Fabric client mod for repeated Crystal Nucleus runs on Hypixel SkyBlock. It starts a split timer when you leave the
Crystal Nucleus, learns the crystal order from the first complete run in a lobby, and compares later runs against your
session or lifetime bests.

Supports Minecraft 26.1.x. Requires Fabric Loader 0.19.3+, Fabric API and Java 25.

## How it behaves

* The timer starts when you cross out of the fixed Crystal Nucleus area. Returning to place a crystal does not restart
  an active timer.
* The first clean run in each Crystal Hollows lobby learns the order of Jade, Amber, Topaz, Sapphire and Amethyst. A
  run must contain all five unique crystal messages before its completion can become the reference.
* Each row shows the crystal, the segment delta and the cumulative run time. Green is ahead of the selected best;
  red is behind it.
* The run ends when Hypixel announces the Crystal Loot Bundle near the Nucleus Vault.
* Moving to another Crystal Hollows server resets the session route and session bests. Lifetime history is kept.
* Original Hypixel crystal messages and compacted messages from mods such as SkyHanni are both recognised.

## Session and all-time

Open chat, your inventory or another screen to reveal the `SESSION / ALL-TIME` switch under the splits.

* `SESSION` compares against the best segment times from the current Crystal Hollows lobby.
* `ALL-TIME` compares against the best segment times saved across game restarts.
* Both views show the average completed run (`AVG`) and the best complete run (`PB`) for the selected Jade profile.

The settings menu contains an `AVG runs` editor. It lists every completed run for the selected Jade profile and lets
you include or exclude each result from both session and all-time AVG. Excluding a run does not delete it and does not
change PBs or best segment comparisons.

To keep AFK time out of the history, an active timer stops at the earlier of `1.5 × all-time AVG` and `08:00`. If no
all-time average exists, session AVG is used. If both averages are empty, the hard `08:00` limit still applies. A run
stopped this way is not saved and cannot affect PB, AVG or run count.

Every clean completed run is stored in `config/nucleus-splits-history.json`. The file contains the crystal order,
individual segment times, Nucleus return time and total time; it does not contain chat history or player data.

## Party PB sharing

Type `!nucpb` in party chat and every party member who has Nucleus Splits automatically replies with their lifetime
best complete run:

```text
Nucleus PB: 03:27.65 (12 runs)
```

Replies are sent through `/pc`, spread over 0.5-2.5 seconds using the player's UUID and limited to one reply every
30 seconds. Guild, private and public chat do not trigger it. A player without a recorded run replies that no PB has
been completed yet.

## Settings menu, HUD editor and commands

`/nucleussplits` opens the main settings menu. The short aliases `/nc` and `/ns` open the same screen. Use it to
switch the Jade route, choose the default Session / All-time comparison, view both averages and edit which runs are
included in AVG.

`/nc gui` opens the position editor, based on SkyHanni's HUD editing controls:

* drag the splits with the left mouse button;
* scroll over them to change scale from 50% to 200%;
* right-click them to reset position and scale;
* press Done or Escape to save and close.

Command | Meaning
--- | ---
`/nucleussplits`, `/nc`, `/ns` | open the settings and statistics menu
`/nc gui` | open the HUD position and scale editor
`/nc reset` | forget the learned lobby route and reset the current run
`/nc preview` | show or hide a sample HUD outside a run
`/nc pos <x> <y>` | set the HUD position directly
`/nc scale <50-200>` | set its scale directly in percent

## Settings

Settings are written to `config/nucleus-splits.json`.

Key | Default | Meaning
--- | --- | ---
`x` | `8` | horizontal HUD position in scaled GUI pixels
`y` | `8` | vertical HUD position in scaled GUI pixels
`scalePercent` | `100` | HUD scale, clamped from 50 to 200
`statsView` | `SESSION` | selected comparison: `SESSION` or `LIFETIME` (`ALL-TIME` in the HUD)
`runProfile` | `prepared_jade` | selected Jade route: `prepared_jade` or `in_run_jade`

## Prepared Jade and in-run Jade

Jade can be routed with prepared materials or with materials collected during the timed run. Those strategies should
not share a full-run leaderboard. Select `Prepared materials` or `Collect materials during run` in the settings menu.
Session bests, all-time bests, average times, run counts and `!nucpb` responses are filtered to that profile. The
profile is locked when a run starts, so changing the menu during a run cannot file it under the wrong strategy.
Existing `unclassified` runs from version 0.3.0 appear under Prepared Jade.

## Detection and limitations

The Crystal Nucleus has fixed coordinates, so leaving its documented bounds starts the timer without sending a
command or reading private API data. Crystal splits and completion use Hypixel's own chat messages. If Hypixel changes
those messages or the area bounds, send a fresh `logs/latest.log` after reproducing the problem.

The mod has no account API, telemetry or remote service. The only outgoing action it can perform is the `/pc` reply
described above, after another party member explicitly sends `!nucpb`.

## Installation

1. Install Fabric Loader 0.19.3 or newer for Minecraft 26.1.x.
2. Put Fabric API and the matching Nucleus Splits jar in the Minecraft `mods` folder.
3. Start the game, use `/nucleussplits` to choose the Jade route, then use `/nc gui` to place the HUD.

Download release jars from the repository's Releases page. Do not use the `-sources` jar.

## Building

With JDK 25 installed:

```shell
./gradlew build
```

The mod jar is created under `build/libs` and tests run as part of the build.

## Hypixel rules

Nucleus Splits observes movement and server messages. It does not move the player, collect items, solve routes or play
the run. Party PB sharing sends one ordinary party message a player could type themselves, only after an explicit
party request. Whether a mod is suitable for the way you play is ultimately your responsibility.

## Something broken?

Send `logs/latest.log` from your game folder to soldey on Discord immediately after the problem happens; the file is
overwritten every time the game starts. Relevant lines are logged under `Nucleus Splits`.
