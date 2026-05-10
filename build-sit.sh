#!/bin/bash
set -e

JAVA_HOME_17=$(/usr/libexec/java_home -v 17 2>/dev/null || echo "$JAVA_HOME")
DIST_DIR="$(pwd)/sit-package"
VERSION="1.0.0"
JAR_NAME="crq-approval-${VERSION}.jar"

echo "======================================"
echo "  CRQ Approval - SIT Package Builder"
echo "======================================"

# ── 1. Build React frontend ────────────────
echo ""
echo "[1/3] Building React frontend..."
cd frontend
npm install
npm run build
echo "      Frontend built → frontend/dist/"
cd ..

# ── 2. Build Spring Boot JAR ──────────────
echo ""
echo "[2/3] Building Spring Boot JAR (with embedded React)..."
cd backend
JAVA_HOME="$JAVA_HOME_17" ./mvnw clean package -DskipTests -q
echo "      JAR built → backend/target/${JAR_NAME}"
cd ..

# ── 3. Assemble deployment package ────────
echo ""
echo "[3/3] Assembling SIT deployment package..."
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

# Copy JAR
cp "backend/target/${JAR_NAME}" "$DIST_DIR/"

# Copy SIT config (to be customised on the SIT server)
cp "backend/src/main/resources/application-sit.properties" "$DIST_DIR/"

# Create start script
cat > "$DIST_DIR/start.sh" << 'EOF'
#!/bin/bash
# Usage: ./start.sh
# Set SIT values in application-sit.properties before running.

JAVA_OPTS="-Xms256m -Xmx512m"
JAR=$(ls crq-approval-*.jar | head -1)

echo "Starting CRQ Approval in SIT mode..."
java $JAVA_OPTS \
  -Dspring.profiles.active=sit \
  -Dspring.config.additional-location=file:./application-sit.properties \
  -jar "$JAR"
EOF
chmod +x "$DIST_DIR/start.sh"

# Create stop script
cat > "$DIST_DIR/stop.sh" << 'EOF'
#!/bin/bash
PID=$(pgrep -f "crq-approval.*\.jar" | head -1)
if [ -n "$PID" ]; then
  echo "Stopping CRQ Approval (PID $PID)..."
  kill "$PID"
else
  echo "CRQ Approval is not running."
fi
EOF
chmod +x "$DIST_DIR/stop.sh"

# Zip it up
ZIP_NAME="crq-approval-sit-${VERSION}.zip"
cd "$(dirname "$DIST_DIR")"
zip -r "$ZIP_NAME" "$(basename "$DIST_DIR")" -x "*.DS_Store"
echo ""
echo "======================================"
echo "  Package ready: ${ZIP_NAME}"
echo "======================================"
echo ""
echo "  Contents:"
echo "    crq-approval-${VERSION}.jar     ← Application (React + Spring Boot)"
echo "    application-sit.properties      ← SIT config (fill in credentials)"
echo "    start.sh                        ← Start the app"
echo "    stop.sh                         ← Stop the app"
echo ""
echo "  SIT Deployment steps:"
echo "    1. Copy ${ZIP_NAME} to the SIT server"
echo "    2. unzip ${ZIP_NAME}"
echo "    3. Edit application-sit.properties with real SIT values"
echo "    4. Ensure Java 17 is installed: java -version"
echo "    5. ./start.sh"
echo "    6. Open http://<sit-server>:8080 in a browser"
echo ""
