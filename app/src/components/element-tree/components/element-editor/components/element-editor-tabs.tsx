import {Tab, Tabs} from '@mui/material';
import {ElementType} from '../../../../../data/element-type/element-type';
import {ElementIsInput} from '../../../../../data/element-type/element-is-input';
import React, {useCallback} from 'react';
import {DefaultTabs} from '../data/default-tabs';
import {ElementEditorTabsProps} from './element-editor-tabs-props';
import {AnyElement} from '../../../../../models/elements/any-element';

export function ElementEditorTabs<T extends AnyElement>({component, currentTab, onTabChange, additionalTabs}: ElementEditorTabsProps<T>) {
    const handleTabChange = useCallback((_, newTab: string) => onTabChange(newTab), [onTabChange]);

    return (
        <Tabs
            value={currentTab}
            onChange={handleTabChange}
        >
            <Tab
                label="Eigenschaften"
                value={DefaultTabs.properties}
            />
            {
                component.type !== ElementType.Root &&
                component.type !== ElementType.IntroductionStep &&
                component.type !== ElementType.SummaryStep &&
                component.type !== ElementType.SubmitStep &&
                <Tab
                    label="Sichtbarkeiten"
                    value={DefaultTabs.visibility}
                />
            }
            {
                ElementIsInput[component.type] &&
                <Tab
                    label="Validierungen"
                    value={DefaultTabs.validation}
                />
            }
            {
                component.type !== ElementType.Root &&
                component.type !== ElementType.IntroductionStep &&
                component.type !== ElementType.SummaryStep &&
                component.type !== ElementType.SubmitStep &&
                <Tab
                    label="Dynamische Werte"
                    value={DefaultTabs.patch}
                />
            }

            {
                additionalTabs.map(add => (
                    <Tab
                        key={add.label}
                        label={add.label}
                        value={add.label}
                    />
                ))
            }

            <Tab
                label="Prüfung"
                value={DefaultTabs.test}
            />

            <Tab
                label="Elementstrukturen"
                value={DefaultTabs.structure}
            />
        </Tabs>
    );
}
