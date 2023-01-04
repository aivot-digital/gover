import {SpacerElement} from '../../models/elements/form-elements/content-elements/spacer-element';
import {BaseViewProps} from '../_lib/base-view-props';

export function SpacerComponentView({element}: BaseViewProps<SpacerElement, void>) {
    return (
        <div
            style={{height: (element.height ?? '0') + 'px'}}
        >
        </div>
    );
}
