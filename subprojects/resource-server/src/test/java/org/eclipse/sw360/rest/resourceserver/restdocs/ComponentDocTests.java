/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.restdocs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.*;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class ComponentDocTests {

    @Autowired
    WebApplicationContext context;

    @Autowired
    FilterChainProxy springSecurityFilterChain;

    @Autowired
    private ObjectMapper objectMapper;

    @Rule
    public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/generated-snippets");

    private RestDocumentationResultHandler documentationHandler;

    MockMvc mockMvc;

    @MockBean
    private Sw360UserService userServiceMock;

    @MockBean
    private Sw360ComponentService componentServiceMock;

    @Before
    public void before() {
        List<Component> componentList = new ArrayList<>();
        Component component = new Component();
        component.setName("Component name");
        component.setDescription("Component description");
        componentList.add(component);

        given(this.componentServiceMock.getComponentsForUser(anyObject())).willReturn(componentList);

        User user = new User();
        user.setId("admin@sw360.com");
        user.setEmail("admin@sw360.com");
        user.setFullname("John Doe");

        given(this.userServiceMock.getUserById("admin@sw360.com")).willReturn(user);

        this.documentationHandler = document("{method-name}",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()));

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .addFilter(springSecurityFilterChain)
                .apply(documentationConfiguration(this.restDocumentation))
                .alwaysDo(this.documentationHandler)
                .build();
    }

    @Test
    public void should_document_components() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, "admin@sw360.com", "sw360-password");
        mockMvc.perform(get("/api/components")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
//                                linkWithRel("self").description("The <<resources-users,Users resource>>"),
//                                linkWithRel("profile").description("The profile describes the data structure of this resource"),
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.sw360:components").description("An array of <<resources-component, Component resources>>"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_component_creation() throws Exception {
        Map<String, String> user = new HashMap<>();
        user.put("type", "component");
        user.put("componentType", "OSS");
        user.put("name", "Test Component");

        String accessToken = TestHelper.getAccessToken(mockMvc, "admin@sw360.com", "sw360-password");
        this.mockMvc.perform(
                post("/api/components").contentType(MediaTypes.HAL_JSON)
                        .content(this.objectMapper.writeValueAsString(user))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andDo(this.documentationHandler.document(
                        requestFields(
                                fieldWithPath("type").description("The type of the component. , possible values are: " + Arrays.asList(ComponentType.values())),
                                fieldWithPath("componentType").description("The full name of the component"),
                                fieldWithPath("name").description("The name of the component. Must be unique."))));
    }
}
