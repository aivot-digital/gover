import type {ProcessNodeProblems} from '../entities/process-node-problems';

export function includeNodeProblems(
    previouslyShownProblems: Readonly<Record<number, boolean>>,
    nodeProblems: ProcessNodeProblems[],
): Record<number, boolean> {
    const shownProblems = {
        ...previouslyShownProblems,
    };

    for (const problem of nodeProblems) {
        shownProblems[problem.node.id] = true;
    }

    return shownProblems;
}
