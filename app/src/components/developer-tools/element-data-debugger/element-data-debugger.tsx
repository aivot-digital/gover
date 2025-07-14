import {AnyElement} from '../../../models/elements/any-element';
import {ElementData} from '../../../models/element-data';
import {Box} from '@mui/material';
import React from 'react';
import {Actions} from '../../actions/actions';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import {format} from 'date-fns';
import {downloadObjectFile, uploadObjectFile} from '../../../utils/download-utils';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {ExpandableCodeBlock} from '../../expandable-code-block/expandable-code-block';

interface ElementDataDebuggerProps {
    rootElement: AnyElement;
    elementData: ElementData;
    onLoadElementData: (elementData: ElementData) => void;
}

export function ElementDataDebugger(props: ElementDataDebuggerProps) {
    const {
        rootElement,
        elementData,
        onLoadElementData,
    } = props;

    const dispatch = useAppDispatch();

    const handleExport = (): void => {
        const filename = `element_daten_${format(new Date(), 'dd-MM-yyyy')}.json`;
        downloadObjectFile(filename, elementData);
    };

    const handleUpload = (): void => {
        uploadObjectFile<ElementData>('.json')
            .then((res) => {
                if (res == null) {
                    onLoadElementData({});
                } else {
                    onLoadElementData(res);
                }
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Nutzereingaben konnten nicht geladen werden'));
            });
    };

    return (
        <Box>
            <Box
                sx={{
                    display: 'flex',
                    mb: 2,
                }}
            >
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
                value={JSON.stringify(elementData, null, 2)}
            />
        </Box>
    );
}
