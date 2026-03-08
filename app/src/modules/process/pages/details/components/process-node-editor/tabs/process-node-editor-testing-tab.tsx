import {Box, Skeleton} from '@mui/material';
import {useProcessNodeEditorContext} from '../process-node-editor-context';
import Typography from '@mui/material/Typography';
import React, {type ReactNode, useEffect, useState} from 'react';
import {type GroupLayout} from '../../../../../../../models/elements/form/layout/group-layout';
import {ProcessNodeApiService} from '../../../../../services/process-node-api-service';
import {useAppDispatch} from '../../../../../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../../../../../slices/snackbar-slice';
import {ElementDerivationContext} from '../../../../../../elements/components/element-derivation-context';

export function ProcessNodeEditorTestingTab(): ReactNode {
    const dispatch = useAppDispatch();

    const {
        node,
        testClaim,
    } = useProcessNodeEditorContext();

    const [layout, setLayout] = useState<GroupLayout | null>(null);

    useEffect(() => {
        if (testClaim == null) {
            setLayout(null);
        }

        new ProcessNodeApiService()
            .getTesting(node.id)
            .then(setLayout)
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Laden des Test-Layouts.'));
            });
    }, [testClaim]);

    if (testClaim == null) {
        return (
            <Typography>
                Der Prozess befindet sich aktuell nicht in der Testphase.
                Sobald der Prozess in die Testphase wechselt, können hier zusätzliche Eigenschaften des Elemente
                konfiguriert werden.
            </Typography>
        );
    }

    if (layout == null) {
        return (
            <Box sx={{py: 1}}>
                <Skeleton/>
            </Box>
        );
    }

    return (
        <Box sx={{py: 1}}>
            <ElementDerivationContext
                element={layout}
                elementData={{}}
                onElementDataChange={() => {
                    // Nothing to do here, since the data is not persisted.
                }}
            />
        </Box>
    );
}
