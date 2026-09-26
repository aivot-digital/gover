import React, {type ReactNode} from 'react';
import {Box, Paper, Skeleton} from '@mui/material';
import {Allotment} from 'allotment';
import {PageWrapper} from '../../../../../components/page-wrapper/page-wrapper';
import {ProcessNodeEditorSkeleton} from './process-node-editor/process-node-editor-skeleton';

export function ProcessDetailsPageSkeleton(): ReactNode {
    return (
        <PageWrapper
            title="Prozess"
            fullWidth={true}
            fullHeight={true}
        >
            <Box
                sx={{
                    height: '100vh',
                }}
            >
                <Allotment>
                    <Allotment.Pane minSize={760}>
                        <Box
                            sx={{
                                px: 2,
                                py: 2,
                                height: '100%',
                                display: 'flex',
                                flexDirection: 'column',
                            }}
                        >
                            <Box
                                sx={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: 1.5,
                                }}
                            >
                                <Skeleton
                                    variant="circular"
                                    width={32}
                                    height={32}
                                />
                                <Skeleton
                                    width={360}
                                    height={38}
                                />
                                <Skeleton
                                    variant="rounded"
                                    width={94}
                                    height={28}
                                />

                                <Box
                                    sx={{
                                        marginLeft: 'auto',
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: 1,
                                    }}
                                >
                                    <Skeleton variant="rounded" width={40} height={40}/>
                                    <Skeleton variant="rounded" width={40} height={40}/>
                                    <Skeleton variant="rounded" width={104} height={40}/>
                                    <Skeleton variant="rounded" width={40} height={40}/>
                                </Box>
                            </Box>

                            <Box
                                sx={{
                                    flex: 1,
                                    minHeight: 0,
                                    mt: 2,
                                    mb: -2,
                                    ml: -2,
                                    mr: -2,
                                    position: 'relative',
                                    overflow: 'hidden',
                                    backgroundColor: '#f8fafc',
                                    borderTop: '1px solid rgba(15, 23, 42, 0.08)',
                                }}
                            >
                                <Box
                                    sx={{
                                        position: 'absolute',
                                        inset: 0,
                                        backgroundImage: 'radial-gradient(circle, rgba(148, 163, 184, 0.35) 1px, transparent 1px)',
                                        backgroundSize: '24px 24px',
                                        opacity: 0.8,
                                    }}
                                />

                                <Skeleton
                                    variant="rounded"
                                    width={180}
                                    height={44}
                                    sx={{
                                        position: 'absolute',
                                        top: 16,
                                        left: 16,
                                        borderRadius: '6px',
                                    }}
                                />

                                <Skeleton
                                    variant="rounded"
                                    width={208}
                                    height={120}
                                    sx={{
                                        position: 'absolute',
                                        top: '16%',
                                        left: '12%',
                                        borderRadius: '14px',
                                    }}
                                />
                                <Skeleton
                                    variant="rounded"
                                    width={224}
                                    height={120}
                                    sx={{
                                        position: 'absolute',
                                        top: '24%',
                                        left: '48%',
                                        borderRadius: '14px',
                                    }}
                                />
                                <Skeleton
                                    variant="rounded"
                                    width={216}
                                    height={120}
                                    sx={{
                                        position: 'absolute',
                                        top: '52%',
                                        left: '30%',
                                        borderRadius: '14px',
                                    }}
                                />

                                <Skeleton
                                    variant="rounded"
                                    width={38}
                                    height={194}
                                    sx={{
                                        position: 'absolute',
                                        left: 16,
                                        bottom: 16,
                                        borderRadius: '6px',
                                    }}
                                />

                                <Skeleton
                                    variant="rounded"
                                    width={184}
                                    height={116}
                                    sx={{
                                        position: 'absolute',
                                        right: 16,
                                        bottom: 16,
                                        borderRadius: '10px',
                                    }}
                                />
                            </Box>
                        </Box>
                    </Allotment.Pane>

                    <Allotment.Pane
                        minSize={560}
                        preferredSize={560}
                    >
                        <Paper
                            sx={{
                                px: 0,
                                boxShadow: '0px 4px 15px rgba(0, 0, 0, 0.1)',
                                borderLeft: '1px solid #E0E7E0',
                                borderRadius: 0,
                                position: 'relative',
                                height: '100%',
                                overflow: 'hidden',
                            }}
                        >
                            <ProcessNodeEditorSkeleton height="100%"/>
                        </Paper>
                    </Allotment.Pane>
                </Allotment>
            </Box>
        </PageWrapper>
    );
}
