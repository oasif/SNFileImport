# ServiceNow File Uploader

A standalone Java application that authenticates with ServiceNow using OAuth 2.0 and uploads employee data from a CSV file.

## Features

- **OAuth 2.0 Authentication**: Secure token-based authentication with ServiceNow
- **Refresh Token Management**: Automatic refresh token generation and renewal
- **File Deletion Control**: Optional automatic file deletion after successful upload
- **TLS 1.2 Support**: Secure HTTPS connections with proper certificate validation
- **Error Handling**: Comprehensive error handling and detailed logging
- **Flexible Input**: Command-line, configuration file, or interactive mode
- **CSV Support**: Windows-1252 encoded file support for legacy systems

## Prerequisites

- Java JDK 8 or higher
- ServiceNow instance with OAuth configured
- Valid ServiceNow credentials (username/password)
- OAuth client credentials (client ID and secret)

## Compilation

```bash
javac ServiceNowFileUploader.java
```

## Quick Start

### Option 1: Using Configuration File (Recommended)

```bash
# Linux/Mac
cp config.properties.sample config.properties
# Edit config.properties with your values
./run.sh --config

# Windows (PowerShell)
Copy-Item config.properties.sample config.properties
# Edit config.properties with your values
.\run.ps1 --config
```

### Option 2: Interactive Mode

```bash
# Linux/Mac
./run.sh

# Windows (PowerShell)
.\run.ps1
```

The application will prompt you for all required values.

### Option 3: Direct Command Line

```bash
java ServiceNowFileUploader <filePath> <endpoint> <queryString> <clientId> <clientSecret> <refreshToken> <refreshTokenFlag> <username> <password> [deleteFileFlag]
```

#### Example:

```bash
java ServiceNowFileUploader \
  ./employee_data.csv \
  instance.service-now.com \
  "/api/now/import/x_abcd_employee" \
  "your_client_id" \
  "your_client_secret" \
  "" \
  false \
  "your_username" \
  "your_password" \
  true
```

## Configuration Parameters

| Parameter | Required | Type | Default | Description |
|-----------|----------|------|---------|-------------|
| `filePath` | Yes | String | - | Path to CSV file to upload |
| `endpoint` | Yes | String | - | ServiceNow instance hostname |
| `queryString` | Yes | String | - | API endpoint path |
| `clientId` | Yes | String | - | OAuth client ID from ServiceNow |
| `clientSecret` | Yes | String | - | OAuth client secret |
| `refreshToken` | No | String | (empty) | Existing refresh token |
| `refreshTokenFlag` | No | Boolean | false | Is refresh token available? |
| `username` | Yes | String | - | ServiceNow username |
| `password` | Yes | String | - | ServiceNow password |
| `deleteFileFlag` | No | Boolean | true | Delete CSV file after upload? |

### Parameter Details

**File Path**
- Absolute or relative path to the CSV file
- Example: `./employee_data.csv` or `C:\Users\admin\employee_data.csv`

**Endpoint**
- ServiceNow instance hostname (without `https://`)
- Example: `instance.service-now.com` or `dev12345.service-now.com`

**Query String**
- REST API endpoint path for importing/uploading
- Examples:
  - `/api/now/import/x_table_name`
  - `/api/x_snc_file_ingest/file_ingest/upload`
  - `/api/now/table/x_custom_import`

**Delete File Flag** (NEW)
- Controls whether the CSV file is automatically deleted after upload
- Set to `true` to delete the file (default, recommended for production)
- Set to `false` to keep the file (recommended during testing)
- File is only deleted on successful upload (HTTP 200 or 201 response)
- If upload fails, the file is always retained

## CSV File Format

The application expects a pipe-delimited CSV file (Windows-1252 encoding) with the following structure:

```csv
Email|Last_Name|First_Name|Employee_Sub_Group_Code|Employee_Sub_Group(FT/PT/Temp)|Status|...
user@domain.com|LastName|FirstName|7A|CP Regular Full-time|Active|...
```

**Requirements:**
- **Delimiter:** Pipe character (`|`)
- **Encoding:** Windows-1252 (for legacy system compatibility)
- **Format:** Pipe-delimited text

## Configuration File Usage

### Creating config.properties

1. **Copy the sample file:**
   ```bash
   cp config.properties.sample config.properties
   ```

2. **Edit config.properties:**
   ```properties
   FILE_PATH=./employee_data.csv
   ENDPOINT=instance.service-now.com
   QUERY_STRING=/api/x_snc_file_ingest/file_ingest/upload
   CLIENT_ID=your_client_id_here
   CLIENT_SECRET=your_client_secret_here
   USERNAME=integrationUser
   PASSWORD=integrationPassword
   REFRESH_TOKEN=
   REFRESH_TOKEN_FLAG=false
   DELETE_FILE_FLAG=true
   ```

3. **Set file permissions (Linux/Mac):**
   ```bash
   chmod 600 config.properties
   ```

4. **Run with configuration:**
   ```bash
   ./run.sh --config
   ```

### Configuration File Format

- **Comments:** Lines starting with `#` are ignored
- **Format:** `KEY=VALUE` (no spaces around `=`)
- **Optional parameters:** Can be left empty or omitted
- **Required parameters:** Must have values

### Example Configurations

**Example 1: First-time setup with delete disabled (testing)**
```properties
FILE_PATH=./test_data.csv
ENDPOINT=dev12345.service-now.com
QUERY_STRING=/api/now/import/x_employees
CLIENT_ID=abc123xyz
CLIENT_SECRET=def456uvw
USERNAME=admin
PASSWORD=AdminPassword
REFRESH_TOKEN=
REFRESH_TOKEN_FLAG=false
DELETE_FILE_FLAG=false
```

**Example 2: Production with existing refresh token and file deletion**
```properties
FILE_PATH=./employee_data.csv
ENDPOINT=prod.service-now.com
QUERY_STRING=/api/now/import/x_employees
CLIENT_ID=abc123xyz
CLIENT_SECRET=def456uvw
USERNAME=integration_user
PASSWORD=ServiceAccountPassword
REFRESH_TOKEN=previous_refresh_token_value
REFRESH_TOKEN_FLAG=true
DELETE_FILE_FLAG=true
```

## Authentication Flow

1. **Initial Setup** (if no refresh token):
   - Sends username/password to `/oauth_token.do`
   - ServiceNow returns access token and refresh token
   
2. **Subsequent Requests**:
   - Uses refresh token to get new access token
   - If refresh token expires, re-authenticates with username/password

3. **File Upload**:
   - Uses access token in Authorization header
   - POSTs file content to specified endpoint
   - Returns response code (201 = created, 200 = success)

## File Deletion Behavior

### When File Is Deleted
- HTTP 200 or 201 response received AND
- `deleteFileFlag` is set to `true`

### When File Is NOT Deleted
- HTTP response code is not 200 or 201 (upload failed)
- `deleteFileFlag` is set to `false` (user configured to keep file)

### Examples

```bash
# File will be deleted on success
java ServiceNowFileUploader ./data.csv instance.service-now.com "/api/import" id secret "" false user pass true

# File will be kept after upload
java ServiceNowFileUploader ./data.csv instance.service-now.com "/api/import" id secret "" false user pass false

# File will be kept if upload fails (HTTP 400, 401, etc.)
```

## HTTP Headers Sent

```
Authorization: Bearer <access_token>
Accept: */*
Cache-Control: no-cache
Host: <endpoint>
Accept-Encoding: gzip, deflate, br
Connection: keep-alive
Content-Length: <file_size>
Content-Type: text/plain
```

## HTTP Response Codes

| Code | Meaning | File Action |
|------|---------|-------------|
| 200 | OK - File accepted | Delete (if deleteFileFlag=true) |
| 201 | Created - File imported successfully | Delete (if deleteFileFlag=true) |
| 400 | Bad Request - Check CSV format, delimiter, encoding | Keep file for retry |
| 401 | Unauthorized - Check credentials and tokens | Keep file for retry |
| 403 | Forbidden - Check user permissions and roles | Keep file for retry |
| 404 | Not Found - Check API endpoint path | Keep file for retry |
| 500 | Server Error - Check ServiceNow logs | Keep file for retry |

## Security Features

- ✓ TLS 1.2 encryption for all connections
- ✓ Proper SSL certificate validation
- ✓ No credentials logged or displayed in output
- ✓ OAuth 2.0 token-based authentication
- ✓ Tokens are not written to disk by default
- ✓ File permissions recommended for config.properties (chmod 600)

## Troubleshooting

### "HTTP 401" Error

If you get an error like:
```
Failed to get refresh token. HTTP 401
Error response: {"error_description":"access_denied","error":"server_error"}
```

This is **NOT a code issue** - it's a ServiceNow OAuth configuration problem.

**See:** [OAUTH_401_TROUBLESHOOTING.md](OAUTH_401_TROUBLESHOOTING.md) for detailed diagnostics.
- Verify the file path is correct
- Use absolute paths for clarity: `/home/user/employee_data.csv`

### "Failed to retrieve access token" Error
- Check that client ID and secret are correct
- Verify endpoint is correct
- Ensure refresh token is valid or leave empty for first run

### "HTTP 401" Error
- Credentials are invalid
- Client secret may be incorrect
- Service account may not have API permissions

### "HTTP 403" Error
- User may not have permission to access the endpoint
- Check ServiceNow role assignments

### "HTTP 404" Error
- Query string (API endpoint) is incorrect
- Verify the endpoint path in ServiceNow

### "HTTP 400" Error (Bad Request)
- CSV file format may be incorrect
- Check delimiter (should be pipe `|`)
- Verify character encoding (should be Windows-1252)

### "File not deleted" Warning
- Check if HTTP response was 200 or 201
- Verify `DELETE_FILE_FLAG` is set to `true`
- Check file permissions (may not have permission to delete)

## Output Example

```
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
```

## Best Practices

### Security
1. **Store Credentials Securely**: Use environment variables or secure vaults instead of hardcoding
2. **Use Service Accounts**: Create dedicated ServiceNow service accounts for API access
3. **Protect config.properties**: Set file permissions to `chmod 600` on Linux/Mac
4. **Rotate Credentials**: Periodically change passwords and regenerate client secrets
5. **Secure Token Storage**: Keep refresh tokens in secure storage, not in version control

### Testing
1. **Test First**: Use `DELETE_FILE_FLAG=false` during initial testing
2. **Verify Uploads**: Check ServiceNow to confirm successful imports
3. **Validate Format**: Test with small sample file before large batch uploads
4. **Monitor Tokens**: Keep refresh tokens secure and rotate them periodically

### Production Deployment
1. **Automate with Batch Processing**: Use scripts to process multiple files
2. **Enable File Deletion**: Set `DELETE_FILE_FLAG=true` in production
3. **Monitor Logs**: Check application output for errors
4. **Use Service Accounts**: Deploy with dedicated integration accounts
5. **Implement Retry Logic**: Handle transient failures gracefully

### Example Batch Processing (Linux/Mac)

```bash
#!/bin/bash

# Process all CSV files in a directory
for file in ./csv_files/*.csv; do
    echo "Processing $file..."
    java ServiceNowFileUploader \
        "$file" \
        instance.service-now.com \
        "/api/now/import/x_table_name" \
        "$CLIENT_ID" \
        "$CLIENT_SECRET" \
        "$REFRESH_TOKEN" \
        true \
        "$USERNAME" \
        "$PASSWORD" \
        true
    
    # Save updated refresh token for next iteration
    # (Parse from output if needed)
    sleep 2
done
```

## Runner Scripts

### PowerShell (Windows)

**Setup (one-time):**
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

**Run with config:**
```powershell
.\run.ps1 --config
```

**Run interactive:**
```powershell
.\run.ps1
```

See [POWERSHELL_GUIDE.md](POWERSHELL_GUIDE.md) for detailed instructions.

### Bash (Linux/Mac)

**Make executable:**
```bash
chmod +x run.sh
```

**Run with config:**
```bash
./run.sh --config
```

**Run interactive:**
```bash
./run.sh
```

See [README_BASH.md](README_BASH.md) for detailed instructions.

## License

Proprietary - ServiceNow Integration

## Support

For issues or questions:
1. Check the Troubleshooting section above
2. Verify all parameters are correct
3. Check ServiceNow logs for detailed error messages
4. Ensure network connectivity to ServiceNow instance
5. Review config.properties.sample for parameter examples
