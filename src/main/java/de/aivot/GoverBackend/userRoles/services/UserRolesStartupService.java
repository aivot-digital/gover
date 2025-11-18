package de.aivot.GoverBackend.userRoles.services;

import de.aivot.GoverBackend.userRoles.entities.UserRoleEntity;
import de.aivot.GoverBackend.userRoles.properties.RoleProperties;
import de.aivot.GoverBackend.userRoles.repositories.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
public class UserRolesStartupService implements ApplicationListener<ApplicationReadyEvent> {
    private final Logger logger = LoggerFactory.getLogger(UserRolesStartupService.class);

    private final RoleProperties roleProperties;
    private final UserRoleRepository userRoleRepository;

    @Autowired
    public UserRolesStartupService(RoleProperties roleProperties, UserRoleRepository userRoleRepository) {
        this.roleProperties = roleProperties;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public void onApplicationEvent(@Nonnull ApplicationReadyEvent event) {
        var roles = roleProperties.getRoles();
        if (roles == null) {
            logger.info("No roles defined in configuration.");
            return;
        }

        for (var role : roles) {
            var rolesMatchingName = userRoleRepository
                    .findAllByName(role.getName());

            if (rolesMatchingName.isEmpty()) {
                var newRole = new UserRoleEntity()
                        .setName(role.getName())
                        .setDescription(role.getDescription())
                        .setFormPermission(role.getFormPermission());

                userRoleRepository
                        .save(newRole);

                logger.info("Created role '{}'.", role.getName());
            } else {
                var existingRole = rolesMatchingName.get(0);

                existingRole
                        .setDescription(role.getDescription())
                        .setFormPermission(role.getFormPermission());

                userRoleRepository
                        .save(existingRole);
            }
        }
    }
}
