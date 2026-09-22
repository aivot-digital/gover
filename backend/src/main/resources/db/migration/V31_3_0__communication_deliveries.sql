CREATE TABLE communication_deliveries (
    id UUID PRIMARY KEY,
    provider_id INTEGER REFERENCES communication_providers(id) ON DELETE SET NULL,
    process_instance_id BIGINT REFERENCES process_instances(id) ON DELETE CASCADE,
    task_id BIGINT UNIQUE REFERENCES process_instance_tasks(id) ON DELETE CASCADE,
    definition_key VARCHAR(255) NOT NULL,
    definition_version INTEGER NOT NULL,
    configuration JSONB NOT NULL,
    status VARCHAR(32) NOT NULL,
    receipt JSONB NOT NULL DEFAULT '{}',
    continuation JSONB NOT NULL DEFAULT '{}',
    next_work JSONB,
    next_task_id BIGINT REFERENCES process_instance_tasks(id) ON DELETE SET NULL,
    created TIMESTAMPTZ NOT NULL,
    updated TIMESTAMPTZ NOT NULL,
    next_check_at TIMESTAMPTZ,
    check_failures INTEGER NOT NULL DEFAULT 0,
    status_message TEXT,
    continuation_applied BOOLEAN NOT NULL DEFAULT FALSE,
    overdue_notified BOOLEAN NOT NULL DEFAULT FALSE,
    CHECK ((task_id IS NULL) = (process_instance_id IS NULL))
);

CREATE INDEX communication_deliveries_due_idx ON communication_deliveries(next_check_at)
    WHERE next_check_at IS NOT NULL;
CREATE INDEX communication_deliveries_instance_idx ON communication_deliveries(process_instance_id);

COMMENT ON COLUMN process_instance_tasks.status IS '0 Running, 1 Paused, 2 Completed, 3 Aborted, 4 Failed, 5 Restarted, 6 AwaitingPayment, 7 AwaitingCustomer, 8 AwaitingCommunication';

CREATE INDEX communication_deliveries_outbox_idx ON communication_deliveries(created) WHERE next_work IS NOT NULL;
