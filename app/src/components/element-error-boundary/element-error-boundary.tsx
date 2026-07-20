import React, {ErrorInfo} from 'react';
import {AlertComponent} from '../alert/alert-component';
import {Accordion, AccordionDetails, AccordionSummary, Typography} from '@mui/material';
import ExpandMoreIcon from '@aivot/mui-material-symbols-400-n25-outlined/ExpandMore';
import {AnyElement} from '../../models/elements/any-element';

interface ElementErrorBoundaryProps {
    children: React.ReactNode;
    element: AnyElement;
}

interface ElementErrorBoundaryState {
    error?: Error;
    errorInfo?: ErrorInfo;
}

export class ElementErrorBoundary extends React.Component<ElementErrorBoundaryProps, ElementErrorBoundaryState> {
    constructor(props: ElementErrorBoundaryProps) {
        super(props);
        this.state = {};
    }

    componentDidCatch(error: Error, errorInfo: ErrorInfo) {
        this.setState({
            error: error,
            errorInfo: errorInfo,
        });
    }

    render() {
        if (this.state.error != null && this.state.errorInfo != null) {
            return (
                <AlertComponent
                    title="Anzeigefehler"
                    color="error"
                >
                    <Typography>
                        Das Element konnte nicht erfolgreich angezeigt werden. Bitte überprüfen Sie die Elementstruktur.
                    </Typography>

                    <Accordion
                        sx={{
                            mt: 2,
                        }}
                    >
                        <AccordionSummary
                            expandIcon={<ExpandMoreIcon/>}
                        >
                            Mehr Informationen
                        </AccordionSummary>
                        <AccordionDetails>
                            <Typography variant="subtitle1">
                                Fehlermeldung
                            </Typography>

                            <Typography
                                component="pre"
                            >
                                {this.state.error.message}
                            </Typography>

                            <Typography
                                component="pre"
                            >
                                {this.state.error.stack}
                            </Typography>

                            <Typography
                                variant="subtitle1"
                                sx={{mt: 2}}
                            >
                                Komponentenstapel
                            </Typography>

                            <Typography
                                component="pre"
                            >
                                {this.state.errorInfo.componentStack}
                            </Typography>

                            <Typography
                                variant="subtitle1"
                                sx={{mt: 2}}
                            >
                                Elementkonfiguration
                            </Typography>

                            <Typography
                                component="pre"
                            >
                                {JSON.stringify(this.props.element, null, 4)}
                            </Typography>
                        </AccordionDetails>
                    </Accordion>
                </AlertComponent>
            );
        }

        return (
            <>
                {this.props.children}
            </>
        );
    }
}
