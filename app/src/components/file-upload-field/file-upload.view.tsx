import React, {useRef, useState} from 'react';
import {Box, Button, FormLabel, IconButton, Typography, useTheme} from '@mui/material';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faTrashCanXmark, faUpload} from '@fortawesome/pro-light-svg-icons';
import {BaseViewProps} from "../_lib/base-view-props";
import {
    FileUploadElement,
    FileUploadElementItem
} from "../../models/elements/form-elements/input-elements/file-upload-element";

export function FileUploadView({
                                   element,
                                   value,
                                   error,
                                   setValue
                               }: BaseViewProps<FileUploadElement, FileUploadElementItem[]>) {
    const theme = useTheme();
    const inputRef = useRef<HTMLInputElement | null>(null);
    const [isFocused, setIsFocused] = useState(false);

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
                setValue(updatedFiles);
            }
        }
    };

    const handleDrop: React.DragEventHandler<HTMLDivElement> = (event) => {
        handleAdd(event.dataTransfer.files);
    };

    const handleAdd = (files: FileList) => {
        const maxFiles = element.isMultifile ? (element.maxFiles != null ? element.maxFiles : null) : 1;

        const fileUploadItems: FileUploadElementItem[] = [
            ...value ?? [],
        ];
        for (let i = 0; (i < files.length && (maxFiles == null || fileUploadItems.length < maxFiles)); i++) {
            const file = files[i];
            fileUploadItems.push({
                name: file.name,
                uri: URL.createObjectURL(file),
            });
        }
        setValue(fileUploadItems);
    }

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
                    htmlFor={element.id}
                    error={error != null}
                >
                    {element.label}
                    {element.required && ' *'}
                </FormLabel>
            </Box>

            <Box
                sx={{
                    p: 2,
                    border: `2px ${isFocused ? 'solid' : 'dashed'} ${error != null ? theme.palette.error.main : (isFocused ? theme.palette.primary.main : theme.palette.grey["500"])}`,
                    textAlign: 'center',
                    backgroundColor: theme.palette.grey["100"],
                    transition: 'background-color 250ms ease-in-out',
                    '&:hover': {
                        backgroundColor: theme.palette.grey["50"],
                    },
                }}
                onDrop={handleDrop}
            >
                {
                    value &&
                    value.length > 0 &&
                    <Box sx={{mb: 2}}>
                        {
                            value.map(file => (
                                <Box
                                    key={file.uri}
                                    sx={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'space-between',
                                    }}
                                >
                                    <Typography>{file.name}</Typography>
                                    <IconButton
                                        sx={{ml: 1}}
                                        onClick={() => handleRemove(file)}
                                    >
                                        <FontAwesomeIcon
                                            size="xs"
                                            icon={faTrashCanXmark}
                                        />
                                    </IconButton>
                                </Box>
                            ))
                        }
                    </Box>
                }
                <Box
                    sx={{
                        position: 'relative',
                        p: 4,
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
                        id={element.id}
                        ref={inputRef}
                        type="file"
                        multiple={element.isMultifile}
                        accept={element.extensions != null ? element.extensions.map(ext => '.' + ext).join(',') : undefined}
                        onChange={handleChange}
                        onFocus={() => setIsFocused(true)}
                        onBlur={() => setIsFocused(false)}
                        disabled={fileMaximumReached}
                    />
                    <Box>
                        <FontAwesomeIcon
                            icon={faUpload}
                            size="3x"
                            color={fileMaximumReached ? theme.palette.grey["500"] : theme.palette.grey["700"]}
                        />

                        <Typography
                            sx={{
                                mt: 4,
                                color: fileMaximumReached ? theme.palette.grey["500"] : undefined,
                            }}
                        >
                            Datei mit Drag & Drop hier reinziehen
                        </Typography>

                        <Typography
                            sx={{
                                my: 2,
                                color: fileMaximumReached ? theme.palette.grey["500"] : undefined,
                            }}
                        >
                            oder
                        </Typography>

                        <Button
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
                        <Box
                            sx={{
                                mt: 4,
                            }}
                        >
                            <Typography
                                color={theme.palette.grey["700"]}
                                variant="caption"
                            >
                                Erlaubte Dateitypen: {element.extensions.map(ext => '.' + ext).join(', ')}
                            </Typography>
                        </Box>
                    }
                </Box>
            </Box>

            <Box
                sx={{
                    mt: 0.5,
                    ml: 1.5,
                }}
            >
                <Typography
                    color={theme.palette.grey["600"]}
                    variant="caption"
                >
                    {element.hint}
                </Typography>
            </Box>
        </Box>
    );
}
