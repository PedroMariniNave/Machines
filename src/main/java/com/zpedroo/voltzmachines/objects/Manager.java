package com.zpedroo.voltzmachines.objects;

import com.zpedroo.voltzmachines.utils.enums.Permission;

import java.util.List;
import java.util.UUID;

public class Manager {

    private UUID uuid;
    private List<Permission> permissions;

    public Manager(UUID uuid, List<Permission> permissions) {
        this.uuid = uuid;
        this.permissions = permissions;
    }

    public UUID getUUID() {
        return uuid;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public Boolean can(Permission permission) {
        return getPermissions().contains(permission);
    }

    public void set(Permission permission, Boolean status) {
        if (status) {
            getPermissions().add(permission);
        } else {
            getPermissions().remove(permission);
        }
    }
}