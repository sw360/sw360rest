/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.restdocs;

import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;

import java.util.*;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ComponentSpec extends RestDocsSpecBase {

    @MockBean
    private Sw360UserService userServiceMock;

    @MockBean
    private Sw360ComponentService componentServiceMock;

    @Before
    public void before() {
        List<Component> componentList = new ArrayList<>();
        Component component = new Component();
        component.setId("17653524");
        component.setName("Angular");
        component.setDescription("Angular is a development platform for building mobile and desktop web applications.");
        component.setCreatedOn("2016-12-15");
        component.setCreatedBy("admin@sw360.org");
        component.setComponentType(ComponentType.OSS);
        component.setVendorNames(new HashSet<>(Arrays.asList("Google")));
        component.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "john@sw360.org")));

        componentList.add(component);

        given(this.componentServiceMock.getComponentsForUser(anyObject())).willReturn(componentList);
        given(this.componentServiceMock.getComponentForUserById(eq("17653524"), anyObject())).willReturn(component);

        User user = new User();
        user.setId("admin@sw360.org");
        user.setEmail("admin@sw360.org");
        user.setFullname("John Doe");

        given(this.userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(user);
    }

    @Test
    public void should_document_get_components() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, "admin@sw360.org", "sw360-password");
        mockMvc.perform(get("/api/components")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.sw360:components").description("An array of <<resources-components, Components resources>>"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_component() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, "admin@sw360.org", "sw360-password");
        mockMvc.perform(get("/api/components/17653524")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("self").description("The <<resources-components,Component resource>>")
                        ),
                        responseFields(
                                fieldWithPath("name").description("The name of the component"),
                                fieldWithPath("description").description("The component description"),
                                fieldWithPath("createdBy").description("The user who created this component"),
                                fieldWithPath("createdOn").description("The date the component was created"),
                                fieldWithPath("type").description("is always 'component'"),
                                fieldWithPath("componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                fieldWithPath("vendorNames").description("All vendors of this component"),
                                fieldWithPath("moderators").description("All moderators of this component"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_create_component() throws Exception {
        Map<String, String> component = new HashMap<>();
        component.put("type", "component");
        component.put("componentType", "OSS");
        component.put("name", "Test Component");

        String accessToken = TestHelper.getAccessToken(mockMvc, "admin@sw360.org", "sw360-password");
        this.mockMvc.perform(
                post("/api/components")
                        .contentType(MediaTypes.HAL_JSON)
                        .content(this.objectMapper.writeValueAsString(component))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andDo(this.documentationHandler.document(
                        requestFields(
                                fieldWithPath("type").description("is always 'component'."),
                                fieldWithPath("componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                fieldWithPath("name").description("The name of the component"))));
    }
}
