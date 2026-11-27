import {type Dispatch, type SetStateAction, useEffect} from 'react';
import {type ProcessDefinitionEdgeEntity} from '../entities/process-definition-edge-entity';
import {type ProcessNodeEntity} from '../entities/process-node-entity';
import {type ProcessNodeProblems} from '../entities/process-node-problems';
import {ProcessDefinitionVersionApiService} from '../services/process-definition-version-api-service';
import {includeNodeProblems} from '../utils/include-node-problems';

/**
 * Validates the process version whenever its nodes or edges change and reveals the problems of nodes saved with errors.
 */
export function useAutomaticNodeValidation(
    processId: number,
    processVersion: number,
    processFlow: {nodes: ProcessNodeEntity[]; edges: ProcessDefinitionEdgeEntity[]} | null,
    setProcessNodeProblems: Dispatch<SetStateAction<ProcessNodeProblems[]>>,
    setShowProcessNodeProblemsForNodes: Dispatch<SetStateAction<Record<number, boolean>>>,
): void {
    useEffect(() => {
        if (processFlow == null || processVersion < 1) {
            setProcessNodeProblems([]);
            return;
        }

        // Responses can arrive out of order. Only apply the latest one, so an older result never replaces a newer one.
        let cancelled = false;

        new ProcessDefinitionVersionApiService()
            .validate({
                processDefinitionId: processId,
                processDefinitionVersion: processVersion,
            })
            .then((problems) => {
                if (cancelled) {
                    return;
                }

                const nodeProblems = problems.nodeProblems;
                setProcessNodeProblems(nodeProblems);

                if (nodeProblems.length === 0) {
                    return;
                }

                setShowProcessNodeProblemsForNodes((previousShownProblems) => includeNodeProblems(
                    previousShownProblems,
                    nodeProblems,
                    processFlow.nodes,
                ));
            });

        return () => {
            cancelled = true;
        };
    }, [processId, processVersion, processFlow?.nodes, processFlow?.edges]);
}
