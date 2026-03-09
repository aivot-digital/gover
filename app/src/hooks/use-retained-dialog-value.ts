import {useEffect, useState} from 'react';

// Keep the last open-state value available until the dialog close transition has finished.
export function useRetainedDialogValue<T>(open: boolean, value: T): T {
    // React treats function values passed to useState/setState as lazy initializers/updaters.
    // Wrap the value so callbacks like filters are retained as plain values.
    const [retainedValue, setRetainedValue] = useState<T>(() => value);

    useEffect(() => {
        if (open) {
            setRetainedValue(() => value);
        }
    }, [open, value]);

    return open ? value : retainedValue;
}
