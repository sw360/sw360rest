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
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProjectController implements ResourceProcessor<RepositoryLinksResource> {
    protected final EntityLinks entityLinks;
    private final String PROJECTS_URL = "/projects";

    @Value("${sw360.thrift-server-url}")
    private String thriftServerUrl;

    @NonNull
    private Sw360ProjectService projectService;

    // @PreAuthorize("hasRole('ROLE_SW360_USER')")
    @RequestMapping(value = PROJECTS_URL, method = RequestMethod.GET)
    public ResponseEntity<Resources<Resource>> getProjectsForUser(OAuth2Authentication oAuth2Authentication) {
        try {
            String userId = (String) oAuth2Authentication.getPrincipal();
            List<Project> projects = projectService.getProjectsForUser(userId);
            List<Resource> projectResources = new ArrayList<>();
            for (Project project : projects) {
                HalResource projectResource = createHalProjectResource(project);
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
    @ResponseBody
    public ResponseEntity<Resource> getProject(
            @PathVariable("id") String id, OAuth2Authentication oAuth2Authentication) {
        try {
            String userId = (String) oAuth2Authentication.getPrincipal();
            Project sw360Project = projectService.getProjectForUserById(id, userId);
            HalResource userHalResource = createHalProjectResource(sw360Project);
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

    private HalResource createHalProjectResource(Project sw360Project) {
        ProjectResource projectResource = new ProjectResource();

        projectResource.setType(sw360Project.getType());
        projectResource.setProjectType(String.valueOf(sw360Project.getProjectType()));
        projectResource.setName(sw360Project.getName());
        projectResource.setDescription(sw360Project.getDescription());
        projectResource.setCreatedBy(sw360Project.getCreatedBy());
        projectResource.setCreatedOn(sw360Project.getCreatedOn());

        String projectUUID = sw360Project.getRevision();
        Link selfLink = linkTo(ProjectController.class).slash("api" + PROJECTS_URL + "/" + projectUUID).withSelfRel();
        projectResource.add(selfLink);
        return new HalResource(projectResource);
    }
}
