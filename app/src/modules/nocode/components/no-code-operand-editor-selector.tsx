import {Box, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Typography} from '@mui/material';
import Article from '@aivot/mui-material-symbols-400-n25-outlined/Article';
import DatabaseSearch from '@aivot/mui-material-symbols-400-n25-outlined/DatabaseSearch';
import MyLocation from '@aivot/mui-material-symbols-400-n25-outlined/MyLocation';
import Functions from '@aivot/mui-material-symbols-400-n25-outlined/Functions';
import {NoCodeOperand} from '../../../models/functions/no-code-expression';
import {useState} from 'react';
import {useNormalizedReactId} from '../../../hooks/use-normalized-react-id';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {SelectOperatorDialog} from '../../../dialogs/select-operator-dialog/select-operator-dialog';
import {NoCodeOperatorDetailsDTO, resolveNoCodeSignature} from '../../../models/dtos/no-code-operator-details-dto';
import {NoCodeOperandEditorContextType} from './no-code-operand-editor';

interface NoCodeOperandEditorSelectorProps {
    allOperators: NoCodeOperatorDetailsDTO[];
    label: string;
    hint?: string | null;
    desiredType?: NoCodeDataType;
    onChange: (value: NoCodeOperand) => void;
    contextType?: NoCodeOperandEditorContextType;
}

export function NoCodeOperandEditorSelector({
                                                allOperators,
                                                label,
                                                hint,
                                                desiredType = NoCodeDataType.Runtime,
                                                onChange,
                                                contextType = 'BOTH',
                                            }: NoCodeOperandEditorSelectorProps) {
    const [showOperatorSelector, setShowOperatorSelector] = useState(false);
    const id = useNormalizedReactId();
    const options = [
        {
            title: 'Operator',
            description: 'Werte vergleichen, verknüpfen oder berechnen.',
            icon: Functions,
            select: () => setShowOperatorSelector(true),
        },
        {
            title: 'Fester Wert',
            description: 'Zum Beispiel Text, eine Zahl oder einen Wahrheitswert eingeben.',
            icon: Article,
            select: () => onChange({
                type: 'NoCodeStaticValue',
                value: '',
            }),
        },
        ...(contextType === 'PROCESS' ? [] : [
            {
                title: 'Wert eines Elements',
                description: 'Einen eingegebenen oder berechneten Formularwert verwenden.',
                icon: MyLocation,
                select: () => onChange({
                    type: 'NoCodeReference',
                    elementId: '',
                }),
            },
        ]),
        ...(contextType === 'FORM' ? [] : [
            {
                title: 'Vorgangsdaten',
                description: 'Einen Wert aus den Vorgangsdaten verwenden.',
                icon: DatabaseSearch,
                select: () => onChange({
                    type: 'NoCodeProcessDataReference',
                    path: '',
                }),
            },
            {
                title: 'Geschützte Vorgangsdaten',
                description: 'Eine systemseitig bereitgestellte Information zum Vorgang verwenden.',
                icon: DatabaseSearch,
                select: () => onChange({
                    type: 'NoCodeInstanceDataReference',
                    path: '',
                }),
            },
            {
                title: 'Elementdaten',
                description: 'Ein Ergebnis eines Prozesselementes verwenden.',
                icon: DatabaseSearch,
                select: () => onChange({
                    type: 'NoCodeNodeDataReference',
                    nodeDataKey: '',
                    path: '',
                }),
            },
        ]),
    ];

    return <>
        <Box data-no-code-selector
             sx={{
                 width: 440,
                 maxWidth: '100%',
             }}>
            <Typography id={`${id}-title`}
                        variant="h6"
                        component="h3"
                        sx={{
                            px: 1.5,
                            pt: 1,
                        }}>{label}</Typography>
            {hint && <Typography variant="body2"
                                 color="text.secondary"
                                 sx={{
                                     px: 1.5,
                                     mt: 0.5,
                                 }}>{hint}</Typography>}
            <List aria-labelledby={`${id}-title`}>
                {options.map(({
                                  title,
                                  description,
                                  icon: Icon,
                                  select,
                              }, index) => <ListItem key={title}
                                                     disablePadding>
                    <ListItemButton component="button"
                                    type="button"
                                    onClick={select}
                                    alignItems="flex-start"
                                    aria-label={title}
                                    aria-describedby={`${id}-option-${index}`}
                                    sx={{
                                        borderRadius: 1,
                                        px: 1.5,
                                        width: '100%',
                                    }}>
                        <ListItemIcon sx={{
                            minWidth: 32,
                            mt: 0.75,
                        }}><Icon fontSize="small"/></ListItemIcon>
                        <ListItemText primary={title}
                                      secondary={description}
                                      slotProps={{
                                          primary: {sx: {fontWeight: 600}},
                                          secondary: {
                                              id: `${id}-option-${index}`,
                                              sx: {fontSize: '0.875rem'},
                                          },
                                      }}/>
                    </ListItemButton>
                </ListItem>)}
            </List>
        </Box>
        <SelectOperatorDialog open={showOperatorSelector}
                              operators={allOperators.filter(operator => operator.signatures.length > 0)}
                              desiredReturnType={desiredType}
                              onClose={() => setShowOperatorSelector(false)}
                              onSelect={(operator) => {
                                  setShowOperatorSelector(false);
                                  const signature = resolveNoCodeSignature(operator, {desiredReturnType: desiredType});
                                  onChange({
                                      type: 'NoCodeExpression',
                                      operatorIdentifier: operator.identifier,
                                      operands: signature.parameters.map(() => null),
                                  });
                              }}/>
    </>;
}
