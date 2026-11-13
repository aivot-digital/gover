CREATE EXTENSION IF NOT EXISTS pg_trgm;

create function jsonb_array_element_has_value(jsonb, text, text) returns boolean
    language sql
    as $$
(select exists (select 1
                from jsonb_array_elements($1) as elem
                where elem ->> $2 = $3))
$$;

create function handle_preset_version_insert_or_update() returns trigger
    language plpgsql
as $$
begin
    if new.status = 0 then
        -- if a new draft is created all other drafts are revoked and the drafted version column for the parent preset is updated

update preset_versions
set status  = 2,
    revoked = now()
where preset_key = new.preset_key
  and status = 0
  and version <> new.version;

update presets
set drafted_version = new.version
where key = new.preset_key;
end if;

    if new.status = 1 then
        -- if a new version is published all other published versions are revoked and the published and drafted version column for the parent preset is updated

update preset_versions
set published = now()
where preset_key = new.preset_key
  and version = new.version;

update preset_versions
set revoked = now(),
    status  = 2
where preset_key = new.preset_key
  and status = 1
  and version <> new.version;

update presets
set published_version = new.version,
    drafted_version   = null
where key = new.preset_key
  and drafted_version = new.version;
end if;

    if new.status = 2 then
        -- if a version is revoked the revoked timestamp is set and if it was the published version the published version column in the parent preset is cleared

update preset_versions
set revoked = now(),
    status  = 2
where preset_key = new.preset_key
  and version = new.version;

update presets
set published_version = null
where key = new.preset_key
  and published_version = new.version;
end if;

return new;
end;
$$;

create function handle_new_preset_version() returns trigger
    language plpgsql
as $$
begin
update presets
set version_count = (select count(*) from preset_versions as vers where vers.preset_key = new.preset_key)
where key = new.preset_key;
return new;
end;
$$;

create function handle_preset_version_update_or_insert_to_set_update_timestamp() returns trigger
    language plpgsql
as $$
begin
update presets set updated = now() where key = new.preset_key;
return new;
end;
$$;

create function fun_count_depth_on_org_unit_insert_or_update() returns trigger
    language plpgsql
as $$
DECLARE
v_current_id   INTEGER;
    v_parent_depth INTEGER;
BEGIN
    -- 1. If parent_department_id is null, it's a top-level node.
    --    It can't create a cycle.
    --    Set depth to 0 and exit early.
    IF NEW.parent_department_id IS NULL THEN
        NEW.depth = 0;
RETURN NEW;
END IF;

    -- 2. Check for the simplest cycle: a record pointing to itself.
    IF NEW.id = NEW.parent_department_id THEN
        RAISE EXCEPTION 'cycle detected: organizational unit id % cannot be its own parent.', NEW.id;
END IF;

    -- 3. Loop to check for deeper cycles.
    -- We start at the *new parent* and walk UP the tree.
    v_current_id := NEW.parent_department_id;

    WHILE v_current_id IS NOT NULL
        LOOP
        -- 4. The cycle check:
        --    If we find the ID of the record we are *trying* to insert/update
        --    in its own ancestry, it's a cycle.
            IF v_current_id = NEW.id THEN
                RAISE EXCEPTION 'cycle detected: organizational unit hierarchy loop involving id %.', NEW.id;
END IF;

            -- 5. Move one level up the tree for the next loop iteration.
SELECT parent_department_id
INTO v_current_id
FROM departments
WHERE id = v_current_id;
END LOOP;

    -- 6. Get the depth of the parent to calculate the new depth.
SELECT depth
INTO v_parent_depth
FROM departments
WHERE id = NEW.parent_department_id;

-- 7. If no such parent exists, raise an error.
IF NOT FOUND THEN
        RAISE EXCEPTION 'Parent org unit ID % does not exist.', NEW.parent_department_id;
END IF;

    -- 8. Set the new depth based on the parent's depth + 1.
    NEW.depth = v_parent_depth + 1;

RETURN NEW;
END;
$$;

create function fun_recalculate_org_unit_child_depths() returns trigger
    language plpgsql
as $$
BEGIN
    -- This function runs AFTER the parent's depth has been updated.
    -- We now use a recursive CTE to find all children and update them.

WITH RECURSIVE sub_tree AS (
    -- 1. Anchor: Start with the direct children
    SELECT id,
           NEW.depth + 1 AS calculated_depth -- Use the parent's NEW depth
    FROM departments
    WHERE parent_department_id = NEW.id

    UNION ALL

    -- 2. Recursive: Find all children of the children
    SELECT d.id,
           st.calculated_depth + 1
    FROM departments d
             JOIN
         sub_tree st ON d.parent_department_id = st.id)
-- 3. Update all rows in the sub-tree
UPDATE
    departments
SET depth = st.calculated_depth
    FROM sub_tree st
WHERE departments.id = st.id
    -- Only update rows that actually need changing
    -- This prevents infinite trigger loops
  AND departments.depth IS DISTINCT FROM st.calculated_depth;

RETURN NULL; -- AFTER triggers must return NULL
END;
$$;

create function array_intersect(anyarray, anyarray) returns anyarray
    immutable
	language sql
as $$
select array(
           -- UNNEST converts array to a set of rows (elements)
           select unnest($1)
               intersect
               select unnest($2)
               -- Add ORDER BY to guarantee a consistent array order, though it's optional
               order by 1
       );
$$;

create function array_unique_union(anyarray, anyarray) returns anyarray
    immutable
	language sql
as $$
select array(
           select unnest($1)
               union
               select unnest($2)
               order by 1 -- Optional: ensures a consistent order
       );
$$;

create function array_unique_union_multi(state anyarray, col1 anyarray, col2 anyarray) returns anyarray
    immutable
	language sql
as $$
select array(
           select unnest(state)
               union
               select unnest(col1)
               union
               select unnest(col2)
               order by 1
       );
$$;

create function fun_check_deputy_circular_reference() returns trigger
    language plpgsql
as $$
begin
    if exists (select 1
               from v_user_is_recursively_deputy_for vh
               where vh.deputy_user_id = NEW.original_user_id
                 and vh.original_user_id = NEW.deputy_user_id) then
        raise exception 'Circular deputy assignment detected between users % and %', NEW.original_user_id, NEW.deputy_user_id;
end if;
return NEW;
end;
$$;

create function handle_process_version_insert() returns trigger
    language plpgsql
as $$
BEGIN
    -- Increment the version count of the parent process
UPDATE processes
SET version_count = version_count + 1,
    updated       = now()
WHERE id = NEW.process_id;

IF NEW.status = 0 THEN -- drafted
    -- Check if there is already a drafted version and raise an exception if so
        IF exists(SELECT 1
                  FROM process_versions
                  WHERE process_id = NEW.process_id
                    AND status = 0
                    AND process_version <> NEW.process_version) THEN
            RAISE EXCEPTION 'A drafted version already exists for this process.';
END IF;

        -- Set the drafted version for the process
UPDATE processes
SET drafted_version = NEW.process_version
WHERE id = NEW.process_id;
ELSIF NEW.status = 1 THEN -- published
    -- Set the published timestamp for the process version
        NEW.published = now();

        -- Set all other versions (published and drafted) to revoked
UPDATE process_versions
SET status  = 2,
    revoked = now()
WHERE process_id = NEW.process_id
  AND process_version <> NEW.process_version
  AND status IN (0, 1);

-- Set the published version for the process
UPDATE processes
SET published_version = NEW.process_version
WHERE id = NEW.process_id;
ELSEIF NEW.status = 2 THEN -- revoked
    -- Raise an exception if trying to insert a revoked version
        RAISE EXCEPTION 'Cannot insert a process version with revoked status.';
END IF;

RETURN NEW;
END;
$$;

create function handle_process_version_update() returns trigger
    language plpgsql
as $$
BEGIN
    IF NEW.status = OLD.status THEN
        -- No status change, just update the updated timestamp of the parent process
UPDATE processes
SET updated = now()
WHERE id = NEW.process_id;

RETURN NEW;
END IF;

    IF NEW.status = 0 THEN -- drafted
    -- Check if the previous status was not drafted
        IF OLD.status <> 0 THEN
            RAISE EXCEPTION 'Cannot change status to drafted from a different status.';
END IF;

        -- Set the drafted version for the process
UPDATE processes
SET drafted_version = NEW.process_version,
    updated         = now()
WHERE id = NEW.process_id;
ELSIF NEW.status = 1 THEN -- published
    -- Set the published timestamp for the process version
        NEW.published = now();

        -- Revoke all other published versions
UPDATE process_versions
SET status  = 2,
    revoked = now()
WHERE process_id = NEW.process_id
  AND process_version <> NEW.process_version
  AND status = 1;

-- Set the published version for the process
UPDATE processes
SET published_version = NEW.process_version,
    updated           = now()
WHERE id = NEW.process_id;

-- Set the drafted version to null if it was the drafted version
UPDATE processes
SET drafted_version = null,
    updated         = now()
WHERE id = NEW.process_id
  AND drafted_version = NEW.process_version;
ELSEIF NEW.status = 2 THEN -- revoked
    -- Set the revoked timestamp for the process version
        NEW.revoked = now();

        -- Remove the published reference if necessary
UPDATE processes
SET published_version = null,
    updated           = now()
WHERE id = NEW.process_id
  AND published_version = NEW.process_version;

-- Remove the drafted reference if necessary
UPDATE processes
SET drafted_version = null,
    updated         = now()
WHERE id = NEW.process_id
  AND drafted_version = NEW.process_version;
END IF;

RETURN NEW;
END;
$$;

create function handle_process_version_delete() returns trigger
    language plpgsql
as $$
BEGIN
    -- Decrement the version count of the parent process
UPDATE processes
SET version_count = version_count - 1,
    updated       = now()
WHERE id = OLD.process_id;

-- Remove the draft version reference if necessary
UPDATE processes
SET drafted_version = null,
    updated         = now()
WHERE id = OLD.process_id
  AND drafted_version = OLD.process_version;

-- Remove the published version reference if necessary
UPDATE processes
SET drafted_version = null,
    updated         = now()
WHERE id = OLD.process_id
  AND published_version = OLD.process_version;

RETURN OLD;
END;
$$;

create function compact_case_number_search_key(value text) returns text
    immutable
	strict
	parallel safe
	language sql
as $$
select case
           when value ~ '^[0-9A-HJKMNP-TV-Z]{4}-[0-9A-HJKMNP-TV-Z]{4}-[0-9A-HJKMNP-TV-Z]{4}$'
        then replace(value, '-', '')
           else null end;
$$;

create function handle_process_instance_insert() returns trigger
    language plpgsql
as $$
begin
insert into process_instance_access_controls (source_team_id,
                                              source_department_id,
                                              target_process_instance_id,
                                              permissions,
                                              created,
                                              updated)
select piacp.source_team_id       as source_team_id,
       piacp.source_department_id as source_department_id,
       NEW.id                     as target_process_instance_id,
       piacp.permissions          as permissions,
       current_timestamp          as created,
       current_timestamp          as updated
from process_instance_access_control_presets piacp
where piacp.target_process_id = NEW.process_id
  and piacp.target_process_version = NEW.initial_process_version;
return NEW;
end;
$$;

create function update_parent_process_entities_timestamps() returns trigger
    language plpgsql
as $$
begin
update processes
set updated = now()
where id = NEW.process_id;

update process_versions
set updated = now()
where process_id = NEW.process_id
  and process_version = NEW.process_version;

return NEW;
end;
$$;

create function jsonb_diff_val(newval jsonb, oldval jsonb) returns jsonb
    language plpgsql
as $$
declare
result jsonb;
    v record;
    i int;
    old_elem jsonb;
    new_elem jsonb;
    diff_elem jsonb;
    arr_result jsonb;
    max_len int;
begin
    -- Case 1: Both are json Objects
    if jsonb_typeof(newVal) = 'object' and jsonb_typeof(oldVal) = 'object' then
        result = '{}'::jsonb;

        -- 1. Check for changes and deletions based on keys that existed in oldVal
for v in select * from jsonb_each(oldVal) loop
    if newVal ? v.key then
                    if newVal -> v.key = v.value then
                        continue; -- Identical, skip it
else
                        -- Value changed: Recurse to find the specific historical parts
                        diff_elem = jsonb_diff_val(newVal -> v.key, v.value);
                        if diff_elem is not null and diff_elem != '{}'::jsonb and diff_elem != '[]'::jsonb then
                            result = result || jsonb_build_object(v.key, diff_elem);
end if;
end if;
else
                    -- Key was entirely deleted in newVal. Preserve its old value.
                    result = result || jsonb_build_object(v.key, v.value);
end if;
end loop;

        -- 2. Check for brand new keys added in newVal
for v in select * from jsonb_each(newVal) loop
    if not (oldVal ? v.key) then
                    -- Key is brand new, meaning its previous value was non-existent ('null')
                    result = result || jsonb_build_object(v.key, 'null'::jsonb);
end if;
end loop;

return result;

-- Case 2: Both are json Arrays
elsif jsonb_typeof(newVal) = 'array' and jsonb_typeof(oldVal) = 'array' then
        arr_result = '[]'::jsonb;
        max_len = greatest(jsonb_array_length(newVal), jsonb_array_length(oldVal));

for i in 0 .. max_len - 1 loop
                new_elem = newVal -> i;
                old_elem = oldVal -> i;

                if old_elem is null then
                    -- Element was added out-of-bounds in newVal. It didn't exist before.
                    arr_result = arr_result || jsonb_build_array('null'::jsonb);
                elsif new_elem is null then
                    -- Element was truncated/deleted in newVal. Keep the old element.
                    arr_result = arr_result || jsonb_build_array(old_elem);
else
                    -- Both exist. Check if they match.
                    if old_elem = new_elem then
                        -- To keep array positions aligned between diff and original array,
                        -- we leave an empty placeholder object '{}' for unchanged items.
                        arr_result = arr_result || jsonb_build_array('{}'::jsonb);
else
                        diff_elem = jsonb_diff_val(new_elem, old_elem);
                        arr_result = arr_result || jsonb_build_array(coalesce(diff_elem, '{}'::jsonb));
end if;
end if;
end loop;

return arr_result;

-- Case 3: Flat values (Scalars) or completely mismatched types
else
        if newVal = oldVal then
            return null;
else
            -- Core Change: Return oldVal instead of newVal to preserve history
            return oldVal;
end if;
end if;
end;
$$;

create function handle_process_task_completion() returns trigger
    language plpgsql
as $$
declare
old_process_data jsonb;
begin
    if NEW.previous_process_instance_task_id is not null then
        old_process_data :=
                (select process_data
                 from process_instance_tasks
                 where id = NEW.previous_process_instance_task_id);
end if;

    if old_process_data is null then
        old_process_data := '{}'::jsonb;
end if;

update process_instance_tasks
set process_data_diff = jsonb_diff_val(NEW.process_data, old_process_data)
where id = NEW.id;

return NEW;
end
$$;

create aggregate array_intersect_agg(anyarray) (
sfunc = array_intersect,
stype = anyarray,
initcond = '{}'
);

create aggregate array_unique_union_agg(anyarray) (
sfunc = array_unique_union,
stype = anyarray,
initcond = '{}'
);

create aggregate array_unique_union_multi_agg(anyarray, anyarray) (
sfunc = array_unique_union_multi,
stype = anyarray,
initcond = '{}'
);

