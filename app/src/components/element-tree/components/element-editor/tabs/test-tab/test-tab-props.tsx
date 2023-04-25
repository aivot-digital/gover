import {AnyElement} from '../../../../../../models/elements/any-element';
import {TestProtocolSet} from "../../../../../../models/lib/test-protocol-set";

export interface TestTabProps<T extends AnyElement> {
    elementModel: T;
    onPatch: (data: TestProtocolSet) => void;
}
