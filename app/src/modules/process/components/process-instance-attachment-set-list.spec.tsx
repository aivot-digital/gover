import React from 'react';
import {render, screen, within} from '@testing-library/react';
import {
    ProcessInstanceAttachmentSetList,
    type ProcessInstanceAttachmentSetListItem,
} from './process-instance-attachment-set-list';
import type {ProcessInstanceAttachmentEntity} from '../entities/process-instance-attachment-entity';
import type {ProcessInstanceAttachmentSetEntity} from '../entities/process-instance-attachment-set-entity';
import {describe, expect, it} from 'vitest';

describe('ProcessInstanceAttachmentSetList', () => {
    it('should render attachments without groups flat', () => {
        render(
            <ProcessInstanceAttachmentSetList
                items={[
                    createItem([
                        createAttachment('1', 'birth-certificate.pdf'),
                        createAttachment('2', 'xray.pdf'),
                    ]),
                ]}
            />,
        );

        expect(screen.getByText('birth-certificate.pdf')).toBeInTheDocument();
        expect(screen.getByText('xray.pdf')).toBeInTheDocument();
        expect(screen.queryByRole('group', {name: 'Anlagengruppe'})).not.toBeInTheDocument();
    });

    it('should render attachments with the same group in one outlined group', () => {
        render(
            <ProcessInstanceAttachmentSetList
                items={[
                    createItem([
                        createAttachment('1', 'xray-1.pdf', 'person-1/dog-1'),
                        createAttachment('2', 'xray-2.pdf', 'person-1/dog-1'),
                    ]),
                ]}
            />,
        );

        const group = screen.getByRole('group', {name: 'Anlagengruppe'});

        expect(within(group).getByText('xray-1.pdf')).toBeInTheDocument();
        expect(within(group).getByText('xray-2.pdf')).toBeInTheDocument();
    });

    it('should render attachments with different groups in separate outlined groups', () => {
        render(
            <ProcessInstanceAttachmentSetList
                items={[
                    createItem([
                        createAttachment('1', 'dog-1.pdf', 'person-1/dog-1'),
                        createAttachment('2', 'dog-2.pdf', 'person-1/dog-2'),
                    ]),
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
            <ProcessInstanceAttachmentSetList
                items={[
                    createItem([
                        createAttachment('1', 'root.pdf'),
                        createAttachment('2', 'dog.pdf', 'person-1/dog-1'),
                    ]),
                ]}
            />,
        );

        const group = screen.getByRole('group', {name: 'Anlagengruppe'});

        expect(within(group).getByText('dog.pdf')).toBeInTheDocument();
        expect(screen.getByText('root.pdf').closest('[role="group"]')).toBeNull();
    });
});

function createItem(attachments: ProcessInstanceAttachmentEntity[]): ProcessInstanceAttachmentSetListItem {
    return {
        attachmentSet: createAttachmentSet(),
        attachments,
    };
}

function createAttachment(
    key: string,
    fileName: string,
    group?: string,
): ProcessInstanceAttachmentEntity {
    return {
        key,
        fileName,
        originalFileName: 'OriginalFileName',
        group,
        attachmentSetId: 1,
        processInstanceId: 42,
        processInstanceTaskId: null,
        storageProviderId: 7,
        storagePathFromRoot: '/proc-1/test/attachments/' + key,
        uploadedByUserId: 'user-1',
    };
}

function createAttachmentSet(): ProcessInstanceAttachmentSetEntity {
    return {
        id: 1,
        dataKey: 'documents',
        name: 'Documents',
        processInstanceId: 42,
        processInstanceTaskId: null,
    };
}
