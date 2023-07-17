import {BaseView} from "./base-view";
import {ContainerComponent} from "../components/container/container-component";
import {GroupLayout} from "../models/elements/form/layout/group-layout";

export const ContainerView: BaseView<GroupLayout, any> = ({
                                                              allElements,
                                                              element,
                                                              idPrefix,
                                                          }) => {
    return (
        <ContainerComponent
            allElements={allElements}
            children={element.children}
            idPrefix={idPrefix}
        />
    );
};
