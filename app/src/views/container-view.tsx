import {BaseView} from "./base-view";
import {GroupLayoutComponent} from "../components/group-layout/group-layout-component";
import {GroupLayout} from "../models/elements/form/layout/group-layout";

export const ContainerView: BaseView<GroupLayout, any> = ({
                                                              allElements,
                                                              element,
                                                              idPrefix,
                                                          }) => {
    return (
        <GroupLayoutComponent
            allElements={allElements}
            children={element.children}
            idPrefix={idPrefix}
        />
    );
};
