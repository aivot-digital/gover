import {useMemo} from 'react';
import {ElementWithParents} from '../../utils/flatten-elements';
import {generateComponentPath, generateComponentTitle} from '../../utils/generate-component-title';
import {AnyElement} from '../../models/elements/any-element';
import {isAnyInputElement} from '../../models/elements/form/input/any-input-element';
import {SearchBaseDialog} from '../search-base-dialog/search-base-dialog';
import {ViewDispatcherComponent} from '../../components/view-dispatcher.component';
import {getElementIcon} from '../../data/element-type/element-icons';

interface SelectElementDialogProps {
    allElements: ElementWithParents[];
    open: boolean;
    onSelect: (element: AnyElement) => void;
    onClose: () => void;
}

export function SelectElementDialog(props: SelectElementDialogProps) {
    const {
        allElements,
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
            .filter(({element}) => isAnyInputElement(element))
            .map(({element, parents}) => ({
                $: element,
                title: generateComponentTitle(element),
                id: element.id,
                pathTitles: generateComponentPath(parents),
                pathIds: parents.map(e => e.id),
            }));
    }, [allElements]);

    return (
        <SearchBaseDialog
            open={open}
            onClose={onClose}
            title="Element auswählen"
            tabs={[{
                title: 'Alle',
                options: allElementsWithParent,
                onSelect: ({$}) => onSelect($),
                searchPlaceholder: 'Element suchen',
                searchKeys: ['title', 'id', 'pathTitles', 'pathIds'],
                primaryTextKey: 'title',
                secondaryTextKey: 'pathTitles',
                getId: o => `${o.pathIds} > ${o.id}`,
                getIcon: (option) => {
                    const Icon = getElementIcon(option.$);
                    return <Icon />;
                },
                detailsBuilder: (option) => {
                    return (
                        <ViewDispatcherComponent
                            rootElement={option.$}
                            allElements={[]}
                            element={option.$}
                            isBusy={false}
                            isDeriving={false}
                            mode="editor"
                            authoredElementValues={{}}
                            derivedData={{effectiveValues: {}, elementStates: {}}}
                            onAuthoredElementValuesChange={() => {
                            }}
                            onElementBlur={undefined}
                            disableVisibility={true}
                            derivationTriggerIdQueue={[]}
                        />
                    );
                },
            }]}
        />
    );
}
