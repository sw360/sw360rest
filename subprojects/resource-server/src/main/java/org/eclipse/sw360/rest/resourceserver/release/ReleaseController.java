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
import org.eclipse.sw360.rest.resourceserver.component.ComponentController;
import org.eclipse.sw360.rest.resourceserver.core.HalHelper;
import org.eclipse.sw360.rest.resourceserver.core.HalResourceWidthEmbeddedItems;
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
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReleaseController implements ResourceProcessor<RepositoryLinksResource> {
    private final String RELEASES_URL = "/releases";

    @Value("${sw360.thrift-server-url}")
    private String thriftServerUrl;

    @NonNull
    private Sw360ReleaseService releaseService;

    @NonNull
    private final HalHelper halHelper;

    // @PreAuthorize("hasRole('ROLE_SW360_USER')")
    @RequestMapping(value = RELEASES_URL, method = RequestMethod.GET)
    public ResponseEntity<Resources<Resource>> getReleasesForUser(OAuth2Authentication oAuth2Authentication) {
        try {
            String userId = (String) oAuth2Authentication.getPrincipal();
            List<Release> releases = releaseService.getReleasesForUser(userId);
            List<Resource> releaseResources = new ArrayList<>();
            for (Release release : releases) {
                HalResourceWidthEmbeddedItems releaseResource = createHalReleaseResource(release);
                releaseResources.add(releaseResource);
            }
            Resources<Resource> resources = new Resources<>(releaseResources);

            return new ResponseEntity<>(resources, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @RequestMapping(RELEASES_URL + "/{id}")
    @ResponseBody
    public ResponseEntity<Resource> getRelease(
            @PathVariable("id") String id, OAuth2Authentication oAuth2Authentication) {
        try {
            String userId = (String) oAuth2Authentication.getPrincipal();
            Release sw360Release = releaseService.getReleaseForUserById(id, userId);
            HalResourceWidthEmbeddedItems userHalResource = createHalReleaseResource(sw360Release);
            return new ResponseEntity<>(userHalResource, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ReleaseController.class).slash("api" + RELEASES_URL).withRel("releases"));
        return resource;
    }

    private HalResourceWidthEmbeddedItems createHalReleaseResource(Release sw360Release) {
        ReleaseResource releaseResource = new ReleaseResource();

        releaseResource.setType(sw360Release.getType());
        releaseResource.setName(sw360Release.getName());
        releaseResource.setCpeid(sw360Release.getCpeid());
        releaseResource.setVersion(sw360Release.getVersion());
        releaseResource.setReleaseDate(sw360Release.getReleaseDate());

        Link selfLink = linkTo(ReleaseController.class)
                .slash("api" + RELEASES_URL + "/" + sw360Release.getId()).withSelfRel();
        releaseResource.add(selfLink);

        Link componentLink = linkTo(ReleaseController.class)
                .slash("api" + ComponentController.COMPONENTS_URL + "/" + sw360Release.getComponentId()).withRel("component");
        releaseResource.add(componentLink);

        HalResourceWidthEmbeddedItems halReleaseResource = new HalResourceWidthEmbeddedItems(releaseResource);
        if (sw360Release.getModerators() != null) {
            Set<String> moderators = sw360Release.getModerators();
            halHelper.addEmbeddedModerators(halReleaseResource, moderators);
        }

        return halReleaseResource;
    }
}
