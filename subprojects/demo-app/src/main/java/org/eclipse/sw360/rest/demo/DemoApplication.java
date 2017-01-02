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
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
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
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class DemoApplication {

    // Currently assuming the all services running in docker containers
    private static final String DOCKER_HOST = "http://192.168.99.100";
    private static final String THRIFT_SERVER_URL = DOCKER_HOST + ":8080";
    private static final String REST_SERVER_URL = DOCKER_HOST + ":8091";
    private static final String AUTH_SERVER_URL = DOCKER_HOST + ":8090";

    // to get test data, download
    // https://repo.spring.io/release/org/springframework/spring/4.3.5.RELEASE/spring-framework-4.3.5.RELEASE-dist.zip
    // and unzip it. SPRING_FRAMEWORK_DIST has to point to the unzipped distribution
    private static final String SPRING_FRAMEWORK_DIST = "D:/downloads/spring-framework-4.3.5.RELEASE";

    private RestTemplate restTemplate = new RestTemplate();
    private ObjectMapper objectMapper = new ObjectMapper();
    private String accessToken;

    private void createProject() throws Exception {
        Map<String, String> project = new HashMap<>();
        project.put("name", "Spring Framework");
        project.put("description", "The Spring Framework provides a comprehensive programming and configuration model for modern Java-based enterprise applications.");
        project.put("projectType", ProjectType.PRODUCT.toString());

        String jsonBody = this.objectMapper.writeValueAsString(project);

        HttpHeaders headers = getHeadersWithBearerToken(getAccessToken());

        HttpEntity<String> httpEntity = new HttpEntity<>(jsonBody, headers);

        try {
            restTemplate.postForObject(REST_SERVER_URL + "/api/projects", httpEntity, String.class);
        } catch (Exception e) {
            System.out.print("could not create project: ");
            if (e instanceof HttpServerErrorException) {
                System.out.println(((HttpServerErrorException) e).getResponseBodyAsString());
            } else {
                System.out.println(e.getMessage());
            }
        }
    }

    private void createComponent(String name, String version) throws Exception {
        Map<String, String> component = new HashMap<>();
        component.put("name", name);
        component.put("description", name + " is part of the Spring framework");
        component.put("componentType", ComponentType.OSS.toString());

        String jsonBody = this.objectMapper.writeValueAsString(component);

        HttpHeaders headers = getHeadersWithBearerToken(getAccessToken());

        HttpEntity<String> httpEntity = new HttpEntity<>(jsonBody, headers);

        try {
            URI location = restTemplate.postForLocation(REST_SERVER_URL + "/api/components", httpEntity);
            String path = location.getPath();
            String componentId = path.substring(path.lastIndexOf('/') + 1);
            createRelease(name, version, componentId);
        } catch (Exception e) {
            System.out.print("could not create component: ");
            if (e instanceof HttpServerErrorException) {
                System.out.println(((HttpServerErrorException) e).getResponseBodyAsString());
            } else {
                System.out.println(e.getMessage());
            }
        }

    }

    private void createRelease(String name, String version, String componentId) throws Exception {
        Map<String, String> release = new HashMap<>();
        release.put("name", name);
        release.put("componentId", componentId);
        release.put("version", version);
        release.put("clearingState", ClearingState.APPROVED.toString());

        String jsonBody = this.objectMapper.writeValueAsString(release);

        HttpHeaders headers = getHeadersWithBearerToken(getAccessToken());

        HttpEntity<String> httpEntity = new HttpEntity<>(jsonBody, headers);

        try {
            restTemplate.postForObject(REST_SERVER_URL + "/api/releases", httpEntity, String.class);
        } catch (Exception e) {
            System.out.print("could not create release: ");
            if (e instanceof HttpServerErrorException) {
                System.out.println(((HttpServerErrorException) e).getResponseBodyAsString());
            } else {
                System.out.println(e.getMessage());
            }
        }

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

    private void checkAndCreateAdminUser() throws TException {
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

    private void createSpringFrameworkComponents() {
        Path dir = Paths.get(SPRING_FRAMEWORK_DIST + "/libs");
        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(dir, "*.jar")) {
            for (Path path : stream) {
                addComponent(path.getFileName().toString());
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private void addComponent(String jarFile) {
        if (jarFile.contains("javadoc") || jarFile.contains("sources")) {
            return;
        }
        int indexOfFirstDigit = 0;
        while (indexOfFirstDigit < jarFile.length() && !Character.isDigit(jarFile.charAt(indexOfFirstDigit)))
            indexOfFirstDigit++;
        String componentName = jarFile.substring(0, indexOfFirstDigit - 1);
        String componentVersion = jarFile.substring(indexOfFirstDigit, jarFile.length() - 4);

        try {
            createComponent(componentName, componentVersion);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        DemoApplication demoClient = new DemoApplication();
        demoClient.checkAndCreateAdminUser();
        demoClient.createProject();
        demoClient.createSpringFrameworkComponents();
    }

}
