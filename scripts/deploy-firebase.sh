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
NC='\033[0m' # No Color

# Check if Firebase CLI is installed
if ! command -v firebase &> /dev/null; then
    echo -e "${RED}Error: Firebase CLI is not installed.${NC}"
    echo "Install it with: npm install -g firebase-tools"
    exit 1
fi

echo -e "${GREEN}✓ Firebase CLI found${NC}"

# Check if logged in
if ! firebase projects:list &> /dev/null; then
    echo -e "${YELLOW}You need to login to Firebase${NC}"
    firebase login
fi

echo -e "${GREEN}✓ Firebase authentication verified${NC}"
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
        firebase deploy --only firestore:rules
        echo -e "${GREEN}✓ Firestore Rules deployed successfully${NC}"
        ;;
    2)
        echo -e "${YELLOW}Deploying Firestore Indexes...${NC}"
        echo "Note: Index creation may take several minutes."
        firebase deploy --only firestore:indexes
        echo -e "${GREEN}✓ Firestore Indexes deployment initiated${NC}"
        echo "Check Firebase Console to monitor index creation status."
        ;;
    3)
        echo -e "${YELLOW}Deploying Storage Rules...${NC}"
        firebase deploy --only storage
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
            firebase deploy --only firestore,storage
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
        firebase emulators:start
        ;;
    6)
        echo "Available projects:"
        firebase projects:list
        echo ""
        read -p "Enter project ID or alias: " project_id
        firebase use "$project_id"
        echo -e "${GREEN}✓ Switched to project: $project_id${NC}"
        echo ""
        read -p "Deploy all components to this project? (y/n): " deploy_confirm
        if [ "$deploy_confirm" = "y" ] || [ "$deploy_confirm" = "Y" ]; then
            firebase deploy --only firestore,storage
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
