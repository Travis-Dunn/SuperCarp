"""
Stitcher - Sprite Atlas Generator
Walks a directory, collects all PNGs, snips them into sprites, and creates a sprite atlas.
"""

import os
import sys
import math
from PIL import Image

# =============================================================================
# CONFIGURATION - Modify these constants as needed
# =============================================================================
SPRITE_SIZE = 16  # Width and height of each sprite in pixels
SOURCE_DIR = "./sprites"  # Directory to scan for PNGs
OUTPUT_FILE = "output.png"  # Output atlas filename
BACKGROUND_COLOR = (22, 13, 19)  # Palette black, treated as empty
# =============================================================================


def is_empty(sprite: Image.Image) -> bool:
    """
    Check if a sprite is empty.
    Empty means either fully transparent or entirely the background color.
    """
    pixels = sprite.getdata()

    for pixel in pixels:
        r, g, b, a = pixel

        # Fully transparent pixel is considered empty
        if a == 0:
            continue

        # Background color pixel is considered empty
        if (r, g, b) == BACKGROUND_COLOR:
            continue

        # Found a non-empty pixel
        return False

    return True


def find_all_pngs(directory: str) -> list[str]:
    """Recursively find all PNG files in the given directory."""
    png_files = []
    for root, _, files in os.walk(directory):
        for file in files:
            if file.lower().endswith(".png"):
                png_files.append(os.path.join(root, file))
    return png_files


def validate_and_snip_image(filepath: str, sprite_size: int) -> list[Image.Image]:
    """
    Validate that image dimensions are multiples of sprite_size,
    then snip into sprite_size x sprite_size squares.
    Returns a list of sprite images.
    """
    img = Image.open(filepath).convert("RGBA")
    width, height = img.size

    if width % sprite_size != 0 or height % sprite_size != 0:
        print(f"Error: '{filepath}' has dimensions {width}x{height}, "
              f"which is not a multiple of {sprite_size}")
        sys.exit(-1)

    sprites = []
    for y in range(0, height, sprite_size):
        for x in range(0, width, sprite_size):
            sprite = img.crop((x, y, x + sprite_size, y + sprite_size))
            if not is_empty(sprite):
                sprites.append(sprite)

    return sprites


def calculate_atlas_size(sprite_count: int, sprite_size: int) -> int:
    """
    Calculate the atlas dimension (width = height) that is:
    - Square
    - Power of 2
    - Large enough to hold all sprites
    Returns the atlas dimension in pixels.
    """
    sprites_per_side = math.ceil(math.sqrt(sprite_count))
    min_dimension = sprites_per_side * sprite_size

    # Find the smallest power of 2 that fits
    power = 1
    while power < min_dimension:
        power *= 2

    return power


def create_atlas(sprites: list[Image.Image], atlas_size: int, sprite_size: int) -> Image.Image:
    """Create the atlas image and paste all sprites onto it."""
    atlas = Image.new("RGBA", (atlas_size, atlas_size), (0, 0, 0, 0))

    sprites_per_row = atlas_size // sprite_size
    for i, sprite in enumerate(sprites):
        x = (i % sprites_per_row) * sprite_size
        y = (i // sprites_per_row) * sprite_size
        atlas.paste(sprite, (x, y))

    return atlas


def main():
    print(f"Stitcher - Sprite Atlas Generator")
    print(f"Source directory: {SOURCE_DIR}")
    print(f"Sprite size: {SPRITE_SIZE}x{SPRITE_SIZE}")
    print("-" * 40)

    # Find all PNGs
    png_files = find_all_pngs(SOURCE_DIR)
    if not png_files:
        print(f"No PNG files found in '{SOURCE_DIR}'")
        sys.exit(-1)

    print(f"Found {len(png_files)} PNG file(s)")

    # Validate and snip all images into sprites
    all_sprites = []
    for filepath in png_files:
        sprites = validate_and_snip_image(filepath, SPRITE_SIZE)
        all_sprites.extend(sprites)
        print(f"  {filepath}: {len(sprites)} sprite(s)")

    print(f"Total sprites: {len(all_sprites)}")

    # Calculate atlas size
    atlas_size = calculate_atlas_size(len(all_sprites), SPRITE_SIZE)
    sprites_capacity = (atlas_size // SPRITE_SIZE) ** 2
    print(f"Atlas size: {atlas_size}x{atlas_size} (capacity: {sprites_capacity} sprites)")

    # Create and save atlas
    atlas = create_atlas(all_sprites, atlas_size, SPRITE_SIZE)
    atlas.save(OUTPUT_FILE)
    print(f"Saved atlas to '{OUTPUT_FILE}'")


if __name__ == "__main__":
    main()