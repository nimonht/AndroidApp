#!/bin/bash

# Firebase Backend Deployment Script

set -e  # Exit on error

text="=================================================================
Firebase Backend Deployment Script, Made by Nimonht with luv  ❤️
================================================================="
colors=(31 33 32 36 34 35)
i=0
for ((j=0; j<${#text}; j++)); do
    char="${text:$j:1}"
    if [ "$char" = $'\n' ]; then
        echo
    else
        echo -ne "\033[${colors[$i]}m$char"
        i=$(( (i+1) % 6 ))
    fi
done

echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# ---------------------------------------------------------------------------
# Resolve project root (one level up from scripts/)
# ---------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# ---------------------------------------------------------------------------
# Detect operating system
# ---------------------------------------------------------------------------
detect_os() {
    case "$(uname -s 2>/dev/null || echo Unknown)" in
        Linux*)   echo "Linux"   ;;
        Darwin*)  echo "macOS"   ;;
        CYGWIN*|MINGW*|MSYS*) echo "Windows" ;;
        *)        echo "Unknown" ;;
    esac
}

OS_NAME="$(detect_os)"
echo -e "${CYAN}Detected OS: ${OS_NAME}${NC}"

# ---------------------------------------------------------------------------
# Docker detection – works on Linux, macOS, and Windows (Git Bash / WSL)
# ---------------------------------------------------------------------------
DOCKER_AVAILABLE=false
DOCKER_CMD=""

detect_docker() {
    local candidate

    # 1. Try the standard "docker" command
    if command -v docker &> /dev/null; then
        # Verify the daemon is reachable
        if docker info &> /dev/null; then
            DOCKER_AVAILABLE=true
            DOCKER_CMD="docker"
            return 0
        fi
    fi

    # 2. On Windows (Git Bash / MSYS / MINGW) check common install paths
    if [ "$OS_NAME" = "Windows" ]; then
        for candidate in \
            "/c/Program Files/Docker/Docker/resources/bin/docker.exe" \
            "/c/ProgramData/DockerDesktop/version-bin/docker.exe"; do
            if [ -x "$candidate" ] && "$candidate" info &> /dev/null; then
                DOCKER_AVAILABLE=true
                DOCKER_CMD="$candidate"
                return 0
            fi
        done
        # Also try docker.exe directly (might be on PATH without "docker")
        if command -v docker.exe &> /dev/null; then
            if docker.exe info &> /dev/null; then
                DOCKER_AVAILABLE=true
                DOCKER_CMD="docker.exe"
                return 0
            fi
        fi
    fi

    # 3. On macOS check Homebrew / Applications paths
    if [ "$OS_NAME" = "macOS" ]; then
        for candidate in \
            "/usr/local/bin/docker" \
            "/opt/homebrew/bin/docker" \
            "$HOME/.docker/bin/docker"; do
            if [ -x "$candidate" ] && "$candidate" info &> /dev/null; then
                DOCKER_AVAILABLE=true
                DOCKER_CMD="$candidate"
                return 0
            fi
        done
    fi

    return 1
}

detect_docker

# ---------------------------------------------------------------------------
# Execution-mode selection (Docker vs native)
# ---------------------------------------------------------------------------
USE_DOCKER=false
FIREBASE_DOCKER_IMAGE="firebase-tools-local"

if [ -n "${XDG_CONFIG_HOME:-}" ]; then
    FIREBASE_CONFIG_BASE_DIR="$XDG_CONFIG_HOME"
elif [ -n "${HOME:-}" ]; then
    FIREBASE_CONFIG_BASE_DIR="${HOME}/.config"
else
    echo -e "${RED}Error: Unable to determine Firebase config directory because neither XDG_CONFIG_HOME nor HOME is available.${NC}"
    echo "Please ensure either XDG_CONFIG_HOME or HOME is set before running this script."
    exit 1
fi

FIREBASE_CONFIG_DIR="$FIREBASE_CONFIG_BASE_DIR/configstore"
FIREBASE_AUTH_CHECK_COMMAND="projects:list"
DOCKER_FIREBASE_EMULATOR_CONFIG_CONTAINER_PATH="/workspace/firebase.docker.json"

if [ "$DOCKER_AVAILABLE" = true ]; then
    echo -e "${GREEN}✓ Docker detected (${DOCKER_CMD})${NC}"
    echo ""
    echo -e "${CYAN}Would you like to run Firebase tools inside a Docker container?${NC}"
    echo "  This avoids installing Firebase CLI or Node.js on your machine."
    echo "  [y] Yes – use Docker   [n] No – use native Firebase CLI"
    read -p "Your choice [y/n]: " docker_choice
    case "$docker_choice" in
        [yY]|[yY][eE][sS])
            USE_DOCKER=true
            echo -e "${GREEN}✓ Docker mode selected${NC}"
            ;;
        *)
            echo -e "${YELLOW}→ Native mode selected${NC}"
            ;;
    esac
else
    echo -e "${YELLOW}⚠ Docker not detected – using native Firebase CLI${NC}"
fi

# ---------------------------------------------------------------------------
# Build the Docker image (only once per session if Docker mode is on)
# ---------------------------------------------------------------------------
build_docker_image() {
    if ! "$DOCKER_CMD" image inspect "$FIREBASE_DOCKER_IMAGE" &> /dev/null; then
        echo -e "${YELLOW}Building Firebase Docker image (first-time only)...${NC}"
        "$DOCKER_CMD" build \
            -t "$FIREBASE_DOCKER_IMAGE" \
            -f "$SCRIPT_DIR/firebase.Dockerfile" \
            "$PROJECT_ROOT"
        echo -e "${GREEN}✓ Docker image built${NC}"
    else
        echo -e "${GREEN}✓ Firebase Docker image already exists${NC}"
    fi
}

docker_run_firebase_raw() {
    "$DOCKER_CMD" run --rm "$@"
}

docker_firebase_auth_mount() {
    local mount_mode="${1:-ro}"
    local mount_path="$FIREBASE_CONFIG_DIR:/root/.config/configstore"

    if [ "$mount_mode" = "rw" ]; then
        printf '%s' "$mount_path"
    else
        printf '%s:ro' "$mount_path"
    fi
}

docker_has_firebase_auth() {
    docker_run_firebase_raw \
        -v "$PROJECT_ROOT:/workspace" \
        -v "$(docker_firebase_auth_mount rw)" \
        "$FIREBASE_DOCKER_IMAGE" "$FIREBASE_AUTH_CHECK_COMMAND" &> /dev/null
}

# ---------------------------------------------------------------------------
# Wrapper: run a firebase command natively or inside Docker
# ---------------------------------------------------------------------------
run_firebase() {
    if [ "$USE_DOCKER" = true ]; then
        local FIREBASE_ARGS
        FIREBASE_ARGS=("$@")

        # Determine whether we need emulator port forwarding
        local DOCKER_PORT_ARGS=()
        local use_docker_emulator_config=false
        for arg in "${FIREBASE_ARGS[@]}"; do
            if [ "$arg" = "emulators:start" ]; then
                DOCKER_PORT_ARGS=(-p 4000:4000 -p 4400:4400 -p 4500:4500 -p 8080:8080 -p 9099:9099 -p 9150:9150 -p 9199:9199)
                use_docker_emulator_config=true
                break
            fi
        done

        if [ "$use_docker_emulator_config" = true ]; then
            FIREBASE_ARGS+=(--config "$DOCKER_FIREBASE_EMULATOR_CONFIG_CONTAINER_PATH")
        fi

        # Mount the project root, forward Firebase credentials, and (if needed) emulator ports
        docker_run_firebase_raw -it \
            -v "$PROJECT_ROOT:/workspace" \
            -v "$(docker_firebase_auth_mount rw)" \
            "${DOCKER_PORT_ARGS[@]}" \
            "$FIREBASE_DOCKER_IMAGE" "${FIREBASE_ARGS[@]}"
    else
        firebase "$@"
    fi
}

ensure_firebase_auth() {
    if [ "$USE_DOCKER" = true ]; then
        mkdir -p "$FIREBASE_CONFIG_DIR"

        if ! docker_has_firebase_auth; then
            echo -e "${YELLOW}Firebase authentication is required before using Docker mode${NC}"

            if command -v firebase &> /dev/null; then
                echo "Opening system browser for Firebase authentication..."
                firebase login
            else
                echo -e "${YELLOW}Native Firebase CLI not found. Falling back to Docker login.${NC}"
                echo "Complete the browser-based sign-in using the URL/code shown below."
                docker_run_firebase_raw -it \
                    -v "$(docker_firebase_auth_mount rw)" \
                    "$FIREBASE_DOCKER_IMAGE" login --no-localhost
            fi
        fi

        if ! docker_has_firebase_auth; then
            echo -e "${RED}Error: Firebase authentication is still unavailable in Docker mode.${NC}"
            echo "Please make sure the login completed successfully and that Firebase CLI credential files were created in: \"$FIREBASE_CONFIG_DIR\""
            echo "If needed, install the Firebase CLI and rerun 'firebase login' natively before starting Docker mode again."
            exit 1
        fi
    else
        if ! firebase "$FIREBASE_AUTH_CHECK_COMMAND" &> /dev/null; then
            echo -e "${YELLOW}You need to login to Firebase${NC}"
            firebase login
        fi
    fi

    echo -e "${GREEN}✓ Firebase authentication verified${NC}"
}

# ---------------------------------------------------------------------------
# Prerequisite checks
# ---------------------------------------------------------------------------
if [ "$USE_DOCKER" = true ]; then
    build_docker_image
else
    # Check if Firebase CLI is installed
    if ! command -v firebase &> /dev/null; then
        echo -e "${RED}Error: Firebase CLI is not installed.${NC}"
        echo "Install it with: npm install -g firebase-tools"
        echo ""
        if [ "$DOCKER_AVAILABLE" = true ]; then
            echo -e "${YELLOW}Tip: You can re-run this script and choose Docker mode instead.${NC}"
        fi
        exit 1
    fi

    echo -e "${GREEN}✓ Firebase CLI found${NC}"
fi

ensure_firebase_auth

echo ""

# Menu
echo "Select deployment option:"
echo "1) Deploy Firestore Rules only"
echo "2) Deploy Firestore Indexes only"
echo "3) Deploy Storage Rules only"
echo "4) Deploy All (Rules + Indexes + Storage)"
echo "5) Start Emulators for testing"
echo "6) Deploy to specific project"
echo "7) Exit"

read -p "Enter your choice [1-7]: " choice

case $choice in
    1)
        echo -e "${YELLOW}Deploying Firestore Rules...${NC}"
        run_firebase deploy --only firestore:rules
        echo -e "${GREEN}✓ Firestore Rules deployed successfully${NC}"
        ;;
    2)
        echo -e "${YELLOW}Deploying Firestore Indexes...${NC}"
        echo "Note: Index creation may take several minutes."
        run_firebase deploy --only firestore:indexes
        echo -e "${GREEN}✓ Firestore Indexes deployment initiated${NC}"
        echo "Check Firebase Console to monitor index creation status."
        ;;
    3)
        echo -e "${YELLOW}Deploying Storage Rules...${NC}"
        run_firebase deploy --only storage
        echo -e "${GREEN}✓ Storage Rules deployed successfully${NC}"
        ;;
    4)
        echo -e "${YELLOW}Deploying All Backend Components...${NC}"
        echo "This will deploy:"
        echo "  - Firestore Security Rules"
        echo "  - Firestore Indexes"
        echo "  - Storage Rules"
        echo ""
        read -p "Continue? (y/n): " confirm
        if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
            run_firebase deploy --only firestore,storage
            echo -e "${GREEN}✓ All components deployed successfully${NC}"
        else
            echo "Deployment cancelled"
        fi
        ;;
    5)
        echo -e "${YELLOW}Starting Firebase Emulators...${NC}"
        echo "Emulator UI will be available at: http://localhost:4000"
        echo "Firestore Emulator: http://localhost:8080"
        echo "Auth Emulator: http://localhost:9099"
        echo "Storage Emulator: http://localhost:9199"
        echo ""
        echo "Press Ctrl+C to stop emulators"
        run_firebase emulators:start
        ;;
    6)
        echo "Available projects:"
        run_firebase projects:list
        echo ""
        read -p "Enter project ID or alias: " project_id
        run_firebase use "$project_id"
        echo -e "${GREEN}✓ Switched to project: $project_id${NC}"
        echo ""
        read -p "Deploy all components to this project? (y/n): " deploy_confirm
        if [ "$deploy_confirm" = "y" ] || [ "$deploy_confirm" = "Y" ]; then
            run_firebase deploy --project "$project_id" --only firestore,storage
            echo -e "${GREEN}✓ Deployed to $project_id${NC}"
        fi
        ;;
    7)
        echo "Sayonara!"
        exit 0
        ;;
    *)
        echo -e "${RED}Invalid option${NC}"
        exit 1
        ;;
esac

echo ""
echo "============================================================="
echo -e "${GREEN}Script completed successfully${NC}"
echo "============================================================="
