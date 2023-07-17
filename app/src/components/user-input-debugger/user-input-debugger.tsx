import React from 'react';
import {Box, Button, Paper, Typography} from '@mui/material';
import {selectCustomerInput, setUserInput} from '../../slices/customer-input-slice';
import {selectShowUserInput} from '../../slices/admin-settings-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faFileDownload, faFileUpload} from '@fortawesome/pro-light-svg-icons';
import {downloadObjectFile, uploadObjectFile} from '../../utils/download-utils';
import {selectLoadedApplication} from '../../slices/app-slice';
import {format} from 'date-fns';
import {type CustomerInput} from '../../models/customer-input';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {isFileUploadElementItem} from '../../models/elements/form/input/file-upload-element';

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
    const userInput = useAppSelector(selectCustomerInput);
    const app = useAppSelector(selectLoadedApplication);

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
                    dispatch(setUserInput({}));
                } else {
                    dispatch(setUserInput(res));
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
            sx={{
                position: 'absolute',
                right: 0,
                bottom: 0,
                p: 4,
                width: '100%',
                height: '25vh',
                border: '1px solid red',
                overflowY: 'auto',
                zIndex: 999,
            }}
        >
            <Box
                sx={{
                    display: 'flex',
                }}
            >
                <Typography
                    variant="overline"
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
                    startIcon={<FontAwesomeIcon icon={faFileDownload}/>}
                >
                    Exportieren
                </Button>

                <Button
                    onClick={handleUpload}
                    startIcon={<FontAwesomeIcon icon={faFileUpload}/>}
                >
                    Importieren
                </Button>
            </Box>
            <Box component="code">
                <Box component="pre">{
                    JSON.stringify(userInput, null, 4)
                }</Box>
            </Box>
        </Box>
    );
}
