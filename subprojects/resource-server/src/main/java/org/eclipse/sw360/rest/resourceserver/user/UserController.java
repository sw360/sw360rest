package org.eclipse.sw360.rest.resourceserver.user;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserController implements ResourceProcessor<RepositoryLinksResource> {

    protected final EntityLinks entityLinks;

    @Value("${sw360.thrift-server-url}")
    private String thriftServerUrl;

    private final String USERS_URL = "/users";

    @NonNull
    private final Sw360UserService userService;

    // @PreAuthorize("hasRole('ROLE_SW360_USER')")
    @RequestMapping(USERS_URL)
    @ResponseBody
    public ResponseEntity<Resources<Resource>> getUsers() {
        try {
            List<User> sw360Users = userService.getAllUsers();

            List<Resource> userResources = new ArrayList<>();
            for (User sw360User : sw360Users) {
                HalResource userHalResource = createHalUserResource(sw360User);
                userResources.add(userHalResource);
            }
            Resources<Resource> resources = new Resources<>(userResources);

            return new ResponseEntity<>(resources, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @RequestMapping(USERS_URL + "/{id:.+}")
    @ResponseBody
    public ResponseEntity<Resource> getUser(
            @PathVariable("id") String id) {
        try {
            User sw360User = userService.getUserById(id);
            HalResource userHalResource = createHalUserResource(sw360User);
            return new ResponseEntity<>(userHalResource, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(UserController.class).slash("api/users").withRel("users"));
        return resource;
    }

    private HalResource createHalUserResource(User sw360User) {
        UserResource userResource = new UserResource();

        userResource.setType(sw360User.getType());
        userResource.setEmail(sw360User.getEmail());
        if(sw360User.getUserGroup() != null) {
            userResource.setUserGroup(sw360User.getUserGroup().toString());
        }
        userResource.setFullName(sw360User.getFullname());
        userResource.setGivenName(sw360User.getGivenname());
        userResource.setLastName(sw360User.getLastname());
        userResource.setDepartment(sw360User.getDepartment());
        userResource.setWantsMailNotification(sw360User.wantsMailNotification);

        // we should not use the email directly as REST resource id,
        // but we don't want to introduce a new persistence layer for that
        String userUUID = userResource.getEmail();
        Link selfLink = linkTo(UserController.class).slash("api/users/" + userUUID).withSelfRel();
        userResource.add(selfLink);
        return new HalResource(userResource);
    }
}
