import {useEffect, useState} from 'react';
import {type AnyElement} from '../../../models/elements/any-element';
import {type ElementConfigurationErrors, ElementsApiService} from '../elements-api-service';

export const ELEMENT_CONFIGURATION_VALIDATION_DELAY_MS = 400;

/**
 * Validates the configuration of an element tree on the server while it is being edited, including unsaved changes.
 *
 * @returns The errors of the latest validated tree, or null before the first result and while disabled.
 */
export function useElementConfigurationErrors(element: AnyElement, enabled: boolean): ElementConfigurationErrors | null {
    const [errors, setErrors] = useState<ElementConfigurationErrors | null>(null);

    useEffect(() => {
        if (!enabled) {
            setErrors(null);
            return;
        }

        const controller = new AbortController();
        // Debounce edits so typing does not send the whole tree on every keystroke.
        const timeout = window.setTimeout(() => {
            new ElementsApiService()
                .validateConfiguration(element, controller.signal)
                .then((result) => {
                    // The response may still be parsed after a newer tree has replaced this one.
                    if (!controller.signal.aborted) {
                        setErrors(result);
                    }
                })
                .catch(() => {
                    // Keep the last result: these errors only guide editing, and a failed check must not interrupt it.
                });
        }, ELEMENT_CONFIGURATION_VALIDATION_DELAY_MS);

        return () => {
            window.clearTimeout(timeout);
            controller.abort();
        };
    }, [element, enabled]);

    return errors;
}
