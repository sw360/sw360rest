/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HalResource<EntityType> extends Resource<EntityType> {

    private Map<String, List<Object>> embedded;

    public HalResource(EntityType content, Iterable<Link> links) {
        super(content, links);
    }

    public HalResource(EntityType content, Link... links) {
        super(content, links);
    }

    public void addEmbeddedItem(String relation, Object embeddedItem) {
        initializeEmbedded();
        List<Object> embeddedItems = embedded.get(relation);
        if(embeddedItems == null) {
            embeddedItems = new ArrayList<>();
        }
        embeddedItems.add(embeddedItem);
        embedded.put(relation, embeddedItems);
    }

    public void addEmbeddedItems(String relation, List<Object> embeddedItems) {
        initializeEmbedded();
        embedded.put(relation, embeddedItems);
    }

    @JsonProperty("_embedded")
    public Map<String, List<Object>> getEmbedded() {
        return embedded;
    }

    private void initializeEmbedded() {
        if(embedded == null) {
            embedded = new HashMap<>();
        }
    }
}
