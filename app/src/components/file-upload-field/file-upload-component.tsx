import React, {useRef, useState} from 'react';
import {Box, Button, FormLabel, IconButton, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography, useMediaQuery, useTheme} from '@mui/material';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {humanizeFileSize, humanizeNumber, pluralize} from '../../utils/huminization-utils';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import BackupOutlinedIcon from '@mui/icons-material/BackupOutlined';
import {FileUploadComponentProps} from './file-upload-component-props';

export function FileUploadComponent(props: FileUploadComponentProps) {
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

    const handleRemove = (file: File) => {
        if (props.value == null) {
            return;
        }

        const index = props.value.indexOf(file);
        if (index >= 0) {
            const updatedFiles = [...props.value];
            updatedFiles.splice(index, 1);
            props.onChange(updatedFiles.length > 0 ? updatedFiles : undefined);
        }
    };

    const handleDragOver: React.DragEventHandler<HTMLDivElement> = (event) => {
        event.stopPropagation();
        event.preventDefault();
        setIsDraggedOver(true);
    }

    const handleDragLeave: React.DragEventHandler<HTMLDivElement> = (event) => {
        event.stopPropagation();
        event.preventDefault();
        setIsDraggedOver(false);
    }

    const handleDrop: React.DragEventHandler<HTMLDivElement> = (event) => {
        event.stopPropagation();
        event.preventDefault();
        handleAdd(event.dataTransfer.files);
        setIsDraggedOver(false);
    };

    const handleAdd = (files: FileList) => {
        const maxFiles = props.isMultifile ? (props.maxFiles != null ? props.maxFiles : null) : 1;

        const fileUploadItems: File[] = [
            ...props.value ?? [],
        ];
        let addedItems = 0;
        for (let i = 0; (i < files.length && (maxFiles == null || fileUploadItems.length < maxFiles)); i++) {
            const file = files[i];
            fileUploadItems.push(file);
            addedItems++;
        }

        if (addedItems < files.length) {
            dispatch(showErrorSnackbar('Einige Anlagen konnten nicht hinzugefügt werden, da das Maximum überschritten wurde.'));
        }

        props.onChange(fileUploadItems.length > 0 ? fileUploadItems : undefined);
    };

    const fileMaximumReached = (
        (props.isMultifile && props.maxFiles != null && (props.value ?? []).length >= props.maxFiles) ||
        (!props.isMultifile && (props.value ?? []).length >= 1)
    );

    return (
        <Box>
            <Box
                sx={{
                    mb: 1,
                }}
            >
                <FormLabel
                    htmlFor={props.id}
                    error={props.error != null}
                >
                    {props.label}
                    {props.required && ' *'}
                </FormLabel>
            </Box>

            {
                props.value != null &&
                props.value.length > 0 &&
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
                                <TableCell />
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {
                                props.value.map(file => (
                                    <TableRow
                                        key={file.name}
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
                                                            fontSize={'small'}
                                                        />
                                                    </IconButton> :
                                                    <Button
                                                        color="error"
                                                        variant="outlined"
                                                        onClick={() => handleRemove(file)}
                                                        startIcon={
                                                            <DeleteForeverOutlinedIcon
                                                                fontSize={'small'}
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
                    display: fileMaximumReached ? 'none' : 'block',
                    p: 2,
                    border: `2px ${isFocused ? 'solid' : 'dashed'} ${props.error != null ? theme.palette.error.main : (isFocused ? theme.palette.primary.main : theme.palette.grey['500'])}`,
                    backgroundColor: theme.palette.grey['100'],
                    transition: 'background-color 250ms ease-in-out',
                    '&:hover': {
                        backgroundColor: theme.palette.grey['50'],
                    },
                    boxShadow: isDraggedOver ? `0 0 0.5em ${theme.palette.primary.main}` : undefined,
                }}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
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
                        id={props.id}
                        ref={inputRef}
                        type="file"
                        multiple={props.isMultifile}
                        accept={props.extensions != null ? props.extensions.map(ext => '.' + ext).join(',') : undefined}
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
                            sx={{color: fileMaximumReached ? theme.palette.grey['500'] : theme.palette.grey['700']}}
                        />

                        <Typography
                            sx={{
                                ml: 1,
                                color: fileMaximumReached ? theme.palette.grey['500'] : undefined,
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
                        props.extensions &&
                        props.extensions.length > 0 &&
                        <Typography
                            color={theme.palette.grey['700']}
                            variant="caption"
                        >
                            Erlaubte Dateitypen: {props.extensions.map(ext => '.' + ext).join(', ')}
                        </Typography>
                    }
                </Box>
            </Box>

            {
                (
                    props.error != null ||
                    props.hint != null ||
                    (
                        props.minFiles != null &&
                        props.minFiles > 0
                    ) || (
                        props.maxFiles != null &&
                        props.maxFiles > 0
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
                        color={props.error != null ? theme.palette.error.main : theme.palette.grey['600']}
                        variant="caption"
                    >
                        {props.error || props.hint}
                    </Typography>

                    {
                        ((
                            props.minFiles != null &&
                            props.minFiles > 0
                        ) || (
                            props.maxFiles != null &&
                            props.maxFiles > 0
                        )) &&
                        <Typography
                            color={theme.palette.grey['600']}
                            variant="caption"
                            sx={{
                                ml: 'auto',
                            }}
                        >
                            {
                                props.minFiles === props.maxFiles ?
                                    'Genau' :
                                    (
                                        (props.minFiles != null && props.minFiles > 0) ?
                                            'Mindestens' :
                                            'Höchstens'
                                    )
                            } {
                            humanizeNumber(props.minFiles != null && props.minFiles > 0 ? props.minFiles : props.maxFiles!)
                        } {
                            pluralize(props.minFiles != null && props.minFiles > 0 ? props.minFiles : props.maxFiles!, 'Anlage', 'Anlagen')
                        }
                        </Typography>
                    }
                </Box>
            }
        </Box>
    );
}
