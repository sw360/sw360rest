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
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.attachment.AttachmentController;
import org.eclipse.sw360.rest.resourceserver.attachment.AttachmentResource;
import org.eclipse.sw360.rest.resourceserver.component.ComponentController;
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
            HalResourceWidthEmbeddedItems<org.eclipse.sw360.rest.resourceserver.component.ComponentResource> halResource,
            Set<String> releases,
            Sw360ReleaseService sw360ReleaseService,
            String userId) {
        for (String releaseId : releases) {
            final Release release = sw360ReleaseService.getReleaseForUserById(releaseId, userId);
            addEmbeddedRelease(halResource, release);
        }
    }

    public void addEmbeddedReleases(
            HalResourceWidthEmbeddedItems<org.eclipse.sw360.rest.resourceserver.component.ComponentResource> halResource,
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

    private void addEmbeddedRelease(HalResourceWidthEmbeddedItems<org.eclipse.sw360.rest.resourceserver.component.ComponentResource> halResource, Release release) {
        ReleaseResource releaseResource = new ReleaseResource();
        try {
            releaseResource.setVersion(release.getVersion());
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


    public void addEmbeddedAttachments(
            HalResourceWidthEmbeddedItems<ReleaseResource> halResource,
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

    public HalResourceWidthEmbeddedItems<ReleaseResource> createHalReleaseResource(Release sw360Release, boolean verbose) {
        ReleaseResource releaseResource = new ReleaseResource();

        releaseResource.setName(sw360Release.getName());
        releaseResource.setCpeId(sw360Release.getCpeid());
        releaseResource.setVersion(sw360Release.getVersion());
        releaseResource.setReleaseDate(sw360Release.getReleaseDate());
        if (sw360Release.getClearingState() != null) {
            releaseResource.setClearingState(sw360Release.getClearingState().toString());
        }

        Link selfLink = linkTo(ReleaseController.class)
                .slash("api" + ReleaseController.RELEASES_URL + "/" + sw360Release.getId()).withSelfRel();
        releaseResource.add(selfLink);

        Link componentLink = linkTo(ReleaseController.class)
                .slash("api" + ComponentController.COMPONENTS_URL + "/" + sw360Release.getComponentId()).withRel("component");
        releaseResource.add(componentLink);

        HalResourceWidthEmbeddedItems<ReleaseResource> halReleaseResource = new HalResourceWidthEmbeddedItems<>(releaseResource);

        if (verbose) {
            releaseResource.setType(sw360Release.getType());
            if (sw360Release.getModerators() != null) {
                Set<String> moderators = sw360Release.getModerators();
                this.addEmbeddedModerators(halReleaseResource, moderators);
            }
            if (sw360Release.getAttachments() != null) {
                Set<Attachment> attachments = sw360Release.getAttachments();
                this.addEmbeddedAttachments(halReleaseResource, attachments);
            }
        }
        return halReleaseResource;
    }


}
