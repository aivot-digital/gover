package de.aivot.GoverBackend.process.models;

import de.aivot.GoverBackend.process.entities.ProcessEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessTestClaimEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntity;
import de.aivot.GoverBackend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;

/**
 * Context information for generating the layout for a process node testing.
 *
 * @param user                     the user requesting the layout.
 * @param processDefinition        the process definition the node belongs to.
 * @param processDefinitionVersion the version of the process definition.
 * @param thisNode                 the process node for which the layout is being generated.
 * @param testClaim                the test claim the layout is being generated for testing purposes.
 */
public record ProcessNodeDefinitionContextTesting(
        @Nonnull UserEntity user,
        @Nonnull ProcessEntity processDefinition,
        @Nonnull ProcessVersionEntity processDefinitionVersion,
        @Nonnull ProcessNodeEntity thisNode,
        @Nonnull ProcessTestClaimEntity testClaim
) {
}
