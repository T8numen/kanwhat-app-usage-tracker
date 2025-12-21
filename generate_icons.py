 """
Generate Android app icons from the source logo
Requires Pillow: pip install Pillow
"""
from PIL import Image
import os

# Define the icon sizes for different densities
ICON_SIZES = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192
}

def generate_icons():
    # Source logo path
    source_logo = 'app/src/main/res/drawable/app_logo.png'

    if not os.path.exists(source_logo):
        print(f"Error: Source logo not found at {source_logo}")
        return

    # Open the source image
    try:
        img = Image.open(source_logo)
        print(f"Source image size: {img.size}")
        print(f"Source image mode: {img.mode}")

        # Convert to RGBA if not already
        if img.mode != 'RGBA':
            img = img.convert('RGBA')

        # Generate icons for each density
        for folder, size in ICON_SIZES.items():
            folder_path = f'app/src/main/res/{folder}'

            # Create folder if it doesn't exist
            os.makedirs(folder_path, exist_ok=True)

            # Resize the image
            resized = img.resize((size, size), Image.Resampling.LANCZOS)

            # Save as PNG
            output_path = os.path.join(folder_path, 'ic_launcher.png')
            resized.save(output_path, 'PNG')
            print(f"Generated: {output_path} ({size}x{size})")

            # Also create round icon (same image)
            output_path_round = os.path.join(folder_path, 'ic_launcher_round.png')
            resized.save(output_path_round, 'PNG')
            print(f"Generated: {output_path_round} ({size}x{size})")

        print("\nIcon generation complete!")
        print("\nNote: The adaptive icon XML files in mipmap-anydpi will use these PNGs.")

    except Exception as e:
        print(f"Error: {e}")

if __name__ == '__main__':
    generate_icons()

