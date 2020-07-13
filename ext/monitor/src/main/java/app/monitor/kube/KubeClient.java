package app.monitor.kube;

import core.framework.api.http.HTTPStatus;
import core.framework.http.ContentType;
import core.framework.http.HTTPClient;
import core.framework.http.HTTPMethod;
import core.framework.http.HTTPRequest;
import core.framework.http.HTTPResponse;
import core.framework.json.JSON;
import core.framework.util.Files;

import java.nio.file.Path;

/**
 * @author neo
 */
public class KubeClient {
    private HTTPClient httpClient;
    private String token;

    // only support Pod ServiceAccount auth within cluster
    public void initialize() {
        // kube 1.6 starts support TLSv1.3, and break java TLSv1.3 impl,
        // workaround is to downgrade to TLSv1.2, jdk 14.0.2 will fix
        // refer to https://bugs.openjdk.java.net/browse/JDK-8236039, https://github.com/golang/go/issues/35722
        httpClient = HTTPClient.builder()
                               .trust(Files.text(Path.of("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt")))
                               .tlsVersions("TLSv1.2")
                               .build();
        // token will not be refreshed after pod created, if the token is changed somehow, it has to recreate pod
        token = Files.text(Path.of("/var/run/secrets/kubernetes.io/serviceaccount/token"));
    }

    public PodList listPods(String namespace) {
        HTTPRequest request = new HTTPRequest(HTTPMethod.GET, "https://kubernetes.default.svc/api/v1/namespaces/" + namespace + "/pods");
        request.bearerAuth(token);
        request.accept(ContentType.APPLICATION_JSON);
        HTTPResponse response = httpClient.execute(request);
        if (response.statusCode != HTTPStatus.OK.code) {
            throw new Error("failed to call kube api, statusCode=" + response.statusCode + ", message=" + response.text());
        }
        // not using validation to reduce overhead
        return JSON.fromJSON(PodList.class, response.text());
    }
}