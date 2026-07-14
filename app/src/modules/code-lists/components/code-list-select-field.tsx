import React, {useEffect, useMemo, useState} from 'react';
import {SelectFieldComponent} from '../../../components/select-field-2/select-field-component';
import {CodeListsApiService} from '../code-lists-api-service';
import {CodeList} from '../models/code-list';

interface CodeListSelectFieldProps {
    label?: string;
    hint?: string;
    value: number | null | undefined;
    onChange: (value: number | null) => void;
    disabled?: boolean;
    required?: boolean;
}

export function CodeListSelectField(props: CodeListSelectFieldProps) {
    const [codeLists, setCodeLists] = useState<CodeList[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        let active = true;
        setLoading(true);

        new CodeListsApiService()
            .listAllOrdered('name', 'ASC')
            .then((page) => {
                if (active) {
                    setCodeLists(page.content);
                }
            })
            .catch(() => {
                if (active) {
                    setCodeLists([]);
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

    const options = useMemo(() => {
        return codeLists.map((codeList) => ({
            label: codeList.name,
            subLabel: codeList.description,
            value: codeList.id,
        }));
    }, [codeLists]);

    return (
        <SelectFieldComponent
            label={props.label ?? 'Code-Liste'}
            hint={props.hint}
            value={props.value}
            onChange={(value) => props.onChange(value)}
            options={options}
            disabled={props.disabled}
            required={props.required}
            emptyStatePlaceholder={loading ? 'Code-Listen werden geladen...' : 'Keine Code-Listen verfügbar'}
        />
    );
}
