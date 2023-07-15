import React, { type FormEvent, type PropsWithChildren, useState } from 'react';
import { type FormPageWrapperProps } from './form-page-wrapper-props';
import { PageWrapper } from '../page-wrapper/page-wrapper';
import { Box, Button, Tab, Tabs } from '@mui/material';

export function FormPageWrapper(props: PropsWithChildren<FormPageWrapperProps>): JSX.Element {
    const {
        hasChanged,
        onSave,
        onReset,
        onDelete,
        tabs,

        children,

        ...pageWrapperProps
    } = props;

    const [currentTab, setCurrentTab] = useState(-1);

    const handleSubmit = (event: FormEvent<HTMLFormElement>): void => {
        event.preventDefault();
        event.stopPropagation();
        onSave();
    };

    return (
        <PageWrapper { ...pageWrapperProps }>
            {
                tabs != null &&
                <Tabs
                    value={ currentTab }
                    onChange={ (_, val) => {
                        setCurrentTab(parseInt(val));
                    } }
                    sx={ {
                        mb: 2,
                    } }
                >
                    <Tab
                        label="Allgemein"
                        value={ -1 }
                    />

                    {
                        tabs.map((tab, index) => (
                            <Tab
                                key={ tab.label }
                                label={ tab.label }
                                value={ index }
                            />
                        ))
                    }
                </Tabs>
            }

            {
                (tabs == null || currentTab === -1) &&
                <form onSubmit={ handleSubmit }>
                    { children }

                    <Box
                        sx={ {
                            mt: 4,
                            display: 'flex',
                        } }
                    >
                        <Button
                            type="submit"
                            disabled={ !hasChanged }
                        >
                            Speichern
                        </Button>

                        {
                            onReset != null &&
                            <Button
                                sx={ {ml: 2} }
                                type="reset"
                                color="error"
                                disabled={ !hasChanged }
                                onClick={ onReset }
                            >
                                Zurücksetzen
                            </Button>
                        }

                        {
                            onDelete != null &&
                            <Button
                                sx={ {
                                    ml: 'auto',
                                } }
                                type="button"
                                color="error"
                                onClick={ onDelete }
                            >
                                Löschen
                            </Button>
                        }
                    </Box>
                </form>
            }

            {
                tabs != null &&
                currentTab !== -1 &&
                tabs[currentTab].content
            }
        </PageWrapper>
    );
}
