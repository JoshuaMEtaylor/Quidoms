# BlockUI scissors fix (Quidoms 0.0.31)

## Bug
`GuiGraphics.enableScissor(x0,y0,x1,y1)` transforms the rectangle through the **current pose**.
Older BlockUI `Pane.scissorsStart` also ran `pose.transformPosition` then called
`enableScissor` with those absolute coords — **double-applying** `BOScreen` centering/scale.

Symptom: Build resource `ScrollingList` left-clipped to blank parchment with only
right-edge scraps (`: Bricks Pillar` + qty) and the scrollbar (sibling, unclipped).

A prior attempt that passed **local** coords alone was not enough in practice on the
fat jar; the reliable fix is absolute corners + identity pose around `enableScissor`.

## Fix
1. `Pane.scissorsStart`: `transformPosition` → absolute rect → `pose.identity()` →
   `enableScissor` → `pose.pop`. `scissorsEnd` only restores screen size + `disableScissor`.
2. `ScrollingContainer.drawSelf`: same pattern (does not call `scissorsStart`), so the
   Build list clip cannot regress if `Pane` is overwritten.

Sources: `patches/ScrollingContainer.java`, patched `Pane` class in the fat jar.
