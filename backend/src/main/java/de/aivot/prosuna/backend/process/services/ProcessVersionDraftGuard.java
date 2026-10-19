package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessTestClaimRepository;
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
 * <p>
 * A draft with a test claim is executed by its test instances as well. While the claim exists, the nodes may still be
 * edited, but the structure of the flow (which nodes exist and how they are connected) must stay fixed.
 */
@Service
public class ProcessVersionDraftGuard {
    private final ProcessVersionRepository processVersionRepository;
    private final ProcessTestClaimRepository processTestClaimRepository;

    public ProcessVersionDraftGuard(ProcessVersionRepository processVersionRepository,
                                    ProcessTestClaimRepository processTestClaimRepository) {
        this.processVersionRepository = processVersionRepository;
        this.processTestClaimRepository = processTestClaimRepository;
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

    /**
     * Requires a draft without a test claim, for adding, replacing or removing nodes and for any change to edges.
     */
    public void requireStructureEditable(@Nullable Integer processId, @Nullable Integer processVersion) throws ResponseException {
        requireDrafted(processId, processVersion);

        if (processTestClaimRepository.existsByProcessIdAndProcessVersion(processId, processVersion)) {
            throw ResponseException.conflict("Im Testmodus können keine Prozesselemente hinzugefügt, ersetzt oder entfernt und keine Verbindungen geändert werden. Beenden Sie den Testmodus, um die Struktur des Prozesses zu ändern.");
        }
    }
}
