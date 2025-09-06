@echo off
setlocal enabledelayedexpansion

:: build-all.bat - Build script for all supported Minecraft versions of Wayfindr
:: Usage: build-all.bat [clean]

:: Colors for output
set "GREEN=[92m"
set "YELLOW=[93m"
set "RED=[91m"
set "NC=[0m"

:: Check if Gradle wrapper exists
if not exist "gradlew.bat" (
    echo %RED%Error: gradlew.bat not found. Make sure you're running this script from the project root.%NC%
    exit /b 1
)

:: Clean build if requested
if "%1"=="clean" (
    echo %YELLOW%Cleaning project...%NC%
    call gradlew.bat clean
)

:: Create build directory if it doesn't exist
if not exist "build\all-versions" mkdir "build\all-versions"

:: Build each version
echo %GREEN%Starting build for all Minecraft versions...%NC%
echo %YELLOW%=============================================%NC%

:: Build for Minecraft 1.21.4
echo %YELLOW%Building for Minecraft 1.21.4...%NC%
call gradlew.bat build -Pminecraft_version=1.21.4 -Pyarn_mappings=1.21.4+build.8 -Pfabric_version=0.119.2+1.21.4 -Parchives_base_name=wayfindr-mc1.21.4
if %ERRORLEVEL% EQU 0 (
    echo %GREEN%Successfully built Wayfindr for Minecraft 1.21.4%NC%
    copy /Y "build\libs\*.jar" "build\all-versions\"
) else (
    echo %RED%Failed to build Wayfindr for Minecraft 1.21.4%NC%
)
echo %YELLOW%=============================================%NC%

:: Build for Minecraft 1.21.5
echo %YELLOW%Building for Minecraft 1.21.5...%NC%
call gradlew.bat build -Pminecraft_version=1.21.5 -Pyarn_mappings=1.21.5+build.1 -Pfabric_version=0.119.2+1.21.5 -Parchives_base_name=wayfindr-mc1.21.5
if %ERRORLEVEL% EQU 0 (
    echo %GREEN%Successfully built Wayfindr for Minecraft 1.21.5%NC%
    copy /Y "build\libs\*.jar" "build\all-versions\"
) else (
    echo %RED%Failed to build Wayfindr for Minecraft 1.21.5%NC%
)
echo %YELLOW%=============================================%NC%

:: Add new versions here using the same pattern
:: Example:
:: echo %YELLOW%Building for Minecraft X.Y.Z...%NC%
:: call gradlew.bat build -Pminecraft_version=X.Y.Z -Pyarn_mappings=X.Y.Z+build.N -Pfabric_version=0.ABC.D+X.Y.Z -Parchives_base_name=wayfindr-mcX.Y.Z
:: if %ERRORLEVEL% EQU 0 (
::     echo %GREEN%Successfully built Wayfindr for Minecraft X.Y.Z%NC%
::     copy /Y "build\libs\*.jar" "build\all-versions\"
:: ) else (
::     echo %RED%Failed to build Wayfindr for Minecraft X.Y.Z%NC%
:: )
:: echo %YELLOW%=============================================%NC%

:: List all built versions
echo %GREEN%Build complete! The following files were created:%NC%
dir "build\all-versions\"

echo %YELLOW%All JAR files are available in the build\all-versions\ directory%NC%

endlocal
