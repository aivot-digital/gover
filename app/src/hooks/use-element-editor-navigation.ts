import {useCallback, useMemo, useSyncExternalStore} from 'react';
import {DefaultTabs} from '../components/element-editor/default-tabs';
import {isStringNullOrEmpty} from '../utils/string-utils';

function subscribeToHashChange(onStoreChange: () => void): () => void {
    window.addEventListener('hashchange', onStoreChange);

    return () => {
        window.removeEventListener('hashchange', onStoreChange);
    };
}

function getHashSnapshot(): string {
    return window.location.hash ?? '';
}

function getHashServerSnapshot(): string {
    return '';
}

function useElementEditorHash(): string {
    return useSyncExternalStore(subscribeToHashChange, getHashSnapshot, getHashServerSnapshot);
}

function updateHash(hash: string, replace = true): void {
    const normalizedHash = hash.startsWith('#') ? hash : `#${hash}`;
    const targetHash = normalizedHash === '#' ? '' : normalizedHash;

    if (window.location.hash === targetHash) {
        return;
    }

    const nextUrl = `${window.location.pathname}${window.location.search}${targetHash}`;

    if (replace) {
        window.history.replaceState(window.history.state, '', nextUrl);
        window.dispatchEvent(new HashChangeEvent('hashchange'));
        return;
    }

    window.location.hash = targetHash;
}

export function useElementEditorNavigationState() {
    const locationHash = useElementEditorHash();

    const hashValue: string | null = useMemo(() => {
        if (locationHash == null) {
            return null;
        }
        const trimmed = locationHash.replace('#', '').trim();

        return isStringNullOrEmpty(trimmed) ? null : trimmed;
    }, [locationHash]);

    const currentEditedElementId: string | null = useMemo(() => {
        return getCurrentEditedElementId(hashValue ?? '');
    }, [hashValue]);

    const currentEditorTab: string | null = useMemo(() => {
        const tabInHash = locationHash.split('/')[1];
        return tabInHash ?? null;
    }, [locationHash]);

    return {
        currentEditedElementId,
        currentEditorTab,
    };
}

export function useElementEditorNavigationActions() {
    const navigateToEditorTab = useCallback((tab: string): void => {
        const currentEditedElementId = getCurrentEditedElementId(window.location.hash ?? '');
        if (currentEditedElementId == null) {
            return;
        }

        updateHash(`#${currentEditedElementId}/${tab}`);
    }, []);

    const navigateToElementEditor = useCallback((elementId: string, tab?: string | null): void => {
        updateHash(createElementEditorNavigationLink(elementId, tab));
    }, []);

    const closeElementEditor = useCallback((): void => {
        updateHash('#');
    }, []);

    return {
        navigateToEditorTab,
        navigateToElementEditor,
        closeElementEditor,
    };
}

export function useElementEditorNavigation() {
    const navigationState = useElementEditorNavigationState();
    const navigationActions = useElementEditorNavigationActions();

    return {
        ...navigationState,
        ...navigationActions,
    };
}

function getCurrentEditedElementId(locationHash: string): string | null {
    if (locationHash == null) {
        return null;
    }

    const trimmed = locationHash.replace('#', '').trim();
    if (isStringNullOrEmpty(trimmed)) {
        return null;
    }

    return trimmed.split('/')[0];
}

export function createElementEditorNavigationLink(elementId: string, tab?: string | null): string {
    if (tab === null) {
        return `#${elementId}`;
    }
    return `#${elementId}/${tab ?? DefaultTabs.properties}`;
}
