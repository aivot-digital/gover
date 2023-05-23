import React, {ComponentType, ErrorInfo} from 'react';
import {connect} from 'react-redux';
import {RootState} from '../store';
import {isElementVisible} from '../utils/is-element-visible';
import {generateComponentPatch} from '../utils/generate-component-patch';
import {updateUserInput} from '../slices/customer-input-slice';
import {Alert, AlertTitle, Grid} from '@mui/material';
import {Dispatch} from '@reduxjs/toolkit';
import {CustomerInput} from '../models/customer-input';
import {CustomerInputErrors} from '../models/customer-input-errors';
import {AnyElement} from '../models/elements/any-element';
import {evaluateFunction} from "../utils/evaluate-function";
import {isAnyInputElement} from "../models/elements/form/input/any-input-element";
import Views from "../views";
import {BaseViewProps} from "../views/base-view";

interface DispatcherComponentProps<M extends AnyElement> {
    element: M;
    idPrefix?: string;
}

interface InternalProps {
    updateUserInput: (update: { key: string, value: any }) => void;
    userInputData: CustomerInput;
    adminSettings: any;
    allErrors: CustomerInputErrors;
}

interface DispatcherComponentState {
    hasError: boolean;
}

class _ViewDispatcherComponent<M extends AnyElement, V> extends React.Component<DispatcherComponentProps<M>, DispatcherComponentState> {
    constructor(props: DispatcherComponentProps<M>) {
        super(props);
        this.state = {
            hasError: false,
        };
    }

    componentDidCatch(error: Error, errorInfo: ErrorInfo) {
        this.setState({
            hasError: true,
        });
    }

    render() {
        const {
            element,
            idPrefix,
            updateUserInput,
            userInputData,
            adminSettings,
            allErrors,
        } = this.props as DispatcherComponentProps<M> & InternalProps;

        if (this.state.hasError) {
            return this.renderError();
        }

        const id = this.makeId(element, idPrefix);

        const isVisible = adminSettings.disableVisibility || isElementVisible(id, element, userInputData);

        if (!isVisible) {
            return null;
        }

        const Component: ComponentType<BaseViewProps<M, V>> | null = Views[element.type];
        if (Component == null) {
            return null;
        }

        const patchedModel = {
            ...element,
            ...generateComponentPatch(id, element, userInputData),
            id,
        };

        const value = this.makeValue(patchedModel, id, userInputData);
        const errors = this.makeErrors(patchedModel, id, allErrors);

        const viewProps: BaseViewProps<M, V> = {
            element: patchedModel,
            setValue: (value: V | null | undefined) => {
                updateUserInput({
                    key: id,
                    value,
                });
            },
            error: errors,
            value,
            idPrefix,
        };

        return (
            <Grid
                item
                xs={('weight' in element && element.weight != null) ? element.weight : 12}
                id={element.id}
            >
                <Component {...viewProps} />
            </Grid>
        );
    }

    renderError() {
        return (
            <Alert severity="error">
                <AlertTitle>
                    Interner Systemfehler
                </AlertTitle>
                Das Element mit der Id "{this.props.element.id}" konnte nicht erfolgreich erzeugt werden.
                Bitte überprüfen Sie die Elementstruktur.
            </Alert>
        );
    }

    makeId(model: AnyElement, idPrefix?: string | null): string {
        return idPrefix != null ? (idPrefix + model.id) : model.id;
    }

    makeValue(model: AnyElement, id: string, global?: any): V | null | undefined {
        if (isAnyInputElement(model) && model.computeValue != null) {
            return evaluateFunction(model.computeValue, (global ?? {}), model, id, false);
        }
        return (model as any).value ?? (global ?? {})[id];
    }

    makeErrors(model: AnyElement, id: string, allErrors?: any): any | undefined {
        return (model as any).errors ?? (allErrors ?? {})[id];
    }
}

const mapStateToProps = (state: RootState) => ({
    userInputData: state.customerInput.input,
    adminSettings: state.adminSettings,
    allErrors: state.customerInputErrors.errors,
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
    updateUserInput: (data: any) => dispatch(updateUserInput(data)),
});

export const ViewDispatcherComponent = connect(mapStateToProps, mapDispatchToProps)(_ViewDispatcherComponent);
