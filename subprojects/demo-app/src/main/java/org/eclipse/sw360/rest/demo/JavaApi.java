/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class JavaApi {

    private final String REST_SERVER_URL;
    private final String AUTH_SERVER_URL;

    private RestTemplate restTemplate = new RestTemplate();
    private ObjectMapper objectMapper = new ObjectMapper();
    private String accessToken;

    public JavaApi(String dockerHost) {
        REST_SERVER_URL = dockerHost + ":8091";
        AUTH_SERVER_URL = dockerHost + ":8090";
    }

    public void createProject(String name, String description) throws Exception {
        Map<String, String> project = new HashMap<>();
        project.put("name", name);
        project.put("description", description);
        project.put("projectType", ProjectType.PRODUCT.toString());

        HttpEntity<String> httpEntity = getHttpEntity(project);

        restTemplate.postForObject(REST_SERVER_URL + "/api/projects", httpEntity, String.class);
    }

    public String createComponent(String name) throws Exception {
        Map<String, String> component = new HashMap<>();
        component.put("name", name);
        component.put("description", name + " is part of the Spring framework");
        component.put("componentType", ComponentType.OSS.toString());

        HttpEntity<String> httpEntity = getHttpEntity(component);

        URI location = restTemplate.postForLocation(REST_SERVER_URL + "/api/components", httpEntity);
        String path = location.getPath();
        String componentId = path.substring(path.lastIndexOf('/') + 1);
        return componentId;
    }

    public void createRelease(String name, String version, String componentId) throws Exception {
        Map<String, String> release = new HashMap<>();
        release.put("name", name);
        release.put("componentId", componentId);
        release.put("version", version);
        release.put("clearingState", ClearingState.APPROVED.toString());

        HttpEntity<String> httpEntity = getHttpEntity(release);

        restTemplate.postForObject(REST_SERVER_URL + "/api/releases", httpEntity, String.class);
    }

    private HttpEntity<String> getHttpEntity(Map<String, String> component) throws IOException {
        String jsonBody = this.objectMapper.writeValueAsString(component);
        HttpHeaders headers = getHeadersWithBearerToken(getAccessToken());
        return new HttpEntity<>(jsonBody, headers);
    }

    private String getAccessToken() throws IOException {
        if (this.accessToken == null) {
            HttpHeaders headers = getHeadersForAccessToken();
            HttpEntity<String> httpEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response =
                    restTemplate
                            .postForEntity(AUTH_SERVER_URL + "/oauth/token?grant_type=password&username=admin@sw360.org&password=sw360-admin-password",
                                    httpEntity,
                                    String.class);

            String responseText = response.getBody();
            HashMap jwtMap = new ObjectMapper().readValue(responseText, HashMap.class);
            this.accessToken = (String) jwtMap.get("access_token");
        }
        return this.accessToken;
    }

    private HttpHeaders getHeadersForAccessToken() throws UnsupportedEncodingException {
        String clientCredentials = "trusted-sw360-client:sw360-secret";
        String base64ClientCredentials = Base64.getEncoder().encodeToString(clientCredentials.getBytes("utf-8"));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + base64ClientCredentials);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        return headers;
    }

    private HttpHeaders getHeadersWithBearerToken(String accessToken) throws UnsupportedEncodingException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        return headers;
    }
}
