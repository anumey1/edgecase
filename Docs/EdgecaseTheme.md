# **EdgeCase UI/UX Theme & Styling Specification**

## **1\. Global Aesthetic Directive**

The EdgeCase application will utilize a hybrid aesthetic designated as "Hellenic Serpent." This merges the architectural grandeur and mythological resonance of Ancient Greek temples with the subdued, cunning, and aquatic undertones of a "Slytherin-esque" palette. The UI will abandon modern Material Design paradigms in favor of skeuomorphic textures, chiseled typography, and lithic (stone-like) interactables.

## **2\. Color Palette Specification**

The color system relies on dark, desaturated greens and teals, offset by ancient stone and weathered metallic accents.

### **Primary Background & Surface Colors**

* **Abyssal Teal (\#071A15):** The absolute deepest background color, used in the deepest layers of the UI (e.g., behind the scrollable lists).  
* **Dark Seaweed (\#122A23):** Secondary background color for elevated panels and list item backgrounds.  
* **Faded Olive Teal (\#3B5249):** Used for inactive states, borders, and subtle dividers.  
* **Temple Sandstone (\#D4C4A8):** The primary color for buttons and interactive stone elements. Possesses a warm, gritty hue.  
* **Aged Marble (\#F5EFE6):** The primary color for high-contrast text and prominent icons.

### **Accent & Interaction Colors**

* **Serpent Emerald (\#2E8B57):** Primary accent color for positive actions, active states, and selection highlights.  
* **Tarnished Silver (\#9AA0A6):** Secondary accent for secondary text, metadata, and metallic filigree ornaments.  
* **Ethereal Pink (\#4DFFC0CB):** A 30% opacity luminescent pink, strictly reserved for the outer hit-box radius of the Edge Sliver (bridging previous technical requirements with a mystical "aura" effect).

## **3\. Typography & Text Rendering**

Typography must simulate text carved into stone or inscribed on ancient parchment.

* **Primary Font (Headers, Titles, Buttons):** Augustus (or equivalent classical Roman/Greek serif).  
  * *Styling:* All caps.  
  * *Rendering:* Must employ a sub-pixel drop shadow to simulate an engraved (chiseled) effect. Shadow color: \#000000, Radius: 1dp, Dx: 0dp, Dy: 1dp.  
* **Secondary Font (List Items, Body Text, App Names):** Cinzel or a highly legible, semi-condensed serif (e.g., Trajan Pro).  
  * *Styling:* Title case.  
  * *Rendering:* Flat, crisp antialiasing. Color: Aged Marble (\#F5EFE6).

## **4\. Global UI Components & Textures**

### **4.1. Background Architecture (The Greek Temple)**

* **Base Texture:** The root view of all internal activities (MainActivity, configuration screens) must feature a subtle, high-resolution repeating texture of dark, weathered stone or slate.  
* **Flanking Pillars:** Both the left and right edges of the screen will host vertical graphical elements depicting Doric or Ionic columns.  
  * *Implementation:* Rendered as an ImageView with scaleType="fitXY".  
  * *Visuals:* The columns will be rendered in Dark Sea Green, deeply shadowed so they blend into the Abyssal Teal background, ensuring they do not distract from the foreground UI. They provide a framing mechanism for the central content.

### **4.2. Buttons (The "Stone Block" Standard)**

All primary interactive buttons (Shortcuts, Position, Dummy, Start Service, Stop Service, Save) must adhere to the following physical properties:

* **Shape:** Strictly rectangular. No rounded corners (radius="0dp").  
* **Texture:** A custom XML drawable or 9-patch PNG simulating sandy, porous sandstone (\#D4C4A8).  
* **Elevation & Depth:**  
  * *Resting State:* Elevated (8dp). Features a distinct bottom bevel (a darker shade like \#A39171) to give the illusion of 3D thickness.  
  * *Pressed State:* The elevation drops to 0dp, and the view translates downward by 4dp (translationY). The bottom bevel disappears, simulating a stone block being physically pushed into the wall.  
* **Typography:** Centered Augustus font, dark teal color (\#071A15), with a slight white inner-shadow to appear deeply carved into the sandstone.

### **4.3. Dividers & Panels**

* **Dividers:** Replaced with horizontal graphical assets resembling Tarnished Silver spears or thin marble plinths.  
* **Panels (List Containers):** Semi-transparent Dark Seaweed (\#B3122A23) rectangles with a 2dp solid border of Faded Olive Teal.

## **5\. Screen-Specific Implementations**

### **5.1. Main Menu (The Atrium)**

* **Layout:** A vertically centered LinearLayout positioned between the two background pillars.  
* **Composition:** Contains the five mandated buttons stacked vertically.  
* **Spacing:** 24dp margins between each stone block button.

### **5.2. Shortcuts Screen (The Bipartite Vault)**

This screen is divided into two distinct interaction zones.

* **Top Zone (Current Shortcuts \- 30% height): "The Altar"**  
  * *Background:* A slightly illuminated focal point, bordered by silver filigree.  
  * *Item Styling:* Selected apps appear as small stone tiles. The app icon is framed in a silver ring.  
  * *Interaction:* Supports drag-and-drop. Dragging an item lifts it (increases shadow, scales up by 1.05x).  
* **Bottom Zone (Available Apps \- 60% height): "The Archives"**  
  * *Background:* Abyssal Teal, simulating a deep pit.  
  * *Item Styling:* RecyclerView rows. Each row is a dark slate slab. When an app is checked, the checkbox is replaced by a glowing Serpent Emerald rune or a silver checkmark.  
* **Action Area (10% height):** Contains the "Save" (Commit) button, positioned on the right side, styled as a prominent Sandstone block.

### **5.3. Positioning Screen (The Astrolabe)**

* **Viewport Mockup:** Centered in the screen is a scaled-down representation of the user's phone, styled as a flat slab of dark marble.  
* **Safe Zones:** The top 10% and bottom 10% of this marble slab are crosshatched with Faded Olive Teal lines, visually indicating the restricted placement zones.  
* **Draggable Sliver Preview:** A glowing representation of the edge sliver. When the user drags it, it emits a subtle trail of silver particles. Upon release, an animation snaps it rigidly to the left or right edge of the marble slab.

## **6\. Overlay System (The Edge Service)**

### **6.1. The Trigger Sliver (Geometric Update)**

The rectangular sliver is replaced with a dual-layered arc system.

* **Inner Arc (The Core):**  
  * *Shape:* A semi-circular arc (a segment of a circle, offset so the flat edge aligns with the screen edge).  
  * *Size:* Width is 60% of the original 12dp (approx. 7dp). Height remains functional (150dp).  
  * *Visuals:* Solid Dark Sea Green (\#2E8B57) with a subtle inner glow. Represents the physical "handle".  
* **Outer Arc (The Aura/Hit-box):**  
  * *Shape:* A concentric semi-circle, perfectly encompassing the inner arc.  
  * *Size:* 300% wider than the inner arc.  
  * *Visuals:* Ethereal Pink (\#4DFFC0CB). This serves as the functional swipe-target. It will pulse very slowly (alpha animating between 20% and 30% over a 4-second loop) to indicate it is active without being distracting.

### **6.2. The Expansion Tray**

* **Transition Animation:** When the sliver is swiped, it does not simply "appear." The tray unfurls horizontally, masking the apps as if a stone door is sliding open or a parchment is unrolling.  
* **Tray Background:** A vertical slab of dark slate. The edge facing the center of the screen features a carved Greek meander (key) pattern bordering the entire height of the tray.  
* **App Icons:** The icons within the tray are dynamically desaturated by 20% to fit the ancient theme, returning to full saturation only when pressed.

## **7\. Animations & Haptics**

* **Stone Grinding (Visuals):** Heavy UI transitions (like opening the Shortcuts screen) should utilize a slow ease-in-out interpolation curve (PathInterpolator(0.4, 0.0, 0.2, 1)) to simulate the weight of moving heavy stone.  
* **Dust Particles:** When a stone button is pressed, a highly subtle, localized particle effect of white dust can trigger around the edges of the button.  
* **Haptic Feedback:**  
  * *Button Press:* Heavy, low-frequency vibration tick (simulating stone impact).  
  * *Sliver Swipe:* A continuous, escalating vibration that snaps abruptly when the expansion tray locks into its open state.  
  * *Reordering:* A light, crisp tick when a shortcut tile is picked up, and a heavy thud when dropped into a new position on the Altar.