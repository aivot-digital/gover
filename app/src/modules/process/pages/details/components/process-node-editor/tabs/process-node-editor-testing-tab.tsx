import {Box, Skeleton} from '@mui/material';
import {useProcessNodeEditorContext} from '../process-node-editor-context';
import Typography from '@mui/material/Typography';
import React, {type ReactNode, useEffect, useState} from 'react';
import {type GroupLayout} from '../../../../../../../models/elements/form/layout/group-layout';
import {ProcessNodeApiService} from '../../../../../services/process-node-api-service';
import {useAppDispatch} from '../../../../../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../../../../../slices/snackbar-slice';
import {ElementDerivationContext} from '../../../../../../elements/components/element-derivation-context';
import {withDelay} from '../../../../../../../utils/with-delay';

export function ProcessNodeEditorTestingTab(): ReactNode {
    const dispatch = useAppDispatch();

    const {
        node,
        testClaim,
    } = useProcessNodeEditorContext();

    const [layout, setLayout] = useState<GroupLayout | null>(null);
    const [noTestingLayout, setNoTestingLayout] = useState<boolean>(false);

    useEffect(() => {
        let cancelled = false;

        if (testClaim == null) {
            setLayout(null);
            return () => {
                cancelled = true;
            };
        }

        withDelay(new ProcessNodeApiService()
                .getTesting(node.id)
            , 1000)
            .then((data) => {
                if (cancelled) {
                    return;
                }
                if (data == null) {
                    setNoTestingLayout(true);
                    setLayout(null);
                } else {
                    setNoTestingLayout(false);
                    setLayout(data);
                }
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

            {
                testClaim == null &&
                <Typography
                    variant="body1"
                    my={2}
                    maxWidth={400}
                >
                    Der Prozess befindet sich aktuell nicht in der Testphase.
                    Sobald der Prozess in die Testphase wechselt, können hier zusätzliche Eigenschaften des Elemente
                    konfiguriert werden.
                </Typography>
            }

            {
                testClaim != null &&
                layout == null &&
                !noTestingLayout &&
                <Skeleton
                    height={100}
                />
            }

            {
                testClaim != null &&
                noTestingLayout &&
                <Typography
                    variant="body1"
                    my={2}
                    maxWidth={400}
                >
                    Für dieses Prozesselement sind keine weiteren Testinformationen vorhanden.
                </Typography>
            }

            {
                testClaim != null &&
                !noTestingLayout &&
                layout != null &&
                <ElementDerivationContext
                    element={layout}
                    authoredElementValues={{}}
                    onAuthoredElementValuesChange={() => {
                        // Nothing to do here, since the data is not persisted.
                    }}
                />
            }
        </Box>
    );
}
