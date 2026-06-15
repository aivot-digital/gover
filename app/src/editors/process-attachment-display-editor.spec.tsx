import React from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import {ElementType} from '../data/element-type/element-type';
import type {ProcessAttachmentDisplayElement} from '../models/elements/form/content/process-attachment-display-element';
import {ProcessAttachmentDisplayEditor} from './process-attachment-display-editor';

describe('ProcessAttachmentDisplayEditor', () => {
    it('should patch the configured hint', () => {
        const onPatch = jest.fn();

        render(
            <ProcessAttachmentDisplayEditor
                element={createElement()}
                onPatch={onPatch}
                editable
                hasSummaryLayoutParent={false}
                scope="application"
            />,
        );

        fireEvent.change(screen.getByRole('textbox', {name: 'Hinweis'}), {
            target: {
                value: 'Bitte prüfen Sie den Anhang sorgfältig.',
            },
        });

        expect(onPatch).toHaveBeenCalledWith({
            hint: 'Bitte prüfen Sie den Anhang sorgfältig.',
        });
    });
});

function createElement(): ProcessAttachmentDisplayElement {
    return {
        id: 'attachment-display',
        type: ElementType.ProcessAttachmentDisplay,
        name: undefined,
        testProtocolSet: undefined,
        visibility: undefined,
        override: undefined,
        metadata: undefined,
        weight: 12,
        fileName: 'Erfolgsbescheid.pdf',
    };
}
