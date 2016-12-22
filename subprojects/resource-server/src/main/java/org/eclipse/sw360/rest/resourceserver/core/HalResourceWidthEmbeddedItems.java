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
public class HalResourceWidthEmbeddedItems<EntityType> extends Resource<EntityType> {

    private Map<String, Object> embeddedMap;

    public HalResourceWidthEmbeddedItems(EntityType content, Link... links) {
        super(content, links);
    }

    @SuppressWarnings("unchecked")
    public void addEmbeddedItem(String relation, Object embeddedItem) {
        if (embeddedMap == null) {
            embeddedMap = new HashMap<>();
        }
        Object embeddedItems = embeddedMap.get(relation);
        if (embeddedItems == null) {
            embeddedItems = embeddedItem;
        } else {
            if (embeddedItems instanceof List) {
                ((List<Object>) embeddedItems).add(embeddedItem);
            } else {
                List<Object> embeddedItemsList = new ArrayList<>();
                embeddedItemsList.add(embeddedItems);
                embeddedItemsList.add(embeddedItem);
                embeddedItems = embeddedItemsList;
            }
        }
        embeddedMap.put(relation, embeddedItems);
    }

    @JsonProperty("_embedded")
    public Map<String, Object> getEmbedded() {
        return embeddedMap;
    }
}
