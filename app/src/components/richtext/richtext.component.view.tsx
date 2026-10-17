import {RichtextElement} from '../../models/elements/form/content/richtext-element';
import {BaseViewProps} from '../../views/base-view';
import React from 'react';
import {RichtextComponent} from './richtext.component';

export function RichtextComponentView({element}: BaseViewProps<RichtextElement, void>) {
    return (
        <RichtextComponent
            content={element.content}
            className={'richtext-component-content content-without-margin-on-childs'}
            sx={{
                typography: 'body2',
            }}
        />
    );
}
