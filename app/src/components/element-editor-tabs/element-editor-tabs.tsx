import {Tab, Tabs} from '@mui/material';
import React, {useCallback} from 'react';
import {type ElementEditorTabsProps} from './element-editor-tabs-props';
import {type AnyElement} from '../../models/elements/any-element';
import {DefaultTabs} from '../element-editor/default-tabs';
import {ElementType} from '../../data/element-type/element-type';
import {ElementIsInput} from '../../data/element-type/element-is-input';

export function ElementEditorTabs<T extends AnyElement>(props: ElementEditorTabsProps<T>): JSX.Element {
    const handleTabChange = useCallback((_, newTab: string) => {
        props.onTabChange(newTab);
    }, [props.onTabChange]);

    return (
        <Tabs
            value={props.currentTab}
            onChange={handleTabChange}
            variant="scrollable"
            scrollButtons="auto"
        >
            <Tab
                label="Eigenschaften"
                value={DefaultTabs.properties}
            />
            {
                props.component.type !== ElementType.Root &&
                props.component.type !== ElementType.IntroductionStep &&
                props.component.type !== ElementType.SummaryStep &&
                props.component.type !== ElementType.SubmitStep &&
                <Tab
                    label="Sichtbarkeiten"
                    value={DefaultTabs.visibility}
                />
            }
            {
                ElementIsInput[props.component.type] &&
                <Tab
                    label="Validierungen"
                    value={DefaultTabs.validation}
                />
            }
            {
                props.additionalTabs.map((add) => (
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

            {
                props.rootEditor &&
                <Tab
                    label="Veröffentlichen"
                    value={DefaultTabs.publish}
                />
            }

            {
                ElementIsInput[props.component.type] &&
                <Tab
                    label="Dynamischer Wert"
                    value={DefaultTabs.value}
                />
            }

            {
                props.component.type !== ElementType.Root &&
                props.component.type !== ElementType.IntroductionStep &&
                props.component.type !== ElementType.SummaryStep &&
                props.component.type !== ElementType.SubmitStep &&
                <Tab
                    label="Dynamische Struktur"
                    value={DefaultTabs.patch}
                />
            }

            {
                ElementIsInput[props.component.type] &&
                <Tab
                    label="Metadaten"
                    value={DefaultTabs.metadata}
                />
            }

            <Tab
                label="Elementstruktur"
                value={DefaultTabs.structure}
            />
        </Tabs>
    );
}
