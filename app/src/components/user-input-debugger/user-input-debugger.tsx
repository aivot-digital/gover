import React, {useState} from 'react';
import {Box, Button, IconButton, Paper, Typography} from '@mui/material';
import {selectShowUserInput, toggleShowUserInput} from '../../slices/admin-settings-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {downloadObjectFile, uploadObjectFile} from '../../utils/download-utils';
import {hydrateCustomerInput, selectLoadedForm, updateCustomerInput} from '../../slices/app-slice';
import {format} from 'date-fns';
import {type CustomerInput} from '../../models/customer-input';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {isFileUploadElementItem} from '../../models/elements/form/input/file-upload-element';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import CloseIcon from '@mui/icons-material/Close';

function cleanCustomerInput(input: CustomerInput): CustomerInput {
    const cleanedInput: CustomerInput = {};
    for (const key of Object.keys(input)) {
        const value = input[key];
        if (Array.isArray(value) && value.length > 0 && isFileUploadElementItem(value[0])) {

        } else {
            cleanedInput[key] = value;
        }
    }
    return cleanedInput;
}

export function UserInputDebugger(): JSX.Element | null {
    const dispatch = useAppDispatch();
    const showUserInput = useAppSelector(selectShowUserInput);
    const userInput = useAppSelector(state => state.app.inputs);
    const app = useAppSelector(selectLoadedForm);
    const [isCollapsed, setIsCollapsed] = useState(false);

    if (!showUserInput || app == null) {
        return null;
    }

    const handleExport = (): void => {
        const filename = `nutzereingaben-${app?.slug}_${format(new Date(), 'dd-MM-yyyy')}.json`;
        const input = cleanCustomerInput(userInput);
        downloadObjectFile(filename, input);
    };

    const handleUpload = (): void => {
        uploadObjectFile<CustomerInput>('.json')
            .then((res) => {
                if (res == null) {
                    dispatch(hydrateCustomerInput({}));
                } else {
                    dispatch(hydrateCustomerInput(res));
                }
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Nutzereingaben konnten nicht geladen werden'));
            });
    };

    return (
        <Box
            component={Paper}
            borderRadius={0}
            sx={{
                position: 'absolute',
                left: 0,
                right: 0,
                bottom: 0,
                p: 2,
                borderTopWidth: 2,
                borderTopStyle: 'solid',
                borderTopColor: 'primary.main',
                zIndex: 999,
            }}
        >
            <Box
                display="flex"
                alignItems="center"
            >
                <Typography
                    variant="h6"
                    component="span"
                    sx={{
                        mr: 'auto',
                    }}
                >
                    Nutzereingaben
                </Typography>

                <Button
                    sx={{
                        mr: 2,
                    }}
                    onClick={handleExport}
                    startIcon={<FileDownloadOutlinedIcon />}
                >
                    Exportieren
                </Button>

                <Button
                    onClick={handleUpload}
                    startIcon={<UploadFileOutlinedIcon />}
                >
                    Importieren
                </Button>

                <IconButton
                    onClick={() => setIsCollapsed(!isCollapsed)}
                    color="primary"
                >
                    {isCollapsed ? <ExpandMoreIcon /> : <ExpandLessIcon />}
                </IconButton>

                <IconButton
                    onClick={() => {
                        dispatch(toggleShowUserInput());
                    }}
                    color="error"
                >
                    <CloseIcon />
                </IconButton>
            </Box>

            <Box
                sx={{
                    overflowY: 'scroll',
                    overflowX: 'scroll',
                    width: '100%',
                    height: isCollapsed ? 0 : '25vh',
                    transition: 'height 0.3s ease-in-out',
                }}
            >
                <Box component="code">
                    <Box component="pre">{
                        JSON.stringify(userInput, null, 4)
                    }</Box>
                </Box>
            </Box>
        </Box>
    );
}
