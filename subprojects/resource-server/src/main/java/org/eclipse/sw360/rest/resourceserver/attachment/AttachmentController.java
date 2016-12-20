/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Component.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.attachment;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.rest.resourceserver.core.HalHelper;
import org.eclipse.sw360.rest.resourceserver.core.HalResourceWidthEmbeddedItems;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AttachmentController implements ResourceProcessor<RepositoryLinksResource> {
    public static final String ATTACHMENTS_URL = "/attachments";

    @NonNull
    private final Sw360AttachmentService attachmentService;

    @NonNull
    private final HalHelper halHelper;

    @Value("${sw360.thrift-server-url}")
    private String thriftServerUrl;

    @RequestMapping(value = ATTACHMENTS_URL, params = "sha1")
    public ResponseEntity<Resource> getAttachmentForSha1(
            OAuth2Authentication oAuth2Authentication,
            @RequestParam String sha1) {
        try {
            String userId = (String) oAuth2Authentication.getPrincipal();
            AttachmentInfo attachmentInfo =
                    attachmentService.getAttachmentBySha1ForUser(sha1, userId);

            HalResourceWidthEmbeddedItems attachmentResource =
                    createHalAttachmentResource(attachmentInfo.getAttachment(), attachmentInfo.getRelease(),true);
            return new ResponseEntity<>(attachmentResource, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @RequestMapping(value = ATTACHMENTS_URL + "/{id}")
    public ResponseEntity<Resource> getAttachmentForId(
            @PathVariable("id") String id,
            OAuth2Authentication oAuth2Authentication) {
        try {
            String userId = (String) oAuth2Authentication.getPrincipal();
            AttachmentInfo attachmentInfo =
                    attachmentService.getAttachmentByIdForUser(id, userId);

            HalResourceWidthEmbeddedItems attachmentResource =
                    createHalAttachmentResource(attachmentInfo.getAttachment(), attachmentInfo.getRelease(),true);
            return new ResponseEntity<>(attachmentResource, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    private HalResourceWidthEmbeddedItems createHalAttachmentResource(
            Attachment sw360Attachment,
            Release sw360Release,
            boolean verbose) {
        AttachmentResource attachmentResource = new AttachmentResource();

        attachmentResource.setFilename(sw360Attachment.getFilename());
        attachmentResource.setSha1(sw360Attachment.getSha1());
        attachmentResource.setAttachmentType(sw360Attachment.getAttachmentType().toString());
        attachmentResource.setCreatedBy(sw360Attachment.getCreatedBy());
        attachmentResource.setCreatedTeam(sw360Attachment.getCreatedTeam());
        attachmentResource.setCreatedComment(sw360Attachment.getCreatedComment());
        attachmentResource.setCreatedOn(sw360Attachment.getCreatedOn());
        attachmentResource.setCheckedBy(sw360Attachment.getCheckedBy());
        attachmentResource.setCheckedTeam(sw360Attachment.getCheckedTeam());
        attachmentResource.setCheckedComment(sw360Attachment.getCheckedComment());
        attachmentResource.setCheckedOn(sw360Attachment.getCheckedOn());
        attachmentResource.setCheckStatus(sw360Attachment.getCheckStatus().toString());

        String componentUUID = sw360Attachment.getAttachmentContentId();
        Link selfLink = linkTo(AttachmentController.class).slash("api/attachments/" + componentUUID).withSelfRel();
        attachmentResource.add(selfLink);

        Link releaseLink = linkTo(AttachmentController.class).slash("api/releases/" + sw360Release.getId()).withRel("release");
        attachmentResource.add(releaseLink);

        HalResourceWidthEmbeddedItems halAttachmentResource = new HalResourceWidthEmbeddedItems(attachmentResource);

        halAttachmentResource.addEmbeddedItem("release", halHelper.createHalReleaseResource(sw360Release, false));
        return halAttachmentResource;
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        final ControllerLinkBuilder controllerLinkBuilder = linkTo(AttachmentController.class);
        final Link attachments = new Link(new UriTemplate(controllerLinkBuilder.toUri().toString() + "/api/attachments{?sha1}"), "attachments");
        resource.add(attachments);
        return resource;
    }
}
