/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.restdocs;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class UserTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    FilterChainProxy springSecurityFilterChain;

    MockMvc mockMvc;

    @MockBean
    private Sw360UserService userServiceMock;

    @Before
    public void before() {
        List<User> userList = new ArrayList<>();

        User user = new User();
        user.setId("admin@sw360.com");
        user.setEmail("admin@sw360.com");
        user.setFullname("John Doe");
        userList.add(user);

        user = new User();
        user.setId("jane@sw360.com");
        user.setEmail("jane@sw360.com");
        user.setFullname("Jane Doe");
        userList.add(user);

        given(this.userServiceMock.getAllUsers()).willReturn(userList);

        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilter(springSecurityFilterChain) .build();
    }

    @Test
    public void should_get_all_users() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, "admin@sw360.com", "sw360-password");
        MockHttpServletResponse response = mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        TestHelper.checkResponse(response.getContentAsString(), "users", 2);
    }
}
