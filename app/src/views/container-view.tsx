import {BaseView} from './base-view';
import {GroupLayoutComponent} from '../components/group-layout/group-layout-component';
import {GroupLayout} from '../models/elements/form/layout/group-layout';
import {useMemo} from 'react';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';

// TODO: Unify with the group layout
export const ContainerView: BaseView<GroupLayout, any> = (props) => {
    const {
        element,
        isDeriving,
    } = props;

    const pass = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    return (
        <GroupLayoutComponent
            allElements={props.allElements}
            children={props.element.children}
            idPrefix={props.idPrefix}
            isBusy={props.isBusy}
            isDeriving={props.isDeriving || pass}
            valueOverride={props.valueOverride}
            errorsOverride={props.errorsOverride}
        />
    );
};
