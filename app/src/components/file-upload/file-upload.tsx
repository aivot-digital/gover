import React, {useRef} from 'react';
import {Box, IconButton, Typography} from '@mui/material';
import {FileUploadProps} from './file-upload-props';
import FileUploadOutlinedIcon from '@mui/icons-material/FileUploadOutlined';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';

/**
 * @deprecated Use FieldUploadFieldComponent
 * @param extensions
 * @param multiple
 * @param onChange
 * @param value
 * @constructor
 */
export function FileUpload({
                               extensions,
                               multiple,
                               onChange,
                               value,
                           }: FileUploadProps) {
    const inputRef = useRef<HTMLInputElement | null>(null);

    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (event.target.files != null && event.target.files.length > 0) {
            const files: File[] = [];
            for (let i = 0; i < event.target.files.length; i++) {
                files.push(event.target.files[i]);
            }
            onChange(files);
        }
    };

    const handleRemove = (file: File) => {
        const index = value.indexOf(file);
        if (index >= 0) {
            const updatedFiles = [...value];
            updatedFiles.splice(index, 1);
            onChange(updatedFiles);
        }
    };

    return (
        <Box
            sx={{
                p: 2,
                border: '2px dashed #eee',
                textAlign: 'center',
                backgroundColor: '#fafafa',
                cursor: 'pointer',
                transition: 'background-color 250ms ease-in-out',
                '&:hover': {
                    backgroundColor: '#fefefe',
                },
            }}
        >
            {
                value.length > 0 &&
                <Box sx={{mb: 2}}>
                    {
                        value.map(file => (
                            <Box
                                key={file.name}
                                sx={{
                                    display: 'flex',
                                    alignItems: 'center',
                                }}
                            >
                                <Typography>{file.name}</Typography>
                                <IconButton
                                    sx={{ml: 1}}
                                    onClick={() => handleRemove(file)}
                                >
                                    <DeleteForeverOutlinedIcon/>
                                </IconButton>
                            </Box>
                        ))
                    }
                </Box>
            }
            <Box
                onClick={() => {
                    if (inputRef.current != null) {
                        inputRef.current.click();
                    }
                }}
            >
                <input
                    ref={inputRef}
                    type="file"
                    hidden
                    multiple={multiple}
                    accept={extensions != null ? extensions.map(ext => '.' + ext).join(',') : undefined}
                    onChange={handleChange}
                />
                <Box>
                    <FileUploadOutlinedIcon sx={{
                        color: '#aaa',
                        fontSize: "3rem",
                    }}/>
                </Box>
                <Box>
                    <Typography
                        sx={{mt: 1}}
                        color="#aaa"
                    >
                        Klicken Sie hier, um eine Datei für den Upload auszuwählen
                    </Typography>
                </Box>
                {
                    extensions != null &&
                    <Box>
                        <Typography
                            color="#ccc"
                            variant="caption"
                            sx={{mt: 1}}
                        >
                            Akzeptierte Formate: {extensions.map(ext => '.' + ext).join(', ')}
                        </Typography>
                    </Box>
                }
            </Box>
        </Box>
    );
}
