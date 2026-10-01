import {MarkdownContent} from '../../../components/markdown-content/markdown-content';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showErrorSnackbar} from '../../../slices/snackbar-slice';
import {ProcessInstanceAttachmentApiService} from '../services/process-instance-attachment-api-service';

interface ProcessExecutionSummaryProps {
    markdown: string;
}

export function ProcessExecutionSummary({markdown}: ProcessExecutionSummaryProps) {
    const dispatch = useAppDispatch();

    const previewAttachment = async (key: string) => {
        // Open synchronously so the authenticated request does not lose the browser's user activation.
        const previewWindow = window.open('', '_blank');
        if (previewWindow == null) {
            dispatch(showErrorSnackbar('Der Anhang konnte nicht geöffnet werden. Bitte erlauben Sie Pop-ups für diese Seite.'));
            return;
        }
        previewWindow.opener = null;
        previewWindow.document.title = 'Dokumentvorschau';
        previewWindow.document.body.textContent = 'Anhang wird geladen…';

        try {
            const blob = await new ProcessInstanceAttachmentApiService().preview(key);
            const objectUrl = URL.createObjectURL(blob);
            window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
            previewWindow.location.replace(objectUrl);
        } catch (error) {
            previewWindow.close();
            dispatch(showApiErrorSnackbar(error, 'Der Anhang konnte nicht angezeigt werden.'));
        }
    };

    return (
        <MarkdownContent
            markdown={markdown}
            components={{
                a: ({href, node: _node, ...props}) => {
                    const attachment = href?.match(/^\/api\/process-instance-attachments\/([^/?#]+)\/file\/\?download=false$/);
                    const external = href != null && /^(https?:)?\/\//.test(href);
                    return (
                        <a
                            {...props}
                            href={href}
                            target={external ? '_blank' : undefined}
                            rel={external ? 'noopener noreferrer' : undefined}
                            onClick={attachment == null ? undefined : (event) => {
                                event.preventDefault();
                                void previewAttachment(decodeURIComponent(attachment[1]));
                            }}
                        />
                    );
                },
            }}
        />
    );
}
