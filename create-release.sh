#!/bin/bash

# Home Library - Release Package Creator
# Creates a distributable package with JAR, dependencies, and launcher scripts

set -e  # Exit on error

VERSION="1.0.0"
RELEASE_NAME="home-library-v${VERSION}"
RELEASE_DIR="release"

echo "========================================="
echo "Home Library Release Package Creator"
echo "Version: ${VERSION}"
echo "========================================="
echo ""

# Step 1: Clean and build
echo "[1/5] Building project..."
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is required to create releases"
    exit 1
fi

mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "Error: Build failed!"
    exit 1
fi

echo "✓ Build successful"
echo ""

# Step 2: Create release directory
echo "[2/5] Creating release directory..."
rm -rf "${RELEASE_DIR}"
mkdir -p "${RELEASE_DIR}/${RELEASE_NAME}"
echo "✓ Release directory created"
echo ""

# Step 3: Copy files
echo "[3/5] Copying files..."

# Copy JAR
cp target/home-library-${VERSION}.jar "${RELEASE_DIR}/${RELEASE_NAME}/"
echo "  ✓ Copied JAR file"

# Copy dependencies
cp -r target/lib "${RELEASE_DIR}/${RELEASE_NAME}/"
echo "  ✓ Copied dependencies ($(ls target/lib | wc -l) files)"

# Copy launcher scripts
cp run.sh "${RELEASE_DIR}/${RELEASE_NAME}/"
cp run.bat "${RELEASE_DIR}/${RELEASE_NAME}/"
chmod +x "${RELEASE_DIR}/${RELEASE_NAME}/run.sh"
echo "  ✓ Copied launcher scripts"

# Copy README
cp README.md "${RELEASE_DIR}/${RELEASE_NAME}/"
echo "  ✓ Copied README"

# Create a simple INSTALL.txt
cat > "${RELEASE_DIR}/${RELEASE_NAME}/INSTALL.txt" << 'EOF'
Home Library - Installation Instructions
=========================================

REQUIREMENTS:
- Java 17 or higher

QUICK START:

  Linux/Mac:
    1. Extract this archive
    2. Open terminal in the extracted folder
    3. Run: ./run.sh

  Windows:
    1. Extract this archive
    2. Open Command Prompt in the extracted folder
    3. Run: run.bat

The application will start automatically!

DATABASE:
- The application creates a 'library.db' file in the same directory
- Cover images are stored in the 'covers/' directory

TROUBLESHOOTING:
- If you get "Java not found", install Java 17+ from:
  https://adoptium.net/

For more information, see README.md
EOF

echo "  ✓ Created INSTALL.txt"
echo ""

# Step 4: Create archives
echo "[4/5] Creating release archives..."

cd "${RELEASE_DIR}"

# Create ZIP archive
zip -r "${RELEASE_NAME}.zip" "${RELEASE_NAME}" > /dev/null
echo "  ✓ Created ${RELEASE_NAME}.zip ($(du -h ${RELEASE_NAME}.zip | cut -f1))"

# Create TAR.GZ archive
tar -czf "${RELEASE_NAME}.tar.gz" "${RELEASE_NAME}"
echo "  ✓ Created ${RELEASE_NAME}.tar.gz ($(du -h ${RELEASE_NAME}.tar.gz | cut -f1))"

cd ..

echo ""

# Step 5: Summary
echo "[5/5] Summary"
echo "========================================="
echo "Release packages created in: ${RELEASE_DIR}/"
echo ""
echo "Files:"
ls -lh "${RELEASE_DIR}"/*.zip "${RELEASE_DIR}"/*.tar.gz 2>/dev/null | awk '{print "  - " $9 " (" $5 ")"}'
echo ""
echo "Contents:"
echo "  - home-library-${VERSION}.jar"
echo "  - lib/ (dependencies)"
echo "  - run.sh (Linux/Mac launcher)"
echo "  - run.bat (Windows launcher)"
echo "  - README.md"
echo "  - INSTALL.txt"
echo ""
echo "✓ Release ready for distribution!"
echo ""
echo "Next steps:"
echo "  1. Test the release: cd ${RELEASE_DIR}/${RELEASE_NAME} && ./run.sh"
echo "  2. Upload to GitHub releases or share the ZIP/TAR.GZ file"
echo "========================================="
