/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Component.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.core;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.rest.resourceserver.release.ReleaseController;
import org.eclipse.sw360.rest.resourceserver.release.ReleaseResource;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.UserController;
import org.eclipse.sw360.rest.resourceserver.user.UserResource;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.Set;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@Slf4j
@Service
public class HalHelper {
    public void addEmbeddedModerators(HalResourceWidthEmbeddedItems halResource, Set<String> moderators) {
        for (String moderatorEmail : moderators) {
            UserResource userResource = new UserResource();
            userResource.setEmail(moderatorEmail);
            try {
                String userUUID = Base64.getEncoder().encodeToString(moderatorEmail.getBytes("utf-8"));
                Link moderatorSelfLink = linkTo(UserController.class).slash("api/users/" + userUUID).withSelfRel();
                userResource.add(moderatorSelfLink);
            } catch (Exception e) {
                log.error("cannot create self link for moderator with email: " + moderatorEmail);
            }

            halResource.addEmbeddedItem("moderators", userResource);
        }
    }

    public void addEmbeddedReleases(
            HalResourceWidthEmbeddedItems halResource,
            Set<String> releases,
            Sw360ReleaseService sw360ReleaseService,
            String userId) {

        for (String releaseId : releases) {
            ReleaseResource releaseResource = new ReleaseResource();
            try {
                final Release release = sw360ReleaseService.getReleaseForUserById(releaseId, userId);
                releaseResource.setVersion(release.getVersion());
                Link releaseLink = linkTo(ReleaseController.class).slash("api/releases/" + releaseId).withSelfRel();
                releaseResource.add(releaseLink);
            } catch (Exception e) {
                log.error("cannot create embedded release with id: " + releaseId);
            }

            halResource.addEmbeddedItem("releases", releaseResource);
        }
    }

    public void addEmbeddedReleases(
            HalResourceWidthEmbeddedItems halResource,
            List<Release> releases) {
        for (Release release : releases) {
            ReleaseResource releaseResource = new ReleaseResource();
            try {
                releaseResource.setVersion(release.getVersion());
                Link releaseLink = linkTo(ReleaseController.class).slash("api/releases/" + release.getId()).withSelfRel();
                releaseResource.add(releaseLink);
            } catch (Exception e) {
                log.error("cannot create embedded release with id: " + release.getId());
            }

            halResource.addEmbeddedItem("releases", releaseResource);
        }
    }
}
