/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Component.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.component;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.HalHelper;
import org.eclipse.sw360.rest.resourceserver.core.HalResourceWidthEmbeddedItems;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ComponentController implements ResourceProcessor<RepositoryLinksResource> {
    public static final String COMPONENTS_URL = "/components";

    @NonNull
    private final Sw360ComponentService componentService;

    @NonNull
    private final Sw360ReleaseService releaseService;

    @NonNull
    private final Sw360UserService userService;

    @NonNull
    private final HalHelper halHelper;

    @Value("${sw360.thrift-server-url}")
    private String thriftServerUrl;

    protected final EntityLinks entityLinks;


    // @PreAuthorize("hasRole('ROLE_SW360_USER')")
    @RequestMapping(value = COMPONENTS_URL)
    public ResponseEntity<Resources<Resource<ComponentResource>>> getComponents(OAuth2Authentication oAuth2Authentication) {
        try {
            String userId = (String) oAuth2Authentication.getPrincipal();
            List<Component> components = componentService.getComponentsForUser(userId);

            List<Resource<ComponentResource>> componentResources = new ArrayList<>();
            for (Component component : components) {
                HalResourceWidthEmbeddedItems<ComponentResource> componentResource = createHalComponentResource(component, userId, false);
                componentResources.add(componentResource);
            }
            Resources<Resource<ComponentResource>> resources = new Resources<>(componentResources);

            return new ResponseEntity<>(resources, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @RequestMapping(COMPONENTS_URL + "/{id}")
    public ResponseEntity<Resource<ComponentResource>> getComponent(
            @PathVariable("id") String id, OAuth2Authentication oAuth2Authentication) {
        try {
            String userId = (String) oAuth2Authentication.getPrincipal();
            Component sw360Component = componentService.getComponentForUserById(id, userId);
            HalResourceWidthEmbeddedItems<ComponentResource> userHalResource = createHalComponentResource(sw360Component, userId, true);
            return new ResponseEntity<>(userHalResource, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @RequestMapping(value = COMPONENTS_URL, method = RequestMethod.POST)
    public ResponseEntity<Resource<ComponentResource>> createComponent(
            OAuth2Authentication oAuth2Authentication,
            @RequestBody ComponentResource componentResource) {

        try {
            String userId = (String) oAuth2Authentication.getPrincipal();
            Component sw360Component = createComponentFromResource(componentResource);
            sw360Component = componentService.createComponent(sw360Component, userId);
            HalResourceWidthEmbeddedItems<ComponentResource> halResource = createHalComponentResource(sw360Component, userId, true);
            return new ResponseEntity<>(halResource, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ComponentController.class).slash("api/components").withRel("components"));
        return resource;
    }

    private HalResourceWidthEmbeddedItems<ComponentResource> createHalComponentResource(Component sw360Component, String userId, boolean verbose) {
        ComponentResource componentResource = new ComponentResource();

        componentResource.setComponentType(String.valueOf(sw360Component.getComponentType()));
        componentResource.setName(sw360Component.getName());
        componentResource.setCreatedOn(sw360Component.getCreatedOn());
        componentResource.setVendorNames(sw360Component.getVendorNames());

        String componentUUID = sw360Component.getId();
        Link selfLink = linkTo(ComponentController.class).slash("api/components/" + componentUUID).withSelfRel();
        componentResource.add(selfLink);


        HalResourceWidthEmbeddedItems<ComponentResource> halComponentResource = new HalResourceWidthEmbeddedItems<>(componentResource);

        if (verbose) {
            componentResource.setType(sw360Component.getType());
            componentResource.setDescription(sw360Component.getDescription());

            if (sw360Component.getReleaseIds() != null) {
                Set<String> releases = sw360Component.getReleaseIds();
                halHelper.addEmbeddedReleases(halComponentResource, releases, releaseService, userId);
            }

            if (sw360Component.getReleases() != null) {
                List<Release> releases = sw360Component.getReleases();
                halHelper.addEmbeddedReleases(halComponentResource, releases);
            }

            if (sw360Component.getModerators() != null) {
                Set<String> moderators = sw360Component.getModerators();
                halHelper.addEmbeddedModerators(halComponentResource, moderators);
            }

            User sw360User = userService.getUserByEmail(userId);
            halHelper.addEmbeddedUser(halComponentResource, sw360User, "createdBy");
        }
        return halComponentResource;
    }

    private Component createComponentFromResource(ComponentResource componentResource) {
        Component component = new Component();

        component.setType(componentResource.getType());
        component.setComponentType(ComponentType.valueOf(componentResource.getComponentType()));
        component.setName(componentResource.getName());
        component.setDescription(componentResource.getDescription());
        component.setCreatedOn(componentResource.getCreatedOn());

        return component;
    }

}
