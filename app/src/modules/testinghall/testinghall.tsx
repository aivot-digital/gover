import {PageWrapper} from '../../components/page-wrapper/page-wrapper';
import Button from '@mui/material/Button';
import Box from '@mui/material/Box';
import Container from '@mui/material/Box';
import Paper from '@mui/material/Paper';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {addSnackbarMessage, setErrorMessage, setLoadingMessage, SnackbarSeverity, SnackbarType} from '../../slices/shell-slice';
import {HintTooltip} from '../../components/hint-tooltip/hint-tooltip';
import {ModuleIcons} from '../../shells/staff/data/module-icons';
import {GenericPageHeader} from '../../components/generic-page-header/generic-page-header';
import {useState} from 'react';
import {TextFieldComponent} from '../../components/text-field/text-field-component';

export function Testinghall() {
    const dispatch = useAppDispatch();

    const [input, setInput] = useState<string | undefined>();

    const buttons: {
        label: string;
        onClick: () => void;
        hint: string;
    }[] = [
        {
            label: 'Start Shell Progress',
            onClick: () => {
                dispatch(setLoadingMessage({
                    message: input ?? 'Shell Progress Started',
                    blocking: false,
                    estimatedTime: 5000,
                }));
            },
            hint: 'Starts a non-blocking shell progress with an estimated time of 5 seconds.',
        },
        {
            label: 'Start Shell Progress (Blocking)',
            onClick: () => {
                dispatch(setLoadingMessage({
                    message: input ?? 'Shell Progress Started',
                    blocking: true,
                    estimatedTime: 5000,
                }));
                setTimeout(() => {
                    dispatch(setLoadingMessage(undefined));
                }, 10000);
            },
            hint: 'Starts a blocking shell progress with an estimated time of 5 seconds. The progress will automatically stop after 10 seconds.',
        },
        {
            label: 'Stop Shell Progress',
            onClick: () => {
                dispatch(setLoadingMessage(undefined));
            },
            hint: 'Stops a non-blocking shell progress.',
        },
        {
            label: 'Create a Test Error Message with Status 500',
            onClick: () => {
                dispatch(setErrorMessage({
                    message: input ?? 'This is a test error message from the Testing Hall.',
                    status: 500,
                }));
            },
            hint: 'Creates a test error message with status code 500.',
        },
        {
            label: 'Create a Test Error Message with Status 404',
            onClick: () => {
                dispatch(setErrorMessage({
                    message: input ?? 'This is a test error message from the Testing Hall.',
                    status: 404,
                }));
            },
            hint: 'Creates a test error message with status code 404.',
        },
        {
            label: 'Create a Test Error Message with Status 403',
            onClick: () => {
                dispatch(setErrorMessage({
                    message: input ?? 'This is a test error message from the Testing Hall.',
                    status: 403,
                }));
            },
            hint: 'Creates a test error message with status code 403.',
        },
        {
            label: 'Create an auto hide snackbar',
            onClick: () => {
                dispatch(addSnackbarMessage({
                    key: `testinghall-snackbar-${Date.now()}`,
                    message: input ?? 'This is a test snackbar message from the Testing Hall.',
                    severity: SnackbarSeverity.Success,
                    type: SnackbarType.AutoHiding,
                }));
            },
            hint: 'Creates a snacbkar message that is permanent but can be dismissed by the user.',
        },
        {
            label: 'Create an permanent but dismissable snackbar',
            onClick: () => {
                dispatch(addSnackbarMessage({
                    key: `testinghall-snackbar-${Date.now()}`,
                    message: input ?? 'This is a test snackbar message from the Testing Hall.',
                    severity: SnackbarSeverity.Info,
                    type: SnackbarType.Dismissable,
                }));
            },
            hint: 'Creates a snacbkar message that is permanent but can be dismissed by the user.',
        },
        {
            label: 'Create a permanent, non dismissable snackbar',
            onClick: () => {
                dispatch(addSnackbarMessage({
                    key: `testinghall-snackbar-${Date.now()}`,
                    message: input ?? 'This is a test snackbar message from the Testing Hall.',
                    severity: SnackbarSeverity.Warning,
                    type: SnackbarType.Permanent,
                }));
            },
            hint: 'Creates a snackbar message that is permanent and cannot be dismissed by the user.',
        },
        {
            label: 'Create a loading snackbar',
            onClick: () => {
                dispatch(addSnackbarMessage({
                    key: `testinghall-snackbar-${Date.now()}`,
                    message: input ?? 'This is a test snackbar message from the Testing Hall.',
                    severity: SnackbarSeverity.Error,
                    type: SnackbarType.Loading,
                }));
            },
            hint: 'Creates a loading snackbar message.',
        },
    ];

    return (
        <PageWrapper
            title="Testing Hall"
        >
            <Container>
                <GenericPageHeader
                    icon={ModuleIcons.dashboard}
                    title="Testing Hall"
                />

                <Paper
                    sx={{
                        p: 4,
                        mt: 4,
                    }}
                >
                    <TextFieldComponent
                        label="Input"
                        onChange={setInput}
                        value={input}
                    />

                    <Box
                        sx={{
                            mt: 2,
                            display: 'flex',
                            gap: 2,
                            flexWrap: 'wrap',
                        }}
                    >
                        {
                            buttons.map((btn) => (
                                <Box
                                    key={btn.label}
                                >
                                    <HintTooltip
                                        title={btn.hint}
                                        arrow
                                    >
                                        <Button
                                            onClick={btn.onClick}
                                            variant="contained"
                                        >
                                            {btn.label}
                                        </Button>
                                    </HintTooltip>
                                </Box>
                            ))
                        }
                    </Box>
                </Paper>
            </Container>
        </PageWrapper>
    );
}