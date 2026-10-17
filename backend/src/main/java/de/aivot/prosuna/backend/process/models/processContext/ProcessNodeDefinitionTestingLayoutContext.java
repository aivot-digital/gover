package de.aivot.prosuna.backend.process.models.processContext;

import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessTestClaimEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.user.entities.UserEntity;
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
public record ProcessNodeDefinitionTestingLayoutContext<NodeConfig>(
        @Nonnull UserEntity user,
        @Nonnull ProcessEntity processDefinition,
        @Nonnull ProcessVersionEntity processDefinitionVersion,
        @Nonnull ProcessNodeEntity thisNode,
        @Nonnull ProcessTestClaimEntity testClaim,
        @Nonnull NodeConfig configuration
) {
}
