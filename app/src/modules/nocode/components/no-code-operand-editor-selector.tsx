import {Box, Divider, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Typography} from '@mui/material';
import Article from '@aivot/mui-material-symbols-400-n25-outlined/Article';
import DatabaseSearch from '@aivot/mui-material-symbols-400-n25-outlined/DatabaseSearch';
import MyLocation from '@aivot/mui-material-symbols-400-n25-outlined/MyLocation';
import Functions from '@aivot/mui-material-symbols-400-n25-outlined/Functions';
import ArrowBack from '@aivot/mui-material-symbols-400-n25-outlined/ArrowBack';
import {NoCodeOperand} from '../../../models/functions/no-code-expression';
import {useEffect, useRef, useState} from 'react';
import {useNormalizedReactId} from '../../../hooks/use-normalized-react-id';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {SelectOperatorDialog} from '../../../dialogs/select-operator-dialog/select-operator-dialog';
import {NoCodeOperatorDetailsDTO, NoCodeParameterOption, resolveNoCodeSignature} from '../../../models/dtos/no-code-operator-details-dto';
import {NoCodeOperandEditorContextType} from './no-code-operand-editor';

interface NoCodeOperandEditorSelectorProps {
    allOperators: NoCodeOperatorDetailsDTO[];
    label: string;
    hint?: string | null;
    desiredType?: NoCodeDataType;
    parameterOptions?: NoCodeParameterOption[];
    onChange: (value: NoCodeOperand, editAfterInsert?: boolean) => void;
    contextType?: NoCodeOperandEditorContextType;
}

export function NoCodeOperandEditorSelector({
                                                allOperators,
                                                label,
                                                hint,
                                                desiredType = NoCodeDataType.Runtime,
                                                parameterOptions = [],
                                                onChange,
                                                contextType = 'BOTH',
                                            }: NoCodeOperandEditorSelectorProps) {
    const [showOperatorSelector, setShowOperatorSelector] = useState(false);
    const [showAlternativeSources, setShowAlternativeSources] = useState(false);
    const listRef = useRef<HTMLUListElement>(null);
    const focusAfterNavigation = useRef(false);
    const id = useNormalizedReactId();
    const hasParameterOptions = parameterOptions.length > 0;
    const showDirectOptions = hasParameterOptions && !showAlternativeSources;
    const navigate = (alternativeSources: boolean) => {
        focusAfterNavigation.current = true;
        setShowAlternativeSources(alternativeSources);
    };

    useEffect(() => {
        if (focusAfterNavigation.current) {
            listRef.current?.querySelector('button')?.focus();
            focusAfterNavigation.current = false;
        }
    }, [showAlternativeSources]);

    const sourceOptions = [
        ...(hasParameterOptions ? [] : [{
            title: 'Wert selbst festlegen',
            description: 'Einen Wert direkt eingeben oder aus verfügbaren Optionen auswählen. Der festgelegte Wert wird bei jeder Ausführung unverändert verwendet.',
            icon: Article,
            select: () => onChange({
                type: 'NoCodeStaticValue',
                value: '',
            }),
        }]),
        {
            title: 'Mit Funktion ermitteln',
            description: 'Das Ergebnis einer Funktion verwenden. Die Funktion ermittelt den Wert bei der Ausführung.',
            icon: Functions,
            select: () => setShowOperatorSelector(true),
        },
        ...(contextType === 'PROCESS' ? [] : [
            {
                title: 'Aus Formularelement übernehmen',
                description: 'Den aktuellen eingegebenen oder berechneten Wert eines Formularelements verwenden.',
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
                        }}>{showDirectOptions ? `${label} auswählen` : label}</Typography>
            {hint && <Typography variant="body2"
                                 color="text.secondary"
                                 sx={{
                                     px: 1.5,
                                     mt: 0.5,
                                 }}>{hint}</Typography>}
            <List ref={listRef} aria-labelledby={`${id}-title`}>
                {showDirectOptions ? <>
                    {parameterOptions.map((option, index) => <ListItem key={`${index}-${option.value}`} disablePadding>
                        <ListItemButton component="button"
                                        type="button"
                                        onClick={() => onChange({
                                            type: 'NoCodeStaticValue',
                                            value: option.value,
                                        }, false)}
                                        sx={{borderRadius: 1, px: 1.5, width: '100%'}}>
                            <ListItemText primary={option.label}
                                          slotProps={{primary: {sx: {overflowWrap: 'anywhere'}}}}/>
                        </ListItemButton>
                    </ListItem>)}
                    <Divider component="li"/>
                    <ListItem disablePadding>
                        <ListItemButton component="button"
                                        type="button"
                                        onClick={() => navigate(true)}
                                        sx={{borderRadius: 1, px: 1.5, width: '100%'}}>
                            <ListItemText primary="Andere Wertquelle verwenden …"/>
                        </ListItemButton>
                    </ListItem>
                </> : <>
                    {hasParameterOptions && <ListItem disablePadding>
                        <ListItemButton component="button"
                                        type="button"
                                        onClick={() => navigate(false)}
                                        sx={{borderRadius: 1, px: 1.5, width: '100%'}}>
                            <ListItemIcon sx={{minWidth: 32}}><ArrowBack fontSize="small"/></ListItemIcon>
                            <ListItemText primary="Zurück zur Auswahl"/>
                        </ListItemButton>
                    </ListItem>}
                    {sourceOptions.map(({
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
                                              primary: {sx: {fontWeight: 600, overflowWrap: 'anywhere'}},
                                              secondary: {
                                                  id: `${id}-option-${index}`,
                                                  sx: {fontSize: '0.875rem'},
                                              },
                                          }}/>
                        </ListItemButton>
                    </ListItem>)}
                </>}
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
