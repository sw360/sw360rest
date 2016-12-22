package org.eclipse.sw360.rest.resourceserver.user;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.HalResourceWidthEmbeddedItems;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Base64;
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
    public ResponseEntity<Resources<Resource<UserResource>>> getUsers() {
        try {
            List<User> sw360Users = userService.getAllUsers();

            List<Resource<UserResource>> userResources = new ArrayList<>();
            for (User sw360User : sw360Users) {
                HalResourceWidthEmbeddedItems<UserResource> userHalResource = createHalUserResource(sw360User, false);
                userResources.add(userHalResource);
            }
            Resources<Resource<UserResource>> resources = new Resources<>(userResources);

            return new ResponseEntity<>(resources, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @RequestMapping(USERS_URL + "/{id:.+}")
    public ResponseEntity<Resource<UserResource>> getUser(
            @PathVariable("id") String id) {
        try {
            byte[] base64decodedBytes = Base64.getDecoder().decode(id);
            String decodedId = new String(base64decodedBytes, "utf-8");

            User sw360User = userService.getUserByEmail(decodedId);
            HalResourceWidthEmbeddedItems<UserResource> userHalResource = createHalUserResource(sw360User, true);
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

    private HalResourceWidthEmbeddedItems<UserResource> createHalUserResource(User sw360User, boolean verbose) {
        UserResource userResource = new UserResource();

        userResource.setEmail(sw360User.getEmail());
        if (sw360User.getUserGroup() != null) {
            userResource.setUserGroup(sw360User.getUserGroup().toString());
        }
        userResource.setFullName(sw360User.getFullname());

        try {
            String userUUID = Base64.getEncoder().encodeToString(userResource.getEmail().getBytes("utf-8"));
            Link selfLink = linkTo(UserController.class).slash("api/users/" + userUUID).withSelfRel();
            userResource.add(selfLink);
        } catch (Exception e) {
            log.error("cannot create self link");
            return null;
        }

        if (verbose) {
            userResource.setGivenName(sw360User.getGivenname());
            userResource.setLastName(sw360User.getLastname());
            userResource.setDepartment(sw360User.getDepartment());
            userResource.setType(sw360User.getType());
        }
        return new HalResourceWidthEmbeddedItems<>(userResource);
    }
}
