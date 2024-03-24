import {shallowEquals} from '../utils/equality-utils';
import {unstable_usePrompt} from 'react-router-dom';

export function useChangeBlocker(original: any, edited: any): boolean {
    const hasChanged = !shallowEquals(original, edited);
    unstable_usePrompt({
        when: hasChanged,
        message: 'Sie haben ungespeicherte Änderungen. Möchten Sie diese Seite wirklich verlassen?',
    });
    return hasChanged;
}
