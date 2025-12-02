import {Box, Tab, Tabs} from '@mui/material';
import React, {useCallback} from 'react';
import {type ElementEditorTabsProps} from './element-editor-tabs-props';
import {type AnyElement} from '../../models/elements/any-element';
import {DefaultTabs} from '../element-editor/default-tabs';
import {ElementType} from '../../data/element-type/element-type';
import {ElementIsInput} from '../../data/element-type/element-is-input';

export function ElementEditorTabs<T extends AnyElement>(props: ElementEditorTabsProps<T>) {
    const handleTabChange = useCallback((_: any, newTab: string) => {
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
                (props.scope === 'application' || props.scope === 'preset') &&
                ElementIsInput[props.component.type] &&
                <Tab
                    label="Datenzuordnung"
                    value={DefaultTabs.metadata}
                />
            }

            {
                (props.scope === 'application' || props.scope === 'preset') &&
                props.additionalTabs.map((add) => (
                    <Tab
                        key={add.label}
                        label={add.label}
                        value={add.label}
                    />
                ))
            }

            <Box
                sx={{
                    height: 24,
                    alignSelf: 'center',
                    borderLeft: '1px solid',
                    borderColor: 'divider',
                    mx: 1,
                }}
            />

            {
                (props.scope === 'application' || props.scope === 'preset') &&
                props.component.type !== ElementType.Root &&
                props.component.type !== ElementType.IntroductionStep &&
                props.component.type !== ElementType.SummaryStep &&
                props.component.type !== ElementType.SubmitStep &&
                <Tab
                    label="Sichtbarkeit"
                    value={DefaultTabs.visibility}
                />
            }
            {
                ElementIsInput[props.component.type] &&
                <Tab
                    label="Validierung"
                    value={DefaultTabs.validation}
                />
            }

            {
                (props.scope === 'application' || props.scope === 'preset') &&
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
                (props.scope === 'application' || props.scope === 'preset') &&
                props.component.type !== ElementType.Root &&
                <Tab
                    label="Referenzen"
                    value={DefaultTabs.references}
                />
            }

            <Tab
                label="Elementstruktur"
                value={DefaultTabs.structure}
            />

            {
                (props.scope === 'application' || props.scope === 'preset') &&
                <Box
                    sx={{
                        height: 24,
                        alignSelf: 'center',
                        borderLeft: '1px solid',
                        borderColor: 'divider',
                        mx: 1,
                    }}
                />
            }

            {
                /* TODO: Check permissions for formAnnotate */
                (props.scope === 'application' || props.scope === 'preset') &&
                <Tab
                    label="Prüfung"
                    value={DefaultTabs.test}
                />
            }

            {
                /* TODO: Check permissions for formPublish */
                (props.scope === 'application' || props.scope === 'preset') &&
                props.rootEditor &&
                <Tab
                    label="Veröffentlichen"
                    value={DefaultTabs.publish}
                />
            }
        </Tabs>
    );
}
