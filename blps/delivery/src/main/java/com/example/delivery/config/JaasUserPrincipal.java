package com.example.delivery.config;

import java.security.Principal;

import com.example.delivery.model.UserRole;


public class JaasUserPrincipal implements Principal {

    private final String name;
    private final UserRole role;
    private final Long userId;

    public JaasUserPrincipal(String name, UserRole role, Long userId) {
        this.name = name;
        this.role = role;
        this.userId = userId;
    }

    @Override
    public String getName() {
        return name;
    }

    public UserRole getRole() {
        return role;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return "JaasUserPrincipal{name='" + name + "', role=" + role + ", userId=" + userId + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JaasUserPrincipal that = (JaasUserPrincipal) o;

        if (!name.equals(that.name)) return false;
        if (role != that.role) return false;
        return userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + role.hashCode();
        result = 31 * result + userId.hashCode();
        return result;
    }
}