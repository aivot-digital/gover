import React, {useRef, useState} from 'react';
import {Box, Button, FormLabel, IconButton, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography, useMediaQuery, useTheme} from '@mui/material';
import {FileUploadElement, FileUploadElementItem} from "../../models/elements/form/input/file-upload-element";
import {useAppDispatch} from "../../hooks/use-app-dispatch";
import {showErrorSnackbar} from "../../slices/snackbar-slice";
import {humanizeFileSize, humanizeNumber, pluralize} from "../../utils/huminization-utils";
import {BaseViewProps} from '../../views/base-view';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import BackupOutlinedIcon from '@mui/icons-material/BackupOutlined';

export function FileUploadView({
                                   element,
                                   value,
                                   error,
                                   setValue,
                               }: BaseViewProps<FileUploadElement, FileUploadElementItem[]>) {
    const theme = useTheme();
    const dispatch = useAppDispatch();
    const inputRef = useRef<HTMLInputElement | null>(null);
    const [isFocused, setIsFocused] = useState(false);
    const [isDraggedOver, setIsDraggedOver] = useState(false);
    const isBreakpointMdAndDown = useMediaQuery(theme.breakpoints.down('md'));

    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (event.target.files != null && event.target.files.length > 0) {
            handleAdd(event.target.files);
        }

        if (inputRef.current != null) {
            inputRef.current.files = null;
            inputRef.current.value = '';
        }
    };

    const handleRemove = (file: FileUploadElementItem) => {
        if (value != null) {
            const index = value.indexOf(file);
            if (index >= 0) {
                const updatedFiles = [...value];
                updatedFiles.splice(index, 1);
                setValue(updatedFiles.length > 0 ? updatedFiles : undefined);
            }
        }
    };

    const handleDrop: React.DragEventHandler<HTMLDivElement> = (event) => {
        handleAdd(event.dataTransfer.files);
        setIsDraggedOver(false);
    };

    const handleAdd = (files: FileList) => {
        const maxFiles = element.isMultifile ? (element.maxFiles != null ? element.maxFiles : null) : 1;

        const fileUploadItems: FileUploadElementItem[] = [
            ...value ?? [],
        ];
        let addedItems = 0;
        for (let i = 0; (i < files.length && (maxFiles == null || fileUploadItems.length < maxFiles)); i++) {
            const file = files[i];
            fileUploadItems.push({
                name: file.name,
                uri: URL.createObjectURL(file),
                size: file.size,
            });
            addedItems++;
        }

        if (addedItems < files.length) {
            dispatch(showErrorSnackbar('Einige Anlagen konnten nicht hinzugefügt werden, da das Maximum überschritten wurde.'));
        }

        setValue(fileUploadItems.length > 0 ? fileUploadItems : undefined);
    };

    const fileMaximumReached = (
        (element.isMultifile && element.maxFiles != null && (value ?? []).length >= element.maxFiles) ||
        (!element.isMultifile && (value ?? []).length >= 1)
    );

    return (
        <Box>
            <Box
                sx={{
                    mb: 1,
                }}
            >
                <FormLabel
                    htmlFor={element.id + '-input'}
                    error={error != null}
                >
                    {element.label}
                    {element.required && ' *'}
                </FormLabel>
            </Box>

            {
                value != null &&
                value.length > 0 &&
                <TableContainer sx={{mb: 2}}>
                    <Table size="small">
                        <TableHead>
                            <TableRow>
                                <TableCell>
                                    Dateiname
                                </TableCell>
                                <TableCell align="right">
                                    Dateigröße
                                </TableCell>
                                <TableCell align="right">
                                    Aktion
                                </TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {
                                value.map(file => (
                                    <TableRow
                                        key={file.uri}
                                    >
                                        <TableCell>
                                            {file.name}
                                        </TableCell>
                                        <TableCell align="right">
                                            {humanizeFileSize(file.size)}
                                        </TableCell>
                                        <TableCell align="right">
                                            {
                                                isBreakpointMdAndDown ?
                                                    <IconButton
                                                        onClick={() => handleRemove(file)}
                                                    >
                                                        <DeleteForeverOutlinedIcon
                                                            fontSize={"small"}
                                                        />
                                                    </IconButton> :
                                                    <Button
                                                        variant="outlined"
                                                        onClick={() => handleRemove(file)}
                                                        startIcon={
                                                            <DeleteForeverOutlinedIcon
                                                                fontSize={"small"}
                                                            />
                                                        }
                                                    >
                                                        Entfernen
                                                    </Button>
                                            }
                                        </TableCell>
                                    </TableRow>
                                ))
                            }
                        </TableBody>
                    </Table>
                </TableContainer>
            }

            <Box
                sx={{
                    p: 2,
                    border: `2px ${isFocused ? 'solid' : 'dashed'} ${error != null ? theme.palette.error.main : (isFocused ? theme.palette.primary.main : theme.palette.grey["500"])}`,
                    backgroundColor: theme.palette.grey["100"],
                    transition: 'background-color 250ms ease-in-out',
                    '&:hover': {
                        backgroundColor: theme.palette.grey["50"],
                    },
                    boxShadow: isDraggedOver ? `0 0 0.5em ${theme.palette.primary.main}` : undefined,
                }}
                onDragOver={() => setIsDraggedOver(true)}
                onDragLeave={() => setIsDraggedOver(false)}
                onDrop={handleDrop}
            >
                <Box
                    sx={{
                        position: 'relative',
                    }}
                >
                    <input
                        style={{
                            position: 'absolute',
                            top: 0,
                            left: 0,
                            opacity: 0,
                            width: 0.1,
                            height: 0.1,
                            overflow: 'hidden',
                            zIndex: -1,
                        }}
                        id={element.id + '-input'}
                        ref={inputRef}
                        type="file"
                        multiple={element.isMultifile}
                        accept={element.extensions != null ? element.extensions.map(ext => '.' + ext).join(',') : undefined}
                        onChange={handleChange}
                        onFocus={() => setIsFocused(true)}
                        onBlur={() => setIsFocused(false)}
                        disabled={fileMaximumReached}
                    />

                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                        }}
                    >

                        <BackupOutlinedIcon
                            fontSize="large"
                            sx={{color: fileMaximumReached ? theme.palette.grey["500"] : theme.palette.grey["700"]}}
                        />

                        <Typography
                            sx={{
                                ml: 1,
                                color: fileMaximumReached ? theme.palette.grey["500"] : undefined,
                            }}
                        >
                            Datei per Drag & Drop auf dieses Feld hochladen
                        </Typography>

                        <Button
                            sx={{ml: 'auto'}}
                            onClick={() => {
                                if (inputRef.current != null) {
                                    inputRef.current.click();
                                }
                            }}
                            disabled={fileMaximumReached}
                        >
                            Datei auswählen
                        </Button>
                    </Box>
                    {
                        element.extensions &&
                        element.extensions.length > 0 &&
                        <Typography
                            color={theme.palette.grey["700"]}
                            variant="caption"
                        >
                            Erlaubte Dateitypen: {element.extensions.map(ext => '.' + ext).join(', ')}
                        </Typography>
                    }
                </Box>
            </Box>

            {
                (
                    error != null ||
                    element.hint != null ||
                    (
                        element.minFiles != null &&
                        element.minFiles > 0
                    ) || (
                        element.maxFiles != null &&
                        element.maxFiles > 0
                    )
                ) &&
                <Box
                    sx={{
                        display: 'flex',
                        mt: 0.5,
                        mx: 1.5,
                    }}
                >
                    <Typography
                        color={error != null ? theme.palette.error.main : theme.palette.grey["600"]}
                        variant="caption"
                    >
                        {error || element.hint}
                    </Typography>

                    {
                        ((
                            element.minFiles != null &&
                            element.minFiles > 0
                        ) || (
                            element.maxFiles != null &&
                            element.maxFiles > 0
                        )) &&
                        <Typography
                            color={theme.palette.grey["600"]}
                            variant="caption"
                            sx={{
                                ml: 'auto',
                            }}
                        >
                            {
                                element.minFiles === element.maxFiles ?
                                    'Genau' :
                                    (
                                        (element.minFiles != null && element.minFiles > 0) ?
                                            'Mindestens' :
                                            'Höchstens'
                                    )
                            } {
                            humanizeNumber(element.minFiles != null && element.minFiles > 0 ? element.minFiles : element.maxFiles!)
                        } {
                            pluralize(element.minFiles != null && element.minFiles > 0 ? element.minFiles : element.maxFiles!, 'Anlage', 'Anlagen')
                        }
                        </Typography>
                    }
                </Box>
            }
        </Box>
    );
}
