import {useSearchParams} from 'react-router-dom';
import {useEffect} from 'react';
import {prefillQueryParamKey} from '../data/prefill-query-param-key';
import {isStringNullOrEmpty} from '../utils/string-utils';
import {useAppDispatch} from './use-app-dispatch';
import {updateCustomerInput} from '../slices/app-slice';

export function usePrefill() {
    const dispatch = useAppDispatch();

    const [searchParams, setSearchParams] = useSearchParams();

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

            dispatch(updateCustomerInput({
                key,
                value,
            }));
        }

        searchParams.delete(prefillQueryParamKey);
        setSearchParams(searchParams);
    }, [searchParams]);
}