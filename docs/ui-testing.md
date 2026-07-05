# UI testing checklist

Layout bugs in this app almost never show up on the phone-portrait canvas the
author is looking at — they show up on the *other* shapes: a phone in landscape,
a tablet, a Chromebook window. The shell adapts by width (bottom tab bar below
600dp, side navigation rail at/above it — see `AdaptiveShell`), but an adaptive
shell only reflows *navigation*. Each tab's own content still has to survive a
short, wide viewport, and that is where things break.

**The failure mode to hunt for: a primary control pushed off-screen.** A landscape
viewport is short. Any screen that sizes a child by width and lets its height
follow — `fillMaxWidth().aspectRatio(...)`, a square preview, a media frame — can
produce a child taller than the whole viewport, shoving the button *inside or
below it* past the fold. If that button is the only way forward (pick an image,
transmit, save), the tab is dead on that device even though it looks fine in
portrait. Issue #20 shipped with exactly this on the TX tab.

## The pass — every tab, both shapes

Do this whenever you touch a screen's layout, add a tab, or change the shell.
Don't trust portrait alone; the rail layout is a different code path.

1. **Both widths.** Exercise a compact width (phone portrait, bottom bar) **and**
   a wide width (≥600dp, side rail). On the emulator you don't need a physical
   tablet — force the wide canvas directly:

   ```
   adb -s <serial> shell wm size 2400x1080   # wide: rail + short height
   adb -s <serial> shell wm size reset        # back to normal
   ```

   (Rotating with `settings put system user_rotation 1` is flakier than a direct
   `wm size` override and doesn't always take when a dialog is up.)

2. **Every tab, in the wide canvas.** RX, Gallery, TX, Waterfall, Logbook,
   Settings. Screenshot each (`adb exec-out screencap -p > shot.png`) and look,
   specifically, for:
   - The tab's **primary action** is visible, or reachable by scrolling. If the
     screen scrolls, actually scroll it and confirm the control appears and is
     tappable — a control centered inside an over-tall box is technically "in the
     scroll range" but is a bug, not a pass.
   - Nothing important is hidden **behind the TX strip** (the always-on strip sits
     between content and the bottom edge in both layouts).
   - No image/preview frame is taller than the viewport. A mode-aspect SSTV frame
     (~4:3) at full landscape width is ~700dp tall on an ~450dp-tall canvas — cap
     it (`heightIn`/`widthIn`) so the surrounding controls stay on screen.

3. **Rail reachability.** All six rail destinations are tappable, including the
   last one (Settings) — on a short canvas the rail scrolls; confirm the bottom
   entry can be reached.

## Device coverage (issue #20)

The emulator `wm size` override reproduces the *layout* class of bug. Before an
issue that touches responsive layout is closed, a human still confirms on real
hardware in **both** orientations — at minimum the Redmi Redpad 2 (Android 16),
plus any other tablet/Chromebook on hand. Record what was tested in the PR.
