import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

/**
 * OAuth Connection Diagnostic Tool
 * 
 * This tool helps diagnose OAuth connection issues with ServiceNow
 */
public class TestOAuthConnection {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  ServiceNow OAuth Diagnostic Tool");
        System.out.println("========================================\n");

        String endpoint;
        String clientId;
        String clientSecret;
        String username;
        String password;

        if (args.length >= 5) {
            endpoint = args[0];
            clientId = args[1];
            clientSecret = args[2];
            username = args[3];
            password = args[4];
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            try {
                System.out.print("Enter ServiceNow endpoint (e.g., instance.service-now.com): ");
                endpoint = reader.readLine().trim();

                System.out.print("Enter Client ID: ");
                clientId = reader.readLine().trim();

                System.out.print("Enter Client Secret: ");
                clientSecret = reader.readLine().trim();

                System.out.print("Enter Username: ");
                username = reader.readLine().trim();

                System.out.print("Enter Password: ");
                password = reader.readLine().trim();

            } catch (IOException e) {
                System.err.println("Error reading input: " + e.getMessage());
                return;
            }
        }

        // Validate inputs
        if (endpoint.isEmpty() || clientId.isEmpty() || clientSecret.isEmpty() 
                || username.isEmpty() || password.isEmpty()) {
            System.err.println("Error: All parameters are required");
            return;
        }

        System.out.println("\nTesting OAuth Connection...\n");
        System.out.println("Configuration:");
        System.out.println("  Endpoint: " + endpoint);
        System.out.println("  Client ID: " + clientId.substring(0, Math.min(10, clientId.length())) + "...");
        System.out.println("  Username: " + username);
        System.out.println();

        testOAuthConnection(endpoint, clientId, clientSecret, username, password);
    }

    private static void testOAuthConnection(String endpoint, String clientId, String clientSecret,
            String username, String password) {
        try {
            // Setup SSL
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, null, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            String tokenUrl = "https://" + endpoint + "/oauth_token.do";
            System.out.println("Step 1: Testing connection to " + tokenUrl);

            URL url = new URL(tokenUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // Build request body
            String requestBody = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                    "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                    "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8) +
                    "&grant_type=password" +
                    "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

            System.out.println("Step 2: Sending OAuth request...\n");

            // Send request
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Get response
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode + "\n");

            // Read response
            InputStream responseStream = (responseCode >= 200 && responseCode < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            StringBuilder responseBody = new StringBuilder();
            if (responseStream != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(responseStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBody.append(line);
                    }
                }
            }

            String response = responseBody.toString();

            // Display results
            if (responseCode >= 200 && responseCode < 300) {
                System.out.println("✓ SUCCESS! OAuth connection working.\n");
                System.out.println("Response contains:");

                if (response.contains("refresh_token")) {
                    System.out.println("  ✓ refresh_token");
                }
                if (response.contains("access_token")) {
                    System.out.println("  ✓ access_token");
                }
                if (response.contains("expires_in")) {
                    System.out.println("  ✓ expires_in");
                }

                System.out.println("\nDiagnosis: Your credentials are correct!");
                System.out.println("You can now use the ServiceNow File Uploader.\n");

            } else if (responseCode == 401) {
                System.out.println("✗ AUTHENTICATION FAILED (HTTP 401)\n");
                System.out.println("Response: " + response + "\n");
                System.out.println("Possible causes:");
                System.out.println("  1. Invalid username - Check your ServiceNow username");
                System.out.println("  2. Invalid password - Check your password");
                System.out.println("  3. Invalid Client ID - Check OAuth application Client ID");
                System.out.println("  4. Invalid Client Secret - Check OAuth application Client Secret");
                System.out.println("  5. Service account doesn't have API access");
                System.out.println("\nHow to fix:");
                System.out.println("  - Verify username and password by logging into ServiceNow");
                System.out.println("  - Check Client ID and Secret in ServiceNow OAuth application settings");
                System.out.println("  - Ensure the service account has 'api_user' role");
                System.out.println("  - Ensure OAuth app is Active in ServiceNow\n");

            } else if (responseCode == 400) {
                System.out.println("✗ BAD REQUEST (HTTP 400)\n");
                System.out.println("Response: " + response + "\n");
                System.out.println("Possible causes:");
                System.out.println("  1. Missing or incorrect grant_type parameter");
                System.out.println("  2. Malformed request body");
                System.out.println("  3. Special characters not properly encoded\n");

            } else if (responseCode == 404) {
                System.out.println("✗ OAUTH ENDPOINT NOT FOUND (HTTP 404)\n");
                System.out.println("Possible causes:");
                System.out.println("  1. Wrong endpoint URL - Check your ServiceNow instance URL");
                System.out.println("  2. OAuth plugin not installed in ServiceNow");
                System.out.println("  3. /oauth_token.do endpoint is incorrect\n");
                System.out.println("Expected URL: https://" + endpoint + "/oauth_token.do\n");

            } else if (responseCode >= 500) {
                System.out.println("✗ SERVER ERROR (HTTP " + responseCode + ")\n");
                System.out.println("Response: " + response + "\n");
                System.out.println("Possible causes:");
                System.out.println("  1. ServiceNow instance is down");
                System.out.println("  2. OAuth service is experiencing issues");
                System.out.println("  3. Try again in a few moments\n");

            } else {
                System.out.println("✗ UNEXPECTED ERROR (HTTP " + responseCode + ")\n");
                System.out.println("Response: " + response + "\n");
            }

        } catch (java.net.ConnectException e) {
            System.out.println("✗ CONNECTION FAILED\n");
            System.out.println("Error: " + e.getMessage());
            System.out.println("\nPossible causes:");
            System.out.println("  1. Cannot reach ServiceNow instance");
            System.out.println("  2. Incorrect endpoint URL - Check spelling");
            System.out.println("  3. Network connectivity issue");
            System.out.println("  4. Firewall blocking connection\n");
            System.out.println("Verify your endpoint: " + endpoint + "\n");

        } catch (java.net.SocketTimeoutException e) {
            System.out.println("✗ CONNECTION TIMEOUT\n");
            System.out.println("Error: " + e.getMessage());
            System.out.println("\nPossible causes:");
            System.out.println("  1. ServiceNow instance is slow or unresponsive");
            System.out.println("  2. Network latency issue");
            System.out.println("  3. Firewall timeout\n");
            System.out.println("Try increasing the timeout or try again later.\n");

        } catch (Exception e) {
            System.out.println("✗ ERROR\n");
            System.out.println("Exception: " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
            System.out.println();
            e.printStackTrace();
        }
    }
}
