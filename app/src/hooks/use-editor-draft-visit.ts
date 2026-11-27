import {useCallback, useLayoutEffect, useMemo, useRef, useState} from 'react';

/** A second visit to the same object needs its own draft decision, even after A → B → A. */
export function useEditorDraftVisit(key: string | null) {
    const visit = useMemo(() => key == null ? null : {key}, [key]);
    const active = useRef<typeof visit>(null);
    const [checkedVisit, setCheckedVisit] = useState<typeof visit>(null);
    useLayoutEffect(() => {
        active.current = visit;
        return () => { active.current = null; };
    }, [visit]);

    const isCurrent = useCallback(() => visit != null && active.current === visit, [visit]);
    const finish = useCallback(() => {
        if (isCurrent()) setCheckedVisit(visit);
    }, [isCurrent, visit]);
    const checked = visit != null && checkedVisit === visit;
    const canEdit = useCallback(() => checked && isCurrent(), [checked, isCurrent]);
    return {checked, isCurrent, finish, canEdit};
}
