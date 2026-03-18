import {AnyElement} from '../../../models/elements/any-element';
import {AuthoredElementValues} from '../../../models/element-data';
import {Box} from '@mui/material';
import React, {useMemo, useState} from 'react';
import {Actions} from '../../actions/actions';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import {format} from 'date-fns';
import {downloadObjectFile, uploadObjectFile} from '../../../utils/download-utils';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {ExpandableCodeBlock} from '../../expandable-code-block/expandable-code-block';
import {SearchInput} from '../../search-input/search-input';
import {filterAuthoredElementValues, walkAuthoredElementValues} from '../../../utils/element-data-utils';
import {cleanAuthoredElementValues} from '../../../utils/element-data-utils';

interface ElementDataDebuggerProps {
    dataLabel: string;
    rootElement: AnyElement;
    elementData: AuthoredElementValues;
    onLoadElementData: (elementData: AuthoredElementValues) => void;
}

export function ElementDataDebugger(props: ElementDataDebuggerProps) {
    const {
        dataLabel,
        rootElement,
        elementData,
        onLoadElementData,
    } = props;

    const dispatch = useAppDispatch();
    const [elementIdSearch, setElementIdSearch] = useState<string>('');

    const handleExport = (): void => {
        const cleanedElementData = cleanAuthoredElementValues(rootElement, elementData);

        const filename = `${dataLabel} ${format(new Date(), 'dd-MM-yyyy')}.json`;
        downloadObjectFile(filename, cleanedElementData);
    };

    const handleUpload = (): void => {
        uploadObjectFile<AuthoredElementValues>('.json')
            .then((res) => {
                if (res == null) {
                    onLoadElementData({});
                } else {
                    let isValid = false;
                    walkAuthoredElementValues(rootElement, res, (_, value) => {
                        if (value != null) {
                            isValid = true;
                        }
                    });

                    if (!isValid) {
                        dispatch(showErrorSnackbar('Die Datei enthält keine gültigen Nutzereingaben'));
                        return;
                    }

                    onLoadElementData(cleanAuthoredElementValues(rootElement, res));
                }
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Nutzereingaben konnten nicht geladen werden'));
            });
    };

    const elementDataToDisplay = useMemo(() => {
        const search = elementIdSearch
            .toLowerCase()
            .trim();

        if (search.length === 0) {
            return elementData;
        }

        return filterAuthoredElementValues(rootElement, elementData, (e) => e.id.toLowerCase().includes(search));
    }, [rootElement, elementData, elementIdSearch]);

    const jsonString = useMemo(() => {
        return JSON.stringify(elementDataToDisplay, null, 2);
    }, [elementDataToDisplay]);

    return (
        <Box>
            <Box
                sx={{
                    display: 'flex',
                    mb: 2,
                }}
            >
                <SearchInput
                    label="Element-ID suchen"
                    placeholder="Element-ID eingeben"
                    value={elementIdSearch}
                    onChange={setElementIdSearch}
                />

                <Actions
                    sx={{
                        ml: 'auto',
                    }}
                    actions={[
                        {
                            tooltip: 'Exportieren',
                            label: 'Exportieren',
                            icon: <FileDownloadOutlinedIcon />,
                            onClick: handleExport,
                        },
                        {
                            tooltip: 'Importieren',
                            label: 'Importieren',
                            icon: <UploadFileOutlinedIcon />,
                            onClick: handleUpload,
                        },
                    ]}
                />
            </Box>

            <ExpandableCodeBlock
                value={jsonString}
            />
        </Box>
    );
}
