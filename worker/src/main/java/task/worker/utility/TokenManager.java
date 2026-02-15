package task.worker.utility;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class TokenManager {

    private final RestTemplate rt;

    private volatile String accessToken;
    private volatile LocalDateTime expiringOn;

    @Value("${worker.serverBaseUrl}")
    private String serverBaseUrl;
    @Value("${worker.nodeId}")
    private String nodeId;
    @Value("${worker.nodeSecret}")
    private String nodeSecret;

    public TokenManager(RestTemplate rt) {
        this.rt = rt;
    }

    public String getValidToken() {
        if (accessToken == null || expiringOn == null || expiringOn.minusSeconds(60).isBefore(LocalDateTime.now())) {
            login();
        }
        return accessToken;
    }

    public synchronized void login() {
        if (accessToken != null && expiringOn != null && expiringOn.minusSeconds(60).isAfter(LocalDateTime.now())) {
            return;
        }

        String url = serverBaseUrl + "/auth/login/node";

        Map<String, Object> body = Map.of(
                "nodeId", nodeId,
                "nodeSecret", nodeSecret
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> resp = rt.postForObject(url, body, Map.class);

        if (resp == null || resp.get("accessToken") == null) {
            throw new IllegalStateException("Login failed: no accessToken");
        }

        this.accessToken = resp.get("accessToken").toString();

        Object exp = resp.get("expiringOn");
        if (exp == null) {
            this.expiringOn = LocalDateTime.now().plusMinutes(55);
        } else {
            this.expiringOn = LocalDateTime.parse(exp.toString());
        }
    }

    public void invalidate() {
        this.accessToken = null;
        this.expiringOn = null;
    }
}

