#!/bin/bash
# ─────────────────────────────────────────────────────────────────
# stop-infra.sh — Stop all Spring Boot services + Docker infra
# Usage:  ./stop-infra.sh
# ─────────────────────────────────────────────────────────────────

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# ── Step 1: Stop Spring Boot services ────────────────────────────
echo ""
echo "[STOP] Stopping Spring Boot services..."
bash "$SCRIPT_DIR/stop-backend.sh"

# ── Step 2: Stop Docker infra containers ─────────────────────────
echo ""
echo "[STOP] Stopping Docker infra containers..."
if ! docker info >/dev/null 2>&1; then
  echo "[STOP] Docker not reachable — skipping."
else
  # Stop kafka-only prod compose if it was used
  if [ -f "$SCRIPT_DIR/docker-compose.kafka-prod.yml" ]; then
    docker compose -f "$SCRIPT_DIR/docker-compose.kafka-prod.yml" down 2>/dev/null
  fi
  # Stop full dev infra compose if it was used
  if [ -f "$SCRIPT_DIR/docker-compose.infra.yml" ]; then
    docker compose -f "$SCRIPT_DIR/docker-compose.infra.yml" down 2>/dev/null
  fi
  echo "[STOP] ✅ Docker containers stopped."
fi

# ── Step 3: Stop Colima (macOS only) ─────────────────────────────
if [ "$(uname -s)" = "Darwin" ] && command -v colima >/dev/null 2>&1; then
  echo ""
  echo "[STOP] Stopping Colima..."
  if colima status 2>/dev/null | grep -q "running"; then
    colima stop && echo "[STOP] ✅ Colima stopped."
  else
    echo "[STOP] Colima was not running."
  fi
fi

# ── Summary ───────────────────────────────────────────────────────
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Full shutdown complete."
echo "  To restart: ./start-backend.sh prod"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
