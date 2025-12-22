"""
Mapper - Tile Map Editor for Whitetail Engine
Modal architecture for extensibility
"""

import tkinter as tk
from tkinter import filedialog, messagebox
from PIL import Image, ImageTk
import os

SPRITE_SIZE = 16
SCALE_FACTOR = 4
DISPLAY_SIZE = SPRITE_SIZE * SCALE_FACTOR  # 64 pixels on screen

# World coordinate system: centered on (0,0)
# Supports tiles from -WORLD_OFFSET to +WORLD_OFFSET-1 in each axis
WORLD_OFFSET = 512
WORLD_SIZE = WORLD_OFFSET * 2  # 1024 tiles total


# =============================================================================
# Editor Modes
# =============================================================================

class EditorMode:
    """Base class for editor modes."""

    def __init__(self, editor):
        self.editor = editor

    def get_name(self):
        """Mode name for status bar."""
        return "unnamed"

    def get_status_hint(self):
        """Help text shown in status bar."""
        return ""

    def on_activate(self):
        """Called when switching to this mode."""
        pass

    def on_deactivate(self):
        """Called when leaving this mode."""
        pass

    def on_map_click(self, world_x, world_y, event):
        """Handle left click on map."""
        pass

    def on_map_drag(self, world_x, world_y, event):
        """Handle drag on map."""
        pass

    def on_map_right_click(self, world_x, world_y, event):
        """Handle right click on map."""
        pass

    def render_overlay(self):
        """Draw mode-specific visuals on map canvas."""
        pass

    def build_panel(self, parent):
        """Build and return a widget for the left panel, or None."""
        return None


class PaintTileMode(EditorMode):
    """Mode for painting tile sprites onto the map."""

    def __init__(self, editor):
        super().__init__(editor)
        self.palette_canvas = None

    def get_name(self):
        return "Paint"

    def get_status_hint(self):
        return "LMB: Paint | Ctrl+LMB: Pan | Hotkeys: [P]aint"

    def on_activate(self):
        self.editor.update_status(f"Brush: tile {self.editor.brush}")

    def on_map_click(self, world_x, world_y, event):
        self.paint_tile(world_x, world_y)

    def on_map_drag(self, world_x, world_y, event):
        self.paint_tile(world_x, world_y)

    def paint_tile(self, tile_x, tile_y):
        """Paint the current brush at the given world coordinates."""
        editor = self.editor

        if not editor.tile_images:
            return

        # bounds check
        if tile_x < -WORLD_OFFSET or tile_x >= WORLD_OFFSET:
            return
        if tile_y < -WORLD_OFFSET or tile_y >= WORLD_OFFSET:
            return

        # check if already painted with same tile
        if editor.map_tiles.get((tile_x, tile_y)) == editor.brush:
            return

        # remove old tile image if present
        editor.map_canvas.delete(f"maptile_{tile_x}_{tile_y}")

        # paint new tile
        px = editor.world_to_canvas_x(tile_x)
        py = editor.world_to_canvas_y(tile_y)
        editor.map_canvas.create_image(px, py, anchor=tk.NW,
                                       image=editor.tile_images[editor.brush],
                                       tags=("maptile", f"maptile_{tile_x}_{tile_y}"))

        editor.map_tiles[(tile_x, tile_y)] = editor.brush

    def build_panel(self, parent):
        """Build the tile palette panel."""
        frame = tk.Frame(parent)

        tk.Label(frame, text="Tile Palette").pack()

        palette_container = tk.Frame(frame)
        palette_container.pack(fill=tk.BOTH, expand=True)

        self.palette_canvas = tk.Canvas(palette_container, bg="#333")
        palette_scrollbar = tk.Scrollbar(palette_container, orient=tk.VERTICAL,
                                         command=self.palette_canvas.yview)
        self.palette_canvas.configure(yscrollcommand=palette_scrollbar.set)

        palette_scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        self.palette_canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        self.palette_canvas.bind("<Button-1>", self.on_palette_click)

        # draw the palette after the widget is mapped
        frame.after(50, self.refresh_palette)

        return frame

    def refresh_palette(self):
        """Redraw the palette canvas with all tiles."""
        if self.palette_canvas is None:
            return

        self.palette_canvas.delete("all")

        editor = self.editor
        if not editor.tile_images:
            return

        # figure out how many tiles fit per row
        self.palette_canvas.update_idletasks()
        canvas_width = self.palette_canvas.winfo_width()
        if canvas_width < DISPLAY_SIZE:
            canvas_width = 200  # fallback

        tiles_per_row = max(1, canvas_width // DISPLAY_SIZE)
        tile_count = len(editor.tile_images)
        rows = (tile_count + tiles_per_row - 1) // tiles_per_row

        for idx in range(tile_count):
            px = (idx % tiles_per_row) * DISPLAY_SIZE
            py = (idx // tiles_per_row) * DISPLAY_SIZE

            self.palette_canvas.create_image(px, py, anchor=tk.NW,
                                             image=editor.tile_images[idx],
                                             tags=f"tile_{idx}")

        # highlight current brush
        self.highlight_brush(tiles_per_row)

        # update scroll region
        self.palette_canvas.configure(scrollregion=(0, 0,
                                                    tiles_per_row * DISPLAY_SIZE,
                                                    rows * DISPLAY_SIZE))

    def highlight_brush(self, tiles_per_row):
        """Draw selection rectangle around current brush tile."""
        if self.palette_canvas is None:
            return

        self.palette_canvas.delete("highlight")

        editor = self.editor
        px = (editor.brush % tiles_per_row) * DISPLAY_SIZE
        py = (editor.brush // tiles_per_row) * DISPLAY_SIZE

        self.palette_canvas.create_rectangle(px, py,
                                             px + DISPLAY_SIZE, py + DISPLAY_SIZE,
                                             outline="#ffff00", width=2,
                                             tags="highlight")

    def on_palette_click(self, event):
        """Select a tile from the palette as the current brush."""
        editor = self.editor

        if not editor.tile_images:
            return

        canvas_width = self.palette_canvas.winfo_width()
        tiles_per_row = max(1, canvas_width // DISPLAY_SIZE)

        # convert canvas coords to tile index
        cx = self.palette_canvas.canvasx(event.x)
        cy = self.palette_canvas.canvasy(event.y)

        tile_x = int(cx) // DISPLAY_SIZE
        tile_y = int(cy) // DISPLAY_SIZE
        idx = tile_y * tiles_per_row + tile_x

        if idx in editor.tile_images:
            editor.brush = idx
            self.highlight_brush(tiles_per_row)
            editor.update_status(f"Brush: tile {idx}")


# =============================================================================
# Main Editor
# =============================================================================

class Mapper:
    def __init__(self, root):
        self.root = root
        self.root.title("Mapper")

        # shared state
        self.atlas_path = None
        self.atlas_image = None
        self.tile_images = {}  # index -> PhotoImage
        self.tile_pil_images = {}  # index -> PIL Image (for reference)
        self.brush = 0  # current selected tile index
        self.map_tiles = {}  # (x, y) -> atlas_index

        # navigation state
        self.is_panning = False

        # mode system
        self.modes = {}
        self.mode_keys = {}  # hotkey -> mode_name
        self.current_mode = None
        self.current_mode_name = None

        # UI references
        self.left_panel_container = None
        self.current_panel = None
        self.map_canvas = None
        self.status_var = None

        self.setup_ui()
        self.setup_modes()

    def setup_modes(self):
        """Register all editor modes."""
        self.register_mode('paint', PaintTileMode(self), 'p')
        self.set_mode('paint')

    def register_mode(self, name, mode, hotkey=None):
        """Register an editor mode."""
        self.modes[name] = mode
        if hotkey:
            self.mode_keys[hotkey.lower()] = name

    def set_mode(self, name):
        """Switch to a different editor mode."""
        if name not in self.modes:
            return

        if self.current_mode:
            self.current_mode.on_deactivate()

        self.current_mode_name = name
        self.current_mode = self.modes[name]
        self.current_mode.on_activate()

        self.rebuild_panel()
        self.refresh_overlay()
        self.update_status()

    def rebuild_panel(self):
        """Rebuild the left panel for the current mode."""
        # destroy old panel
        if self.current_panel:
            self.current_panel.destroy()
            self.current_panel = None

        # build new panel
        if self.current_mode:
            self.current_panel = self.current_mode.build_panel(self.left_panel_container)
            if self.current_panel:
                self.current_panel.pack(fill=tk.BOTH, expand=True)

    def refresh_overlay(self):
        """Refresh mode-specific overlay on map canvas."""
        # clear old overlay
        self.map_canvas.delete("overlay")

        # draw new overlay
        if self.current_mode:
            self.current_mode.render_overlay()

    def update_status(self, message=None):
        """Update status bar with mode info and optional message."""
        if message:
            self.status_var.set(f"[{self.current_mode.get_name()}] {message}")
        else:
            hint = self.current_mode.get_status_hint() if self.current_mode else ""
            self.status_var.set(f"[{self.current_mode.get_name()}] {hint}")

    def setup_ui(self):
        # menu bar
        menubar = tk.Menu(self.root)
        file_menu = tk.Menu(menubar, tearoff=0)
        file_menu.add_command(label="Load Atlas...", command=self.load_atlas)
        file_menu.add_separator()
        file_menu.add_command(label="Load Map...", command=self.load_map)
        file_menu.add_command(label="Save Map...", command=self.save_map)
        file_menu.add_separator()
        file_menu.add_command(label="Exit", command=self.root.quit)
        menubar.add_cascade(label="File", menu=file_menu)
        self.root.config(menu=menubar)

        # main horizontal paned window
        paned = tk.PanedWindow(self.root, orient=tk.HORIZONTAL)
        paned.pack(fill=tk.BOTH, expand=True)

        # left frame: dynamic panel container
        self.left_panel_container = tk.Frame(paned)
        paned.add(self.left_panel_container, width=200)

        # right frame: map canvas
        right_frame = tk.Frame(paned)
        paned.add(right_frame)

        tk.Label(right_frame, text="Map").pack()

        map_container = tk.Frame(right_frame)
        map_container.pack(fill=tk.BOTH, expand=True)

        self.map_canvas = tk.Canvas(map_container, bg="#1a1a1a",
                                    scrollregion=(0, 0,
                                                  WORLD_SIZE * DISPLAY_SIZE,
                                                  WORLD_SIZE * DISPLAY_SIZE))

        map_scrollbar_v = tk.Scrollbar(map_container, orient=tk.VERTICAL,
                                       command=self.map_canvas.yview)
        map_scrollbar_h = tk.Scrollbar(map_container, orient=tk.HORIZONTAL,
                                       command=self.map_canvas.xview)
        self.map_canvas.configure(yscrollcommand=map_scrollbar_v.set,
                                  xscrollcommand=map_scrollbar_h.set)

        map_scrollbar_v.pack(side=tk.RIGHT, fill=tk.Y)
        map_scrollbar_h.pack(side=tk.BOTTOM, fill=tk.X)
        self.map_canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        # bind map events
        self.map_canvas.bind("<Button-1>", self.on_map_click)
        self.map_canvas.bind("<B1-Motion>", self.on_map_drag)
        self.map_canvas.bind("<ButtonRelease-1>", self.on_map_release)
        self.map_canvas.bind("<Button-3>", self.on_map_right_click)

        # bind hotkeys
        self.root.bind("<Key>", self.on_key)

        # draw grid on map canvas
        self.draw_map_grid()

        # center view on origin
        self.root.after(100, lambda: self.center_view_on(0, 0))

        # status bar
        self.status_var = tk.StringVar(value="Load an atlas to begin")
        status_bar = tk.Label(self.root, textvariable=self.status_var,
                              anchor=tk.W, relief=tk.SUNKEN)
        status_bar.pack(side=tk.BOTTOM, fill=tk.X)

    def on_key(self, event):
        """Handle hotkey presses."""
        key = event.char.lower()
        if key in self.mode_keys:
            self.set_mode(self.mode_keys[key])

    def on_map_click(self, event):
        """Handle map click (Paint vs Pan)."""
        # Check if Control key is held (Bit 2 is usually 4)
        if event.state & 0x0004:
            self.is_panning = True
            # scan_mark sets the anchor point for the drag
            self.map_canvas.scan_mark(event.x, event.y)
            self.map_canvas.config(cursor="fleur")  # Visual feedback
            return

        if not self.current_mode:
            return

        cx = self.map_canvas.canvasx(event.x)
        cy = self.map_canvas.canvasy(event.y)
        world_x = self.canvas_to_world_x(cx)
        world_y = self.canvas_to_world_y(cy)

        self.current_mode.on_map_click(world_x, world_y, event)

    def on_map_drag(self, event):
        """Handle map drag (Paint vs Pan)."""
        if self.is_panning:
            # scan_dragto scrolls the canvas relative to scan_mark
            self.map_canvas.scan_dragto(event.x, event.y, gain=1)
            return

        if not self.current_mode:
            return

        cx = self.map_canvas.canvasx(event.x)
        cy = self.map_canvas.canvasy(event.y)
        world_x = self.canvas_to_world_x(cx)
        world_y = self.canvas_to_world_y(cy)

        self.current_mode.on_map_drag(world_x, world_y, event)

    def on_map_release(self, event):
        """Handle mouse release."""
        if self.is_panning:
            self.is_panning = False
            self.map_canvas.config(cursor="")  # Reset cursor

    def on_map_right_click(self, event):
        """Delegate map right-click to current mode."""
        if not self.current_mode:
            return

        cx = self.map_canvas.canvasx(event.x)
        cy = self.map_canvas.canvasy(event.y)
        world_x = self.canvas_to_world_x(cx)
        world_y = self.canvas_to_world_y(cy)

        self.current_mode.on_map_right_click(world_x, world_y, event)

    def draw_map_grid(self):
        """Draw a subtle grid on the map canvas."""
        GRID_RANGE = 64

        for i in range(-GRID_RANGE, GRID_RANGE + 1):
            cx = self.world_to_canvas_x(i)
            cy_start = self.world_to_canvas_y(-GRID_RANGE)
            cy_end = self.world_to_canvas_y(GRID_RANGE)
            self.map_canvas.create_line(cx, cy_start, cx, cy_end,
                                        fill="#2a2a2a", tags="grid")
        for i in range(-GRID_RANGE, GRID_RANGE + 1):
            cy = self.world_to_canvas_y(i)
            cx_start = self.world_to_canvas_x(-GRID_RANGE)
            cx_end = self.world_to_canvas_x(GRID_RANGE)
            self.map_canvas.create_line(cx_start, cy, cx_end, cy,
                                        fill="#2a2a2a", tags="grid")

        # draw origin crosshair
        origin_x = self.world_to_canvas_x(0)
        origin_y = self.world_to_canvas_y(0)
        self.map_canvas.create_line(origin_x, cy_start, origin_x, cy_end,
                                    fill="#444", tags="grid")
        self.map_canvas.create_line(cx_start, origin_y, cx_end, origin_y,
                                    fill="#444", tags="grid")

    def world_to_canvas_x(self, world_x):
        """Convert world tile X to canvas pixel X."""
        return (world_x + WORLD_OFFSET) * DISPLAY_SIZE

    def world_to_canvas_y(self, world_y):
        """Convert world tile Y to canvas pixel Y."""
        return (world_y + WORLD_OFFSET) * DISPLAY_SIZE

    def canvas_to_world_x(self, canvas_x):
        """Convert canvas pixel X to world tile X."""
        return int(canvas_x) // DISPLAY_SIZE - WORLD_OFFSET

    def canvas_to_world_y(self, canvas_y):
        """Convert canvas pixel Y to world tile Y."""
        return int(canvas_y) // DISPLAY_SIZE - WORLD_OFFSET

    def center_view_on(self, world_x, world_y):
        """Center the map view on a world coordinate."""
        canvas_x = self.world_to_canvas_x(world_x)
        canvas_y = self.world_to_canvas_y(world_y)

        total_width = WORLD_SIZE * DISPLAY_SIZE
        total_height = WORLD_SIZE * DISPLAY_SIZE

        frac_x = canvas_x / total_width
        frac_y = canvas_y / total_height

        self.map_canvas.xview_moveto(max(0, frac_x - 0.1))
        self.map_canvas.yview_moveto(max(0, frac_y - 0.1))

    def load_atlas(self):
        """Load a sprite atlas PNG and build tile index."""
        path = filedialog.askopenfilename(
            title="Select Sprite Atlas",
            filetypes=[("PNG files", "*.png"), ("All files", "*.*")]
        )
        if not path:
            return

        try:
            img = Image.open(path)
        except Exception as e:
            messagebox.showerror("Error", f"Failed to load image: {e}")
            return

        width, height = img.size
        if width != height:
            messagebox.showerror("Error", "Atlas must be square")
            return
        if width & (width - 1) != 0:
            messagebox.showerror("Error", "Atlas dimensions must be power of 2")
            return
        if width % SPRITE_SIZE != 0:
            messagebox.showerror("Error", f"Atlas size must be multiple of {SPRITE_SIZE}")
            return

        if img.mode != "RGBA":
            img = img.convert("RGBA")

        self.atlas_path = path
        self.atlas_image = img
        self.tile_images.clear()
        self.tile_pil_images.clear()

        tiles_per_row = width // SPRITE_SIZE
        tile_count = tiles_per_row * tiles_per_row

        for idx in range(tile_count):
            tx = (idx % tiles_per_row) * SPRITE_SIZE
            ty = (idx // tiles_per_row) * SPRITE_SIZE

            tile_pil = img.crop((tx, ty, tx + SPRITE_SIZE, ty + SPRITE_SIZE))
            tile_scaled = tile_pil.resize((DISPLAY_SIZE, DISPLAY_SIZE), Image.NEAREST)
            tile_tk = ImageTk.PhotoImage(tile_scaled)

            self.tile_pil_images[idx] = tile_pil
            self.tile_images[idx] = tile_tk

        self.brush = 0

        # refresh the current mode's panel (for palette update)
        self.rebuild_panel()

        self.update_status(f"Loaded atlas: {os.path.basename(path)} ({tile_count} tiles)")

    def load_map(self):
        """Load a map file."""
        if not self.tile_images:
            messagebox.showwarning("Warning", "Load an atlas first")
            return

        path = filedialog.askopenfilename(
            title="Load Map",
            filetypes=[("Map files", "*.map"), ("All files", "*.*")]
        )
        if not path:
            return

        try:
            with open(path, 'r') as f:
                lines = f.readlines()
        except Exception as e:
            messagebox.showerror("Error", f"Failed to read file: {e}")
            return

        header = {}
        tiles = {}
        in_tiles = False

        for line_num, line in enumerate(lines, 1):
            line = line.strip()

            if not line or line.startswith('#'):
                continue

            if line == '---':
                in_tiles = True
                continue

            if not in_tiles:
                if ':' in line:
                    key, value = line.split(':', 1)
                    header[key.strip()] = value.strip()
            else:
                parts = line.rstrip(',').split(',')
                if len(parts) >= 3:
                    try:
                        x = int(parts[0])
                        y = int(parts[1])
                        atlas_idx = int(parts[2])

                        if atlas_idx in self.tile_images:
                            tiles[(x, y)] = atlas_idx
                        else:
                            print(f"Warning line {line_num}: atlas index {atlas_idx} not in loaded atlas")
                    except ValueError as e:
                        print(f"Warning line {line_num}: failed to parse tile: {e}")

        self.map_tiles.clear()
        self.map_tiles.update(tiles)

        self.redraw_map_tiles()

        if self.map_tiles:
            min_x = min(x for x, y in self.map_tiles.keys())
            max_x = max(x for x, y in self.map_tiles.keys())
            min_y = min(y for x, y in self.map_tiles.keys())
            max_y = max(y for x, y in self.map_tiles.keys())
            center_x = (min_x + max_x) // 2
            center_y = (min_y + max_y) // 2
            self.center_view_on(center_x, center_y)
        else:
            self.center_view_on(0, 0)

        tile_count = len(self.map_tiles)
        map_name = header.get('name', 'Unknown')
        self.update_status(f"Loaded map: {map_name} ({tile_count} tiles)")

    def redraw_map_tiles(self):
        """Clear and redraw all tiles on the map canvas."""
        self.map_canvas.delete("maptile")

        for (x, y), atlas_idx in self.map_tiles.items():
            if atlas_idx in self.tile_images:
                px = self.world_to_canvas_x(x)
                py = self.world_to_canvas_y(y)
                self.map_canvas.create_image(px, py, anchor=tk.NW,
                                             image=self.tile_images[atlas_idx],
                                             tags=("maptile", f"maptile_{x}_{y}"))

    def save_map(self):
        """Save the current map to a file."""
        if not self.map_tiles:
            messagebox.showwarning("Warning", "Map is empty, nothing to save")
            return

        path = filedialog.asksaveasfilename(
            title="Save Map",
            defaultextension=".map",
            filetypes=[("Map files", "*.map"), ("All files", "*.*")]
        )
        if not path:
            return

        min_x = min(x for x, y in self.map_tiles.keys())
        max_x = max(x for x, y in self.map_tiles.keys())
        min_y = min(y for x, y in self.map_tiles.keys())
        max_y = max(y for x, y in self.map_tiles.keys())

        width = max_x - min_x + 1
        height = max_y - min_y + 1

        atlas_name = os.path.basename(self.atlas_path) if self.atlas_path else "unknown"
        try:
            with open(path, 'w') as f:
                f.write(f"name:Untitled\n")
                f.write(f"width:{width}\n")
                f.write(f"height:{height}\n")
                f.write(f"origin:{min_x},{min_y}\n")
                f.write(f"tileset:{atlas_name}\n")
                f.write("---\n")

                for (x, y), atlas_idx in sorted(self.map_tiles.items()):
                    f.write(f"{x},{y},{atlas_idx},0,\n")

            self.update_status(f"Saved: {os.path.basename(path)} ({width}x{height}, {len(self.map_tiles)} tiles)")
        except Exception as e:
            messagebox.showerror("Error", f"Failed to save: {e}")


def main():
    root = tk.Tk()
    root.geometry("1024x768")
    app = Mapper(root)
    root.mainloop()


if __name__ == "__main__":
    main()