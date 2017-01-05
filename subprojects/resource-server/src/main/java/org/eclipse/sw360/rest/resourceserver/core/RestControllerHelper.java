/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Component.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.core;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.attachment.AttachmentController;
import org.eclipse.sw360.rest.resourceserver.component.ComponentController;
import org.eclipse.sw360.rest.resourceserver.release.ReleaseController;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.eclipse.sw360.rest.resourceserver.user.UserController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.Set;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RestControllerHelper {
    @NonNull
    private Sw360UserService userService;

    public User getSw360UserFromAuthentication(OAuth2Authentication oAuth2Authentication) {
        String userId = oAuth2Authentication.getName();
        return userService.getUserByEmail(userId);
    }

    public void addEmbeddedModerators(HalResource halResource, Set<String> moderators) {
        for (String moderatorEmail : moderators) {
            User userResource = new User();
            HalResource<User> halUser = new HalResource<>(userResource);
            userResource.setEmail(moderatorEmail);
            try {
                String userUUID = Base64.getEncoder().encodeToString(moderatorEmail.getBytes("utf-8"));
                Link moderatorSelfLink = linkTo(UserController.class).slash("api/users/" + userUUID).withSelfRel();
                halUser.add(moderatorSelfLink);
            } catch (Exception e) {
                log.error("cannot create self link for moderator with email: " + moderatorEmail);
            }

            halResource.addEmbeddedResource("moderators", halUser);
        }
    }

    public void addEmbeddedReleases(
            HalResource halResource,
            Set<String> releases,
            Sw360ReleaseService sw360ReleaseService,
            User user,
            String linkRelation) {
        for (String releaseId : releases) {
            final Release release = sw360ReleaseService.getReleaseForUserById(releaseId, user);
            addEmbeddedRelease(halResource, release, linkRelation);
        }
    }

    public void addEmbeddedReleases(
            HalResource halResource,
            List<Release> releases) {
        for (Release release : releases) {
            addEmbeddedRelease(halResource, release, "releases");
        }
    }

    public void addEmbeddedUser(HalResource halResource, User user, String relation) {
        User embeddedUser = new User();
        Resource<User> embeddedUserResource = new Resource<>(embeddedUser);
        try {
            embeddedUser.setEmail(user.getEmail());
            embeddedUser.setType(null);
            String userUUID = Base64.getEncoder().encodeToString(user.getEmail().getBytes("utf-8"));
            Link userLink = linkTo(UserController.class).slash("api/users/" + userUUID).withSelfRel();
            embeddedUserResource.add(userLink);
        } catch (Exception e) {
            log.error("cannot create embedded user with email: " + user.getEmail());
        }

        halResource.addEmbeddedResource(relation, embeddedUserResource);
    }


    public HalResource<Release> createHalReleaseResource(Release release, boolean verbose) {
        HalResource<Release> halRelease = new HalResource<>(release);

        Link componentLink = linkTo(ReleaseController.class)
                .slash("api" + ComponentController.COMPONENTS_URL + "/" + release.getComponentId()).withRel("component");
        halRelease.add(componentLink);
        release.setComponentId(null);

        if (verbose) {
            if (release.getModerators() != null) {
                Set<String> moderators = release.getModerators();
                this.addEmbeddedModerators(halRelease, moderators);
            }
            if (release.getAttachments() != null) {
                Set<Attachment> attachments = release.getAttachments();
                this.addEmbeddedAttachments(halRelease, attachments);
            }
        }
        return halRelease;
    }

    private void addEmbeddedRelease(HalResource halResource, Release release, String linkRelation) {
        release.setType(null);
        HalResource<Release> halRelease = new HalResource<>(release);
        try {
            Link releaseLink = linkTo(ReleaseController.class).slash("api/releases/" + release.getId()).withSelfRel();
            halRelease.add(releaseLink);
        } catch (Exception e) {
            log.error("cannot create embedded release with id: " + release.getId());
        }

        halResource.addEmbeddedResource(linkRelation, halRelease);
    }

    private void addEmbeddedAttachments(
            HalResource halResource,
            Set<Attachment> attachments) {
        for (Attachment attachment : attachments) {

            HalResource<Attachment> halAttachmentResource = new HalResource<>(attachment);
            try {
                Link attachmentLink = linkTo(AttachmentController.class).slash("api/attachments/" + attachment.getAttachmentContentId()).withSelfRel();
                halAttachmentResource.add(attachmentLink);
            } catch (Exception e) {
                log.error("cannot create embedded attachment with content id: " + attachment.getAttachmentContentId());
            }

            halResource.addEmbeddedResource("attachments", halAttachmentResource);
        }
    }
}
