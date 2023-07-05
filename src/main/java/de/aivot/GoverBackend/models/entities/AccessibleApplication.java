package de.aivot.GoverBackend.models.entities;

import de.aivot.GoverBackend.enums.UserRole;
import org.hibernate.annotations.Immutable;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import java.io.Serializable;
import java.util.Objects;

@Immutable
@Entity(name = "accessible_applications")
public class AccessibleApplication {
    @EmbeddedId
    private AccessibleApplicationKey key;

    private UserRole role;

    @Embeddable
    public static class AccessibleApplicationKey implements Serializable {
        private Integer membershipId;

        private Integer applicationId;

        private Integer departmentId;

        private Integer userId;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            AccessibleApplicationKey that = (AccessibleApplicationKey) o;
            return Objects.equals(membershipId, that.membershipId) && Objects.equals(applicationId, that.applicationId) && Objects.equals(departmentId, that.departmentId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(membershipId, applicationId, departmentId, userId);
        }

        // region Getters & Setters

        public Integer getMembershipId() {
            return membershipId;
        }

        public void setMembershipId(Integer membershipId) {
            this.membershipId = membershipId;
        }

        public Integer getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(Integer applicationId) {
            this.applicationId = applicationId;
        }

        public Integer getDepartmentId() {
            return departmentId;
        }

        public void setDepartmentId(Integer departmentId) {
            this.departmentId = departmentId;
        }

        public Integer getUserId() {
            return userId;
        }

        public void setUserId(Integer userId) {
            this.userId = userId;
        }


        // endregion
    }

    // region Getters & Setters

    public AccessibleApplicationKey getKey() {
        return key;
    }

    public void setKey(AccessibleApplicationKey key) {
        this.key = key;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }


    // endregion
}
