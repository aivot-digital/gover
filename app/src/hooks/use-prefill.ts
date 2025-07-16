import {useSearchParams} from 'react-router-dom';
import {useEffect} from 'react';
import {prefillQueryParamKey} from '../data/prefill-query-param-key';
import {isStringNullOrEmpty} from '../utils/string-utils';
import {selectAllElements} from '../slices/app-slice';
import {useAppSelector} from './use-app-selector';
import {canPrefillElement} from '../dialogs/prefill-form-dialog/prefill-form-dialog';
import {type ElementData, newElementDataObject} from '../models/element-data';

interface PrefillOptions {
    onPrefill: (prefillData: ElementData) => void;
}

export function usePrefill(options: PrefillOptions) {
    const [searchParams, setSearchParams] = useSearchParams();
    const allElements = useAppSelector(selectAllElements);

    const {
        onPrefill,
    } = options;

    useEffect(() => {
        const prefill = searchParams.get(prefillQueryParamKey);

        if (prefill == null || isStringNullOrEmpty(prefill)) {
            return;
        }

        const prefillData = JSON.parse(decodeURIComponent(prefill));

        if (prefillData == null || typeof prefillData !== 'object') {
            return;
        }

        if (!allElements) {
            return;
        }

        const cleanedPrefillData: ElementData = {};

        for (const key of Object.keys(prefillData)) {
            const value = prefillData[key];

            const elem = (allElements ?? [])
                .find(e => e.id === key);

            if (elem != null && canPrefillElement(elem)) {
                const dataObject = newElementDataObject(elem.type);
                dataObject.inputValue = value;
                cleanedPrefillData[key] = dataObject;
            }
        }

        searchParams.delete(prefillQueryParamKey);
        setSearchParams(searchParams);

        onPrefill(cleanedPrefillData);
    }, [searchParams, allElements, onPrefill]);
}