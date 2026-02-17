-- create function for array intersection
create function array_intersect(anyarray, anyarray)
    returns anyarray AS
$$
select array(
           -- UNNEST converts array to a set of rows (elements)
               select unnest($1)
               intersect
               select unnest($2)
               -- Add ORDER BY to guarantee a consistent array order, though it's optional
               order by 1
       );
$$ language sql immutable;

-- create aggregate function for array intersection
create aggregate array_intersect_agg(anyarray) (
    sfunc = array_intersect,
    stype = anyarray,
    -- Initial condition should be NULL so the first array becomes the initial state
    initcond = '{}'
    );

-- create function for array unique union
create function array_unique_union(anyarray, anyarray)
    returns anyarray AS
$$
select array(
               select unnest($1)
               union
               select unnest($2)
               order by 1 -- Optional: ensures a consistent order
       );
$$ language sql immutable;

-- create aggregate function for array unique union
create aggregate array_unique_union_agg(anyarray) (
    sfunc = array_unique_union,
    stype = anyarray,
    -- INITCOND is NULL, meaning the first array becomes the initial state
    initcond = '{}'
    );

-- create function to handle multiple array inputs for unique union
create function array_unique_union_multi(state anyarray, col1 anyarray, col2 anyarray)
    returns anyarray as
$$
select array(
               select unnest(state)
               union
               select unnest(col1)
               union
               select unnest(col2)
               order by 1
       );
$$ language sql immutable;

-- create aggregate function for multiple array unique union
create aggregate array_unique_union_multi_agg(anyarray, anyarray) (
    sfunc = array_unique_union_multi,
    stype = anyarray,
    initcond = '{}'
    );