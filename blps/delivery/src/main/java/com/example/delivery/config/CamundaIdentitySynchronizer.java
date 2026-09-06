package com.example.delivery.config;

import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.authorization.Permission;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resource;
import org.camunda.bpm.engine.authorization.Resources;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.config.XmlUserDetailsService.XmlUser;
import com.example.delivery.model.UserRole;
import com.example.delivery.process.DeliveryProcessService;

@Component
public class CamundaIdentitySynchronizer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CamundaIdentitySynchronizer.class);
    private static final String TASKLIST = "tasklist";
    private static final String CAMUNDA_ADMIN = "camunda-admin";

    private final IdentityService identityService;
    private final AuthorizationService authorizationService;
    private final XmlUserDetailsService xmlUserDetailsService;

    public CamundaIdentitySynchronizer(
            IdentityService identityService,
            AuthorizationService authorizationService,
            XmlUserDetailsService xmlUserDetailsService) {
        this.identityService = identityService;
        this.authorizationService = authorizationService;
        this.xmlUserDetailsService = xmlUserDetailsService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        synchronizeGroups();
        synchronizeUsers();
        synchronizeAuthorizations();
        log.info("Synchronized {} XML users and delivery roles with Camunda Identity Service",
                xmlUserDetailsService.getAllUsers().size());
    }

    private void synchronizeGroups() {
        for (UserRole role : UserRole.values()) {
            String groupId = role.name();
            Group group = identityService.createGroupQuery().groupId(groupId).singleResult();
            if (group == null) {
                group = identityService.newGroup(groupId);
            }
            group.setName("Delivery " + role.name().toLowerCase());
            group.setType(role == UserRole.ADMIN ? "SYSTEM" : "WORKFLOW");
            identityService.saveGroup(group);
        }
        Group camundaAdmin = identityService.createGroupQuery().groupId(CAMUNDA_ADMIN).singleResult();
        if (camundaAdmin == null) {
            camundaAdmin = identityService.newGroup(CAMUNDA_ADMIN);
        }
        camundaAdmin.setName("camunda BPM Administrators");
        camundaAdmin.setType("SYSTEM");
        identityService.saveGroup(camundaAdmin);
    }

    private void synchronizeUsers() {
        for (XmlUser xmlUser : xmlUserDetailsService.getAllUsers()) {
            User user = identityService.createUserQuery().userId(xmlUser.username()).singleResult();
            if (user == null) {
                user = identityService.newUser(xmlUser.username());
            }
            user.setFirstName(xmlUser.username());
            user.setLastName(xmlUser.role());
            identityService.saveUser(user);

            if (identityService.createGroupQuery()
                    .groupId(xmlUser.role())
                    .groupMember(xmlUser.username())
                    .count() == 0) {
                identityService.createMembership(xmlUser.username(), xmlUser.role());
            }
            if (UserRole.ADMIN.name().equals(xmlUser.role()) && identityService.createGroupQuery()
                    .groupId(CAMUNDA_ADMIN)
                    .groupMember(xmlUser.username())
                    .count() == 0) {
                identityService.createMembership(xmlUser.username(), CAMUNDA_ADMIN);
            }
            for (UserRole role : UserRole.values()) {
                if (!role.name().equals(xmlUser.role()) && identityService.createGroupQuery()
                        .groupId(role.name())
                        .groupMember(xmlUser.username())
                        .count() > 0) {
                    identityService.deleteMembership(xmlUser.username(), role.name());
                }
            }

            grantUser(xmlUser.username(), Resources.USER, xmlUser.username(), Permissions.READ);
            grantUser(xmlUser.username(), Resources.GROUP, xmlUser.role(), Permissions.READ);
            if (UserRole.ADMIN.name().equals(xmlUser.role())) {
                for (Resources resource : Resources.values()) {
                    grantUser(xmlUser.username(), resource, Authorization.ANY, Permissions.ALL);
                }
            }
        }
    }

    private void synchronizeAuthorizations() {
        deleteGrant("CUSTOMER", Resources.TASK, Authorization.ANY);
        deleteGrant("SHOP", Resources.TASK, Authorization.ANY);
        deleteGrant("COURIER", Resources.TASK, Authorization.ANY);
        revoke("CUSTOMER", Resources.PROCESS_DEFINITION, DeliveryProcessService.PROCESS_KEY,
                Permissions.READ_INSTANCE, Permissions.READ_TASK, Permissions.UPDATE_TASK);
        revoke("SHOP", Resources.PROCESS_DEFINITION, DeliveryProcessService.PROCESS_KEY,
                Permissions.READ_TASK, Permissions.UPDATE_TASK);
        revoke("COURIER", Resources.PROCESS_DEFINITION, DeliveryProcessService.COURIER_AVAILABILITY_PROCESS_KEY,
                Permissions.READ_INSTANCE, Permissions.READ_TASK, Permissions.UPDATE_TASK);
        revoke("COURIER", Resources.PROCESS_DEFINITION, DeliveryProcessService.PROCESS_KEY,
                Permissions.READ_INSTANCE, Permissions.READ_TASK, Permissions.UPDATE_TASK);

        for (Resources resource : Resources.values()) {
            grant("ADMIN", resource, Authorization.ANY, Permissions.ALL);
            grant(CAMUNDA_ADMIN, resource, Authorization.ANY, Permissions.ALL);
        }

        grant("CUSTOMER", Resources.APPLICATION, TASKLIST, Permissions.ACCESS);
        grant("CUSTOMER", Resources.PROCESS_DEFINITION, DeliveryProcessService.PROCESS_KEY,
                Permissions.READ, Permissions.CREATE_INSTANCE);
        grant("CUSTOMER", Resources.PROCESS_INSTANCE, Authorization.ANY, Permissions.CREATE);
        grant("CUSTOMER", Resources.FILTER, Authorization.ANY, Permissions.READ, Permissions.CREATE);

        grant("SHOP", Resources.APPLICATION, TASKLIST, Permissions.ACCESS);
        grant("SHOP", Resources.PROCESS_DEFINITION, DeliveryProcessService.PROCESS_KEY,
                Permissions.READ, Permissions.READ_INSTANCE);
        grant("SHOP", Resources.FILTER, Authorization.ANY, Permissions.READ, Permissions.CREATE);

        grant("COURIER", Resources.APPLICATION, TASKLIST, Permissions.ACCESS);
        grant("COURIER", Resources.PROCESS_DEFINITION, DeliveryProcessService.COURIER_AVAILABILITY_PROCESS_KEY,
                Permissions.READ, Permissions.CREATE_INSTANCE);
        grant("COURIER", Resources.PROCESS_INSTANCE, Authorization.ANY, Permissions.CREATE);
        grant("COURIER", Resources.PROCESS_DEFINITION, DeliveryProcessService.PROCESS_KEY,
                Permissions.READ);
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

    private void revoke(String groupId, Resource resource, String resourceId, Permission... permissions) {
        authorizationService.createAuthorizationQuery()
                .authorizationType(Authorization.AUTH_TYPE_GRANT)
                .groupIdIn(groupId)
                .resourceType(resource)
                .resourceId(resourceId)
                .list()
                .forEach(authorization -> {
                    for (Permission permission : permissions) {
                        authorization.removePermission(permission);
                    }
                    authorizationService.saveAuthorization(authorization);
                });
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
        for (Permission permission : permissions) {
            if (!authorization.isPermissionGranted(permission)) {
                authorization.addPermission(permission);
            }
        }
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
        for (Permission permission : permissions) {
            if (!authorization.isPermissionGranted(permission)) {
                authorization.addPermission(permission);
            }
        }
        authorizationService.saveAuthorization(authorization);
    }
}
