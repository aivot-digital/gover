import {type ElementEditorContentProps} from '../element-editor-content/element-editor-content-props';
import React from 'react';
import {type Form as Application} from '../../models/entities/form';
import {type RootElement} from '../../models/elements/root-element';
import {ApplicationInternalPublishTab} from './application-internal-publish-tab';

export function ApplicationPublishTab<T extends RootElement, E extends Application>(props: ElementEditorContentProps<T, E>): JSX.Element {
    return <ApplicationInternalPublishTab {...props} />;
}
