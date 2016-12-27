/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Release.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.release;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ReleaseService {
    @Value("${sw360.thrift-server-url}")
    private String thriftServerUrl;

    @NonNull
    private final Sw360UserService sw360UserService;

    public List<Release> getReleasesForUser(String userId) {
        try {
            ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
            User sw360User = sw360UserService.getUserByEmail(userId);
            return sw360ComponentClient.getReleaseSummary(sw360User);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public Release getReleaseForUserById(String releaseId, String userId) {
        try {
            ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
            User sw360User = sw360UserService.getUserByEmail(userId);
            return sw360ComponentClient.getReleaseById(releaseId, sw360User);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public Release createRelease(Release release, String userId) {
        try {
            ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
            User sw360User = sw360UserService.getUserByEmail(userId);
            AddDocumentRequestSummary documentRequestSummary = sw360ComponentClient.addRelease(release, sw360User);
            if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.SUCCESS) {
                release.setId(documentRequestSummary.getId());
                return release;
            } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.DUPLICATE) {
                throw new DataIntegrityViolationException("sw360 release with name '" + release.getName() + " already exists.");
            }
        } catch (TException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private ComponentService.Iface getThriftComponentClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/components/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new ComponentService.Client(protocol);
    }
}
