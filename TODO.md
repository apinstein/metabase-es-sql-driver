## List of potential methods to implement

Per driver docs:

> :sql-jdbc implements most of the four main features, but instead you must implement sql-jdbc multimethods found in metabase.driver.sql-jdbc.* namespaces, as well as some methods in metabase.driver.sql.* namespaces

So...
`metabase.driver.sql-jdbc.*` multimethods from `lein run driver-methods` in `metabase-core`:

```
metabase.driver.sql-jdbc.connection
    connection-details->spec [driver details-map]
    data-warehouse-connection-pool-properties [driver]

metabase.driver.sql-jdbc.execute
    column-metadata [driver rsmeta]
    connection-with-timezone [driver database timezone-id]
    execute-query! [driver stmt]
    prepared-statement [driver connection sql params]
    read-column [driver _ rs rsmeta i]
    read-column-thunk [driver rs rsmeta i]
    set-parameter [driver prepared-statement i object]
    set-timezone-sql [driver]

metabase.driver.sql-jdbc.execute.old-impl
    read-column [driver _ rs rsmeta i]
    set-timezone-sql [driver]

metabase.driver.sql-jdbc.sync
    active-tables [driver connection]
    column->special-type [driver database-type column-name]
    database-type->base-type [driver database-type]
    excluded-schemas [driver]
    fallback-metadata-query [driver schema table]
    have-select-privilege? [driver connection table-schema table-name]

metabase.driver.sql-jdbc.sync.interface
    active-tables [driver connection]
    column->special-type [driver database-type column-name]
    database-type->base-type [driver database-type]
    excluded-schemas [driver]
    fallback-metadata-query [driver schema table]
    have-select-privilege? [driver connection table-schema table-name]
```

How to call parent method if needed:

> You can get a parent driver's implementation for a method by using get-method:

```
(defmethod driver/mbql->native :bigquery [driver query]
  ((get-method driver/mbql-native :sql) driver query))
```
