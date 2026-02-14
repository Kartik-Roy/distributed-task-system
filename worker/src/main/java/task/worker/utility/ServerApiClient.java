package task.worker.utility;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import task.worker.dto.GetTaskDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class ServerApiClient {

    private final RestTemplate rt;
    private final TokenManager tokenManager;

    @Value("${worker.serverBaseUrl}")
    private String serverBaseUrl;

    public ServerApiClient(RestTemplate rt, TokenManager tokenManager) {
        this.rt = rt;
        this.tokenManager = tokenManager;
    }

    public GetTaskDto getByTaskId(String taskId) {
        String url = serverBaseUrl + "/task/getByTaskId?taskId=" + enc(taskId);
        return callWithAuth(url, HttpMethod.GET, null, GetTaskDto.class);
    }

    public List<GetTaskDto> getAllForNode() {
        String url = serverBaseUrl + "/task/getAllForNode";
        GetTaskDto[] arr = callWithAuth(url, HttpMethod.GET, null, GetTaskDto[].class);
        return arr == null ? List.of() : Arrays.asList(arr);
    }

    public void updateStatus(String taskId, String oldStatus, String newStatus) {
        String url = serverBaseUrl + "/task/updateStatus?taskId=" + enc(taskId)
                + "&oldStatus=" + enc(oldStatus)
                + "&newStatus=" + enc(newStatus);

        callWithAuth(url, HttpMethod.PUT, null, String.class);
    }

    private <T> T callWithAuth(String url, HttpMethod method, Object body, Class<T> respType) {
        try {
            return doCall(url, method, body, respType);
        } catch (HttpClientErrorException.Unauthorized e) {
            // re-login once
            tokenManager.invalidate();
            tokenManager.login();
            return doCall(url, method, body, respType);
        }
    }

    private <T> T doCall(String url, HttpMethod method, Object body, Class<T> respType) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(tokenManager.getValidToken());
        if (body != null) h.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> entity = new HttpEntity<>(body, h);
        ResponseEntity<T> resp = rt.exchange(url, method, entity, respType);

        return resp.getBody();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // DTO as you described

}
