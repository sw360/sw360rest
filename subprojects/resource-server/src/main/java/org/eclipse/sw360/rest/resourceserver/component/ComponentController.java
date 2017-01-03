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
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
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
    private final RestControllerHelper restControllerHelper;

    protected final EntityLinks entityLinks;


    @RequestMapping(value = COMPONENTS_URL)
    public ResponseEntity<Resources<Resource<ComponentResource>>> getComponents(OAuth2Authentication oAuth2Authentication) {
        User user = restControllerHelper.getSw360UserFromAuthentication(oAuth2Authentication);
        List<Component> components = componentService.getComponentsForUser(user);

        List<Resource<ComponentResource>> componentResources = new ArrayList<>();
        for (Component component : components) {
            HalResource<ComponentResource> componentResource = createHalComponentResource(component, user, false);
            componentResources.add(componentResource);
        }
        Resources<Resource<ComponentResource>> resources = new Resources<>(componentResources);

        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @RequestMapping(COMPONENTS_URL + "/{id}")
    public ResponseEntity<Resource<ComponentResource>> getComponent(
            @PathVariable("id") String id, OAuth2Authentication oAuth2Authentication) {
        User user = restControllerHelper.getSw360UserFromAuthentication(oAuth2Authentication);
        Component sw360Component = componentService.getComponentForUserById(id, user);
        HalResource<ComponentResource> userHalResource = createHalComponentResource(sw360Component, user, true);
        return new ResponseEntity<>(userHalResource, HttpStatus.OK);
    }

    @RequestMapping(value = COMPONENTS_URL, method = RequestMethod.POST)
    public ResponseEntity<Resource<ComponentResource>> createComponent(
            OAuth2Authentication oAuth2Authentication,
            @RequestBody ComponentResource componentResource) throws URISyntaxException {

        User user = restControllerHelper.getSw360UserFromAuthentication(oAuth2Authentication);
        Component sw360Component = createComponentFromResource(componentResource);
        sw360Component = componentService.createComponent(sw360Component, user);
        HalResource<ComponentResource> halResource = createHalComponentResource(sw360Component, user, true);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(sw360Component.getId()).toUri();

        return ResponseEntity.created(location).body(halResource);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ComponentController.class).slash("api/components").withRel("components"));
        return resource;
    }

    private HalResource<ComponentResource> createHalComponentResource(Component sw360Component, User user, boolean verbose) {
        ComponentResource componentResource = new ComponentResource();

        componentResource.setComponentType(String.valueOf(sw360Component.getComponentType()));
        componentResource.setName(sw360Component.getName());
        componentResource.setCreatedOn(sw360Component.getCreatedOn());
        componentResource.setVendorNames(sw360Component.getVendorNames());

        HalResource<ComponentResource> halComponentResource = new HalResource<>(componentResource);
        String componentUUID = sw360Component.getId();
        Link selfLink = linkTo(ComponentController.class).slash("api/components/" + componentUUID).withSelfRel();
        halComponentResource.add(selfLink);

        if (verbose) {
            componentResource.setType(sw360Component.getType());
            componentResource.setDescription(sw360Component.getDescription());

            if (sw360Component.getReleaseIds() != null) {
                Set<String> releases = sw360Component.getReleaseIds();
                restControllerHelper.addEmbeddedReleases(halComponentResource, releases, releaseService, user);
            }

            if (sw360Component.getReleases() != null) {
                List<Release> releases = sw360Component.getReleases();
                restControllerHelper.addEmbeddedReleases(halComponentResource, releases);
            }

            if (sw360Component.getModerators() != null) {
                Set<String> moderators = sw360Component.getModerators();
                restControllerHelper.addEmbeddedModerators(halComponentResource, moderators);
            }

            restControllerHelper.addEmbeddedUser(halComponentResource, user, "createdBy");
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
