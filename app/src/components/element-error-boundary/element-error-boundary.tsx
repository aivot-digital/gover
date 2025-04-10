import React, {ErrorInfo} from 'react';
import {AlertComponent} from '../alert/alert-component';
import {BaseViewProps} from '../../views/base-view';
import {Accordion, AccordionDetails, AccordionSummary, Typography} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

interface ElementErrorBoundaryProps {
    children: React.ReactNode;
    viewProps: BaseViewProps<any, any>;
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
                            expandIcon={<ExpandMoreIcon />}
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
                                {JSON.stringify(this.props.viewProps.element, null, 4)}
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
