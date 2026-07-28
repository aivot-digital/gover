import React, {type ReactNode} from 'react';
import {Box, IconButton, Paper, Tooltip, Typography} from '@mui/material';
import type {SxProps, Theme} from '@mui/material';
import Description from '@aivot/mui-material-symbols-400-n25-outlined/Description';
import Download from '@aivot/mui-material-symbols-400-n25-outlined/Download';
import type {ProcessInstanceAttachmentEntity} from '../entities/process-instance-attachment-entity';
import type {ProcessInstanceAttachmentSetEntity} from '../entities/process-instance-attachment-set-entity';
import type {ProcessInstanceEntity} from '../entities/process-instance-entity';
import type {ProcessInstanceTaskEntity} from '../entities/process-instance-task-entity';

export interface ProcessInstanceAttachmentSetListItem {
    attachmentSet: ProcessInstanceAttachmentSetEntity;
    attachments: ProcessInstanceAttachmentEntity[];
    createdByLabel?: string;
    createdBySubLabel?: string;
}

interface ProcessInstanceAttachmentSetListProps {
    items: ProcessInstanceAttachmentSetListItem[];
    title?: ReactNode;
    sx?: SxProps<Theme>;
    onDownload?: (attachment: ProcessInstanceAttachmentEntity) => void;
}

interface BuildProcessInstanceAttachmentSetItemsOptions {
    includeEmpty?: boolean;
}

export function buildProcessInstanceAttachmentSetItems(
    attachmentSets: ProcessInstanceAttachmentSetEntity[],
    attachments: ProcessInstanceAttachmentEntity[],
    options?: BuildProcessInstanceAttachmentSetItemsOptions,
): ProcessInstanceAttachmentSetListItem[] {
    const attachmentsBySetId = new Map<number, ProcessInstanceAttachmentEntity[]>();
    for (const attachment of attachments) {
        const setAttachments = attachmentsBySetId.get(attachment.attachmentSetId) ?? [];
        setAttachments.push(attachment);
        attachmentsBySetId.set(attachment.attachmentSetId, setAttachments);
    }

    return attachmentSets
        .map((attachmentSet) => ({
            attachmentSet,
            attachments: attachmentsBySetId.get(attachmentSet.id) ?? [],
        }))
        .filter(({attachments}) => options?.includeEmpty === true || attachments.length > 0);
}

export function buildTaskProcessInstanceAttachmentSetItems(
    instance: ProcessInstanceEntity,
    task: ProcessInstanceTaskEntity,
    attachmentSets: ProcessInstanceAttachmentSetEntity[],
    attachments: ProcessInstanceAttachmentEntity[],
): ProcessInstanceAttachmentSetListItem[] {
    const isInitialTask = task.processNodeId === instance.initialNodeId;

    return buildProcessInstanceAttachmentSetItems(
        attachmentSets.filter((attachmentSet) => (
            attachmentSet.processInstanceTaskId === task.id ||
            (attachmentSet.processInstanceTaskId == null && isInitialTask)
        )),
        attachments,
    );
}

export function ProcessInstanceAttachmentSetList(props: ProcessInstanceAttachmentSetListProps): React.JSX.Element | null {
    if (props.items.length === 0) {
        return null;
    }

    return (
        <Box sx={props.sx}>
            {
                props.title !== null &&
                <Typography variant="h6" sx={{mb: 1}}>
                    {props.title ?? 'Anlagensätze'}
                </Typography>
            }

            <Box
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 1,
                }}
            >
                {
                    props.items.map((item) => (
                        <AttachmentSetItem
                            key={item.attachmentSet.id}
                            item={item}
                            onDownload={props.onDownload}
                        />
                    ))
                }
            </Box>
        </Box>
    );
}

interface AttachmentSetItemProps {
    item: ProcessInstanceAttachmentSetListItem;
    onDownload?: (attachment: ProcessInstanceAttachmentEntity) => void;
}

function AttachmentSetItem(props: AttachmentSetItemProps): React.JSX.Element {
    return (
        <Box
            sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: '6px',
                p: 1.5,
                backgroundColor: 'background.paper',
            }}
        >
            <Typography
                sx={{
                    fontWeight: 600,
                    overflowWrap: 'anywhere',
                }}
            >
                {props.item.attachmentSet.name}
            </Typography>

            <Typography
                color="text.secondary"
                sx={{
                    fontSize: '0.8125rem',
                    overflowWrap: 'anywhere',
                }}
            >
                Datenschlüssel: {props.item.attachmentSet.dataKey}
            </Typography>

            {
                props.item.createdByLabel != null &&
                <Typography
                    color="text.secondary"
                    sx={{
                        fontSize: '0.8125rem',
                        overflowWrap: 'anywhere',
                    }}
                >
                    Erstellt durch: {props.item.createdByLabel}{props.item.createdBySubLabel == null ? '' : ` · ${props.item.createdBySubLabel}`}
                </Typography>
            }

            <Box
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 0.75,
                    mt: 1,
                }}
            >
                {
                    props.item.attachments.length === 0 ?
                        <Typography
                            color="text.secondary"
                            sx={{fontSize: '0.875rem'}}
                        >
                            Keine Anhänge
                        </Typography> :
                        <AttachmentItems
                            attachments={props.item.attachments}
                            onDownload={props.onDownload}
                        />
                }
            </Box>
        </Box>
    );
}

interface AttachmentItemsProps {
    attachments: ProcessInstanceAttachmentEntity[];
    onDownload?: (attachment: ProcessInstanceAttachmentEntity) => void;
}

function AttachmentItems(props: AttachmentItemsProps): React.JSX.Element {
    const attachmentsByGroup = new Map<string, ProcessInstanceAttachmentEntity[]>();
    for (const attachment of props.attachments) {
        const group = normalizeAttachmentGroup(attachment.group);
        if (group == null) {
            continue;
        }

        const groupAttachments = attachmentsByGroup.get(group) ?? [];
        groupAttachments.push(attachment);
        attachmentsByGroup.set(group, groupAttachments);
    }

    if (attachmentsByGroup.size === 0) {
        return (
            <>
                {
                    props.attachments.map((attachment) => (
                        <AttachmentItem
                            key={attachment.key}
                            attachment={attachment}
                            onDownload={props.onDownload}
                        />
                    ))
                }
            </>
        );
    }

    const renderedGroups = new Set<string>();

    return (
        <>
            {
                props.attachments.map((attachment) => {
                    const group = normalizeAttachmentGroup(attachment.group);

                    if (group == null) {
                        return (
                            <AttachmentItem
                                key={attachment.key}
                                attachment={attachment}
                                onDownload={props.onDownload}
                            />
                        );
                    }

                    if (renderedGroups.has(group)) {
                        return null;
                    }

                    renderedGroups.add(group);

                    return (
                        <Paper
                            key={`group:${group}`}
                            aria-label="Anlagengruppe"
                            role="group"
                            variant="outlined"
                            sx={{
                                display: 'flex',
                                flexDirection: 'column',
                                gap: 0.75,
                                p: 1,
                            }}
                        >
                            {
                                attachmentsByGroup.get(group)?.map((groupedAttachment) => (
                                    <AttachmentItem
                                        key={groupedAttachment.key}
                                        attachment={groupedAttachment}
                                        onDownload={props.onDownload}
                                    />
                                ))
                            }
                        </Paper>
                    );
                })
            }
        </>
    );
}

function normalizeAttachmentGroup(group: string | null | undefined): string | null {
    const normalizedGroup = group?.trim();
    return normalizedGroup == null || normalizedGroup.length === 0 ? null : normalizedGroup;
}

interface AttachmentItemProps {
    attachment: ProcessInstanceAttachmentEntity;
    onDownload?: (attachment: ProcessInstanceAttachmentEntity) => void;
}

function AttachmentItem(props: AttachmentItemProps): React.JSX.Element {
    return (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                minHeight: 36,
                minWidth: 0,
            }}
        >
            <Description color="primary" sx={{fontSize: 20, flexShrink: 0}} />

            <Typography
                sx={{
                    flex: 1,
                    minWidth: 0,
                    fontSize: '0.875rem',
                    overflowWrap: 'anywhere',
                }}
            >
                {props.attachment.fileName}
            </Typography>

            {
                props.onDownload != null &&
                <Tooltip title="Herunterladen" arrow>
                    <IconButton
                        aria-label={`${props.attachment.fileName} herunterladen`}
                        onClick={() => props.onDownload?.(props.attachment)}
                        size="small"
                        sx={{color: 'text.secondary'}}
                    >
                        <Download fontSize="small" />
                    </IconButton>
                </Tooltip>
            }
        </Box>
    );
}
