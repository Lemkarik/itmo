package com.example.delivery.config;

import com.example.delivery.model.Privilege;
import com.example.delivery.model.UserRole;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;


public class PrivilegeMapping {

    private static final Map<UserRole, Set<Privilege>> ROLE_PRIVILEGES = new EnumMap<>(UserRole.class);

    static {
        ROLE_PRIVILEGES.put(UserRole.CUSTOMER, EnumSet.of(
                Privilege.VIEW_ITEMS,
                Privilege.MANAGE_CART,
                Privilege.CREATE_ORDER,
                Privilege.VIEW_OWN_ORDERS,
                Privilege.VIEW_NOTIFICATIONS,
                Privilege.MARK_NOTIFICATION_READ
        ));

        ROLE_PRIVILEGES.put(UserRole.SHOP, EnumSet.of(
                Privilege.VIEW_ITEMS,
                Privilege.VIEW_ALL_ORDERS,
                Privilege.MANAGE_ORDER_STATUS,
                Privilege.VIEW_NOTIFICATIONS,
                Privilege.MARK_NOTIFICATION_READ,
                Privilege.VIEW_SHOP_NOTIFICATIONS
        ));

        ROLE_PRIVILEGES.put(UserRole.COURIER, EnumSet.of(
                Privilege.VIEW_ITEMS,
                Privilege.VIEW_OWN_ORDERS,
                Privilege.MANAGE_COURIER_AVAILABILITY,
                Privilege.VIEW_NOTIFICATIONS,
                Privilege.MARK_NOTIFICATION_READ
        ));

        ROLE_PRIVILEGES.put(UserRole.ADMIN, EnumSet.allOf(Privilege.class));
    }

    public static Set<Privilege> getPrivilegesForRole(UserRole role) {
        Set<Privilege> privileges = ROLE_PRIVILEGES.get(role);
        return privileges != null ? privileges : EnumSet.noneOf(Privilege.class);
    }

    public static boolean hasPrivilege(UserRole role, Privilege privilege) {
        return getPrivilegesForRole(role).contains(privilege);
    }
}
