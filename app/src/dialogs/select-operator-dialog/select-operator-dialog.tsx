import React, {useEffect, useMemo, useState} from 'react';
import {SelectOperatorDialogProps} from './select-operator-dialog-props';
import {NoCodeDataType, NoCodeDataTypeLabels} from '../../data/no-code-data-type';
import {SearchBaseDialog} from '../search-base-dialog/search-base-dialog';
import {OperatorInfo} from '../../components/operator-info/operator-info';
import {Box, Button, Typography} from '@mui/material';

export function SelectOperatorDialog(props: SelectOperatorDialogProps) {
    const [respectDesiredReturnType, setRespectDesiredReturnType] = useState(true);

    useEffect(() => {
        setRespectDesiredReturnType(true);
    }, [props.open]);

    const filteredOperators = useMemo(() => {
        return props
            .operators
            .filter(op => {
                return !respectDesiredReturnType || (
                    props.desiredReturnType === NoCodeDataType.Any ||
                    op.returnType === props.desiredReturnType ||
                    op.returnType === NoCodeDataType.Any
                );
            });
    }, [props.operators, respectDesiredReturnType]);

    return (
        <SearchBaseDialog
            open={props.open}
            onClose={props.onClose}
            title="Operator auswählen"
            tabs={[{
                title: 'Alle',
                options: filteredOperators,
                onSelect: props.onSelect,
                searchPlaceholder: 'Operator suchen',
                searchKeys: ['label', 'description', 'identifier'],
                primaryTextKey: 'label',
                secondaryTextKey: 'abstractDescription',
                getId: o => o.identifier,
                detailsBuilder: (option) => (
                    <OperatorInfo operator={option} />
                ),
                noSearchResultsMessage: 'Keine Operatoren gefunden, die zu Ihrem Suchbegriff passen.',
                noOptionsMessage: (
                    <Box>
                        <Typography>
                            Keine Operatoren für den Datentyp <strong>{NoCodeDataTypeLabels[props.desiredReturnType]}</strong> verfügbar.
                        </Typography>

                        <Button
                            sx={{
                                mt: 2,
                            }}
                            onClick={() => setRespectDesiredReturnType(false)}
                        >
                            Auch unpassende Operatoren anzeigen
                        </Button>
                    </Box>
                ),
            }]}
        />
    );

}