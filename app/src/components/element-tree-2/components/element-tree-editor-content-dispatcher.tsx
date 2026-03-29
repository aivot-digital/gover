import React from 'react';
import {AnyElement} from '../../../models/elements/any-element';
import {DefaultTabs} from '../../element-editor/default-tabs';
import {StructureTab} from '../../element-editor-structure-tab/structure-tab';
import {ElementTreeEditorContentTabProperties} from './element-tree-editor-content-tab-properties';
import {ElementTreeEditorContentTabVisibility} from './element-tree-editor-content-tab-visibility';
import {ElementTreeEditorContentTabValidation} from './element-tree-editor-content-tab-validation';
import {ElementTreeEditorContentTabValue} from './element-tree-editor-content-tab-value';
import {ElementTreeEditorContentTabOverride} from './element-tree-editor-content-tab-override';

export interface ElementTreeEditorContentDispatcherProps<T extends AnyElement> {
    element: T;
    currentTab: string;
    onChange: (update: Partial<T>) => void;
    editable: boolean;
}

export function ElementTreeEditorContentDispatcher<T extends AnyElement>(props: ElementTreeEditorContentDispatcherProps<T>): React.ReactNode | null {
    const {
        element,
        currentTab,
        editable,
        onChange,
    } = props;

    switch (currentTab) {
        case DefaultTabs.properties:
            return <ElementTreeEditorContentTabProperties/>;
        case DefaultTabs.visibility:
            return <ElementTreeEditorContentTabVisibility/>;
        case DefaultTabs.validation:
            return <ElementTreeEditorContentTabValidation/>;
        case DefaultTabs.value:
            return <ElementTreeEditorContentTabValue/>;
        case DefaultTabs.patch:
            return <ElementTreeEditorContentTabOverride/>;
        case DefaultTabs.structure:
            return (
                <StructureTab
                    elementModel={element}
                    onChange={onChange}
                    editable={editable}
                />
            );
        default:
            return null;
    }
}
