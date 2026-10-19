import React, {useEffect, useMemo, useState} from 'react';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {CodeListsApiService} from '../code-lists-api-service';
import {CodeList} from '../models/code-list';

interface CodeListSelectFieldProps {
    label?: string;
    hint?: string;
    value: string | null | undefined;
    onChange: (value: string | undefined) => void;
    disabled?: boolean;
    required?: boolean;
}

export function CodeListSelectField(props: CodeListSelectFieldProps) {
    const {
        label,
        hint,
        value,
        onChange,
        disabled,
        required,
    } = props;
    const [codeLists, setCodeLists] = useState<CodeList[]>([]);
    const [loading, setLoading] = useState(false);
    const [loaded, setLoaded] = useState(false);

    useEffect(() => {
        let active = true;
        setLoading(true);
        setLoaded(false);

        new CodeListsApiService()
            .listAllOrdered('name', 'ASC')
            .then((page) => {
                if (active) {
                    setCodeLists(page.content);
                    setLoaded(true);
                }
            })
            .catch(() => {
                if (active) {
                    setCodeLists([]);
                    setLoaded(false);
                }
            })
            .finally(() => {
                if (active) {
                    setLoading(false);
                }
            });

        return () => {
            active = false;
        };
    }, []);

    useEffect(() => {
        if (!loaded || disabled === true || value == null) {
            return;
        }

        if (codeLists.some((codeList) => codeList.key === value)) {
            return;
        }

        // Remove stale references so deleted codelists behave like a missing codelist selection.
        onChange(undefined);
    }, [codeLists, disabled, loaded, onChange, value]);

    const options = useMemo(() => {
        return codeLists.map((codeList) => ({
            label: codeList.name,
            subLabel: codeList.description || codeList.key,
            value: codeList.key,
        }));
    }, [codeLists]);

    return (
        <SelectFieldComponent
            label={label ?? 'Codeliste'}
            hint={hint}
            value={value}
            onChange={(value) => onChange(value ?? undefined)}
            options={options}
            disabled={disabled}
            required={required}
            emptyStatePlaceholder={loading ? 'Codelisten werden geladen...' : 'Keine Codelisten verfügbar'}
        />
    );
}
