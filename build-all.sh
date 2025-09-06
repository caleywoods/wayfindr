#!/bin/bash

# build-all.sh - Build script for all supported Minecraft versions of Wayfindr
# Usage: ./build-all.sh [clean]

# Set of supported Minecraft versions with their corresponding Yarn mappings and Fabric API versions
declare -A MC_VERSIONS=(
  ["1.21.4"]="1.21.4+build.8 0.119.2+1.21.4"
  ["1.21.5"]="1.21.5+build.1 0.119.2+1.21.5"
  # Add new versions here in the format:
  # ["mc_version"]="yarn_mappings fabric_api_version"
)

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if Gradle wrapper exists
if [ ! -f "./gradlew" ]; then
  echo -e "${RED}Error: gradlew not found. Make sure you're running this script from the project root.${NC}"
  exit 1
fi

# Make gradlew executable if it's not already
chmod +x ./gradlew

# Clean build if requested
if [ "$1" == "clean" ]; then
  echo -e "${YELLOW}Cleaning project...${NC}"
  ./gradlew clean
fi

# Create build directory if it doesn't exist
mkdir -p build/all-versions

# Build each version
echo -e "${GREEN}Starting build for all Minecraft versions...${NC}"
echo -e "${YELLOW}==============================================${NC}"

for mc_version in "${!MC_VERSIONS[@]}"; do
  # Split the value into yarn_mappings and fabric_version
  read -r yarn_mappings fabric_version <<< "${MC_VERSIONS[$mc_version]}"
  
  echo -e "${YELLOW}Building for Minecraft ${mc_version}...${NC}"
  
  # Build with the specific version parameters
  ./gradlew build \
    -Pminecraft_version="$mc_version" \
    -Pyarn_mappings="$yarn_mappings" \
    -Pfabric_version="$fabric_version" \
    -Parchives_base_name="wayfindr-mc$mc_version"
  
  # Check if build was successful
  if [ $? -eq 0 ]; then
    echo -e "${GREEN}Successfully built Wayfindr for Minecraft ${mc_version}${NC}"
    
    # Copy the built JAR files to the all-versions directory
    cp build/libs/*.jar build/all-versions/
  else
    echo -e "${RED}Failed to build Wayfindr for Minecraft ${mc_version}${NC}"
  fi
  
  echo -e "${YELLOW}==============================================${NC}"
done

# List all built versions
echo -e "${GREEN}Build complete! The following files were created:${NC}"
ls -la build/all-versions/

echo -e "${YELLOW}All JAR files are available in the build/all-versions/ directory${NC}"
