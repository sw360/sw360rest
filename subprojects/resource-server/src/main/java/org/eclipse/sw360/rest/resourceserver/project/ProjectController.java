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
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProjectController implements ResourceProcessor<RepositoryLinksResource> {
    private final String PROJECTS_URL = "/projects";

    @NonNull
    private Sw360ProjectService projectService;

    @NonNull
    private Sw360ReleaseService releaseService;

    @NonNull
    private Sw360UserService userService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @RequestMapping(value = PROJECTS_URL, method = RequestMethod.GET)
    public ResponseEntity<Resources<Resource<ProjectResource>>> getProjectsForUser(OAuth2Authentication oAuth2Authentication) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication(oAuth2Authentication);
        List<Project> projects = projectService.getProjectsForUser(sw360User);
        List<Resource<ProjectResource>> projectResources = new ArrayList<>();
        for (Project sw360Project : projects) {
            User sw360ProjectUser = userService.getUserByEmail(sw360Project.getCreatedBy());
            HalResource<ProjectResource> projectResource = createHalProjectResource(sw360Project, sw360ProjectUser, false);
            projectResources.add(projectResource);
        }
        Resources<Resource<ProjectResource>> resources = new Resources<>(projectResources);

        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @RequestMapping(PROJECTS_URL + "/{id}")
    public ResponseEntity<Resource<ProjectResource>> getProject(
            @PathVariable("id") String id, OAuth2Authentication oAuth2Authentication) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication(oAuth2Authentication);
        Project sw360Project = projectService.getProjectForUserById(id, sw360User);
        HalResource<ProjectResource> userHalResource = createHalProjectResource(sw360Project, sw360User, true);
        return new ResponseEntity<>(userHalResource, HttpStatus.OK);
    }

    @RequestMapping(value = PROJECTS_URL, method = RequestMethod.POST)
    public ResponseEntity createProject(
            OAuth2Authentication oAuth2Authentication,
            @RequestBody ProjectResource projectResource) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication(oAuth2Authentication);
        Project sw360Project = createProjectFromResource(projectResource);
        sw360Project = projectService.createProject(sw360Project, sw360User);
        HalResource<ProjectResource> halResource = createHalProjectResource(sw360Project, sw360User, true);
        return new ResponseEntity<>(halResource, HttpStatus.CREATED);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ProjectController.class).slash("api" + PROJECTS_URL).withRel("projects"));
        return resource;
    }

    private Project createProjectFromResource(ProjectResource projectResource) {
        Project project = new Project();

        project.setName(projectResource.getName());
        project.setDescription(projectResource.getDescription());
        project.setProjectType(ProjectType.valueOf(projectResource.getProjectType()));

        return project;
    }

    private HalResource<ProjectResource> createHalProjectResource(Project sw360Project, User sw360User, boolean verbose) {
        ProjectResource projectResource = new ProjectResource();

        projectResource.setProjectType(String.valueOf(sw360Project.getProjectType()));
        projectResource.setName(sw360Project.getName());

        HalResource<ProjectResource> halProjectResource = new HalResource<>(projectResource);

        String projectUUID = sw360Project.getId();
        Link selfLink = linkTo(ProjectController.class).slash("api" + PROJECTS_URL + "/" + projectUUID).withSelfRel();
        halProjectResource.add(selfLink);

        if (verbose) {
            projectResource.setCreatedOn(sw360Project.getCreatedOn());
            restControllerHelper.addEmbeddedUser(halProjectResource, sw360User, "createdBy");
            projectResource.setType(sw360Project.getType());
            projectResource.setDescription(sw360Project.getDescription());
            Set<String> releaseIds = new HashSet<>();
            if (sw360Project.getReleaseIdToUsage() != null) {
                Map<String, String> releaseIdToUsage = sw360Project.getReleaseIdToUsage();
                for(String releaseId: releaseIdToUsage.keySet()) {
                    // TODO kai Toedter 2016-12-29: Is there a constant for "contained"
                    if(releaseIdToUsage.get(releaseId).equals("contained")) {
                        releaseIds.add(releaseId);
                    }
                }
                restControllerHelper.addEmbeddedReleases(halProjectResource, releaseIds, releaseService, sw360User);
            }
            if (sw360Project.getModerators() != null) {
                Set<String> moderators = sw360Project.getModerators();
                restControllerHelper.addEmbeddedModerators(halProjectResource, moderators);
            }
        }

        return halProjectResource;
    }
}
