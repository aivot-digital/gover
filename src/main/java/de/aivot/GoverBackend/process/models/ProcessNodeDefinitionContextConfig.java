package de.aivot.GoverBackend.process.models;

import de.aivot.GoverBackend.process.entities.ProcessEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntity;
import de.aivot.GoverBackend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;

/**
 * Context information for generating the configuration for a process node.
 *
 * @param user                     the user requesting the configuration.
 * @param processDefinition        the process definition the node belongs to.
 * @param processDefinitionVersion the version of the process definition.
 * @param thisNode                 the process node for which the configuration is being generated.
 */
public record ProcessNodeDefinitionContextConfig(
        @Nonnull UserEntity user,
        @Nonnull ProcessEntity processDefinition,
        @Nonnull ProcessVersionEntity processDefinitionVersion,
        @Nonnull ProcessNodeEntity thisNode
) {
}
