import React, {useMemo, useRef, useState} from 'react';
import {Box, FormLabel} from '@mui/material';
import {FileUploadElement, FileUploadElementItem} from '../../models/elements/form/input/file-upload-element';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showErrorSnackbar} from '../../slices/snackbar-slice';
import {humanizeFileSize} from '../../utils/humanization-utils';
import {BaseViewProps} from '../../views/base-view';
import {hasDerivableAspects} from '../../utils/has-derivable-aspects';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import Download from '@aivot/mui-material-symbols-400-n25-outlined/Download';
import {BaseApiService} from '../../services/base-api-service';
import {FileUploadFileList, FileUploadHelper, FileUploadInputArea} from './file-upload-field-layout';

const PROCESS_INSTANCE_ATTACHMENT_URI_PREFIX = 'process-instance-attachment:';

export function FileUploadView(props: BaseViewProps<FileUploadElement, FileUploadElementItem[]>) {
    const {
        element,
        setValue,
        value,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const {
        disabled,
        extensions,
    } = element;

    const isDisabled = useMemo(() => {
        return disabled || isGloballyDisabled;
    }, [disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    const dispatch = useAppDispatch();
    const inputRef = useRef<HTMLInputElement | null>(null);
    const [isFocused, setIsFocused] = useState(false);
    const [isDraggedOver, setIsDraggedOver] = useState(false);

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
        // Persisted process attachments are backend-owned; removing them here would only corrupt the form value.
        if (isProcessInstanceAttachment(file)) {
            return;
        }

        if (value != null) {
            const index = value.indexOf(file);
            if (index >= 0) {
                const updatedFiles = [...value];
                updatedFiles.splice(index, 1);
                setValue(updatedFiles.length > 0 ? updatedFiles : null);
                if (file.uri.startsWith('blob:')) {
                    URL.revokeObjectURL(file.uri);
                }
            }
        }
    };

    const handleDownload = async (file: FileUploadElementItem) => {
        const attachmentKey = resolveProcessInstanceAttachmentKey(file);
        if (attachmentKey == null) {
            return;
        }

        try {
            const blob = await new BaseApiService().getBlob(`/api/process-instance-attachments/${encodeURIComponent(attachmentKey)}/file/?download=true`);
            const objectUrl = URL.createObjectURL(blob);

            const link = document.createElement('a');
            link.href = objectUrl;
            link.download = file.name;
            link.style.display = 'none';

            document.body.appendChild(link);
            link.click();
            link.remove();

            URL.revokeObjectURL(objectUrl);
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Der Anhang konnte nicht heruntergeladen werden.'));
        }
    };

    const handleDrop: React.DragEventHandler<HTMLDivElement> = (event) => {
        if (!isDisabled) {
            handleAdd(event.dataTransfer.files);
        }
        setIsDraggedOver(false);
    };

    const handleAdd = (originalFileList: FileList) => {
        const cleanedFiles: File[] = [];
        for (let i = 0; i < originalFileList.length; i++) {
            const file = originalFileList[i];

            if (extensions == null || extensions.length === 0) {
                cleanedFiles.push(file);
            } else {
                const fileExtension = file.name.split('.').pop();

                if (fileExtension == null || extensions.every(ext => ext.toLowerCase() !== fileExtension.toLowerCase())) {
                    dispatch(showErrorSnackbar('Die Datei ' + file.name + ' hat ein ungültiges Dateiformat.'));
                } else {
                    cleanedFiles.push(file);
                }
            }
        }

        const maxFiles = element.isMultifile ? (element.maxFiles != null && element.maxFiles > 0 ? element.maxFiles : null) : 1;

        const fileUploadItems: FileUploadElementItem[] = [
            ...(value ?? []),
        ];
        let addedItems = 0;
        for (let i = 0; (i < cleanedFiles.length && (maxFiles == null || fileUploadItems.length < maxFiles)); i++) {
            const file = cleanedFiles[i];
            fileUploadItems.push({
                name: file.name,
                originalFileName: file.name,
                uri: URL.createObjectURL(file),
                size: file.size,
            });
            addedItems++;
        }

        if (addedItems < cleanedFiles.length) {
            dispatch(showErrorSnackbar('Einige Dateien konnten nicht hinzugefügt werden, da das Maximum überschritten wurde.'));
        }

        setValue(fileUploadItems.length > 0 ? fileUploadItems : null);
    };

    const fileMaximumReached = (
        element.isMultifile &&
        element.maxFiles != null &&
        element.maxFiles > 0 &&
        (value ?? []).length >= element.maxFiles
    ) || (
        !element.isMultifile &&
        (value ?? []).length >= 1
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
                    error={errors != null && errors.length > 0}
                    disabled={isDisabled || isBusy}
                >
                    {element.label}
                    {element.required && ' *'}
                </FormLabel>
            </Box>

            {
                value != null &&
                value.length > 0 &&
                <FileUploadFileList
                    items={value.map(file => {
                        const isPersistedAttachment = isProcessInstanceAttachment(file);
                        const actionLabel = isPersistedAttachment
                            ? `${file.name} herunterladen`
                            : `${file.name} entfernen`;

                        return {
                            key: file.uri,
                            name: file.name,
                            size: humanizeFileSize(file.size),
                            detail: isPersistedAttachment && file.originalFileName != null
                                ? `Hochgeladen als ${file.originalFileName}`
                                : undefined,
                            actionLabel,
                            actionIcon: isPersistedAttachment
                                ? <Download fontSize="small" />
                                : <Delete fontSize="small" />,
                            actionDisabled: !isPersistedAttachment && (isDisabled || isBusy),
                            onAction: () => isPersistedAttachment
                                ? void handleDownload(file)
                                : handleRemove(file),
                        };
                    })}
                />
            }

            {
                !fileMaximumReached &&
                <FileUploadInputArea
                    id={`${element.id}-input`}
                    inputRef={inputRef}
                    multiple={element.isMultifile ?? undefined}
                    extensions={element.extensions}
                    disabled={isDisabled || isBusy}
                    error={errors != null && errors.length > 0}
                    focused={isFocused}
                    draggedOver={isDraggedOver}
                    onChange={handleChange}
                    onFocus={() => setIsFocused(true)}
                    onBlur={() => setIsFocused(false)}
                    onDragOver={(event) => {
                        event.preventDefault();
                        if (!isBusy && !isDisabled) {
                            setIsDraggedOver(true);
                        }
                    }}
                    onDragLeave={(event) => {
                        event.preventDefault();
                        setIsDraggedOver(false);
                    }}
                    onDrop={(event) => {
                        event.preventDefault();
                        if (!isBusy && !isDisabled) {
                            handleDrop(event);
                        }
                    }}
                />
            }

            <FileUploadHelper
                error={errors != null && errors.length > 0 ? errors.join(', ') : undefined}
                hint={element.hint}
                fileCount={(value ?? []).length}
                minFiles={element.minFiles}
                maxFiles={element.maxFiles}
            />
        </Box>
    );
}

function isProcessInstanceAttachment(file: FileUploadElementItem): boolean {
    return file.uri.startsWith(PROCESS_INSTANCE_ATTACHMENT_URI_PREFIX);
}

function resolveProcessInstanceAttachmentKey(file: FileUploadElementItem): string | null {
    if (!file.uri.startsWith(PROCESS_INSTANCE_ATTACHMENT_URI_PREFIX)) {
        return null;
    }

    const attachmentKey = file.uri.slice(PROCESS_INSTANCE_ATTACHMENT_URI_PREFIX.length).trim();
    return attachmentKey.length === 0 ? null : attachmentKey;
}
