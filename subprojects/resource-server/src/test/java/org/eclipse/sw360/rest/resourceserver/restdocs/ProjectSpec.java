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
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.*;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class ProjectSpec {

    @Autowired
    WebApplicationContext context;

    @Autowired
    FilterChainProxy springSecurityFilterChain;

    @Autowired
    private ObjectMapper objectMapper;

    MockMvc mockMvc;

    @Rule
    public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/generated-snippets");

    private RestDocumentationResultHandler documentationHandler;

    @MockBean
    private Sw360UserService userServiceMock;

    @MockBean
    private Sw360ProjectService projectServiceMock;

    private Project project;

    @Before
    public void before() {
        List<Project> projectList = new ArrayList<>();
        project = new Project();
        project.setId("376576");
        project.setName("Emerald Web");
        project.setType("project");
        project.setProjectType(ProjectType.PRODUCT);
        project.setDescription("Emerald Web provides a suite of components for Critical Infrastructures.");
        project.setCreatedOn("2016-12-15");
        project.setCreatedBy("kai.toedter@siemens.com");
        project.setModerators(new HashSet<>(Arrays.asList("kai.toedter@siemens.com", "michael.c.jaeger@siemens.com")));
        projectList.add(project);

        given(this.projectServiceMock.getProjectsForUser(anyObject())).willReturn(projectList);
        given(this.projectServiceMock.getProjectForUserById(eq("376576"), anyString())).willReturn(project);

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
    public void should_document_get_projects()  throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, "admin@sw360.com", "sw360-password");
        mockMvc.perform(get("/api/projects")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.sw360:projects").description("An array of <<resources-project, Project resources>>"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_project() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, "admin@sw360.com", "sw360-password");
        mockMvc.perform(get("/api/projects/376576")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("self").description("The <<resources-project,Component resource>>")
                        ),
                        responseFields(
                                fieldWithPath("name").description("The name of the project"),
                                fieldWithPath("description").description("The project description"),
                                fieldWithPath("createdBy").description("The user who created this project"),
                                fieldWithPath("createdOn").description("The date the project was created"),
                                fieldWithPath("type").description("is always 'project'."),
                                fieldWithPath("projectType").description("The project type, possible values are: " + Arrays.asList(ProjectType.values())),
                                fieldWithPath("moderators").description("All moderators of this project"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }
}
