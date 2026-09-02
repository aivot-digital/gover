import {useId} from 'react';

/**
 * React-generated IDs may contain colons. Restricting them to selector-friendly characters keeps
 * derived IDs usable with DOM and CSS selector APIs without requiring escaping at every call site.
 * @return {string} A stable ID containing only selector-friendly characters.
 */
export function useNormalizedReactId(): string {
    return useId().replace(/[^A-Za-z0-9_-]/g, '');
}
