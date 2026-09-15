package com.example.delivery.camunda.identity;

import java.util.List;

import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.authorization.Permission;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resource;
import org.camunda.bpm.engine.authorization.Resources;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;

final class CamundaIdentitySynchronizer {

    private static final String DELIVERY_PROCESS = "delivery-order";
    private static final String AVAILABILITY_PROCESS = "courier-availability";
    private static final String TASKLIST = "tasklist";
    private static final String CAMUNDA_ADMIN = "camunda-admin";
    private static final List<String> DELIVERY_ROLES = List.of("CUSTOMER", "SHOP", "COURIER", "ADMIN");

    private final IdentityService identityService;
    private final AuthorizationService authorizationService;
    private final XmlUserStore userStore;

    CamundaIdentitySynchronizer(ProcessEngine processEngine, XmlUserStore userStore) {
        this.identityService = processEngine.getIdentityService();
        this.authorizationService = processEngine.getAuthorizationService();
        this.userStore = userStore;
    }

    void synchronize() {
        synchronizeGroups();
        synchronizeUsers();
        synchronizeAuthorizations();
    }

    private void synchronizeGroups() {
        DELIVERY_ROLES.forEach(role -> saveGroup(
                role, "Delivery " + role.toLowerCase(), "ADMIN".equals(role) ? "SYSTEM" : "WORKFLOW"));
        saveGroup(CAMUNDA_ADMIN, "camunda BPM Administrators", "SYSTEM");
    }

    private void saveGroup(String id, String name, String type) {
        Group group = identityService.createGroupQuery().groupId(id).singleResult();
        if (group == null) {
            group = identityService.newGroup(id);
        }
        group.setName(name);
        group.setType(type);
        identityService.saveGroup(group);
    }

    private void synchronizeUsers() {
        for (XmlUserStore.UserAccount account : userStore.users()) {
            User user = identityService.createUserQuery().userId(account.username()).singleResult();
            if (user == null) {
                user = identityService.newUser(account.username());
            }
            user.setFirstName(account.username());
            user.setLastName(account.role());
            identityService.saveUser(user);

            ensureMembership(account.username(), account.role());
            if ("ADMIN".equals(account.role())) {
                ensureMembership(account.username(), CAMUNDA_ADMIN);
            }
            for (String role : DELIVERY_ROLES) {
                if (!role.equals(account.role()) && hasMembership(account.username(), role)) {
                    identityService.deleteMembership(account.username(), role);
                }
            }
            grantUser(account.username(), Resources.USER, account.username(), Permissions.READ);
            grantUser(account.username(), Resources.GROUP, account.role(), Permissions.READ);
        }
    }

    private void ensureMembership(String userId, String groupId) {
        if (!hasMembership(userId, groupId)) {
            identityService.createMembership(userId, groupId);
        }
    }

    private boolean hasMembership(String userId, String groupId) {
        return identityService.createGroupQuery().groupId(groupId).groupMember(userId).count() > 0;
    }

    private void synchronizeAuthorizations() {
        deleteGrant("CUSTOMER", Resources.TASK, Authorization.ANY);
        deleteGrant("SHOP", Resources.TASK, Authorization.ANY);
        deleteGrant("COURIER", Resources.TASK, Authorization.ANY);

        for (Resources resource : Resources.values()) {
            grant("ADMIN", resource, Authorization.ANY, Permissions.ALL);
            grant(CAMUNDA_ADMIN, resource, Authorization.ANY, Permissions.ALL);
        }

        grant("CUSTOMER", Resources.APPLICATION, TASKLIST, Permissions.ACCESS);
        grant("CUSTOMER", Resources.PROCESS_DEFINITION, DELIVERY_PROCESS,
                Permissions.READ, Permissions.CREATE_INSTANCE, Permissions.READ_INSTANCE,
                Permissions.READ_TASK, Permissions.UPDATE_TASK);
        grant("CUSTOMER", Resources.PROCESS_INSTANCE, Authorization.ANY, Permissions.CREATE);
        grant("CUSTOMER", Resources.FILTER, Authorization.ANY, Permissions.READ, Permissions.CREATE);

        grant("SHOP", Resources.APPLICATION, TASKLIST, Permissions.ACCESS);
        grant("SHOP", Resources.PROCESS_DEFINITION, DELIVERY_PROCESS,
                Permissions.READ, Permissions.READ_INSTANCE, Permissions.READ_TASK,
                Permissions.UPDATE_TASK);
        grant("SHOP", Resources.FILTER, Authorization.ANY, Permissions.READ, Permissions.CREATE);

        grant("COURIER", Resources.APPLICATION, TASKLIST, Permissions.ACCESS);
        grant("COURIER", Resources.PROCESS_DEFINITION, AVAILABILITY_PROCESS,
                Permissions.READ, Permissions.CREATE_INSTANCE, Permissions.READ_INSTANCE,
                Permissions.READ_TASK, Permissions.UPDATE_TASK);
        grant("COURIER", Resources.PROCESS_INSTANCE, Authorization.ANY, Permissions.CREATE);
        grant("COURIER", Resources.PROCESS_DEFINITION, DELIVERY_PROCESS,
                Permissions.READ, Permissions.READ_INSTANCE, Permissions.READ_TASK,
                Permissions.UPDATE_TASK);
        grant("COURIER", Resources.FILTER, Authorization.ANY, Permissions.READ, Permissions.CREATE);
    }

    private void deleteGrant(String groupId, Resource resource, String resourceId) {
        authorizationService.createAuthorizationQuery()
                .authorizationType(Authorization.AUTH_TYPE_GRANT)
                .groupIdIn(groupId)
                .resourceType(resource)
                .resourceId(resourceId)
                .list()
                .forEach(authorization -> authorizationService.deleteAuthorization(authorization.getId()));
    }

    private void grant(String groupId, Resource resource, String resourceId, Permission... permissions) {
        Authorization authorization = authorizationService.createAuthorizationQuery()
                .authorizationType(Authorization.AUTH_TYPE_GRANT)
                .groupIdIn(groupId)
                .resourceType(resource)
                .resourceId(resourceId)
                .singleResult();
        if (authorization == null) {
            authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
            authorization.setGroupId(groupId);
            authorization.setResource(resource);
            authorization.setResourceId(resourceId);
        }
        addPermissions(authorization, permissions);
        authorizationService.saveAuthorization(authorization);
    }

    private void grantUser(String userId, Resource resource, String resourceId, Permission... permissions) {
        Authorization authorization = authorizationService.createAuthorizationQuery()
                .authorizationType(Authorization.AUTH_TYPE_GRANT)
                .userIdIn(userId)
                .resourceType(resource)
                .resourceId(resourceId)
                .singleResult();
        if (authorization == null) {
            authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
            authorization.setUserId(userId);
            authorization.setResource(resource);
            authorization.setResourceId(resourceId);
        }
        addPermissions(authorization, permissions);
        authorizationService.saveAuthorization(authorization);
    }

    private void addPermissions(Authorization authorization, Permission... permissions) {
        for (Permission permission : permissions) {
            if (!authorization.isPermissionGranted(permission)) {
                authorization.addPermission(permission);
            }
        }
    }
}
