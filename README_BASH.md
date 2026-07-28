# ServiceNow File Uploader - Bash Runner

This is a bash/shell script version of the PowerShell runner for the ServiceNow File Uploader Java application.

## Prerequisites

- Java installed and available in your `PATH`
- Bash shell (version 4.0+) - for associative array support
- On Windows: Windows Subsystem for Linux (WSL), Git Bash, or similar

## Files

- `run.sh` - Main bash runner script
- `config.properties` - Configuration file for `--config` mode
- `config.properties.sample` - Sample configuration with documentation
- `ServiceNowFileUploader.java` - The Java application

## Usage

### Make the script executable

```bash
chmod +x run.sh
```

### Run in interactive mode (default)

```bash
./run.sh
```

You will be prompted to enter all required values interactively.

### Run with configuration file

```bash
./run.sh --config
```

This mode reads values from `config.properties` file. Before running:

1. Copy sample configuration:
   ```bash
   cp config.properties.sample config.properties
   ```

2. Edit `config.properties` and fill in your values:
   ```properties
   FILE_PATH=./employee_data.csv
   ENDPOINT=instance.service-now.com
   QUERY_STRING=/api/x_snc_file_ingest/file_ingest/upload
   CLIENT_ID=your_client_id
   CLIENT_SECRET=your_client_secret
   USERNAME=integrationUser
   PASSWORD=IntegrationPassword
   REFRESH_TOKEN=
   REFRESH_TOKEN_FLAG=false
   DELETE_FILE_FLAG=true
   ```

3. Secure the configuration file:
   ```bash
   chmod 600 config.properties
   ```

4. Run the script:
   ```bash
   ./run.sh --config
   ```

### Show help

```bash
./run.sh --help
```

## Configuration File (config.properties)

The `config.properties` file supports the following properties:

### Required Parameters

| Property | Description | Example |
|----------|-------------|---------|
| `FILE_PATH` | Path to the CSV file to upload | `./employee_data.csv` |
| `ENDPOINT` | ServiceNow instance endpoint (without `https://`) | `instance.service-now.com` |
| `QUERY_STRING` | API endpoint path (e.g., `/api/now/table/x_snc_file_ingest_employee`) | `/api/now/import/x_employees` |
| `CLIENT_ID` | OAuth 2.0 Client ID | (obtainable from ServiceNow) |
| `CLIENT_SECRET` | OAuth 2.0 Client Secret | (obtainable from ServiceNow) |
| `USERNAME` | ServiceNow username for OAuth password flow | `integrationUser` |
| `PASSWORD` | ServiceNow password for OAuth password flow | `integrationPassword` |

### Optional Parameters

| Property | Description | Default | Example |
|----------|-------------|---------|---------|
| `REFRESH_TOKEN` | Existing refresh token (leave empty to obtain new one) | (empty) | `token_value` |
| `REFRESH_TOKEN_FLAG` | Set to `true` if using existing refresh token, `false` otherwise | `false` | `false` or `true` |
| `DELETE_FILE_FLAG` | Delete CSV file after successful upload? | `true` | `true` or `false` |

### Delete File Flag

The `DELETE_FILE_FLAG` parameter controls whether the CSV file is automatically deleted after upload:

- **Set to `true`** (default): File is deleted after successful upload (HTTP 200 or 201)
- **Set to `false`**: File is retained after upload

**Recommended usage:**
- Set to `false` during testing and validation
- Set to `true` for production uploads
- File is always retained if upload fails (HTTP error code)

**Example:**
```properties
# Testing (keep file for verification)
DELETE_FILE_FLAG=false

# Production (delete after successful upload)
DELETE_FILE_FLAG=true
```

## Configuration Examples

### Example 1: First-Time Setup (Testing)

```properties
FILE_PATH=./test_data.csv
ENDPOINT=dev.service-now.com
QUERY_STRING=/api/now/import/x_employees
CLIENT_ID=abc123xyz
CLIENT_SECRET=def456uvw
USERNAME=admin
PASSWORD=AdminPassword
REFRESH_TOKEN=
REFRESH_TOKEN_FLAG=false
DELETE_FILE_FLAG=false
```

### Example 2: Production with Refresh Token

```properties
FILE_PATH=./employee_data.csv
ENDPOINT=prod.service-now.com
QUERY_STRING=/api/now/import/x_employees
CLIENT_ID=abc123xyz
CLIENT_SECRET=def456uvw
USERNAME=integration_user
PASSWORD=ServiceAccountPassword
REFRESH_TOKEN=saved_refresh_token_value
REFRESH_TOKEN_FLAG=true
DELETE_FILE_FLAG=true
```

### Example 3: Batch Processing with File Retention

```properties
FILE_PATH=./batch/employees.csv
ENDPOINT=instance.service-now.com
QUERY_STRING=/api/x_snc_file_ingest/file_ingest/upload
CLIENT_ID=abc123
CLIENT_SECRET=def456
USERNAME=batch_user
PASSWORD=BatchPassword
REFRESH_TOKEN=
REFRESH_TOKEN_FLAG=false
DELETE_FILE_FLAG=false
```

## Differences from PowerShell Version

1. **No temporary batch file** - Bash version executes Java directly without creating intermediate files
2. **Uses ANSI color codes** - Works on Linux/Mac terminals natively
3. **Associative arrays** - Requires Bash 4.0+ (standard on most systems)
4. **Path handling** - Uses POSIX paths, may need adjustments on Windows WSL

## Workflow Example

```bash
# 1. Copy and navigate to directory
cd ~/servicenow-uploader

# 2. Make script executable
chmod +x run.sh

# 3. Create configuration from sample
cp config.properties.sample config.properties

# 4. Edit configuration with your editor
nano config.properties

# 5. Secure the configuration file
chmod 600 config.properties

# 6. Run with configuration
./run.sh --config
```

## Troubleshooting

### "Java is not installed or not in PATH"

Ensure Java is installed and accessible:
```bash
java -version
which java
```

If Java is not in PATH, add it to your PATH:
```bash
export PATH=$PATH:/path/to/java/bin
```

### "Compilation failed"

Check for syntax errors in `ServiceNowFileUploader.java`. The script will show the compilation errors on retry.

### "config.properties not found"

Make sure `config.properties` exists in the same directory as `run.sh`:
```bash
ls -la config.properties
```

If missing, create it from the sample:
```bash
cp config.properties.sample config.properties
```

### Missing required config values

Verify all required properties are present and not empty in `config.properties`:
```bash
# Check for empty values
grep -E "^[A-Z_]+=$" config.properties
```

### "File not found" during upload

Verify the FILE_PATH is correct and the file exists:
```bash
# Check if file exists
ls -la ./employee_data.csv

# Use absolute path if relative path fails
FILE_PATH=/home/user/employee_data.csv
```

### "Permission denied" when deleting file

Ensure the user running the script has permission to delete the file:
```bash
# Check file ownership
ls -la ./employee_data.csv

# Check directory permissions
ls -lad .
```

### Color Output Not Working

If your terminal doesn't support ANSI color codes:
- The script will still function but without colored output
- Consider using a modern terminal: iTerm2 (Mac), GNOME Terminal (Linux), Windows Terminal (WSL)

## Security Best Practices

### File Permissions

```bash
# Secure config.properties (readable only by owner)
chmod 600 config.properties

# Verify permissions
ls -la config.properties
```

### Protecting Credentials

1. **Never commit credentials to version control:**
   ```bash
   echo "config.properties" >> .gitignore
   ```

2. **Securely delete configuration files:**
   ```bash
   # Using shred (Linux)
   shred -vfz config.properties
   
   # Using rm with secure delete (Mac)
   rm -P config.properties
   ```

3. **Use service accounts:**
   - Create a dedicated ServiceNow user for API integrations
   - Don't use personal user accounts

4. **Rotate credentials:**
   ```bash
   # After changing credentials in ServiceNow, update config.properties
   chmod 600 config.properties
   nano config.properties
   ```

### Environment Variables (Advanced)

For added security, use environment variables instead of config files:

```bash
#!/bin/bash
export FILE_PATH="./employee_data.csv"
export ENDPOINT="instance.service-now.com"
export QUERY_STRING="/api/now/import/x_employees"
export CLIENT_ID="$SERVICENOW_CLIENT_ID"
export CLIENT_SECRET="$SERVICENOW_CLIENT_SECRET"
export USERNAME="$SERVICENOW_USER"
export PASSWORD="$SERVICENOW_PASSWORD"

java -cp . ServiceNowFileUploader \
    "$FILE_PATH" "$ENDPOINT" "$QUERY_STRING" \
    "$CLIENT_ID" "$CLIENT_SECRET" "" false \
    "$USERNAME" "$PASSWORD" true
```

## Batch Processing Example

```bash
#!/bin/bash
# Process all CSV files in a directory

ENDPOINT="instance.service-now.com"
QUERY_STRING="/api/now/import/x_employees"
CLIENT_ID="abc123xyz"
CLIENT_SECRET="def456uvw"
USERNAME="integration_user"
PASSWORD="integration_password"

for csvfile in ./csv_files/*.csv; do
    echo "Processing: $csvfile"
    
    java -cp . ServiceNowFileUploader \
        "$csvfile" \
        "$ENDPOINT" \
        "$QUERY_STRING" \
        "$CLIENT_ID" \
        "$CLIENT_SECRET" \
        "" \
        false \
        "$USERNAME" \
        "$PASSWORD" \
        true
    
    if [ $? -eq 0 ]; then
        echo "✓ Successfully uploaded: $csvfile"
    else
        echo "✗ Failed to upload: $csvfile"
    fi
    
    # Wait between uploads
    sleep 2
done

echo "Batch processing complete!"
```

Save as `batch_upload.sh`, make executable, and run:
```bash
chmod +x batch_upload.sh
./batch_upload.sh
```

## Integration with cron/scheduled tasks

Example crontab entry for daily uploads:

```bash
# Run upload at 2:00 AM daily
0 2 * * * cd /home/user/servicenow-uploader && ./run.sh --config >> /var/log/servicenow-upload.log 2>&1
```

## Additional Resources

- See [README.md](README.md) for complete application documentation
- See [config.properties.sample](config.properties.sample) for parameter reference
- See [POWERSHELL_GUIDE.md](POWERSHELL_GUIDE.md) for Windows PowerShell version
