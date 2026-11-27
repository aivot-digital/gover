import {lazy, Suspense, useState} from 'react';
import {Badge, Box, Button, IconButton, Popover, Tooltip, Typography} from '@mui/material';
import Notes from '@aivot/mui-material-symbols-400-n25-outlined/Notes';
import Close from '@aivot/mui-material-symbols-400-n25-outlined/Close';
import {useNormalizedReactId} from '../../../hooks/use-normalized-react-id';

const RichTextInput = lazy(() => import('../../../components/rich-text-input-component/rich-text-input-component')
    .then(module => ({default: module.RichTextInputComponent})));

export function NoCodeEditorNotes(props: {
    value?: string | null;
    hasExpression: boolean;
    disabled: boolean;
    onChange: (notes: string | null) => void
}) {
    const [anchor, setAnchor] = useState<HTMLElement | null>(null);
    const id = `${useNormalizedReactId()}-notes`;
    const hasNotes = Boolean(props.value?.trim());
    return <>
        <Button size="small"
                startIcon={<Badge color="primary"
                                  variant="dot"
                                  invisible={!hasNotes}><Notes/></Badge>}
                aria-label={hasNotes ? 'Notizen anzeigen' : 'Notizen'}
                aria-haspopup="dialog"
                aria-expanded={anchor != null}
                aria-controls={anchor != null ? id : undefined}
                onClick={event => setAnchor(event.currentTarget)}>
            Notizen
        </Button>
        <Popover open={anchor != null}
                 anchorEl={anchor}
                 onClose={() => setAnchor(null)}
                 anchorOrigin={{
                     vertical: 'bottom',
                     horizontal: 'right',
                 }}
                 transformOrigin={{
                     vertical: 'top',
                     horizontal: 'right',
                 }}
                 slotProps={{
                     paper: {
                         id,
                         role: 'dialog',
                         'aria-labelledby': `${id}-title`,
                         sx: {
                             width: 480,
                             maxWidth: 'calc(100vw - 48px)',
                             p: 2,
                         },
                     },
                 }}>
            <Box sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                mb: 1,
            }}>
                <Typography id={`${id}-title`}
                            variant="h5"
                            component="h2">Notizen zum Ausdruck</Typography>
                <Tooltip arrow
                         title="Schließen"><IconButton size="small"
                                                       aria-label="Notizen schließen"
                                                       onClick={() => setAnchor(null)}><Close/></IconButton></Tooltip>
            </Box>
            <Suspense fallback={<Typography color="text.secondary">Notizen werden geladen …</Typography>}>
                <RichTextInput label=""
                               ariaLabel="Notizen zum Ausdruck"
                               value={props.value}
                               onChange={props.onChange}
                               hint={props.hasExpression
                                   ? 'Interne Hinweise zur fachlichen Bedeutung dieses Ausdrucks.'
                                   : 'Notizen können erst erfasst werden, wenn ein Ausdruck festgelegt ist.'}
                               disabled={props.disabled}
                               customerMode={false}
                               reducedMode
                               margin="none"
                               controlSx={{
                                   '& .prosuna-mdx-editor [class*="_contentEditable_"]:not([class*="_placeholder_"]), & .prosuna-mdx-editor .cm-sourceView, & .prosuna-mdx-editor .cm-sourceView .cm-scroller': {
                                       minHeight: 'calc(96px + 1.6em)',
                                       maxHeight: 240,
                                   },
                               }}/>
            </Suspense>
        </Popover>
    </>;
}
