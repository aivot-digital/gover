import {useLocation, useNavigate} from 'react-router-dom';
import {useCallback, useMemo} from 'react';
import {DefaultTabs} from '../components/element-editor/default-tabs';
import {isStringNullOrEmpty} from '../utils/string-utils';

export function useElementEditorNavigation() {
    const {
        hash: locationHash
    } = useLocation();

    const navigate = useNavigate();

    const hashValue: string | null = useMemo(() => {
        if (locationHash == null) {
            return null;
        }
        const trimmed = locationHash.replace('#', '').trim();

        return isStringNullOrEmpty(trimmed) ? null : trimmed;
    }, [locationHash]);

    const currentEditedElementId: string | null = useMemo(() => {
        if (hashValue == null) {
            return null;
        }

        return hashValue.split('/')[0];
    }, [hashValue]);

    const currentEditorTab: string = useMemo(() => {
        const tabInHash = locationHash.split('/')[1]
        return tabInHash ?? DefaultTabs.properties;
    }, [locationHash]);

    const navigateToEditorTab = useCallback((tab: string): void => {
        navigate(`#${currentEditedElementId}/${tab}`, {
            replace: true,
        });
    }, [currentEditedElementId]);

    const navigateToElementEditor = useCallback((elementId: string, tab?: string): void => {
        navigate(createElementEditorNavigationLink(elementId, tab), {
            replace: true,
        });
    }, []);

    const closeElementEditor = useCallback((): void => {
        navigate(`#`, {
            replace: true,
        });
    }, []);

    return {
        currentEditedElementId,
        currentEditorTab,
        navigateToEditorTab,
        navigateToElementEditor,
        closeElementEditor,
    };
}

export function createElementEditorNavigationLink(elementId: string, tab?: string): string {
    return `#${elementId}/${tab ?? DefaultTabs.properties}`;
}