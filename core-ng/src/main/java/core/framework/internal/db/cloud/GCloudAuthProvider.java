package core.framework.internal.db.cloud;

import core.framework.db.CloudAuthProvider;
import core.framework.db.Dialect;
import core.framework.http.HTTPClient;
import core.framework.http.HTTPMethod;
import core.framework.http.HTTPRequest;
import core.framework.http.HTTPResponse;
import core.framework.util.Strings;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * @author neo
 */
public final class GCloudAuthProvider implements CloudAuthProvider {
    // for timeout settings, refer to gcloud sdk com.google.auth.oauth2.ComputeEngineCredentials#runningOnComputeEngine
    private final HTTPClient httpClient = HTTPClient.builder()
        .connectTimeout(Duration.ofMillis(500))
        .timeout(Duration.ofSeconds(1))
        .maxRetries(3)
        .retryWaitTime(Duration.ofMillis(50))
        .build();

    @Nullable
    String user;    // iam user, won't change once pod is created
    @Nullable
    String accessToken;
    long expirationTime;

    @Override
    public String user(Dialect dialect) {
        // email won't change once pod created
        if (user == null) {
            user = parseUser(dialect, metadata("email"));
        }
        return user;
    }

    @Override
    public String accessToken() {
        if (accessToken != null && System.currentTimeMillis() < expirationTime) {
            return accessToken;
        }

        long now = System.currentTimeMillis();  // record current time, to make sure expire time is always earlier than metadata side
        String tokenJSON = metadata("token");
        accessToken = parseAccessToken(tokenJSON);
        // token will be refreshed 300s before expiration, and connected connection will not be invalidated
        // set expirationTime after access token, to handle multi thread conditions
        expirationTime = now + (parseExpirationTimeInSec(tokenJSON) - 300) * 1_000L;
        return accessToken;
    }

    // MySQL: strip entire domain
    // PostgreSQL: strip ".gserviceaccount.com", refer to https://cloud.google.com/sql/docs/postgres/add-manage-iam-users
    private String parseUser(Dialect dialect, String email) {
        if (dialect == Dialect.MYSQL) {
            int index = email.indexOf('@');
            return email.substring(0, index);
        } else if (dialect == Dialect.POSTGRESQL && email.endsWith(".gserviceaccount.com")) {
            return email.substring(0, email.length() - 20);  // remove ".gserviceaccount.com"
        }
        throw new Error(Strings.format("unsupported gcloud iam user, dialect={}, email={}", dialect, email));
    }

    private String parseAccessToken(String tokenJSON) {
        // use fastest way to parse to reduce overhead
        // example value: {"access_token":"ya29.c.b0AXv0zTNPzp_Hl__8DJzw1KHKq_B9vhP7eHbRz18ar38gl_hbPt5YugF_2P7WOaqXPIM9JkllokdupT3vs7-rCssMoK0x2eguM8KG_wuOcDCJkUHNcDxriip6hq2Ww8FvE_XIlHivxMO607P1wIHIyv0kOV0iorcJ-G-1oEO4Pa5Rq9x1lQNXY2O3CoEjA4d8tFB0LPfkvfOOQpqpdwbww5LFiAFWy6OJzZphxxXisXaS_AIurTNduA0BQJViwprb8awSrUDbB3owais8wadFrZ-GV09aMqHvaORsydFb12T5TRhpzdqFSRr__H734VBH_if3XzbW5o2oqPUP9Ye3ynE9WYZqhBDHV7y0nqeNm5c2qD7KmRizO2ZdMzPxRVmQeafyOJEwb67H3vX3Cc6Wmz7Orujcas8R9UNNXDEPVy0GJdIlIXXFZJvwq0h95g.........................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................","expires_in":3561,"token_type":"Bearer"}
        int index = tokenJSON.indexOf('"', 17);
        return tokenJSON.substring(17, index);
    }

    int parseExpirationTimeInSec(String tokenJSON) {
        int startIndex = tokenJSON.indexOf("\"expires_in\":") + 13;
        int endIndex = tokenJSON.indexOf(',', startIndex);
        return Integer.parseInt(tokenJSON.substring(startIndex, endIndex));
    }

    String metadata(String attribute) {
        // 169.254.169.254 is metadata.google.internal, use ip to save dns query
        var request = new HTTPRequest(HTTPMethod.GET, "http://169.254.169.254/computeMetadata/v1/instance/service-accounts/default/" + attribute);
        request.headers.put("Metadata-Flavor", "Google");
        HTTPResponse response = httpClient.execute(request);
        if (response.statusCode != 200) throw new Error("failed to fetch gcloud iam metadata, status=" + response.statusCode);
        return response.text();
    }
}
