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
        let cancelled = false;

        if (testClaim == null) {
            setLayout(null);
            return () => {
                cancelled = true;
            };
        }

        new ProcessNodeApiService()
            .getTesting(node.id)
            .then((data) => {
                if (cancelled) {
                    return;
                }
                setLayout(data);
            })
            .catch((err) => {
                if (cancelled) {
                    return;
                }
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Laden des Test-Layouts.'));
            });

        return () => {
            cancelled = true;
        };
    }, [dispatch, node.id, testClaim]);

    if (testClaim == null) {
        return (
            <Box
                sx={{
                    pt: 1,
                    pb: 2,
                }}
            >
                <Typography variant="h4">
                    Prozesselement testen
                </Typography>
                <Typography variant="body1"
                            mt={1} mb={2} maxWidth={400}>
                    Der Prozess befindet sich aktuell nicht in der Testphase.
                    Sobald der Prozess in die Testphase wechselt, können hier zusätzliche Eigenschaften des Elemente
                    konfiguriert werden.
                </Typography>
            </Box>
        );
    }

    if (layout == null) {
        return (
            <Box
                sx={{
                    pt: 1,
                    pb: 2,
                }}
            >
                <Typography variant="h4" mb={2}>
                    Prozesselement testen
                </Typography>
                <Skeleton/>
            </Box>
        );
    }

    return (
        <Box
            sx={{
                pt: 1,
                pb: 2,
            }}
        >
            <Typography variant="h4">
                Prozesselement testen
            </Typography>
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
