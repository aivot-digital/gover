import {useRef} from 'react';

// Keep the last open-state value available until the dialog close transition has finished.
export function useRetainedDialogValue<T>(open: boolean, value: T): T {
    const retainedValueRef = useRef(value);

    // Snapshot the latest open-state value during render. This avoids state/effect feedback
    // loops for unstable values such as callbacks or freshly created ReactNodes.
    if (open) {
        retainedValueRef.current = value;
    }

    return open ? value : retainedValueRef.current;
}
