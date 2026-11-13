-- Whether saving the deputy relation original -> deputy for the given period would close a cycle, i.e. whether the
-- original user already represents the deputy, directly or through further deputies, on a day of that period. Only
-- relations whose periods overlap form a cycle: mutual cover for different holidays stays possible. The relation with
-- the ignored ID is left out, so an update does not compare a relation with its own previous version.
create or replace function fun_user_deputy_creates_cycle(p_original_user_id varchar,
                                                         p_deputy_user_id varchar,
                                                         p_from_date date,
                                                         p_until_date date,
                                                         p_ignored_id integer) returns boolean
    language sql
    stable
as
$$
WITH RECURSIVE reachable AS (SELECT ud.deputy_user_id::text                   AS user_id,
                                    greatest(ud.from_date, p_from_date)        AS from_date,
                                    -- least ignores NULL, which matches NULL meaning an unlimited period
                                    least(ud.until_date, p_until_date)         AS until_date,
                                    ARRAY [ud.original_user_id::text, ud.deputy_user_id::text] AS path
                             FROM user_deputies ud
                             WHERE ud.original_user_id::text = p_deputy_user_id::text
                               AND ud.id IS DISTINCT FROM p_ignored_id
                               AND (ud.until_date IS NULL OR p_from_date <= ud.until_date)
                               AND (p_until_date IS NULL OR ud.from_date <= p_until_date)
                             UNION ALL
                             SELECT ud.deputy_user_id::text,
                                    greatest(ud.from_date, r.from_date),
                                    least(ud.until_date, r.until_date),
                                    r.path || ud.deputy_user_id::text
                             FROM user_deputies ud
                                      JOIN reachable r ON ud.original_user_id::text = r.user_id
                             WHERE ud.id IS DISTINCT FROM p_ignored_id
                               AND r.user_id <> p_original_user_id::text
                               AND (ud.until_date IS NULL OR r.from_date <= ud.until_date)
                               AND (r.until_date IS NULL OR ud.from_date <= r.until_date)
                               -- Existing cycles must not make the search endless
                               AND NOT ud.deputy_user_id::text = ANY (r.path))
SELECT EXISTS(SELECT 1 FROM reachable WHERE user_id = p_original_user_id::text);
$$;

-- The previous check only ran on insert and only saw relations active today, so relations starting later or extended
-- by an update could still close a cycle.
create or replace function fun_check_deputy_circular_reference() returns trigger
    language plpgsql
as
$$
begin
    if fun_user_deputy_creates_cycle(NEW.original_user_id, NEW.deputy_user_id, NEW.from_date, NEW.until_date, NEW.id) then
        raise exception 'Circular deputy assignment detected between users % and %', NEW.original_user_id, NEW.deputy_user_id;
    end if;
    return NEW;
end;
$$;

drop trigger if exists trg_check_deputy_circular_reference on user_deputies;

create trigger trg_check_deputy_circular_reference
    before insert or update
    on user_deputies
    for each row
execute procedure fun_check_deputy_circular_reference();

-- Without a guard, an active cycle made this recursion endless and with it every permission check built on it. The
-- trigger prevents new cycles, but cycles that already exist or that concurrent saves create must stay harmless.
create or replace view v_user_is_recursively_deputy_for(deputy_user_id, original_user_id, depth) as
WITH RECURSIVE deputy_hierarchy AS (SELECT ud.original_user_id,
                                           ud.deputy_user_id,
                                           1                                                         AS depth,
                                           ARRAY [ud.original_user_id::text, ud.deputy_user_id::text] AS path
                                    FROM user_deputies ud
                                    WHERE ud.from_date <= CURRENT_DATE
                                      AND (ud.until_date IS NULL OR CURRENT_DATE <= ud.until_date)
                                    UNION ALL
                                    SELECT dh_1.original_user_id,
                                           ud.deputy_user_id,
                                           dh_1.depth + 1                     AS depth,
                                           dh_1.path || ud.deputy_user_id::text AS path
                                    FROM user_deputies ud
                                             JOIN deputy_hierarchy dh_1
                                                  ON ud.original_user_id::text = dh_1.deputy_user_id::text
                                    WHERE ud.from_date <= CURRENT_DATE
                                      AND (ud.until_date IS NULL OR CURRENT_DATE <= ud.until_date)
                                      AND NOT ud.deputy_user_id::text = ANY (dh_1.path))
SELECT DISTINCT deputy_user_id,
                original_user_id,
                depth
FROM deputy_hierarchy dh;
