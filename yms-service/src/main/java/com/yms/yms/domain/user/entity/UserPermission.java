package com.yms.yms.domain.user.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_permissions")
@Getter
@Setter
@NoArgsConstructor
public class UserPermission {
    @EmbeddedId
    private UserPermissionId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @MapsId("permissionId")
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @Column(name = "effect", nullable = false, length = 10)
    private String effect = "GRANT";

    public UserPermission(User user, Permission permission, String effect) {
        this.id = new UserPermissionId(user.getId(), permission.getId());
        this.user = user;
        this.permission = permission;
        this.effect = effect;
    }
}
