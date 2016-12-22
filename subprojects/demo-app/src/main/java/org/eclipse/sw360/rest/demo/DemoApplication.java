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
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class DemoApplication {

    private static final String THRIFT_SERVER_URL = "http://localhost:8080";
    private static final String REST_SERVER_URL = "http://localhost:8091";
    private static final String AUTH_SERVER_URL = "http://localhost:8090";

    private RestTemplate restTemplate = new RestTemplate();
    private ObjectMapper objectMapper = new ObjectMapper();
    private String accessToken;

    private void createProject() throws Exception {
        Map<String, String> project = new HashMap<>();
        project.put("name", "Spring Framework");
        project.put("description", "The Spring Framework provides a comprehensive programming and configuration model for modern Java-based enterprise applications.");
        project.put("projectType", "PRODUCT");

        String jsonBody = this.objectMapper.writeValueAsString(project);

        HttpHeaders headers = getHeadersWithBearerToken(getAccessToken());

        HttpEntity<String> httpEntity = new HttpEntity<>(jsonBody,headers);

        try {
            restTemplate.postForObject(REST_SERVER_URL + "/api/projects", httpEntity, String.class);
        } catch (Exception e) {
            System.out.print("could not create project: ");
            if(e instanceof HttpServerErrorException) {
                System.out.println(((HttpServerErrorException)e).getResponseBodyAsString());
            } else {
                System.out.println(e.getMessage());
            }
        }
    }

    String getAccessToken() throws IOException {
        if(this.accessToken == null) {
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

    HttpHeaders getHeadersForAccessToken() throws UnsupportedEncodingException {
        String clientCredentials="trusted-sw360-client:sw360-secret";
        String base64ClientCredentials = Base64.getEncoder().encodeToString(clientCredentials.getBytes("utf-8"));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + base64ClientCredentials);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        return headers;
    }

    HttpHeaders getHeadersWithBearerToken(String accessToken) throws UnsupportedEncodingException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        return headers;
    }

    void checkAndCreateAdminUser() throws TException {
        THttpClient thriftClient = new THttpClient(THRIFT_SERVER_URL + "/users/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        UserService.Iface userClient = new UserService.Client(protocol);

        User admin = new User("admin", "admin@sw360.org", "SW360 Administration");
        admin.setUserGroup(UserGroup.ADMIN);

        try {
            userClient.getByEmail("admin@sw360.org");
            System.out.println("sw360 admin user already exists");
        } catch (Exception e) {
            System.out.println("creating admin user  => " + userClient.addUser(admin));
        }
    }

    public static void main(String[] args) throws Exception {
        DemoApplication demoClient = new DemoApplication();
        demoClient.checkAndCreateAdminUser();
        demoClient.createProject();
    }

}
