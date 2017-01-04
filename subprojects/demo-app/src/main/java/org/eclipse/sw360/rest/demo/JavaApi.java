/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.springframework.http.*;
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
    private String projectsUrl;
    private String componentsUrl;
    private String releasesUrl;

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

        restTemplate.postForObject(projectsUrl, httpEntity, String.class);
    }

    public URI createComponent(String name) throws Exception {
        Map<String, String> component = new HashMap<>();
        component.put("name", name);
        component.put("description", name + " is part of the Spring framework");
        component.put("componentType", ComponentType.OSS.toString());

        HttpEntity<String> httpEntity = getHttpEntity(component);

        URI location = restTemplate.postForLocation(componentsUrl, httpEntity);
        return location;
    }

    public void createRelease(String name, String version, URI componentURI) throws Exception {
        Map<String, String> release = new HashMap<>();
        release.put("name", name);
        release.put("componentId", componentURI.toString());
        release.put("version", version);
        release.put("clearingState", ClearingState.APPROVED.toString());

        HttpEntity<String> httpEntity = getHttpEntity(release);

        restTemplate.postForObject(releasesUrl, httpEntity, String.class);
    }

    public void getLinksFromApiRoot() throws Exception {
        Map<String, String> dummy = new HashMap<>();
        HttpEntity<String> httpEntity = getHttpEntity(dummy);

        ResponseEntity<String> response =
                restTemplate.exchange(REST_SERVER_URL + "/api",
                        HttpMethod.GET,
                        httpEntity,
                        String.class);

        JsonNode responseNode = new ObjectMapper().readTree(response.getBody());

        JsonNode linksNode = responseNode.get("_links");
        String curieName = linksNode.get("curies").get(0).get("name").asText();
        this.projectsUrl = linksNode.get(curieName + ":projects").get("href").asText();
        this.componentsUrl = linksNode.get(curieName + ":components").get("href").asText();
        this.releasesUrl = linksNode.get(curieName + ":releases").get("href").asText();
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
                            .postForEntity(AUTH_SERVER_URL + "/oauth/token?"
                                    + "grant_type=password&username=admin@sw360.org&password=sw360-admin-password",
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
        String base64ClientCredentials =
                Base64.getEncoder().encodeToString(clientCredentials.getBytes("utf-8"));

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
