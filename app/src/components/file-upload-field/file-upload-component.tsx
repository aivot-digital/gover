import React, {useRef, useState} from 'react';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {humanizeFileSize} from '../../utils/humanization-utils';
import {FileUploadComponentProps} from './file-upload-component-props';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {FileUploadFieldLayout} from './file-upload-field-layout';

export function FileUploadComponent(props: FileUploadComponentProps) {
    const dispatch = useAppDispatch();
    const inputRef = useRef<HTMLInputElement | null>(null);
    const [isFocused, setIsFocused] = useState(false);
    const [isDraggedOver, setIsDraggedOver] = useState(false);
    const isInteractionDisabled = props.disabled === true || props.busy === true;

    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (isInteractionDisabled) {
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
        if (isInteractionDisabled) {
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
        if (isInteractionDisabled) {
            return;
        }

        event.stopPropagation();
        event.preventDefault();
        setIsDraggedOver(true);
    };

    const handleDragLeave: React.DragEventHandler<HTMLDivElement> = (event) => {
        if (isInteractionDisabled) {
            return;
        }

        event.stopPropagation();
        event.preventDefault();
        setIsDraggedOver(false);
    };

    const handleDrop: React.DragEventHandler<HTMLDivElement> = (event) => {
        if (isInteractionDisabled) {
            return;
        }

        event.stopPropagation();
        event.preventDefault();
        handleAdd(event.dataTransfer.files);
        setIsDraggedOver(false);
    };

    const handleAdd = (files: FileList) => {
        if (isInteractionDisabled) {
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
    const isUploadDisabled = isInteractionDisabled || fileMaximumReached;

    return (
        <FileUploadFieldLayout
            id={props.id}
            label={props.label}
            ariaLabel={props.ariaLabel}
            ariaDescribedBy={props.ariaDescribedBy}
            labelAction={props.labelAction}
            required={props.required}
            disabled={props.disabled}
            busy={props.busy}
            error={props.error}
            hint={props.hint}
            fileCount={(props.value ?? []).length}
            minFiles={props.minFiles}
            maxFiles={props.maxFiles}
            items={(props.value ?? []).map((file, index) => ({
                key: `${file.name}-${file.lastModified}-${index}`,
                name: file.name,
                size: humanizeFileSize(file.size),
                contentType: file.type,
                actionLabel: `${file.name} entfernen`,
                actionIcon: <Delete fontSize="small" />,
                actionDisabled: isInteractionDisabled,
                onAction: () => handleRemove(file),
            }))}
            showInput={!fileMaximumReached}
            inputAreaProps={{
                inputRef,
                multiple: props.isMultifile,
                extensions: props.extensions,
                disabled: isUploadDisabled,
                error: props.error != null && props.error.length > 0,
                focused: isFocused,
                draggedOver: isDraggedOver,
                placeholder: props.placeholder,
                onChange: handleChange,
                onFocus: () => setIsFocused(true),
                onBlur: () => setIsFocused(false),
                onDragOver: handleDragOver,
                onDragLeave: handleDragLeave,
                onDrop: handleDrop,
            }}
            margin={props.margin ?? 'normal'}
            showOptionalIndicator={props.showOptionalIndicator}
            sx={props.sx}
            controlSx={props.controlSx}
        />
    );
}
