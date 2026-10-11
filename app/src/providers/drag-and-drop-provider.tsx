import {type PropsWithChildren} from 'react';
import {DndProvider} from 'react-dnd';
import {HTML5Backend} from 'react-dnd-html5-backend';

/**
 * Provides the single HTML5 drag-and-drop backend for a shell.
 * Mount it once at the shell root instead of around individual editors or dialogs.
 */
export function DragAndDropProvider({children}: PropsWithChildren) {
    // An explicit window context keeps React DnD from reference counting its default
    // global manager. That manager is discarded whenever its count drops to zero, for
    // example during StrictMode effect replays, and the next render then switches all
    // drag sources to a new manager and HTML5 backend, cancelling running drags.
    return (
        <DndProvider
            backend={HTML5Backend}
            context={window}
        >
            {children}
        </DndProvider>
    );
}
