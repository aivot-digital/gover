package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessVersionRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

/**
 * Restricts changes to the definition of a process version to drafts.
 * <p>
 * The workflow engine executes the nodes and edges of a process version directly. Changing a published version would
 * therefore alter new and running process instances without the publish permission and without the validation that
 * publishing performs. Published and revoked versions only change their status through publishing and revoking.
 */
@Service
public class ProcessVersionDraftGuard {
    private final ProcessVersionRepository processVersionRepository;

    public ProcessVersionDraftGuard(ProcessVersionRepository processVersionRepository) {
        this.processVersionRepository = processVersionRepository;
    }

    public void requireDrafted(@Nullable Integer processId, @Nullable Integer processVersion) throws ResponseException {
        if (processId == null || processVersion == null) {
            throw ResponseException.badRequest("Die angegebene Prozessversion existiert nicht.");
        }

        var version = processVersionRepository
                .findById(ProcessVersionEntityId.of(processId, processVersion))
                .orElseThrow(() -> ResponseException.badRequest("Die angegebene Prozessversion existiert nicht."));

        requireDrafted(version);
    }

    public static void requireDrafted(@Nonnull ProcessVersionEntity version) throws ResponseException {
        if (version.getStatus() != ProcessVersionStatus.Drafted) {
            throw ResponseException.conflict("Veröffentlichte und zurückgezogene Prozessversionen können nicht bearbeitet werden. Nehmen Sie Änderungen in einem Entwurf vor.");
        }
    }
}
