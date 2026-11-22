import {useMemo, useState} from 'react';
import {Box, Button, Typography} from '@mui/material';
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import SwapHoriz from '@aivot/mui-material-symbols-400-n25-outlined/SwapHoriz';
import Functions from '@aivot/mui-material-symbols-400-n25-outlined/Functions';
import {SelectOperatorDialogProps} from './select-operator-dialog-props';
import {NoCodeDataTypeLabels} from '../../data/no-code-data-type';
import {OperatorInfo} from '../../components/operator-info/operator-info';
import {SearchSelectionDialog} from '../../components/selection-dialog/search-selection-dialog';
import {matchesDesiredNoCodeReturnType} from '../../models/dtos/no-code-operator-details-dto';

export function SelectOperatorDialog(props: SelectOperatorDialogProps) {
    const [respectDesiredReturnType, setRespectDesiredReturnType] = useState(true);
    const options = useMemo(() => props.operators
        .filter(op => !respectDesiredReturnType || matchesDesiredNoCodeReturnType(op, props.desiredReturnType))
        .map(op => ({
            id: op.identifier,
            title: op.label,
            description: op.abstractDescription,
            searchTerms: [op.identifier, op.description, op.abstractDescription],
            icon: <Functions sx={{fontSize: 20}}/>,
            value: op,
        })), [props.operators, props.desiredReturnType, respectDesiredReturnType]);
    const replacing = props.mode === 'replace';

    return <SearchSelectionDialog open={props.open}
                                  title="Funktion auswählen"
                                  searchLabel="Funktion suchen"
                                  detailLabel="Funktion"
                                  options={options}
                                  onClose={props.onClose}
                                  onSelect={props.onSelect}
                                  primaryActionLabel={replacing ? 'Ersetzen' : 'Hinzufügen'}
                                  primaryActionIcon={replacing ? <SwapHoriz sx={{fontSize: 18}}/> :
                                      <Add sx={{fontSize: 18}}/>}
                                  onExited={() => {
                                      setRespectDesiredReturnType(true);
                                      props.onExited?.();
                                  }}
                                  renderDetails={operator => <OperatorInfo operator={operator}
                                                                           headingBaseLevel={4}/>}
                                  noSearchResultsMessage="Keine Funktionen gefunden, die zu Ihrem Suchbegriff passen."
                                  noOptionsMessage={<Box>
                                      <Typography>Keine Funktionen für den
                                                  Datentyp <strong>{NoCodeDataTypeLabels[props.desiredReturnType]}</strong> verfügbar.</Typography>
                                      {respectDesiredReturnType && <Button sx={{mt: 2}}
                                                                           onClick={() => setRespectDesiredReturnType(false)}>
                                          Auch unpassende Funktionen anzeigen
                                      </Button>}
                                  </Box>}/>;
}
