import {AnyElement} from '../../../../../../models/elements/any-element';
import {Function} from "../../../../../../models/functions/function";

export interface CodeTabProps<T extends AnyElement> {
    func?: Function;
    allowNoCode: boolean;
    onChange: (updatedFunc: Function | undefined) => void;
}
