/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.authserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AccessTokenPrinter {
    public static Properties getPropertiesFromApplicationYml() {
        YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        yamlPropertiesFactoryBean.setResources(new ClassPathResource("application.yml"));
        return yamlPropertiesFactoryBean.getObject();
    }

    public static void main(String[] args) throws IOException {
        AccessTokenPrinter accessTokenPrinter = new AccessTokenPrinter();
        accessTokenPrinter.printAccessToken();
    }

    private void printAccessToken() throws IOException {
        Properties properties = getPropertiesFromApplicationYml();
        String testUserId = properties.get("sw360.test-user-id").toString();
        String testUserPassword = properties.get("sw360.test-user-password").toString();

        String url = "http://localhost:8090/oauth/token?grant_type=password&username=" + testUserId + "&password=" + testUserPassword;

        ResponseEntity<String> responseEntity =  new TestRestTemplate(
                "trusted-sw360-client",
                "sw360-secret")
                .postForEntity(url,
                        null,
                        String.class);

        String responseBody = responseEntity.getBody();
        JsonNode responseBodyJsonNode = new ObjectMapper().readTree(responseBody);
        assertThat(responseBodyJsonNode.has("access_token"), is(true));

        String accessToken = responseBodyJsonNode.get("access_token").asText();
        System.out.println("-----------------------------------------");
        System.out.println("Authorization: Bearer " + accessToken);
    }
}
