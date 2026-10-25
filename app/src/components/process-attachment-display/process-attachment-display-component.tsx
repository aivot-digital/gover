import type {ReactNode} from 'react';
import {Box, CircularProgress, Typography} from '@mui/material';
import AttachFile from '@aivot/mui-material-symbols-400-n25-outlined/AttachFile';
import Download from '@aivot/mui-material-symbols-400-n25-outlined/Download';
import OpenInNew from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import {FileList, type FileListAction} from '../file-list/file-list';
import {FormField, type FormFieldLayoutProps, getCompositeControlAriaProps} from '../form-field';
import {FormFieldTokens} from '../../theming/form-field-tokens';
import {humanizeFileSize} from '../../utils/humanization-utils';

export interface ProcessAttachmentDisplayItem {
    key: string;
    fileName: string;
    originalFileName: string;
    sizeInBytes?: number | null;
    group?: string | undefined | null;
    onView?: () => void;
    onDownload?: () => void;
}

interface ProcessAttachmentDisplayComponentProps extends FormFieldLayoutProps {
    items?: ProcessAttachmentDisplayItem[];
    labelText?: string | null;
    statusText?: string;
    hintText?: string | null;
    previewText?: string;
    loading?: boolean;
}

export function ProcessAttachmentDisplayComponent(props: ProcessAttachmentDisplayComponentProps) {
    const items = props.items ?? [];
    const hints = [props.hintText, props.previewText].filter(text => text != null && text.trim().length > 0);

    return (
        <FormField
            id={props.id}
            label={props.labelText?.trim() ? props.labelText : 'Anlagensatz-Anzeige'}
            hint={hints.length > 0 ? hints.map((text, index) => <Box key={index}>{text}</Box>) : undefined}
            busy={props.loading}
            ariaLabel={props.ariaLabel}
            ariaDescribedBy={props.ariaDescribedBy}
            externalAction={props.externalAction}
            labelAction={props.labelAction}
            margin={props.margin ?? 'normal'}
            showOptionalIndicator={false}
            sx={props.sx}
        >
            {field => (
                <Box id={field.controlId} role="group" {...getCompositeControlAriaProps(field)}>
                    {items.length > 0 ? (
                        <AttachmentItems items={items}/>
                    ) : (
                        <AttachmentStatus loading={props.loading}>
                            {props.statusText ?? 'Keine Anlagen vorhanden.'}
                        </AttachmentStatus>
                    )}
                </Box>
            )}
        </FormField>
    );
}

interface AttachmentSection {
    group: string | null;
    items: ProcessAttachmentDisplayItem[];
}

function AttachmentItems({items}: {items: ProcessAttachmentDisplayItem[]}) {
    const sections: AttachmentSection[] = [];
    const groupedSections = new Map<string, AttachmentSection>();

    // Keep each group at its first occurrence and leave ungrouped attachments between groups.
    for (const item of items) {
        const group = item.group?.trim() || null;
        const lastSection = sections.at(-1);
        const existing = group == null
            ? (lastSection?.group === null ? lastSection : undefined)
            : groupedSections.get(group);
        if (existing) {
            existing.items.push(item);
        } else {
            const section = {group, items: [item]};
            sections.push(section);
            if (group != null) {
                groupedSections.set(group, section);
            }
        }
    }

    return (
        <Box sx={{display: 'flex', flexDirection: 'column', gap: 1.5}}>
            {sections.map(section => (
                <Box
                    key={section.group == null ? `file:${section.items[0].key}` : `group:${section.group}`}
                    role={section.group == null ? undefined : 'group'}
                    aria-label={section.group == null ? undefined : 'Anlagengruppe'}
                >
                    {section.group != null && (
                        <Typography variant="caption" component="div" color="text.secondary"
                                    sx={{mb: 0.5, overflowWrap: 'anywhere'}}>
                            Gruppenkennzeichnung: {section.group}
                        </Typography>
                    )}
                    <FileList items={section.items.map(item => {
                        const actions: FileListAction[] = [];
                        if (item.onView) {
                            actions.push({
                                label: `${item.fileName} in neuem Tab ansehen`,
                                icon: <OpenInNew fontSize="small"/>,
                                onClick: item.onView,
                            });
                        }
                        if (item.onDownload) {
                            actions.push({
                                label: `${item.fileName} herunterladen`,
                                icon: <Download fontSize="small"/>,
                                onClick: item.onDownload,
                            });
                        }
                        return {
                            key: item.key,
                            name: item.fileName,
                            secondaryText: [
                                item.sizeInBytes != null ? humanizeFileSize(item.sizeInBytes) : undefined,
                                item.originalFileName ? `Hochgeladen als ${item.originalFileName}` : undefined,
                            ].filter(Boolean).join(' · '),
                            onView: item.onView,
                            actions,
                        };
                    })}/>
                </Box>
            ))}
        </Box>
    );
}

function AttachmentStatus({children, loading}: {children: ReactNode; loading?: boolean}) {
    return (
        <Box role="status" sx={{
            display: 'flex', alignItems: 'center', gap: 1,
            minHeight: FormFieldTokens.groupedControlRowMinHeight,
            px: 1.5, py: 0.5, border: 1, borderColor: 'divider', borderRadius: 1,
        }}>
            {loading ? <CircularProgress size={20} aria-hidden="true"/> : <AttachFile fontSize="small" color="disabled"/>}
            <Typography variant="body2" color="text.secondary">{children}</Typography>
        </Box>
    );
}
