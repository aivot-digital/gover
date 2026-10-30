import React from 'react';
import {fireEvent, render, screen, within} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {
    ProcessAttachmentDisplayComponent,
    type ProcessAttachmentDisplayItem,
} from './process-attachment-display-component';

describe('ProcessAttachmentDisplayComponent', () => {
    it('formats byte sizes like the file upload and distinguishes empty files from unknown sizes', () => {
        render(<ProcessAttachmentDisplayComponent items={[
            {...createItem('1', 'empty.pdf'), sizeInBytes: 0},
            {...createItem('2', 'small.pdf'), sizeInBytes: 245000},
            {...createItem('3', 'large.pdf'), sizeInBytes: 1520000},
            {...createItem('4', 'unknown.pdf'), sizeInBytes: null},
            createItem('5', 'legacy.pdf'),
        ]}/>);
        const rows = screen.getAllByRole('listitem');
        expect(rows[0]).toHaveTextContent('0 Bytes · Hochgeladen als OriginalFileName');
        expect(rows[1]).toHaveTextContent('245 KB · Hochgeladen als OriginalFileName');
        expect(rows[2]).toHaveTextContent('1.52 MB · Hochgeladen als OriginalFileName');
        expect(rows[3]).not.toHaveTextContent(/Bytes|KB|MB|·/);
        expect(rows[4]).not.toHaveTextContent(/Bytes|KB|MB|·/);
    });

    it('uses one accessible file list and associates its label and hints', () => {
        render(<ProcessAttachmentDisplayComponent labelText="Unterlagen" hintText="Bitte prüfen."
                                                  previewText="Vorschau" items={[createItem('1', 'A.pdf'), createItem('2', 'B.pdf')]}/>);
        const group = screen.getByRole('group', {name: 'Unterlagen'});
        expect(group).toHaveAccessibleDescription('Bitte prüfen. Vorschau');
        expect(within(group).getAllByRole('list')).toHaveLength(1);
        expect(within(group).getAllByRole('listitem')).toHaveLength(2);
        expect(within(group).queryByRole('button')).not.toBeInTheDocument();
        expect(screen.queryByText(/optional/)).not.toBeInTheDocument();
    });

    it('keeps view and download actions separate and retains long original names', () => {
        const onView = vi.fn();
        const onDownload = vi.fn();
        const originalFileName = 'original-'.repeat(20) + '.pdf';
        render(<ProcessAttachmentDisplayComponent items={[{
            ...createItem('1', 'Bescheid.pdf'), originalFileName, onView, onDownload,
        }]}/>);
        fireEvent.click(screen.getByRole('button', {name: 'Bescheid.pdf ansehen'}));
        fireEvent.click(screen.getByRole('button', {name: 'Bescheid.pdf in neuem Tab ansehen'}));
        expect(onView).toHaveBeenCalledTimes(2);
        expect(onDownload).not.toHaveBeenCalled();
        fireEvent.click(screen.getByRole('button', {name: 'Bescheid.pdf herunterladen'}));
        expect(onDownload).toHaveBeenCalledTimes(1);
        expect(screen.getByText(`Hochgeladen als ${originalFileName}`)).toHaveStyle({overflowWrap: 'anywhere'});
        expect(screen.queryByText(/MB|KB/)).not.toBeInTheDocument();
    });

    it('exposes loading and empty states without upload controls', () => {
        const {rerender} = render(<ProcessAttachmentDisplayComponent loading statusText="Anhänge werden geladen..."/>);
        expect(screen.getByRole('group', {name: 'Anlagensatz-Anzeige'})).toHaveAttribute('aria-busy', 'true');
        expect(screen.getByRole('status')).toHaveTextContent('Anhänge werden geladen...');
        rerender(<ProcessAttachmentDisplayComponent labelText=" "/>);
        expect(screen.getByRole('group', {name: 'Anlagensatz-Anzeige'})).not.toHaveAttribute('aria-busy', 'true');
        expect(screen.getByRole('status')).toHaveTextContent('Keine Anhänge vorhanden.');
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('preserves grouping and first-occurrence order for interleaved attachments', () => {
        render(<ProcessAttachmentDisplayComponent items={[
            createItem('1', 'first.pdf'),
            createItem('2', 'group-first.pdf', ' A '),
            createItem('3', 'middle.pdf', ' '),
            createItem('4', 'group-second.pdf', 'A'),
            createItem('5', 'last.pdf'),
        ]}/>);
        const lists = screen.getAllByRole('list');
        expect(lists).toHaveLength(3);
        expect(within(lists[0]).getByText('first.pdf')).toBeInTheDocument();
        expect(within(lists[1]).getAllByRole('listitem')).toHaveLength(2);
        expect(within(lists[2]).getAllByRole('listitem')).toHaveLength(2);
        expect(screen.getAllByRole('group', {name: 'Anlagengruppe'})).toHaveLength(1);
    });

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

    it('should render attachments with the same group in one file list', () => {
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

    it('should render attachments with different groups in separate file lists', () => {
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

    it('should keep ungrouped attachments outside named groups', () => {
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
        expect(group).not.toContainElement(screen.getByText('root.pdf'));
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
