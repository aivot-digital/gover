import {Box} from '@mui/material';
import {Fragment} from 'react';
import {type Components} from 'react-markdown';
import {parseExecutionSummary} from '../utils/parse-execution-summary';
import {MarkdownContent} from '../../../components/markdown-content/markdown-content';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showErrorSnackbar} from '../../../slices/snackbar-slice';
import {ProcessInstanceAttachmentApiService} from '../services/process-instance-attachment-api-service';

interface ProcessExecutionSummaryProps {
    markdown: string;
}

export function ProcessExecutionSummary({markdown}: ProcessExecutionSummaryProps) {
    const dispatch = useAppDispatch();

    const previewAttachment = async (encodedKey: string) => {
        // Open synchronously so the authenticated request does not lose the browser's user activation.
        const previewWindow = window.open('', '_blank');
        if (previewWindow == null) {
            dispatch(showErrorSnackbar('Die Anlage konnte nicht geöffnet werden. Bitte erlauben Sie Pop-ups für diese Seite.'));
            return;
        }
        previewWindow.opener = null;
        previewWindow.document.title = 'Dokumentvorschau';
        previewWindow.document.body.textContent = 'Anlage wird geladen …';

        try {
            const blob = await new ProcessInstanceAttachmentApiService().preview(decodeURIComponent(encodedKey));
            const objectUrl = URL.createObjectURL(blob);
            window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
            previewWindow.location.replace(objectUrl);
        } catch (error) {
            previewWindow.close();
            dispatch(showApiErrorSnackbar(error, 'Die Anlage konnte nicht angezeigt werden.'));
        }
    };

    const components: Components = {
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
                        void previewAttachment(attachment[1]);
                    }}
                />
            );
        },
    };

    return (
        <Box sx={{display: 'flex', flexDirection: 'column', gap: 2}}>
            {parseExecutionSummary(markdown).map((block, index) => block.kind === 'markdown' ? (
                <MarkdownContent key={index} markdown={block.markdown} components={components}/>
            ) : (
                <Box
                    key={index}
                    component="dl"
                    sx={{
                        m: 0, px: 1.5, py: 2, borderRadius: 1, bgcolor: 'background.default',
                        '& dt': {typography: 'caption', color: 'text.secondary', mb: 0.5},
                        '& dd': {m: 0},
                        '& dd + dt': {mt: 1.5},
                    }}
                >
                    {block.entries.map((entry, entryIndex) => (
                        <Fragment key={entryIndex}>
                            <Box component="dt">
                                <MarkdownContent markdown={entry.term} components={components}/>
                            </Box>
                            <Box component="dd">
                                <MarkdownContent markdown={entry.markdown} components={components}/>
                            </Box>
                        </Fragment>
                    ))}
                </Box>
            ))}
        </Box>
    );
}
