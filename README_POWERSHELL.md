# Using ServiceNow File Uploader with PowerShell

Since you're using PowerShell on Windows, use the **run.ps1** script instead of batch files. PowerShell handles special characters in config files much better than batch files.

## ✅ PowerShell Version Compatibility

**This script works with:**
- ✓ PowerShell 5.1 (Windows 10, Windows 11, Windows Server 2016+)
- ✓ PowerShell 7.0+ (newer versions)

No version-specific errors - fully compatible across all PowerShell versions!

For detailed compatibility information, see [POWERSHELL_COMPATIBILITY.md](POWERSHELL_COMPATIBILITY.md).

## Setup

### 1. First Time: Allow PowerShell Scripts (One-time setup)

If you get an execution policy error, run this **once** in PowerShell as Administrator:

```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

This allows local PowerShell scripts to run. Answer `Y` when prompted.

### 2. Create config.properties File

Copy the sample configuration file:

```powershell
Copy-Item config.properties.sample config.properties
```

Then edit `config.properties` with your favorite text editor (Notepad, VS Code, etc.).

### 3. Configuration File

A `config.properties` file is used to store your parameters:

```properties
FILE_PATH=./employee_data.csv
ENDPOINT=instance.service-now.com
QUERY_STRING=/api/x_snc_file_ingest/file_ingest/upload
CLIENT_ID=your_client_id_here
CLIENT_SECRET=your_client_secret_here
REFRESH_TOKEN=
REFRESH_TOKEN_FLAG=false
USERNAME=integrationUser
PASSWORD=IntegrationPassword
DELETE_FILE_FLAG=true
```

## Usage

### Option 1: Config File Mode (Recommended)

```powershell
.\run.ps1 --config
```

This reads all parameters from `config.properties` and uploads the file.

### Option 2: Interactive Mode

```powershell
.\run.ps1
```

Or explicitly:

```powershell
.\run.ps1 --interactive
```

You'll be prompted to enter all values interactively (same as before).

### Option 3: Show Help

```powershell
.\run.ps1 --help
```

## Configuration File Parameters

### Required Parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| `FILE_PATH` | Path to the CSV file to upload | `./employee_data.csv` |
| `ENDPOINT` | ServiceNow instance endpoint (without `https://`) | `instance.service-now.com` |
| `QUERY_STRING` | API endpoint path | `/api/now/import/x_employees` |
| `CLIENT_ID` | OAuth 2.0 Client ID | (from ServiceNow OAuth setup) |
| `CLIENT_SECRET` | OAuth 2.0 Client Secret | (from ServiceNow OAuth setup) |
| `USERNAME` | ServiceNow username | `integrationUser` |
| `PASSWORD` | ServiceNow password | `IntegrationPassword` |

### Optional Parameters

| Parameter | Description | Default | Example |
|-----------|-------------|---------|---------|
| `REFRESH_TOKEN` | Existing refresh token (empty for first run) | (empty) | `token_value` |
| `REFRESH_TOKEN_FLAG` | Use existing refresh token? | `false` | `true` or `false` |
| `DELETE_FILE_FLAG` | Delete CSV file after upload? | `true` | `true` or `false` |

### Delete File Flag (NEW FEATURE)

The `DELETE_FILE_FLAG` parameter controls file deletion after upload:

- **`true`** (default): Automatically delete CSV file after successful upload
- **`false`**: Keep CSV file after upload (recommended for testing)

**Usage Examples:**

```properties
# Testing - keep the file to verify upload
DELETE_FILE_FLAG=false

# Production - delete file after successful upload
DELETE_FILE_FLAG=true
```

**Important Notes:**
- File is ONLY deleted on successful upload (HTTP 200 or 201 response)
- File is ALWAYS retained if upload fails
- Allows verification of upload before automatic deletion

## Configuration Examples

### Example 1: First-Time Setup with Testing

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

Then run:
```powershell
.\run.ps1 --config
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
REFRESH_TOKEN=saved_token_from_previous_run
REFRESH_TOKEN_FLAG=true
DELETE_FILE_FLAG=true
```

Then run:
```powershell
.\run.ps1 --config
```

## Advantages of PowerShell Version

✓ Handles special characters correctly (brackets, commas, colons, etc.)
✓ Better error messages and diagnostics
✓ **Calls Java directly (no batch file overhead)** ✨ NEW
✓ Faster config file parsing
✓ More reliable with complex passwords and secrets
✓ **DELETE_FILE_FLAG now works correctly** ✨ FIXED
✓ Better file deletion handling
✓ Native Windows integration

### PowerShell-Only Feature (v2.0.1+)

The PowerShell runner has been optimized to call Java directly instead of using a batch file intermediary. This provides:
- ✓ Correct parameter passing for all values, including `DELETE_FILE_FLAG`
- ✓ Better performance (no temporary file creation)
- ✓ Clearer debugging output
- ✓ More reliable handling of special characters in credentials

## Workflow

```powershell
# 1. First time only - set execution policy
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# 2. Copy sample config
Copy-Item config.properties.sample config.properties

# 3. Edit config in your favorite editor
notepad config.properties

# 4. Run with config
.\run.ps1 --config
```

## Troubleshooting

### "execution policy" error

```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

Answer `Y` and try again. This only needs to be done once.

### Java not found

Ensure Java is installed and in your PATH:

```powershell
java -version
```

If this fails, add Java to your PATH:
1. Find your Java installation folder (usually `C:\Program Files\Java\jdk*`)
2. Open Environment Variables: Press `Win+X`, then select "System"
3. Click "Environment Variables"
4. Add Java `bin` folder to PATH

### config.properties not found

Make sure `config.properties` exists in the same directory as `run.ps1`:

```powershell
# List files in current directory
ls

# Create config from sample if needed
Copy-Item config.properties.sample config.properties
```

### "Missing or empty required config values"

Verify all required parameters are filled in `config.properties`:

```powershell
# Open in default editor
Invoke-Item config.properties

# Or use Notepad
notepad config.properties
```

### File not deleted after upload

Check the following:
1. Verify the upload succeeded (check ServiceNow)
2. Confirm `DELETE_FILE_FLAG=true` in config.properties
3. Ensure you have permission to delete the file
4. Check the HTTP response code in the output (should be 200 or 201)

### Still having issues?

Use **interactive mode** - this bypasses config file parsing:

```powershell
.\run.ps1
```

This works the same as before and is useful for troubleshooting.

## Security Best Practices

### Protect Your Credentials

⚠️ **WARNING**: The `config.properties` file contains your password and secrets.

**Best Practices:**
1. **Never commit to version control** - Add to `.gitignore`:
   ```
   config.properties
   *.properties
   ```

2. **Restrict file access** - Use NTFS permissions:
   ```powershell
   # Right-click config.properties > Properties > Security
   # Remove inheritance, keep only your user account
   ```

3. **Delete after use** - For testing environments:
   ```powershell
   Remove-Item config.properties -Force
   ```

4. **Use service accounts** - Create dedicated ServiceNow accounts for API access instead of personal accounts

5. **Rotate credentials** - Change passwords periodically:
   ```powershell
   # After changing in ServiceNow:
   # 1. Edit config.properties with new password
   # 2. Delete old config.properties after confirming it works
   # 3. Regenerate Client Secret in OAuth app
   ```

### For Production Environments

Consider:
- **Azure Key Vault** - Store secrets securely
- **Windows Credential Manager** - Store credentials locally
- **Environment Variables** - Load credentials from secure sources
- **Service Accounts** - Use dedicated integration accounts

Example with environment variables:

```powershell
$env:SERVICENOW_PASSWORD = "YourSecurePassword"
# Then reference in script or config
```

## Batch Processing

Process multiple files:

```powershell
# Create batch_upload.ps1
$files = Get-ChildItem ".\csv_files\*.csv"

foreach ($file in $files) {
    Write-Host "Processing: $($file.Name)"
    
    # Copy file to expected location
    Copy-Item $file.FullName ".\employee_data.csv"
    
    # Run upload
    .\run.ps1 --config
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Successfully uploaded: $($file.Name)"
    } else {
        Write-Host "✗ Failed to upload: $($file.Name)"
    }
    
    Start-Sleep -Seconds 2
}

Write-Host "Batch processing complete!"
```

Save as `batch_upload.ps1` and run:
```powershell
.\batch_upload.ps1
```

## Scheduled Tasks (Windows Task Scheduler)

To run automatically on a schedule:

1. Open **Task Scheduler**
2. Create Basic Task
3. Set trigger (e.g., Daily at 2:00 AM)
4. Set action:
   - Program: `C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe`
   - Arguments: `-NoProfile -ExecutionPolicy Bypass -File "C:\path\to\run.ps1" --config`
   - Start in: `C:\path\to\servicenow-uploader\`
5. Configure credentials and advanced settings
6. Save

## Output Example

```
========================================
   ServiceNow File Uploader
========================================

[OK] Java found
[OK] Source file found

[INFO] Compiling ServiceNowFileUploader.java...
[OK] Compilation successful

[INFO] Loading configuration from config.properties...
[OK] Configuration loaded

========================================
Running ServiceNow File Uploader
========================================

=== ServiceNow File Uploader ===

Reading file: ./employee_data.csv
✓ File read successfully (2048 bytes)
Obtaining refresh token...
✓ Refresh token retrieved successfully
Obtaining access token...
✓ Access token retrieved successfully
Setting up SSL connection...
Uploading to: https://instance.service-now.com/api/now/import/x_table_name
Response Code: 201
✓ File deleted after upload

=== Upload Results ===
Status: Success
Response Code: 201
Delete File Flag: true
Refresh Token Updated: true
New Refresh Token: abc123xyz...

✓ File successfully uploaded to ServiceNow

Java process exited with code: 0
```

## Quick Reference

| Task | Command |
|------|---------|
| Run with config | `.\run.ps1 --config` |
| Run interactive | `.\run.ps1` |
| Show help | `.\run.ps1 --help` |
| Edit config | `notepad config.properties` |
| Set execution policy | `Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser` |
| View Java version | `java -version` |

## Additional Resources

- Complete documentation: [README.md](README.md)
- Bash version guide: [README_BASH.md](README_BASH.md)
- Configuration reference: [config.properties.sample](config.properties.sample)
- **Troubleshooting DELETE_FILE_FLAG**: [TROUBLESHOOTING_DELETE_FLAG.md](TROUBLESHOOTING_DELETE_FLAG.md)
- Changes and what's new: [CHANGES_SUMMARY.md](CHANGES_SUMMARY.md)
- Java source code: `ServiceNowFileUploader.java`

## Support

For issues or questions:
1. Check the Troubleshooting section above
2. Verify all parameters in `config.properties`
3. Use interactive mode (`.\run.ps1`) to test
4. Check ServiceNow logs for API errors
5. Ensure network connectivity to ServiceNow instance
