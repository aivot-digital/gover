import {SelectElementDialogProps} from './select-element-dialog-props';
import {useMemo} from 'react';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectLoadedForm} from '../../slices/app-slice';
import {flattenElementsWithParents} from '../../utils/flatten-elements';
import {generateComponentTitle} from '../../utils/generate-component-title';
import {AnyElement} from '../../models/elements/any-element';
import {isAnyInputElement} from '../../models/elements/form/input/any-input-element';
import {SearchBaseDialog} from '../search-base-dialog/search-base-dialog';
import {ViewDispatcherComponent} from '../../components/view-dispatcher.component';
import {getElementIcon} from '../../data/element-type/element-icons';

export function SelectElementDialog(props: SelectElementDialogProps) {
    const form = useAppSelector(selectLoadedForm);

    const allElementsWithParent: {
        $: AnyElement;
        title: string;
        id: string;
        pathTitles: string;
        pathIds: string[];
    }[] = useMemo(() => {
        if (form == null) {
            return [];
        }
        return flattenElementsWithParents(form.root, [])
            .filter(({element}) => isAnyInputElement(element))
            .map(({element, parents}) => ({
                $: element,
                title: generateComponentTitle(element),
                id: element.id,
                pathTitles: parents.map(e => generateComponentTitle(e)).join(' > '),
                pathIds: parents.map(e => e.id),
            }));
    }, [form]);

    return (
        <SearchBaseDialog
            open={props.open}
            onClose={props.onClose}
            title="Element auswählen"
            tabs={[{
                title: 'Alle',
                options: allElementsWithParent,
                onSelect: ({$}) => props.onSelect($),
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
                            allElements={[]}
                            element={option.$}
                            isBusy={false}
                            isDeriving={false}
                        />
                    );
                },
            }]}
        />
    );
}