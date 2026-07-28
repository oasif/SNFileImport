#!/bin/bash

# ServiceNow File Uploader - Bash Runner

MODE="${1:-interactive}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLASS_NAME="ServiceNowFileUploader"
SOURCE_FILE="$SCRIPT_DIR/$CLASS_NAME.java"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo ""
echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}   ServiceNow File Uploader${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

# Check if Java is installed
if command -v java &> /dev/null; then
    echo -e "${GREEN}[OK] Java found${NC}"
else
    echo -e "${RED}[ERROR] Java is not installed or not in PATH${NC}"
    exit 1
fi

# Check if source file exists
if [ ! -f "$SOURCE_FILE" ]; then
    echo -e "${RED}[ERROR] Source file not found: $SOURCE_FILE${NC}"
    exit 1
fi

echo -e "${GREEN}[OK] Source file found${NC}"
echo ""

# Compile
echo -e "${YELLOW}[INFO] Compiling $CLASS_NAME.java...${NC}"
javac "$SOURCE_FILE" 2>&1 > /dev/null

if [ $? -ne 0 ]; then
    echo -e "${RED}[ERROR] Compilation failed${NC}"
    javac "$SOURCE_FILE"
    exit 1
fi

echo -e "${GREEN}[OK] Compilation successful${NC}"
echo ""

# Handle different modes
case "$MODE" in
    --help)
        echo -e "${CYAN}Usage: ./run.sh [mode]${NC}"
        echo ""
        echo -e "${CYAN}Modes:${NC}"
        echo "  (default)  Interactive mode - you will be prompted for all values"
        echo "  --config   Load values from config.properties file"
        echo "  --help     Show this help message"
        echo ""
        exit 0
        ;;
    
    --config)
        echo -e "${YELLOW}[INFO] Loading configuration from config.properties...${NC}"
        
        CONFIG_FILE="$SCRIPT_DIR/config.properties"
        
        if [ ! -f "$CONFIG_FILE" ]; then
            echo -e "${RED}[ERROR] config.properties not found at: $CONFIG_FILE${NC}"
            echo -e "${RED}Please create config.properties file first${NC}"
            exit 1
        fi
        
        # Read config file into associative array
        declare -A config
        
        while IFS='=' read -r key value; do
            # Skip empty lines and comments
            if [[ -z "$key" || "$key" =~ ^[[:space:]]*# ]]; then
                continue
            fi
            
            # Trim leading/trailing whitespace from key and value
            key=$(echo "$key" | xargs)
            value=$(echo "$value" | xargs)
            
            config["$key"]="$value"
        done < "$CONFIG_FILE"
        
        echo -e "${GREEN}[OK] Configuration loaded${NC}"
        echo ""
        echo -e "${YELLOW}========================================${NC}"
        echo -e "${YELLOW}Running ServiceNow File Uploader${NC}"
        echo -e "${YELLOW}========================================${NC}"
        echo ""
        
        # Check for required config values
        required_keys=("FILE_PATH" "ENDPOINT" "QUERY_STRING" "CLIENT_ID" "CLIENT_SECRET" "USERNAME" "PASSWORD")
        missing_keys=()
        
        for key in "${required_keys[@]}"; do
            if [[ -z "${config[$key]}" ]]; then
                missing_keys+=("$key")
            fi
        done
        
        if [ ${#missing_keys[@]} -gt 0 ]; then
            echo -e "${RED}[ERROR] Missing or empty required config values:${NC}"
            for key in "${missing_keys[@]}"; do
                echo -e "${RED}  - $key${NC}"
            done
            echo -e "${RED}Please check your config.properties file and try again.${NC}"
            exit 1
        fi
        
        # Get optional config values with defaults
        REFRESH_TOKEN="${config[REFRESH_TOKEN]:-}"
        REFRESH_TOKEN_FLAG="${config[REFRESH_TOKEN_FLAG]:-false}"
        DELETE_FILE_FLAG="${config[DELETE_FILE_FLAG]:-true}"
        
        echo -e "${CYAN}[DEBUG] Configuration loaded:${NC}"
        echo -e "${CYAN}  FILE_PATH: ${config[FILE_PATH]}${NC}"
        echo -e "${CYAN}  ENDPOINT: ${config[ENDPOINT]}${NC}"
        echo -e "${CYAN}  DELETE_FILE_FLAG: '${DELETE_FILE_FLAG}'${NC}"
        echo ""
        
        echo -e "${YELLOW}[INFO] Running Java with configuration from config.properties${NC}"
        echo ""
        
        # Run Java with config values
        java -cp "$SCRIPT_DIR" "$CLASS_NAME" \
            "${config[FILE_PATH]}" \
            "${config[ENDPOINT]}" \
            "${config[QUERY_STRING]}" \
            "${config[CLIENT_ID]}" \
            "${config[CLIENT_SECRET]}" \
            "$REFRESH_TOKEN" \
            "$REFRESH_TOKEN_FLAG" \
            "${config[USERNAME]}" \
            "${config[PASSWORD]}" \
            "$DELETE_FILE_FLAG"
        
        exit_code=$?
        
        echo ""
        echo -e "${YELLOW}Java process exited with code: $exit_code${NC}"
        
        exit $exit_code
        ;;
    
    *)
        # Default: Interactive mode
        echo -e "${YELLOW}[INFO] Running in interactive mode${NC}"
        echo -e "${YELLOW}Please answer the following prompts:${NC}"
        echo ""
        
        java -cp "$SCRIPT_DIR" "$CLASS_NAME"
        exit $?
        ;;
esac