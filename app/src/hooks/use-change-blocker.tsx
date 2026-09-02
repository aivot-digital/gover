import {useChangeBlocker as useChangeBlocker2} from './use-change-blocker-2';

/**
 * @deprecated use the change blocker from the use-change-blocker-2 hook
 * @param original
 * @param edited
 * @param customTitle
 * @param customMessage
 * @param useDeepEquals
 */
export const useChangeBlocker = (
    original: any,
    edited: any,
    customTitle?: string,
    customMessage?: string,
    useDeepEquals: boolean = true,
) => useChangeBlocker2({
    original,
    edited,
    customTitle,
    customMessage,
    useDeepEquals,
});
