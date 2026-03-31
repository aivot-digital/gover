import {Box, Button, Typography} from '@mui/material';
import {ProcessNodeEditorPlaceholderIllustration} from './components/process-node-editor-placeholder-illustration';
import Counter1 from '@aivot/mui-material-symbols-400-outlined/dist/counter-1/Counter1';
import Counter2 from '@aivot/mui-material-symbols-400-outlined/dist/counter-2/Counter2';
import Counter3 from '@aivot/mui-material-symbols-400-outlined/dist/counter-3/Counter3';
import {type ReactNode} from 'react';
import MenuBook from '@aivot/mui-material-symbols-400-outlined/dist/menu-book/MenuBook';
import {useNotImplemented} from '../../../../../../hooks/use-not-implemented';

const placeholderList: {
    icon: ReactNode;
    text: ReactNode;
}[] = [
    {
        icon: <Counter1 fontSize="large"
                        color="disabled"/>,
        text: (
            <Typography>
                Wählen Sie einen geeigneten Auslöser für diesen Prozess.
            </Typography>
        ),
    },
    {
        icon: <Counter2 fontSize="large"
                        color="disabled"/>,
        text: (
            <Typography>
                Fügen Sie über die Plus-Schaltflächen im Editor weitere Prozesselemente hinzu.
            </Typography>
        ),
    },
    {
        icon: <Counter3 fontSize="large"
                        color="disabled"/>,
        text: (
            <Typography>
                Konfigurieren Sie einzelne Prozesselemente, indem Sie darauf klicken.
            </Typography>
        ),
    },
];

export function ProcessNodeEditorPlaceholder() {
    const notImplemented = useNotImplemented();

    return (
        <Box
            sx={{
                p: 4,
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'flex-start',
                justifyContent: 'center',
                maxWidth: 440,
            }}
        >
            <Box sx={{ml: 2, maxWidth: 440}}>
                <ProcessNodeEditorPlaceholderIllustration/>
            </Box>

            <Typography
                variant="caption"
                color="text.secondary"
                sx={{
                    mt: 2,
                }}
            >
                Schnellstart
            </Typography>

            <Typography
                variant="h5"
                sx={{
                    mb: 2,
                }}
            >
                So modellieren Sie einen Prozess
            </Typography>

            {
                placeholderList.map((placeholder, index) => (
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            mb: 2,
                            '&:last-child': {
                                mb: 0,
                            },
                        }}
                    >
                        <Box>
                            {placeholder.icon}
                        </Box>

                        <Box
                            sx={{
                                ml: 2,
                            }}
                        >
                            {placeholder.text}
                        </Box>
                    </Box>
                ))
            }

            <Button
                startIcon={<MenuBook/>}
                variant="outlined"
                sx={{
                    mt: 3,
                }}
                onClick={() => {
                    notImplemented();
                }}
            >
                Handbuch aufrufen
            </Button>
        </Box>
    );
}