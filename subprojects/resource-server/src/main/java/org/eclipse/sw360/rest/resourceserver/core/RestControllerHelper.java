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
import org.eclipse.sw360.rest.resourceserver.attachment.AttachmentResource;
import org.eclipse.sw360.rest.resourceserver.component.ComponentController;
import org.eclipse.sw360.rest.resourceserver.release.ReleaseController;
import org.eclipse.sw360.rest.resourceserver.release.ReleaseResource;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.eclipse.sw360.rest.resourceserver.user.UserController;
import org.eclipse.sw360.rest.resourceserver.user.UserResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
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
            User user) {
        for (String releaseId : releases) {
            final Release release = sw360ReleaseService.getReleaseForUserById(releaseId, user);
            addEmbeddedRelease(halResource, release);
        }
    }

    public void addEmbeddedReleases(
            HalResourceWidthEmbeddedItems halResource,
            List<Release> releases) {
        for (Release release : releases) {
            addEmbeddedRelease(halResource, release);
        }
    }

    public void addEmbeddedUser(HalResourceWidthEmbeddedItems halResource, User user, String relation) {
        UserResource userResource = new UserResource();
        try {
            userResource.setEmail(user.getEmail());
            String userUUID = Base64.getEncoder().encodeToString(user.getEmail().getBytes("utf-8"));
            Link userLink = linkTo(UserController.class).slash("api/users/" + userUUID).withSelfRel();
            userResource.add(userLink);
        } catch (Exception e) {
            log.error("cannot create embedded user with email: " + user.getEmail());
        }

        halResource.addEmbeddedItem(relation, userResource);
    }


    public HalResourceWidthEmbeddedItems<ReleaseResource> createHalReleaseResource(Release release, boolean verbose) {
        ReleaseResource releaseResource = new ReleaseResource();

        releaseResource.setName(release.getName());
        releaseResource.setCpeId(release.getCpeid());
        releaseResource.setVersion(release.getVersion());
        releaseResource.setReleaseDate(release.getReleaseDate());
        if (release.getClearingState() != null) {
            releaseResource.setClearingState(release.getClearingState().toString());
        }

        Link selfLink = linkTo(ReleaseController.class)
                .slash("api" + ReleaseController.RELEASES_URL + "/" + release.getId()).withSelfRel();
        releaseResource.add(selfLink);

        Link componentLink = linkTo(ReleaseController.class)
                .slash("api" + ComponentController.COMPONENTS_URL + "/" + release.getComponentId()).withRel("component");
        releaseResource.add(componentLink);

        HalResourceWidthEmbeddedItems<ReleaseResource> halReleaseResource = new HalResourceWidthEmbeddedItems<>(releaseResource);

        if (verbose) {
            releaseResource.setType(release.getType());
            if (release.getModerators() != null) {
                Set<String> moderators = release.getModerators();
                this.addEmbeddedModerators(halReleaseResource, moderators);
            }
            if (release.getAttachments() != null) {
                Set<Attachment> attachments = release.getAttachments();
                this.addEmbeddedAttachments(halReleaseResource, attachments);
            }
        }
        return halReleaseResource;
    }

    private void addEmbeddedRelease(HalResourceWidthEmbeddedItems halResource, Release release) {
        ReleaseResource releaseResource = new ReleaseResource();
        try {
            releaseResource.setVersion(release.getVersion());
            if(release.getName() != null) {
                releaseResource.setName(release.getName());
            }
            if(release.getClearingState() != null) {
                releaseResource.setClearingState(release.getClearingState().toString());
            }
            Link releaseLink = linkTo(ReleaseController.class).slash("api/releases/" + release.getId()).withSelfRel();
            releaseResource.add(releaseLink);
        } catch (Exception e) {
            log.error("cannot create embedded release with id: " + release.getId());
        }

        halResource.addEmbeddedItem("releases", releaseResource);
    }


    private void addEmbeddedAttachments(
            HalResourceWidthEmbeddedItems halResource,
            Set<Attachment> attachments) {
        for (Attachment attachment : attachments) {
            AttachmentResource attachmentResource = new AttachmentResource();
            try {
                if (attachment.getAttachmentType() != null) {
                    attachmentResource.setAttachmentType(attachment.getAttachmentType().toString());
                }
                if (attachment.getFilename() != null) {
                    attachmentResource.setFilename(attachment.getFilename());
                }
                Link attachmentLink = linkTo(AttachmentController.class).slash("api/attachments/" + attachment.getAttachmentContentId()).withSelfRel();
                attachmentResource.add(attachmentLink);
            } catch (Exception e) {
                log.error("cannot create embedded attachment with content id: " + attachment.getAttachmentContentId());
            }

            halResource.addEmbeddedItem("attachments", attachmentResource);
        }

    }
}
