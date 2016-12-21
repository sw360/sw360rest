/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.project;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.HalHelper;
import org.eclipse.sw360.rest.resourceserver.core.HalResourceWidthEmbeddedItems;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProjectController implements ResourceProcessor<RepositoryLinksResource> {
    private final String PROJECTS_URL = "/projects";

    @Value("${sw360.thrift-server-url}")
    private String thriftServerUrl;

    @NonNull
    private Sw360ProjectService projectService;

    @NonNull
    private Sw360UserService userService;

    @NonNull
    private final HalHelper halHelper;

    // @PreAuthorize("hasRole('ROLE_SW360_USER')")
    @RequestMapping(value = PROJECTS_URL, method = RequestMethod.GET)
    public ResponseEntity<Resources<Resource>> getProjectsForUser(OAuth2Authentication oAuth2Authentication) {
        try {
            String userId = (String) oAuth2Authentication.getPrincipal();
            List<Project> projects = projectService.getProjectsForUser(userId);
            List<Resource> projectResources = new ArrayList<>();
            for (Project sw360Project : projects) {
                User sw360User = userService.getUserByEmail(sw360Project.getCreatedBy());
                HalResourceWidthEmbeddedItems projectResource = createHalProjectResource(sw360Project, sw360User, false);
                projectResources.add(projectResource);
            }
            Resources<Resource> resources = new Resources<>(projectResources);

            return new ResponseEntity<>(resources, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @RequestMapping(PROJECTS_URL + "/{id}")
    public ResponseEntity<Resource> getProject(
            @PathVariable("id") String id, OAuth2Authentication oAuth2Authentication) {
        try {
            String userId = (String) oAuth2Authentication.getPrincipal();
            User sw360User = userService.getUserByEmail(userId);
            Project sw360Project = projectService.getProjectForUserById(id, userId);
            HalResourceWidthEmbeddedItems userHalResource = createHalProjectResource(sw360Project, sw360User, true);
            return new ResponseEntity<>(userHalResource, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ProjectController.class).slash("api" + PROJECTS_URL).withRel("projects"));
        return resource;
    }

    private HalResourceWidthEmbeddedItems createHalProjectResource(Project sw360Project, User sw360User, boolean verbose) {
        ProjectResource projectResource = new ProjectResource();

        projectResource.setProjectType(String.valueOf(sw360Project.getProjectType()));
        projectResource.setName(sw360Project.getName());

        String projectUUID = sw360Project.getId();
        Link selfLink = linkTo(ProjectController.class).slash("api" + PROJECTS_URL + "/" + projectUUID).withSelfRel();
        projectResource.add(selfLink);

        HalResourceWidthEmbeddedItems halProjectResource = new HalResourceWidthEmbeddedItems(projectResource);
        if (verbose) {
            projectResource.setCreatedOn(sw360Project.getCreatedOn());
            halHelper.addEmbeddedUser(halProjectResource, sw360User, "createdBy");
            projectResource.setType(sw360Project.getType());
            projectResource.setDescription(sw360Project.getDescription());
            if (sw360Project.getModerators() != null) {
                Set<String> moderators = sw360Project.getModerators();
                halHelper.addEmbeddedModerators(halProjectResource, moderators);
            }
        }

        return halProjectResource;
    }
}
