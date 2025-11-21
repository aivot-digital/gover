-- Add new columns to support hierarchical organizational units
ALTER TABLE organizational_units
    -- Make existing columns nullable
    ALTER COLUMN address DROP NOT NULL,
    ALTER COLUMN imprint DROP NOT NULL,
    ALTER COLUMN privacy DROP NOT NULL,
    ALTER COLUMN accessibility DROP NOT NULL,
    ALTER COLUMN technical_support_address DROP NOT NULL,
    ADD COLUMN technical_support_phone VARCHAR(96) NULL,
    ADD COLUMN technical_support_info  TEXT        NULL,
    ALTER COLUMN special_support_address DROP NOT NULL,
    ADD COLUMN special_support_phone   VARCHAR(96) NULL,
    ADD COLUMN special_support_info    TEXT        NULL,
    ADD COLUMN additional_info         TEXT        NULL,
    ADD COLUMN depth                   INTEGER     NOT NULL DEFAULT 0, -- 0: Organization (Organisation), 1: Uni1t (Bereiche), 2: Department (Abteilungen) 3...N: Sub-Department (Unterabteilungen)
    ADD COLUMN parent_org_unit_id      INTEGER     NULL REFERENCES organizational_units (id) ON DELETE SET NULL;

ALTER TABLE organizational_units
    RENAME COLUMN privacy TO common_privacy;

ALTER TABLE organizational_units
    RENAME COLUMN accessibility TO common_accessibility;

-- Create a function to check for cycles and calculate depth
CREATE FUNCTION count_depth_on_org_unit_insert_or_update()
    RETURNS TRIGGER
    LANGUAGE plpgsql AS
$$
DECLARE
    v_current_id   INTEGER;
    v_parent_depth INTEGER;
BEGIN
    -- 1. If parent_org_unit_id is null, it's a top-level node.
    --    It can't create a cycle.
    --    Set depth to 0 and exit early.
    IF NEW.parent_org_unit_id IS NULL THEN
        NEW.depth = 0;
        RETURN NEW;
    END IF;

    -- 2. Check for the simplest cycle: a record pointing to itself.
    IF NEW.id = NEW.parent_org_unit_id THEN
        RAISE EXCEPTION 'cycle detected: organizational unit id % cannot be its own parent.', NEW.id;
    END IF;

    -- 3. Loop to check for deeper cycles.
    -- We start at the *new parent* and walk UP the tree.
    v_current_id := NEW.parent_org_unit_id;

    WHILE v_current_id IS NOT NULL
        LOOP
        -- 4. The cycle check:
        --    If we find the ID of the record we are *trying* to insert/update
        --    in its own ancestry, it's a cycle.
            IF v_current_id = NEW.id THEN
                RAISE EXCEPTION 'cycle detected: organizational unit hierarchy loop involving id %.', NEW.id;
            END IF;

            -- 5. Move one level up the tree for the next loop iteration.
            SELECT parent_org_unit_id
            INTO v_current_id
            FROM organizational_units
            WHERE id = v_current_id;
        END LOOP;

    -- 6. Get the depth of the parent to calculate the new depth.
    SELECT depth
    INTO v_parent_depth
    FROM organizational_units
    WHERE id = NEW.parent_org_unit_id;

    -- 7. If no such parent exists, raise an error.
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Parent org unit ID % does not exist.', NEW.parent_org_unit_id;
    END IF;

    -- 8. Set the new depth based on the parent's depth + 1.
    NEW.depth = v_parent_depth + 1;

    RETURN NEW;
END;
$$;

-- Create trigger to invoke the function before insert or update
CREATE TRIGGER on_org_unit_insert_or_update
    BEFORE INSERT OR UPDATE OF parent_org_unit_id
    ON organizational_units
    FOR EACH ROW
EXECUTE FUNCTION count_depth_on_org_unit_insert_or_update();

-- Create a function to recalculate depths for all child org units after an update
CREATE OR REPLACE FUNCTION recalculate_org_unit_child_depths()
    RETURNS TRIGGER
    LANGUAGE plpgsql AS
$$
BEGIN
    -- This function runs AFTER the parent's depth has been updated.
    -- We now use a recursive CTE to find all children and update them.

    WITH RECURSIVE sub_tree AS (
        -- 1. Anchor: Start with the direct children
        SELECT id,
               NEW.depth + 1 AS calculated_depth -- Use the parent's NEW depth
        FROM organizational_units
        WHERE parent_org_unit_id = NEW.id

        UNION ALL

        -- 2. Recursive: Find all children of the children
        SELECT d.id,
               st.calculated_depth + 1
        FROM organizational_units d
                 JOIN
             sub_tree st ON d.parent_org_unit_id = st.id)
    -- 3. Update all rows in the sub-tree
    UPDATE
        organizational_units
    SET depth = st.calculated_depth
    FROM sub_tree st
    WHERE organizational_units.id = st.id
      -- Only update rows that actually need changing
      -- This prevents infinite trigger loops
      AND organizational_units.depth IS DISTINCT FROM st.calculated_depth;

    RETURN NULL; -- AFTER triggers must return NULL
END;
$$;

CREATE TRIGGER trg_recalculate_child_depths
    AFTER UPDATE
    ON organizational_units
    FOR EACH ROW
    WHEN (OLD.parent_org_unit_id IS DISTINCT FROM NEW.parent_org_unit_id)
EXECUTE FUNCTION recalculate_org_unit_child_depths();
