# Changelog

## 0.5.0

* Stop and discard AFK runs at the earlier of `1.5 × all-time AVG` and 8 minutes.
* Fall back to session AVG when an all-time average is unavailable; use 8 minutes when both averages are empty.
* Add an `AVG runs` editor with a paginated list of every run in the selected Jade profile.
* Include or exclude individual runs from session/all-time AVG without deleting them or changing PBs.
* Treat the new `includedInAverage` history field as enabled for existing saved runs.

## 0.4.0

* Open the settings menu with `/nucleussplits`, `/nc` or `/ns`.
* Separate Prepared Jade and In-run Jade history, PBs and session statistics.
* Show session and all-time average run times in the menu and HUD.
* Add the Session / All-time comparison selector to the settings menu.
* Widen the HUD and right-align time columns so deltas and cumulative times never overlap.
* Treat legacy `unclassified` 0.3.0 history as Prepared Jade history.

## 0.3.0

* Start Nucleus runs when leaving the Crystal Nucleus area.
* Learn the crystal order from the first complete run in a lobby.
* Track per-segment session and persistent lifetime bests.
* Add the `/nc gui` drag-and-scroll HUD editor.
* Add the in-screen `SESSION / LIFETIME` switch.
* Save complete run history for future Jade strategy profiles.
* Add party PB sharing through `!nucpb`.
