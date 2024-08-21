import {SpacerElement} from '../../models/elements/form/content/spacer-element';
import {BaseViewProps} from "../../views/base-view";

export function SpacerComponentView({element}: BaseViewProps<SpacerElement, void>) {
    return (
        <div
            style={{height: (element.height ?? '0') + 'px'}}
        >
        </div>
    );
}
