import React, {ErrorInfo} from "react";
import {AlertComponent} from "../alert/alert-component";

interface ErrorBoundaryProps {
    children: React.ReactNode;
}

interface ErrorBoundaryState {
    hasError: boolean;
}

export class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
    constructor(props: ErrorBoundaryProps) {
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
        if (this.state.hasError) {
            return (
                <AlertComponent
                    title="Anzeigefehler"
                    text="Das Element konnte nicht erfolgreich angezeigt werden. Bitte überprüfen Sie die Elementstruktur."
                    color="error"
                />
            );
        }

        return (
            <>
                {this.props.children}
            </>
        );
    }
}
