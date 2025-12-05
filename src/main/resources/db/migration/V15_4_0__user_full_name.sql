alter table users
drop
column full_name;

alter table users
    add column
        full_name text generated always as
            (first_name || ' ' || last_name) stored;
