import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

/**
 * ServiceNow File Uploader Application
 * 
 * This application handles OAuth authentication with ServiceNow and uploads
 * employee data from a CSV file to a specified ServiceNow endpoint.
 * 
 * Supports optional file deletion control via DELETE_FILE_FLAG parameter.
 */
public class ServiceNowFileUploader {

	// Constants
	private static final String HTTPS_PROTOCOL = "https://";
	private static final String CONTENT_TYPE_HEADER = "Content-Type";
	private static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";

	public static void main(String[] args) {
		// Initialize parameters
		String filePath = null;
		String endpointServiceNow = null;
		String queryString = null;
		String clientId = null;
		String clientSecret = null;
		String refreshToken = null;
		Boolean refreshTokenFlag = false;
		String username = null;
		String password = null;
		Boolean deleteFileFlag = true; // Default to true (delete file)

		// Parse command line arguments or use interactive input
		if (args.length > 0) {
			// Command line mode
			if (args.length < 9) {
				printUsage();
				System.exit(1);
			}
			filePath = args[0];
			endpointServiceNow = args[1];
			queryString = args[2];
			clientId = args[3];
			clientSecret = args[4];
			refreshToken = args[5];
			refreshTokenFlag = Boolean.parseBoolean(args[6]);
			username = args[7];
			password = args[8];
			// Optional 10th parameter for delete file flag
			if (args.length > 9) {
				deleteFileFlag = !args[9].equalsIgnoreCase("false");
			}
		} else {
			// Interactive mode
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			try {
				System.out.println("=== ServiceNow File Uploader ===\n");
				
				System.out.print("Enter file path: ");
				filePath = reader.readLine().trim();
				
				System.out.print("Enter ServiceNow endpoint (e.g., instance.service-now.com): ");
				endpointServiceNow = reader.readLine().trim();
				
				System.out.print("Enter query string (API endpoint): ");
				queryString = reader.readLine().trim();
				
				System.out.print("Enter client ID: ");
				clientId = reader.readLine().trim();
				
				System.out.print("Enter client secret: ");
				clientSecret = reader.readLine().trim();
				
				System.out.print("Enter refresh token (or press Enter if not available): ");
				refreshToken = reader.readLine().trim();
				
				System.out.print("Is refresh token already available? (true/false): ");
				refreshTokenFlag = Boolean.parseBoolean(reader.readLine().trim());
				
				System.out.print("Enter username: ");
				username = reader.readLine().trim();
				
				System.out.print("Enter password: ");
				password = reader.readLine().trim();
				
				System.out.print("Delete CSV file after successful upload? (true/false) [default: true]: ");
				String deleteInput = reader.readLine().trim();
				if (!deleteInput.isEmpty()) {
					deleteFileFlag = !deleteInput.equalsIgnoreCase("false");
				}
				
			} catch (IOException e) {
				System.err.println("Error reading input: " + e.getMessage());
				System.exit(1);
			}
		}

		// Validate inputs
		if (!validateInputs(filePath, endpointServiceNow, clientId, clientSecret, username, password)) {
			System.exit(1);
		}

		try {
			// Execute the file upload
			System.out.println("\nInitiating ServiceNow file upload...");
			PostResult result = postFileToServiceNow(filePath, endpointServiceNow, queryString, 
					clientId, clientSecret, refreshToken, refreshTokenFlag, username, password, deleteFileFlag);
			
			// Display results
			displayResults(result);
			
		} catch (IOException e) {
			System.err.println("IO Error: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			System.err.println("Security Error: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Validates all required input parameters
	 */
	private static boolean validateInputs(String filePath, String endpoint, String clientId, 
			String clientSecret, String username, String password) {
		if (filePath == null || filePath.isEmpty()) {
			System.err.println("Error: File path is required");
			return false;
		}
		
		File file = new File(filePath);
		if (!file.exists()) {
			System.err.println("Error: File not found at " + filePath);
			return false;
		}
		
		if (endpoint == null || endpoint.isEmpty()) {
			System.err.println("Error: ServiceNow endpoint is required");
			return false;
		}
		
		if (clientId == null || clientId.isEmpty()) {
			System.err.println("Error: Client ID is required");
			return false;
		}
		
		if (clientSecret == null || clientSecret.isEmpty()) {
			System.err.println("Error: Client secret is required");
			return false;
		}
		
		if (username == null || username.isEmpty()) {
			System.err.println("Error: Username is required");
			return false;
		}
		
		if (password == null || password.isEmpty()) {
			System.err.println("Error: Password is required");
			return false;
		}
		
		return true;
	}

	/**
	 * Displays upload results
	 */
	private static void displayResults(PostResult result) {
		System.out.println("\n=== Upload Results ===");
		System.out.println("Status: " + result.getStatus());
		System.out.println("Response Code: " + result.getResponseCode());
		System.out.println("Refresh Token Updated: " + result.isRefreshTokenUpdated());
		System.out.println("Delete File Flag: " + result.isDeleteFileFlag());
		
		if (result.getRefreshToken() != null && !result.getRefreshToken().isEmpty()) {
			System.out.println("New Refresh Token: " + 
				result.getRefreshToken().substring(0, Math.min(20, result.getRefreshToken().length())) + "...");
		}
		
		if ("Success".equals(result.getStatus())) {
			System.out.println("\n✓ File successfully uploaded to ServiceNow");
		} else {
			System.out.println("\n✗ File upload failed");
		}
	}

	/**
	 * Prints usage information
	 */
	private static void printUsage() {
		System.out.println("Usage: java ServiceNowFileUploader [options]");
		System.out.println("\nCommand line mode:");
		System.out.println("java ServiceNowFileUploader <filePath> <endpoint> <queryString> <clientId> " +
				"<clientSecret> <refreshToken> <refreshTokenFlag> <username> <password> [deleteFileFlag]");
		System.out.println("\nInteractive mode (no arguments):");
		System.out.println("java ServiceNowFileUploader");
		System.out.println("\nParameters:");
		System.out.println("  filePath          - Path to the CSV file to upload");
		System.out.println("  endpoint          - ServiceNow instance endpoint");
		System.out.println("  queryString       - API query string/endpoint");
		System.out.println("  clientId          - OAuth client ID");
		System.out.println("  clientSecret      - OAuth client secret");
		System.out.println("  refreshToken      - OAuth refresh token (can be empty initially)");
		System.out.println("  refreshTokenFlag  - Whether refresh token is already available (true/false)");
		System.out.println("  username          - ServiceNow username");
		System.out.println("  password          - ServiceNow password");
		System.out.println("  deleteFileFlag    - Whether to delete file after upload (true/false, default: true)");
	}

	/**
	 * Retrieves a new access token using the refresh token
	 */
	private static String getAccessToken(String endpointServiceNow, String clientId, String clientSec, 
			String refreshToken) throws IOException, NoSuchAlgorithmException, KeyManagementException {
		
		String accessToken = "";

		// Use default SSL context with proper certificate validation
		SSLContext sc = SSLContext.getInstance("TLSv1.2");
		sc.init(null, null, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// URL of the endpoint
		URL tokenurl = new URL(HTTPS_PROTOCOL + endpointServiceNow + "/oauth_token.do");
		HttpURLConnection tokenConnection = (HttpURLConnection) tokenurl.openConnection();

		// Set the request method to POST
		tokenConnection.setRequestMethod("POST");

		// Set the request headers
		tokenConnection.setRequestProperty(CONTENT_TYPE_HEADER, CONTENT_TYPE_FORM_URLENCODED);

		// Enable input and output streams
		tokenConnection.setDoOutput(true);

		// Create the request body - properly URL encode ALL parameters
		String requestBody = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
				"&client_secret=" + URLEncoder.encode(clientSec, StandardCharsets.UTF_8) +
				"&grant_type=refresh_token" +
				"&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

		// Write the request body to the output stream
		try (OutputStream os = tokenConnection.getOutputStream()) {
			byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
			os.write(input, 0, input.length);
		}

		// Get the response code
		int tokenresponseCode = tokenConnection.getResponseCode();

		// Read the response (use error stream for non-2xx)
		InputStream is = (tokenresponseCode >= 200 && tokenresponseCode < 300)
				? tokenConnection.getInputStream()
				: tokenConnection.getErrorStream();

		StringBuilder tokenresponse = new StringBuilder();
		if (is != null) {
			try (BufferedReader restokenread = new BufferedReader(
					new InputStreamReader(is, StandardCharsets.UTF_8))) {
				String responseLine;
				while ((responseLine = restokenread.readLine()) != null) {
					tokenresponse.append(responseLine.trim());
				}
			}
		}

		// Parse the JSON response of token
		String responseStr = tokenresponse.toString();
		String tokenKey = "\"access_token\":\"";
		int startIndex = responseStr.indexOf(tokenKey);
		if (startIndex < 0) {
			System.err.println("Failed to retrieve access token. Response: " + responseStr);
			return null;
		}
		startIndex += tokenKey.length();
		int endIndex = responseStr.indexOf("\"", startIndex);
		if (endIndex < 0) {
			System.err.println("Failed to parse access token from response");
			return null;
		}

		accessToken = responseStr.substring(startIndex, endIndex);
		System.out.println("✓ Access token retrieved successfully");

		return accessToken;
	}

	/**
	 * Retrieves a refresh token using username and password
	 */
	private static String getRefreshToken(String endpointServiceNow, String clientId, String clientSec, 
			String username, String password) throws IOException, NoSuchAlgorithmException, 
			KeyManagementException {
		
		String refreshToken = "";

		// Use default SSL context with proper certificate validation
		SSLContext sc = SSLContext.getInstance("TLSv1.2");
		sc.init(null, null, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// URL of the endpoint
		URL tokenurl = new URL(HTTPS_PROTOCOL + endpointServiceNow + "/oauth_token.do");
		HttpURLConnection tokenConnection = (HttpURLConnection) tokenurl.openConnection();

		// Set the request method to POST
		tokenConnection.setRequestMethod("POST");

		// Set the request headers
		tokenConnection.setRequestProperty(CONTENT_TYPE_HEADER, CONTENT_TYPE_FORM_URLENCODED);

		// Enable input and output streams
		tokenConnection.setDoOutput(true);

		// Create the request body - properly URL encode ALL parameters
		String requestBody = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
				"&client_secret=" + URLEncoder.encode(clientSec, StandardCharsets.UTF_8) +
				"&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8) +
				"&grant_type=password" +
				"&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

		System.out.println("DEBUG: Requesting refresh token from: " + HTTPS_PROTOCOL + endpointServiceNow + "/oauth_token.do");

		// Write the request body to the output stream
		try (OutputStream os = tokenConnection.getOutputStream()) {
			byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
			os.write(input, 0, input.length);
		}

		// Get response code and handle errors
		int responseCode = tokenConnection.getResponseCode();
		
		// Read the response (error or success)
		InputStream responseStream = (responseCode >= 200 && responseCode < 300) 
				? tokenConnection.getInputStream()
				: tokenConnection.getErrorStream();
		
		StringBuilder tokenresponse = new StringBuilder();
		if (responseStream != null) {
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(responseStream, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					tokenresponse.append(line.trim());
				}
			}
		}
		
		String responseStr = tokenresponse.toString();
		
		if (responseCode < 200 || responseCode >= 300) {
			System.err.println("Failed to get refresh token. HTTP " + responseCode);
			System.err.println("Error response: " + responseStr);
			System.err.println("\nPossible causes:");
			System.err.println("  - Invalid username or password");
			System.err.println("  - Invalid client ID or client secret");
			System.err.println("  - OAuth application not configured in ServiceNow");
			System.err.println("  - Service account doesn't have API access");
			return null;
		}

		// Parse the JSON response of token
		String tokenKey = "\"refresh_token\":\"";
		int startIndex = responseStr.indexOf(tokenKey);
		if (startIndex < 0) {
			System.err.println("Failed to parse refresh token from response: " + responseStr);
			return null;
		}
		startIndex += tokenKey.length();
		int endIndex = responseStr.indexOf("\"", startIndex);
		if (endIndex < 0) {
			System.err.println("Failed to find end of refresh token");
			return null;
		}
		refreshToken = responseStr.substring(startIndex, endIndex);
		System.out.println("✓ Refresh token retrieved successfully");

		return refreshToken;
	}

	/**
	 * Helper method to decompress gzip-compressed response
	 */
	private static InputStream getDecompressedStream(HttpsURLConnection connection) throws IOException {
		String encoding = connection.getContentEncoding();
		InputStream stream = connection.getInputStream();
		
		if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
			return new GZIPInputStream(stream);
		}
		return stream;
	}

	/**
	 * Posts the file content to ServiceNow
	 */
	private static PostResult postFileToServiceNow(String filePath, String endpointServiceNow, 
			String queryString, String clientId, String clientSec, String refreshToken, 
			Boolean refreshTokenFlag, String username, String password, Boolean deleteFileFlag) 
			throws IOException, NoSuchAlgorithmException, KeyManagementException {
		
		// Read the file content
		System.out.println("Reading file: " + filePath);
		File file = new File(filePath);
		StringBuilder fileContent = new StringBuilder();
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(new FileInputStream(filePath), 
				Charset.forName("UTF-8")))) {
			String line;
			while ((line = br.readLine()) != null) {
				fileContent.append(line).append("\n");
			}
		}
		System.out.println("✓ File read successfully (" + fileContent.length() + " bytes)");

		boolean refreshTokenUpdated = false;

		// Get refresh token if not provided
		if (!refreshTokenFlag) {
			System.out.println("Obtaining refresh token...");
			refreshToken = getRefreshToken(endpointServiceNow, clientId, clientSec, username, password);
			if (refreshToken == null || refreshToken.isEmpty()) {
				return new PostResult("Failure - Could not obtain refresh token", refreshToken, 
						true, 401, deleteFileFlag);
			}
			refreshTokenUpdated = true;
		}

		// Get access token
		System.out.println("Obtaining access token...");
		String accessToken = getAccessToken(endpointServiceNow, clientId, clientSec, refreshToken);

		if (accessToken == null || accessToken.isEmpty()) {
			System.out.println("Access token expired, refreshing...");
			refreshToken = getRefreshToken(endpointServiceNow, clientId, clientSec, username, password);
			if (refreshToken == null || refreshToken.isEmpty()) {
				return new PostResult("Failure - Could not refresh token", refreshToken, true, 401, deleteFileFlag);
			}
			refreshTokenUpdated = true;
			accessToken = getAccessToken(endpointServiceNow, clientId, clientSec, refreshToken);
		}

		if (accessToken == null || accessToken.isEmpty()) {
			return new PostResult("Failure - Could not obtain access token", refreshToken, 
					refreshTokenUpdated, 401, deleteFileFlag);
		}

		// Use default SSL context with proper certificate validation
		System.out.println("Setting up SSL connection...");
		SSLContext sslc = SSLContext.getInstance("TLSv1.2");
		sslc.init(null, null, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sslc.getSocketFactory());

		// Build the URL
		String builtUrl = HTTPS_PROTOCOL + endpointServiceNow + queryString;
		System.out.println("Uploading to: " + builtUrl);

		// Create the HTTP connection
		URL url = new URL(builtUrl);
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");

		// Set the headers
		connection.setRequestProperty("Authorization", "Bearer " + accessToken);
		connection.setRequestProperty("Accept", "*/*");
		connection.setRequestProperty("Cache-Control", "no-cache");
		connection.setRequestProperty("Host", endpointServiceNow);
		connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
		connection.setRequestProperty("Connection", "keep-alive");
		connection.setRequestProperty("Content-Length", String.valueOf(fileContent.length()));
		connection.setRequestProperty("Content-Type", "text/plain");

		// Write the file content to the request body
		try (OutputStream os = connection.getOutputStream()) {
			os.write(fileContent.toString().getBytes(StandardCharsets.UTF_8));
			os.flush();
		}

		// Get the response
		int responseCode = connection.getResponseCode();
		System.out.println("Response Code: " + responseCode);

		// Handle file deletion based on flag
		if (responseCode == 201 || responseCode == 200) {
			if (deleteFileFlag) {
				try {
					Files.delete(file.toPath());
					System.out.println("✓ File deleted after upload");
				} catch (IOException e) {
					System.err.println("Warning: Could not delete file: " + e.getMessage());
				}
			} else {
				System.out.println("✓ File retained (DELETE_FILE_FLAG is false)");
			}
			return new PostResult("Success", refreshToken, refreshTokenUpdated, responseCode, deleteFileFlag);
		} else {
			System.out.println("ℹ File retained (upload did not return success code)");
			// Read error response with proper decompression
			try (InputStream errorStream = connection.getErrorStream()) {
				if (errorStream != null) {
					String encoding = connection.getContentEncoding();
					InputStream decompressedStream = errorStream;
					
					if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
						decompressedStream = new GZIPInputStream(errorStream);
					}
					
					try (BufferedReader reader = new BufferedReader(
							new InputStreamReader(decompressedStream, StandardCharsets.UTF_8))) {
						String line;
						StringBuilder errorResponse = new StringBuilder();
						while ((line = reader.readLine()) != null) {
							errorResponse.append(line);
						}
						String errorMsg = errorResponse.toString();
						if (!errorMsg.isEmpty()) {
							System.err.println("Error Response: " + errorMsg);
						}
					}
				}
			}
			return new PostResult("Failure", refreshToken, refreshTokenUpdated, responseCode, deleteFileFlag);
		}
	}
}

/**
 * Helper class to hold POST operation results
 */
class PostResult {
	private String status;
	private String refreshToken;
	private boolean refreshTokenUpdated;
	private int responseCode;
	private boolean deleteFileFlag;

	public PostResult(String status, String refreshToken, boolean refreshTokenUpdated, 
			int responseCode, boolean deleteFileFlag) {
		this.status = status;
		this.refreshToken = refreshToken;
		this.refreshTokenUpdated = refreshTokenUpdated;
		this.responseCode = responseCode;
		this.deleteFileFlag = deleteFileFlag;
	}

	public String getStatus() {
		return status;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public boolean isRefreshTokenUpdated() {
		return refreshTokenUpdated;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public boolean isDeleteFileFlag() {
		return deleteFileFlag;
	}
}