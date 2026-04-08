import {AnyElement} from '../../../models/elements/any-element';
import {DerivedRuntimeElementData} from '../../../models/element-data';
import {Box, Typography} from '@mui/material';
import React, {useMemo, useState} from 'react';
import {ExpandableCodeBlock} from '../../expandable-code-block/expandable-code-block';
import {SearchInput} from '../../search-input/search-input';
import {filterAuthoredElementValues} from '../../../utils/element-data-utils';

interface ElementDataDebuggerProps {
    rootElement: AnyElement;
    derivedData: DerivedRuntimeElementData;
}

export function DerivedDataDebugger(props: ElementDataDebuggerProps) {
    const {
        rootElement,
        derivedData,
    } = props;

    const {
        effectiveValues,
        elementStates,
    } = derivedData;

    console.log(derivedData);

    const [elementIdSearch, setElementIdSearch] = useState<string>('');

    const effectiveValuesToDisplay = useMemo(() => {
        const search = elementIdSearch
            .toLowerCase()
            .trim();

        if (search.length === 0) {
            return effectiveValues;
        }

        return filterAuthoredElementValues(rootElement, effectiveValues, (e) => e.id.toLowerCase().includes(search));
    }, [rootElement, effectiveValues, elementIdSearch]);

    const effectiveValuesJsonString = useMemo(() => {
        return JSON.stringify(effectiveValuesToDisplay, null, 2);
    }, [effectiveValuesToDisplay]);

    const elementStatesToDisplay = useMemo(() => {
        const search = elementIdSearch
            .toLowerCase()
            .trim();

        if (search.length === 0) {
            return elementStates;
        }

        return filterAuthoredElementValues(rootElement, elementStates, (e) => e.id.toLowerCase().includes(search));
    }, [rootElement, elementStates, elementIdSearch]);

    const elementStatesJsonString = useMemo(() => {
        return JSON.stringify(elementStatesToDisplay, null, 2);
    }, [elementStatesToDisplay]);

    return (
        <Box>
            <Box
                sx={{
                    display: 'flex',
                    mb: 2,
                }}
            >
                <SearchInput
                    label="Element-ID suchen"
                    placeholder="Element-ID eingeben"
                    value={elementIdSearch}
                    onChange={setElementIdSearch}
                />
            </Box>

            <Typography variant="h6">
                Effektive Werte
            </Typography>

            <ExpandableCodeBlock
                value={effectiveValuesJsonString}
            />

            <Typography variant="h6">
                Berechnete Eigenschaften
            </Typography>

            <ExpandableCodeBlock
                value={elementStatesJsonString}
            />
        </Box>
    );
}
