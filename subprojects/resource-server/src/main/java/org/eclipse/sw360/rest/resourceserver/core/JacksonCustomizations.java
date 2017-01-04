/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class JacksonCustomizations {

    @Bean
    public Module sw360Module() {
        return new Sw360Module();
    }

    @SuppressWarnings("serial")
    static class Sw360Module extends SimpleModule {

        public Sw360Module() {
            setMixInAnnotation(Project.class, Sw360Module.ProjectMixin.class);
            setMixInAnnotation(User.class, Sw360Module.UserMixin.class);
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "version",
                "externalIds",
                "attachments",
                "businessUnit",
                "createdBy",
                "state",
                "tag",
                "projectResponsible",
                "leadArchitect",
                "moderators",
                "contributors",
                "visbility",
                "linkedProjects",
                "releaseIdToUsage",
                "clearingTeam",
                "preevaluationDeadline",
                "systemTestStart",
                "systemTestEnd",
                "deliveryStart",
                "phaseOutSince",
                "homepage",
                "wiki",
                "documentState",
                "releaseIds",
                "releaseClearingStateSummary",
                "permissions",
                "attachmentsIterator",
                "moderatorsIterator",
                "contributorsIterator",
                "releaseIdsIterator",
                "setId",
                "setRevision",
                "setType",
                "setName",
                "setDescription",
                "setVersion",
                "setExternalIds",
                "setAttachments",
                "setCreatedOn",
                "setState",
                "setProjectType",
                "setTag",
                "setCreatedBy",
                "setModerators",
                "setVisbility",
                "setHomepage",
                "externalIdsSize",
                "attachmentsSize",
                "setBusinessUnit",
                "setProjectResponsible",
                "setLeadArchitect",
                "moderatorsSize",
                "contributorsSize",
                "setContributors",
                "linkedProjectsSize",
                "setLinkedProjects",
                "releaseIdToUsageSize",
                "setReleaseIdToUsage",
                "setClearingTeam",
                "setPreevaluationDeadline",
                "setSystemTestStart",
                "setSystemTestEnd",
                "setDeliveryStart",
                "setPhaseOutSince",
                "setDocumentState",
                "releaseIdsSize",
                "setReleaseClearingStateSummary",
                "permissionsSize",
                "setWiki",
                "setReleaseIds",
                "setPermissions",
        })
        static abstract class ProjectMixin {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "externalid",
                "wantsMailNotification",
                "setWantsMailNotification",
                "setId",
                "setRevision",
                "setType",
                "setEmail",
                "setUserGroup",
                "setExternalid",
                "setFullname",
                "setGivenname",
                "setLastname",
                "setDepartment"
        })
        static abstract class UserMixin extends User {
            @Override @JsonProperty("fullName") abstract public String getFullname();
            @Override @JsonProperty("givenName") abstract public String getGivenname();
            @Override @JsonProperty("lastName") abstract public String getLastname();
        }
    }
}
