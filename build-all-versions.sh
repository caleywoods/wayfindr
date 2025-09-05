#!/bin/bash

# Script to build Wayfindr mod for all supported Minecraft versions
# This script handles updating the gradle.properties file for each version

# Define the supported versions and their corresponding fabric API versions
declare -A VERSIONS=(
  ["1.21.4"]="0.92.0+1.21.4"
  ["1.21.5"]="0.96.11+1.21.5"
  ["1.21.6"]="0.128.2+1.21.6"
)

# Store the original gradle.properties
cp gradle.properties gradle.properties.backup

# Function to restore the original gradle.properties on exit
function cleanup {
  echo "Restoring original gradle.properties..."
  cp gradle.properties.backup gradle.properties
  rm gradle.properties.backup
}

# Register the cleanup function to run on script exit
trap cleanup EXIT

# Build for each version
for mc_version in "${!VERSIONS[@]}"; do
  fabric_version="${VERSIONS[$mc_version]}"
  
  echo "====================================================="
  echo "Building Wayfindr for Minecraft $mc_version with Fabric API $fabric_version"
  echo "====================================================="
  
  # Update gradle.properties for this version
  sed -i '' "s/minecraft_version=.*/minecraft_version=$mc_version/" gradle.properties
  sed -i '' "s/fabric_version=.*/fabric_version=$fabric_version/" gradle.properties
  
  # For 1.21.4 and 1.21.5, use yarn mappings with build.10, for 1.21.6 use build.1
  if [[ "$mc_version" == "1.21.6" ]]; then
    sed -i '' "s/yarn_mappings=.*/yarn_mappings=$mc_version+build.1/" gradle.properties
  else
    sed -i '' "s/yarn_mappings=.*/yarn_mappings=$mc_version+build.10/" gradle.properties
  fi
  
  # Build the mod
  ./gradlew clean build
  
  # Check if build was successful
  if [ $? -eq 0 ]; then
    echo "✅ Successfully built Wayfindr for Minecraft $mc_version"
    
    # Copy the built JAR to a version-specific name
    cp build/libs/Wayfindr-*.jar build/libs/Wayfindr-$mc_version.jar
    echo "✅ Copied JAR to build/libs/Wayfindr-$mc_version.jar"
  else
    echo "❌ Failed to build Wayfindr for Minecraft $mc_version"
  fi
  
  echo ""
done

echo "====================================================="
echo "Build process complete!"
echo "JAR files are available in the build/libs directory:"
ls -la build/libs/Wayfindr-*.jar
echo "====================================================="
