import React, {useRef, useState} from 'react';
import {Box, FormLabel} from '@mui/material';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {humanizeFileSize} from '../../utils/humanization-utils';
import {FileUploadComponentProps} from './file-upload-component-props';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {FileUploadFileList, FileUploadHelper, FileUploadInputArea} from './file-upload-field-layout';

export function FileUploadComponent(props: FileUploadComponentProps) {
    const dispatch = useAppDispatch();
    const inputRef = useRef<HTMLInputElement | null>(null);
    const [isFocused, setIsFocused] = useState(false);
    const [isDraggedOver, setIsDraggedOver] = useState(false);

    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (props.disabled) {
            return;
        }

        if (event.target.files != null && event.target.files.length > 0) {
            handleAdd(event.target.files);
        }

        if (inputRef.current != null) {
            inputRef.current.files = null;
            inputRef.current.value = '';
        }
    };

    const handleRemove = (file: File) => {
        if (props.disabled) {
            return;
        }

        if (props.value == null) {
            return;
        }

        const index = props.value.indexOf(file);
        if (index >= 0) {
            const updatedFiles = [...props.value];
            updatedFiles.splice(index, 1);
            props.onChange(updatedFiles.length > 0 ? updatedFiles : null);
        }
    };

    const handleDragOver: React.DragEventHandler<HTMLDivElement> = (event) => {
        if (props.disabled) {
            return;
        }

        event.stopPropagation();
        event.preventDefault();
        setIsDraggedOver(true);
    };

    const handleDragLeave: React.DragEventHandler<HTMLDivElement> = (event) => {
        if (props.disabled) {
            return;
        }

        event.stopPropagation();
        event.preventDefault();
        setIsDraggedOver(false);
    };

    const handleDrop: React.DragEventHandler<HTMLDivElement> = (event) => {
        if (props.disabled) {
            return;
        }

        event.stopPropagation();
        event.preventDefault();
        handleAdd(event.dataTransfer.files);
        setIsDraggedOver(false);
    };

    const handleAdd = (files: FileList) => {
        if (props.disabled) {
            return;
        }

        const maxFiles = props.isMultifile ? (props.maxFiles != null && props.maxFiles > 0 ? props.maxFiles : null) : 1;

        const fileUploadItems: File[] = [
            ...(props.value ?? []),
        ];
        let addedItems = 0;
        for (let i = 0; (i < files.length && (maxFiles == null || fileUploadItems.length < maxFiles)); i++) {
            const file = files[i];
            fileUploadItems.push(file);
            addedItems++;
        }

        if (addedItems < files.length) {
            dispatch(showErrorSnackbar('Einige Dateien konnten nicht hinzugefügt werden, da das Maximum überschritten wurde.'));
        }

        props.onChange(fileUploadItems.length > 0 ? fileUploadItems : null);
    };

    const fileMaximumReached = (
        props.isMultifile &&
        props.maxFiles != null &&
        props.maxFiles > 0 &&
        (props.value ?? []).length >= props.maxFiles
    ) || (
        !props.isMultifile &&
        (props.value ?? []).length >= 1
    );
    const isUploadDisabled = props.disabled || fileMaximumReached;

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
                    disabled={props.disabled}
                >
                    {props.label}
                    {props.required && ' *'}
                </FormLabel>
            </Box>

            {
                props.value != null &&
                props.value.length > 0 &&
                <FileUploadFileList
                    items={props.value.map((file, index) => ({
                        key: `${file.name}-${file.lastModified}-${index}`,
                        name: file.name,
                        size: humanizeFileSize(file.size),
                        actionLabel: `${file.name} entfernen`,
                        actionIcon: <Delete fontSize="small" />,
                        actionDisabled: props.disabled,
                        onAction: () => handleRemove(file),
                    }))}
                />
            }

            {
                !fileMaximumReached &&
                <FileUploadInputArea
                    id={props.id}
                    inputRef={inputRef}
                    multiple={props.isMultifile}
                    extensions={props.extensions}
                    disabled={isUploadDisabled}
                    error={props.error != null}
                    focused={isFocused}
                    draggedOver={isDraggedOver}
                    placeholder={props.placeholder}
                    onChange={handleChange}
                    onFocus={() => setIsFocused(true)}
                    onBlur={() => setIsFocused(false)}
                    onDragOver={handleDragOver}
                    onDragLeave={handleDragLeave}
                    onDrop={handleDrop}
                />
            }

            <FileUploadHelper
                error={props.error}
                hint={props.hint}
                fileCount={(props.value ?? []).length}
                minFiles={props.minFiles}
                maxFiles={props.maxFiles}
            />
        </Box>
    );
}
