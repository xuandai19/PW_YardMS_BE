package com.yms.yms.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class UserPermissionId implements Serializable {
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "permission_id")
    private UUID permissionId;

    public UserPermissionId(UUID userId, UUID permissionId) {
        this.userId = userId;
        this.permissionId = permissionId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof UserPermissionId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(permissionId, that.permissionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, permissionId);
    }
}
