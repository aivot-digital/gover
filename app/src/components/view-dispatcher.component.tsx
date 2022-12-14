import React, {ComponentType, ErrorInfo} from 'react';
import {connect} from 'react-redux';
import {RootState} from '../store';
import {isComponentVisible} from '../utils/is-component-visible';
import {generateComponentPatch} from '../utils/generate-component-patch';
import {ViewMap} from './view.map';
import {BaseViewProps} from './_lib/base-view-props';
import {updateUserInput} from '../slices/customer-input-slice';
import {Alert, AlertTitle, Grid} from '@mui/material';
import {ElementType} from '../data/element-type/element-type';
import {Dispatch} from '@reduxjs/toolkit';
import {CustomerInput} from '../models/customer-input';
import {CustomerInputErrors} from '../models/customer-input-errors';
import {AnyElement} from '../models/elements/any-element';

interface DispatcherComponentProps<M extends AnyElement> {
    model: M & any;
    idPrefix?: string;
}

interface InternalProps {
    updateUserInput: any;
    userInputData?: CustomerInput;
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
            model,
            idPrefix,
            updateUserInput,
            userInputData,
            adminSettings,
            allErrors,
        } = this.props as DispatcherComponentProps<M> & InternalProps;

        if (this.state.hasError) {
            return this.renderError();
        }

        const id = this.makeId(model, idPrefix);

        const isVisible = adminSettings.disableVisibility || isComponentVisible(id, model, userInputData);

        if (!isVisible) {
            return null;
        }

        const Component: ComponentType<BaseViewProps<M, V>> = ViewMap[model.type as ElementType];
        if (Component == null) {
            return null;
        }

        const patchedModel = {
            ...model,
            ...generateComponentPatch(id, model, userInputData),
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
                xs={model.weight ?? 12}
                id={model.id}
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
                Das Element mit der Id "{this.props.model.id}" konnte nicht erfolgreich erzeugt werden.
                Bitte ??berpr??fen Sie die Elementstruktur.
            </Alert>
        );
    }

    makeId(model: M, idPrefix?: string | null): string {
        return idPrefix != null ? (idPrefix + model.id) : model.id;
    }

    makeValue(model: M, id: string, global?: any): V | null | undefined {
        return (model as any).value ?? (global ?? {})[id];
    }

    makeErrors(model: M, id: string, allErrors?: any): any | undefined {
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
