import {useSearchParams} from 'react-router-dom';
import {useEffect} from 'react';
import {prefillQueryParamKey} from '../data/prefill-query-param-key';
import {isStringNullOrEmpty} from '../utils/string-utils';
import {useAppDispatch} from './use-app-dispatch';
import {selectAllElements, updateCustomerInput} from '../slices/app-slice';
import {useAppSelector} from './use-app-selector';
import {canPrefillElement} from '../dialogs/prefill-form-dialog/prefill-form-dialog';

export function usePrefill() {
    const dispatch = useAppDispatch();
    const [searchParams, setSearchParams] = useSearchParams();
    const allElements = useAppSelector(selectAllElements);

    useEffect(() => {
        const prefill = searchParams.get(prefillQueryParamKey);

        if (prefill == null || isStringNullOrEmpty(prefill)) {
            return;
        }

        const prefillData = JSON.parse(decodeURIComponent(prefill));

        if (prefillData == null || typeof prefillData !== 'object') {
            return;
        }

        for (const key of Object.keys(prefillData)) {
            const value = prefillData[key];

            const elem = (allElements ?? [])
                .find(e => e.id === key);

            if (elem != null && canPrefillElement(elem)) {
                dispatch(updateCustomerInput({
                    key,
                    value,
                }));
            }
        }

        searchParams.delete(prefillQueryParamKey);
        setSearchParams(searchParams);
    }, [searchParams]);
}