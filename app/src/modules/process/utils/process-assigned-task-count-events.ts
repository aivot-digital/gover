export const PROCESS_ASSIGNED_TASK_COUNT_REFRESH_EVENT = 'process-assigned-task-count-refresh';

export function dispatchProcessAssignedTaskCountRefreshEvent(): void {
    if (typeof window === 'undefined') {
        return;
    }

    window.dispatchEvent(new Event(PROCESS_ASSIGNED_TASK_COUNT_REFRESH_EVENT));
}

export function subscribeProcessAssignedTaskCountRefreshEvent(listener: () => void): () => void {
    if (typeof window === 'undefined') {
        return () => undefined;
    }

    window.addEventListener(PROCESS_ASSIGNED_TASK_COUNT_REFRESH_EVENT, listener);

    return () => {
        window.removeEventListener(PROCESS_ASSIGNED_TASK_COUNT_REFRESH_EVENT, listener);
    };
}
