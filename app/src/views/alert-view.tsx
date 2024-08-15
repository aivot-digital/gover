import {BaseView} from "./base-view";
import {AlertElement} from "../models/elements/form/content/alert-element";
import {AlertComponent} from "../components/alert/alert-component";

export const AlertView: BaseView<AlertElement, any> = ({element}) => {
    return (
        <AlertComponent
            title={element.title}
            text={element.text ?? ''}
            color={element.alertType ?? 'info'}
            richtext
        />
    );
};
