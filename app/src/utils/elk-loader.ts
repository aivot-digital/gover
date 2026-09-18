import {type ELK} from 'elkjs/lib/elk.bundled.js';
import {withAsyncWrapper} from './with-async-wrapper';

export class ElkLoadError extends Error {
    constructor(cause: unknown) {
        super('Could not load the diagram layout engine', {cause});
        this.name = 'ElkLoadError';
    }
}

// Keep each diagram's own ELK instance, but share its initialization between
// concurrent layout requests. A failed download must not poison later attempts.
export function createElkLoader(): () => Promise<ELK> {
    let instancePromise: Promise<ELK> | undefined;
    return () => {
        instancePromise ??= withAsyncWrapper({
            desiredMinRuntime: 500,
            // Delay initial loading and retries, including failures, but not subsequent layouts.
            main: () => Promise.allSettled([
                import('elkjs/lib/elk.bundled.js').then(({default: ELK}) => new ELK()),
            ]),
        })
            .then(([result]) => {
                if (result.status === 'rejected') {
                    throw result.reason;
                }
                return result.value;
            })
            .catch((error: unknown) => {
                instancePromise = undefined;
                throw new ElkLoadError(error);
            });
        return instancePromise;
    };
}
