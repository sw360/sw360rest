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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AccessTokenPrinter {
    public static void main(String[] args) throws IOException {
        AccessTokenPrinter accessTokenPrinter = new AccessTokenPrinter();
        accessTokenPrinter.printAccessToken();
    }

    private void printAccessToken() throws IOException {
        String url = "http://localhost:8090/oauth/token?grant_type=password&username=admin@sw360.com&password=sw360-admin-password";
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
