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
    const configuredFileName = element.fileName ?? '';
    const hasConfiguredFileName = configuredFileName.trim().length > 0;

    const matchingAttachments = useMemo(() => {
        if (attachmentContext == null || !hasConfiguredFileName) {
            return [];
        }

        return attachmentContext.attachments.filter((attachment) => attachment.fileName === configuredFileName);
    }, [
        attachmentContext,
        configuredFileName,
        hasConfiguredFileName,
    ]);

    if (attachmentContext == null) {
        if (!hasConfiguredFileName) {
            return (
                <ProcessAttachmentDisplayComponent
                    hintText={element.hint}
                    statusText="Konfigurieren Sie einen Dateinamen, um passende Vorgangsanhänge anzuzeigen."
                />
            );
        }

        return (
            <ProcessAttachmentDisplayComponent
                hintText={element.hint}
                items={[
                    {
                        key: 'preview',
                        fileName: configuredFileName,
                    },
                ]}
                previewText="Dies ist eine Vorschau. Anhänge können im Modellierungsmodus nicht angesehen oder heruntergeladen werden."
            />
        );
    }

    if (attachmentContext.isLoadingAttachments) {
        return (
            <ProcessAttachmentDisplayComponent
                hintText={element.hint}
                loading
                statusText="Anhänge werden geladen..."
            />
        );
    }

    if (!hasConfiguredFileName) {
        return (
            <ProcessAttachmentDisplayComponent
                hintText={element.hint}
                statusText="Es ist kein Dateiname für die anzuzeigenden Anhänge konfiguriert."
            />
        );
    }

    if (matchingAttachments.length === 0) {
        return (
            <ProcessAttachmentDisplayComponent
                hintText={element.hint}
                statusText="Für den konfigurierten Dateinamen wurden keine Anhänge gefunden."
            />
        );
    }

    return (
        <ProcessAttachmentDisplayComponent
            hintText={element.hint}
            items={matchingAttachments.map((attachment) => ({
                key: attachment.key,
                fileName: attachment.fileName,
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
