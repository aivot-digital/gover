import React, {type FormEvent, type PropsWithChildren, useState} from 'react';
import {type FormPageWrapperProps} from './form-page-wrapper-props';
import {PageWrapper} from '../page-wrapper/page-wrapper';
import {Box, Button, Tab, Tabs} from '@mui/material';
import SaveOutlinedIcon from "@mui/icons-material/SaveOutlined";
import DeleteForeverOutlinedIcon from "@mui/icons-material/DeleteForeverOutlined";

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
        if (onSave != null) {
            onSave(event);
        }
    };

    return (
        <PageWrapper {...pageWrapperProps}>
            {
                tabs != null &&
                <Tabs
                    value={currentTab}
                    onChange={(_, val) => {
                        setCurrentTab(parseInt(val));
                    }}
                    sx={{
                        mb: 2,
                    }}
                    variant="scrollable"
                    scrollButtons="auto"
                >
                    <Tab
                        label="Allgemein"
                        value={-1}
                    />

                    {
                        tabs.map((tab, index) => (
                            <Tab
                                key={tab.label}
                                label={tab.label}
                                value={index}
                            />
                        ))
                    }
                </Tabs>
            }

            {
                (tabs == null || currentTab === -1) &&
                <form onSubmit={handleSubmit}>
                    {children}

                    <Box
                        sx={{
                            mt: 4,
                            display: 'flex',
                        }}
                    >
                        {
                            onSave != null &&
                            <Button
                                type="submit"
                                disabled={!hasChanged}
                                variant="contained"
                                startIcon={<SaveOutlinedIcon
                                    sx={{
                                        marginTop: '-2px',
                                    }}
                                />}
                            >
                                Speichern
                            </Button>
                        }

                        {
                            onReset != null &&
                            <Button
                                sx={{ml: 2}}
                                type="reset"
                                color="error"
                                disabled={!hasChanged}
                                onClick={onReset}
                            >
                                Zurücksetzen
                            </Button>
                        }

                        {
                            onDelete != null &&
                            <Button
                                sx={{
                                    ml: 'auto',
                                }}
                                type="button"
                                color="error"
                                onClick={onDelete}
                                variant="outlined"
                                startIcon={<DeleteForeverOutlinedIcon
                                    sx={{
                                        marginTop: '-4px',
                                    }}
                                />}
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
