import type {BaseViewProps} from './base-view';
import type {ProcessAttachmentDisplayElement} from '../models/elements/form/content/process-attachment-display-element';
import React, {useMemo} from 'react';
import {useOptionalProcessTaskViewAttachmentContext} from '../modules/process/pages/details/process-task-view-attachment-context';
import {ProcessAttachmentDisplayComponent} from '../components/process-attachment-display/process-attachment-display-component';

export function ProcessAttachmentDisplayView(props: BaseViewProps<ProcessAttachmentDisplayElement, void>): React.JSX.Element {
    const {
        element,
    } = props;

    const attachmentContext = useOptionalProcessTaskViewAttachmentContext();
    const configuredAttachmentSetKey = element.attachmentSetKey ?? '';
    const hasConfiguredAttachmentSetKey = configuredAttachmentSetKey.trim().length > 0;

    const matchingAttachmentSetIds = useMemo(() => {
        if (attachmentContext == null || !hasConfiguredAttachmentSetKey) {
            return new Set<number>();
        }

        // Attachment set keys are the stable process contract; filenames can be configured per upload and may repeat.
        return new Set(
            attachmentContext.attachmentSets
                .filter((attachmentSet) => attachmentSet.dataKey === configuredAttachmentSetKey)
                .map((attachmentSet) => attachmentSet.id),
        );
    }, [
        attachmentContext,
        configuredAttachmentSetKey,
        hasConfiguredAttachmentSetKey,
    ]);

    const matchingAttachments = useMemo(() => {
        if (attachmentContext == null || matchingAttachmentSetIds.size === 0) {
            return [];
        }

        return attachmentContext.attachments.filter((attachment) => matchingAttachmentSetIds.has(attachment.attachmentSetId));
    }, [
        attachmentContext,
        matchingAttachmentSetIds,
    ]);

    if (attachmentContext == null) {
        if (!hasConfiguredAttachmentSetKey) {
            return (
                <ProcessAttachmentDisplayComponent
                    labelText={element.label}
                    hintText={element.hint}
                    statusText="Konfigurieren Sie einen Anlagensatz-Schlüssel, um Vorgangsanhänge anzuzeigen."
                />
            );
        }

        return (
            <ProcessAttachmentDisplayComponent
                labelText={element.label}
                hintText={element.hint}
                items={[
                    {
                        key: 'preview',
                        fileName: element.label ?? configuredAttachmentSetKey,
                        originalFileName: element.label ?? configuredAttachmentSetKey,
                    },
                ]}
                previewText="Dies ist eine Vorschau. Anhänge können im Modellierungsmodus nicht angesehen oder heruntergeladen werden."
            />
        );
    }

    if (attachmentContext.isLoadingAttachments) {
        return (
            <ProcessAttachmentDisplayComponent
                labelText={element.label}
                hintText={element.hint}
                loading
                statusText="Anhänge werden geladen..."
            />
        );
    }

    if (!hasConfiguredAttachmentSetKey) {
        return (
            <ProcessAttachmentDisplayComponent
                labelText={element.label}
                hintText={element.hint}
                statusText="Es ist kein Anlagensatz-Schlüssel für die anzuzeigenden Anhänge konfiguriert."
            />
        );
    }

    if (matchingAttachmentSetIds.size === 0) {
        return (
            <ProcessAttachmentDisplayComponent
                labelText={element.label}
                hintText={element.hint}
                statusText="Für den konfigurierten Anlagensatz-Schlüssel wurde kein Anlagensatz gefunden."
            />
        );
    }

    if (matchingAttachments.length === 0) {
        return (
            <ProcessAttachmentDisplayComponent
                labelText={element.label}
                hintText={element.hint}
                statusText="Der konfigurierte Anlagensatz enthält keine Anhänge."
            />
        );
    }

    return (
        <ProcessAttachmentDisplayComponent
            labelText={element.label}
            hintText={element.hint}
            items={matchingAttachments.map((attachment) => ({
                key: attachment.key,
                fileName: attachment.fileName,
                originalFileName: attachment.originalFileName,
                group: attachment.group,
                onView: () => {
                    void attachmentContext.viewAttachment(attachment);
                },
                onDownload: () => {
                    void attachmentContext.downloadAttachment(attachment);
                },
            }))}
        />
    );
}
