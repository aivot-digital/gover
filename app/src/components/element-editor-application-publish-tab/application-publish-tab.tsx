import {type ElementEditorContentProps} from '../element-editor-content/element-editor-content-props';
import React from 'react';
import {type RootElement} from '../../models/elements/root-element';
import {ApplicationInternalPublishTab} from './application-internal-publish-tab';
import {LoadedForm} from '../../slices/app-slice';

export function ApplicationPublishTab<T extends RootElement, E extends LoadedForm>(props: ElementEditorContentProps<T, E>) {
    return <ApplicationInternalPublishTab {...props} />;
}
