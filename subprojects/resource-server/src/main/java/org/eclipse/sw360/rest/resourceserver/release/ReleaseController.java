/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Release.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.release;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
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

import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReleaseController implements ResourceProcessor<RepositoryLinksResource> {
    public static final String RELEASES_URL = "/releases";

    @NonNull
    private Sw360ReleaseService releaseService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @RequestMapping(value = RELEASES_URL)
    public ResponseEntity<Resources<Resource>> getReleasesForUser(OAuth2Authentication oAuth2Authentication) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication(oAuth2Authentication);
        List<Release> releases = releaseService.getReleasesForUser(sw360User);
        List<Resource> releaseResources = new ArrayList<>();

        for (Release release : releases) {
            release.setComponentId(null);
            Resource<Release> releaseResource = new Resource(release);
            releaseResources.add(releaseResource);
        }
        Resources<Resource> resources = new Resources<>(releaseResources);

        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @RequestMapping(RELEASES_URL + "/{id}")
    public ResponseEntity<Resource> getRelease(
            @PathVariable("id") String id, OAuth2Authentication oAuth2Authentication) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication(oAuth2Authentication);
        Release sw360Release = releaseService.getReleaseForUserById(id, sw360User);
        HalResource halRelease = restControllerHelper.createHalReleaseResource(sw360Release, true);
        return new ResponseEntity<>(halRelease, HttpStatus.OK);
    }

    @RequestMapping(value = RELEASES_URL, method = RequestMethod.POST)
    public ResponseEntity<Resource<Release>> createRelease(
            OAuth2Authentication oAuth2Authentication,
            @RequestBody Release releaseResource) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication(oAuth2Authentication);
        Release sw360Release = releaseService.createRelease(releaseResource, sw360User);
        HalResource<Release> halResource = restControllerHelper.createHalReleaseResource(sw360Release, true);
        return new ResponseEntity<>(halResource, HttpStatus.CREATED);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ReleaseController.class).slash("api" + RELEASES_URL).withRel("releases"));
        return resource;
    }
}
