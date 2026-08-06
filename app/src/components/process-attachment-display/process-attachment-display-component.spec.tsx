import React from 'react';
import {render, screen, within} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {
    ProcessAttachmentDisplayComponent,
    type ProcessAttachmentDisplayItem,
} from './process-attachment-display-component';

describe('ProcessAttachmentDisplayComponent', () => {
    it('should render attachments without groups flat', () => {
        render(
            <ProcessAttachmentDisplayComponent
                items={[
                    createItem('1', 'birth-certificate.pdf'),
                    createItem('2', 'xray.pdf'),
                ]}
            />,
        );

        expect(screen.getByText('birth-certificate.pdf')).toBeInTheDocument();
        expect(screen.getByText('xray.pdf')).toBeInTheDocument();
        expect(screen.queryByRole('group', {name: 'Anlagengruppe'})).not.toBeInTheDocument();
    });

    it('should render attachments with the same group in one outlined group', () => {
        render(
            <ProcessAttachmentDisplayComponent
                items={[
                    createItem('1', 'xray-1.pdf', 'person-1/dog-1'),
                    createItem('2', 'xray-2.pdf', 'person-1/dog-1'),
                ]}
            />,
        );

        const group = screen.getByRole('group', {name: 'Anlagengruppe'});

        expect(within(group).getByText('xray-1.pdf')).toBeInTheDocument();
        expect(within(group).getByText('xray-2.pdf')).toBeInTheDocument();
    });

    it('should render attachments with different groups in separate outlined groups', () => {
        render(
            <ProcessAttachmentDisplayComponent
                items={[
                    createItem('1', 'dog-1.pdf', 'person-1/dog-1'),
                    createItem('2', 'dog-2.pdf', 'person-1/dog-2'),
                ]}
            />,
        );

        const groups = screen.getAllByRole('group', {name: 'Anlagengruppe'});

        expect(groups).toHaveLength(2);
        expect(within(groups[0]).getByText('dog-1.pdf')).toBeInTheDocument();
        expect(within(groups[1]).getByText('dog-2.pdf')).toBeInTheDocument();
    });

    it('should keep ungrouped attachments outside outlined groups', () => {
        render(
            <ProcessAttachmentDisplayComponent
                items={[
                    createItem('1', 'root.pdf'),
                    createItem('2', 'dog.pdf', 'person-1/dog-1'),
                ]}
            />,
        );

        const group = screen.getByRole('group', {name: 'Anlagengruppe'});

        expect(within(group).getByText('dog.pdf')).toBeInTheDocument();
        expect(screen.getByText('root.pdf').closest('[role="group"]')).toBeNull();
    });
});

function createItem(
    key: string,
    fileName: string,
    group?: string,
): ProcessAttachmentDisplayItem {
    return {
        key,
        fileName,
        originalFileName: 'OriginalFileName',
        group,
    };
}
