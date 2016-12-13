/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.project;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ProjectService {
    @Value("${sw360.thrift-server-url}")
    private String thriftServerUrl;

    @NonNull
    private final Sw360UserService sw360UserService;

    public List<Project> getProjectsForUser(String userId) {
        try {
            ProjectService.Iface sw360ProjectClient = getThriftProjectClient();

            // TODO Kai Toedter 2016-12-09
            // It is inconsistent with componentClient.getMyComponents(User user)
            List<Project> projects = sw360ProjectClient.getMyProjects(userId);
            return projects;
        } catch (TException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public Project getProjectForUserById(String projectId, String userId) {
        try {
            ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
            User sw360User = sw360UserService.getUserById(userId);
            sw360ProjectClient.getProjectById(projectId, sw360User);
        } catch (TException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ProjectService.Iface getThriftProjectClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/projects/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new ProjectService.Client(protocol);
    }
}
