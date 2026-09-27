import type {ProcessNodeProblems} from '../entities/process-node-problems';
import type {ProcessNodeEntity} from '../entities/process-node-entity';

export function includeNodeProblems(
    previouslyShownProblems: Readonly<Record<number, boolean>>,
    nodeProblems: ProcessNodeProblems[],
    processNodes: ReadonlyArray<Pick<ProcessNodeEntity, 'id' | 'savedWithErrors'>>,
): Record<number, boolean> {
    const savedWithErrorsNodeIds = new Set(
        processNodes.filter((node) => node.savedWithErrors).map((node) => node.id),
    );
    let shownProblems: Record<number, boolean> | null = null;

    for (const problem of nodeProblems) {
        if (!savedWithErrorsNodeIds.has(problem.node.id) || previouslyShownProblems[problem.node.id] === true) {
            continue;
        }

        shownProblems ??= {...previouslyShownProblems};
        shownProblems[problem.node.id] = true;
    }

    return shownProblems ?? previouslyShownProblems;
}
