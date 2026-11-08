# BlockUI `Pane.scissorsStart` fix (Quidoms 0.0.31)

## Bug
`GuiGraphics.enableScissor(x0,y0,x1,y1)` transforms the rectangle through the **current pose**.
The fat-jar `Pane.scissorsStart` also ran `pose.transformPosition` first, then called
`enableScissor` with those absolute coords — **double-applying** `BOScreen` centering/scale.

Symptom on Build (`windowbuildbuilding.xml`): blank parchment with only right-edge scraps
(e.g. `": Bricks Pillar"` + qty). The parchment image was behind the list; the list was
clipped away by a wrong scissor region.

## Fix
Pass **local** pane coords only:

```java
target.enableScissor(this.x, this.y, this.x + this.width, this.y + this.height);
```

`scissorsEnd` only restores `BOScreen` width/height and calls `disableScissor()` —
do **not** re-`enableScissor` parent absolute coords after pop (vanilla stack restores parent).

Matches BlockUI-26 approach. Keep the 3-arg signature for `ScrollingContainer` callers.
