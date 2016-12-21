/*
 * Copyright Siemens AG; 2016. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution; and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.attachment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.core.Relation;

@Data
@EqualsAndHashCode(callSuper=false)
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Relation(collectionRelation = "attachments")
public class AttachmentResource extends ResourceSupport {
    private String filename;
    private String sha1;
    private String attachmentType;
    private String createdTeam;
    private String createdComment;
    private String createdOn;
    private String checkedBy;
    private String checkedTeam;
    private String checkedComment;
    private String checkedOn;
    private String checkStatus;
}
