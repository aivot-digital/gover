import {useMemo} from 'react';
import {ElementWithParents} from '../../utils/flatten-elements';
import {generateComponentPath, generateComponentTitle} from '../../utils/generate-component-title';
import {AnyElement} from '../../models/elements/any-element';
import {isAnyInputElement} from '../../models/elements/form/input/any-input-element';
import {SearchSelectionDialog} from '../../components/selection-dialog/search-selection-dialog';
import {Box, Typography} from '@mui/material';
import Check from '@aivot/mui-material-symbols-400-n25-outlined/Check';
import {getElementIcon} from '../../data/element-type/element-icons';
import {NoCodeDataType, NoCodeDataTypeLabels} from '../../data/no-code-data-type';
import {elementMatchesDesiredNoCodeDataType} from '../../modules/nocode/data/no-code-data-type-map';
import {ViewDispatcherComponent} from '../../components/view-dispatcher/view-dispatcher.component';
import {
    ViewDispatcherContextProvider,
    ViewDispatcherMode,
} from '../../components/view-dispatcher/view-dispatcher.context';

interface SelectElementDialogProps {
    allElements: ElementWithParents[];
    desiredType?: NoCodeDataType;
    open: boolean;
    onSelect: (element: AnyElement) => void;
    onClose: () => void;
    onExited?: () => void;
    primaryActionLabel?: string;
}

export function SelectElementDialog(props: SelectElementDialogProps) {
    const {
        allElements,
        desiredType,
        open,
        onSelect,
        onClose,
    } = props;

    const allElementsWithParent: {
        $: AnyElement;
        title: string;
        id: string;
        pathTitles: string;
        pathIds: string[];
    }[] = useMemo(() => {
        return allElements
            .filter(({element}) => (
                isAnyInputElement(element) &&
                elementMatchesDesiredNoCodeDataType(element.type, desiredType)
            ))
            .map(({
                      element,
                      parents,
                  }) => ({
                $: element,
                title: generateComponentTitle(element),
                id: element.id,
                pathTitles: generateComponentPath(parents),
                pathIds: parents.map(e => e.id),
            }));
    }, [allElements, desiredType]);

    const options = useMemo(() => allElementsWithParent.map(option => {
        const Icon = getElementIcon(option.$);
        return {
            id: JSON.stringify([...option.pathIds, option.id]),
            title: option.title,
            searchTerms: [option.id, option.pathTitles, ...option.pathIds],
            description: <>
                {option.pathTitles && <Box component="span"
                                           sx={{display: 'block'}}>{option.pathTitles}</Box>}
                <Box component="span"
                     sx={{
                         display: 'block',
                         typography: 'caption',
                         fontFamily: 'monospace',
                     }}>ID: {option.id}</Box>
            </>,
            icon: <Icon sx={{fontSize: 20}}/>,
            value: option.$,
        };
    }), [allElementsWithParent]);

    return <SearchSelectionDialog open={open}
                                  title="Element auswählen"
                                  searchLabel="Element suchen"
                                  detailLabel="Formularelement"
                                  options={options}
                                  onSelect={onSelect}
                                  onClose={onClose}
                                  onExited={props.onExited}
                                  primaryActionLabel={props.primaryActionLabel ?? 'Referenz verwenden'}
                                  primaryActionIcon={<Check sx={{fontSize: 18}}/>}
                                  noOptionsMessage={<Typography>{desiredType == null ? 'Keine Elemente verfügbar.'
                                      : `Keine Elemente für den Datentyp „${NoCodeDataTypeLabels[desiredType]}“ verfügbar.`}</Typography>}
                                  noSearchResultsMessage="Keine Elemente gefunden, die zu Ihrem Suchbegriff passen."
                                  renderDetails={element => (
                                      <ViewDispatcherContextProvider
                                          value={{
                                              rootElement: element,
                                              allElements: [],
                                              mode: ViewDispatcherMode.Editor,
                                              showInvisibleElements: true,
                                              rootAuthoredElementValues: {},
                                              rootDerivedData: {
                                                  effectiveValues: {},
                                                  elementStates: {},
                                              },
                                          }}
                                      >
                                          <ViewDispatcherComponent
                                              element={element}
                                              isBusy={false}
                                              isDeriving={false}
                                              authoredElementValues={{}}
                                              derivedData={{
                                                  effectiveValues: {},
                                                  elementStates: {},
                                              }}
                                              onAuthoredElementValuesChange={() => {
                                              }}
                                              onElementBlur={undefined}
                                              derivationTriggerIdQueue={[]}
                                              onDerive={() => Promise.resolve({
                                                  effectiveValues: {},
                                                  elementStates: {},
                                              })}
                                              onEvent={() => Promise.resolve()}
                                              onResetErrors={() => {

                                              }}
                                              suppressErrors={true}
                                          />
                                      </ViewDispatcherContextProvider>
                                  )}/>;
}
