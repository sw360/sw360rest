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
import org.springframework.web.client.HttpServerErrorException;

import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DemoApplication {
    private String thriftServerUrl;

    // to get test data, download
    // https://repo.spring.io/release/org/springframework/spring/4.3.5.RELEASE/spring-framework-4.3.5.RELEASE-dist.zip
    // and unzip it. springFrameworkDist has to point to the unzipped distribution
    private String springFrameworkDist;

    private JavaApi javaApi;
    private List<URI> releaseURIs = new ArrayList<>();

    public DemoApplication(String restServerUrl, String authServerUrl, String thriftServerUrl, String springFrameworkDist) {
        this.thriftServerUrl = thriftServerUrl;
        this.springFrameworkDist = springFrameworkDist;

        javaApi = new JavaApi(restServerUrl,authServerUrl);
    }

    private void checkAndCreateAdminUser() throws TException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/users/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        UserService.Iface userClient = new UserService.Client(protocol);

        User admin = new User("admin", "admin@sw360.org", "SW360 Administration");
        admin.setFullname("John Doe");
        admin.setGivenname("John");
        admin.setLastname("Doe");
        admin.setUserGroup(UserGroup.ADMIN);

        try {
            userClient.getByEmail("admin@sw360.org");
            System.out.println("sw360 admin user already exists");
        } catch (Exception e) {
            System.out.println("creating admin user  => " + userClient.addUser(admin));
        }
    }

    private void createSpringFramework() throws Exception {
        javaApi.getLinksFromApiRoot();

        URI vendorUri = javaApi.createVendor("Pivotal Software, Inc.", "Pivotal", new URL("https://pivotal.io/"));

        Path dir = Paths.get(springFrameworkDist + "/libs");
        DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.jar");
        for (Path path : stream) {
            addComponent(path.getFileName().toString(), vendorUri);
        }

        URI projectURI = javaApi.createProject(
                "Spring Framework",
                "The Spring Framework provides a comprehensive programming"
                        + " and configuration model for modern Java-based enterprise applications.",
                "4.3.5.RELEASE",
                releaseURIs);
    }

    private void addComponent(String jarFile, URI vendorUri) throws Exception {
        if (jarFile.contains("javadoc") || jarFile.contains("sources")) {
            return;
        }
        int indexOfFirstDigit = 0;
        while (indexOfFirstDigit < jarFile.length() && !Character.isDigit(jarFile.charAt(indexOfFirstDigit)))
            indexOfFirstDigit++;
        String componentName = jarFile.substring(0, indexOfFirstDigit - 1);
        String componentVersion = jarFile.substring(indexOfFirstDigit, jarFile.length() - 4);

        URI componentURI = javaApi.createComponent(componentName, vendorUri);
        URI releaseURI = javaApi.createRelease(componentName, componentVersion, componentURI);
        releaseURIs.add(releaseURI);
    }

    public static void main(String[] args) throws Exception {
        String restServerURL = getEnv("SW360DEMO_REST_SERVER_URL", "http://localhost:8091");
        String authServerURL = getEnv("SW360DEMO_AUTH_SERVER_URL", "http://localhost:8090");
        String thriftServerURL = getEnv("SW360DEMO_THRIFT_SERVER_URL", "http://localhost:8080");
        String springDistLocation = getEnv("SW360DEMO_SPRING_DIST_LOCATION",
                System.getProperty("user.home") + "/spring-framework-4.3.5.RELEASE");

        DemoApplication demoClient = new DemoApplication(restServerURL, authServerURL, thriftServerURL, springDistLocation);
        try {
            demoClient.checkAndCreateAdminUser();
            demoClient.createSpringFramework();
        } catch (Exception e) {
            System.out.println("Something went wrong:");
            if (e instanceof HttpServerErrorException) {
                ObjectMapper mapper = new ObjectMapper();
                String errorJson = ((HttpServerErrorException) e).getResponseBodyAsString();
                Object jsonErrorObject = mapper.readValue(errorJson, Object.class);
                String errorPrettyPrinted = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonErrorObject);

                System.out.println(errorPrettyPrinted);
            } else {
                System.out.println(e.getMessage());
            }
        }
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if(value != null ) {
            return value;
        }
        return defaultValue;
    }

}
