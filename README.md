### Motivation
Stream in real time the MySQL database events, such as CRUD and DDL. That may be useful for building Data Warehouse, integration with monolith application on the database level, etc.

### Example 
```scala
MysqlBinlogStream
              .rawEvents[IO](binlogClient)
              .through(streamEvents[IO](transactionState))
              .evalTap(msg => logger.info(s"received $msg"))
```
Refer to [examples](examples) for complete snippet

#### Event examples

Create:
```json
{
  "table" : "sku",
  "timestamp" : 1554754028000,
  "action" : "create",
  "fileName" : "acd03b3a873f-bin.000003",
  "offset" : 2177,
  "endOfTransaction" : false,
  "pk" : {
    "id" : 5
  },
  "row" : {
    "before" : null,
    "after" : {
      "id" : 5,
      "sku" : "sku5"
    }
  }
}
```

Update:

```json
{
  "table" : "sku",
  "timestamp" : 1554754028000,
  "action" : "create",
  "fileName" : "acd03b3a873f-bin.000003",
  "offset" : 2177,
  "endOfTransaction" : false,
  "pk" : {
    "id" : 5
  },
  "row" : {
    "before" : {
      "id" : 5,
      "sku" : "sku5"
    },
    "after" : {
      "id" : 5,
      "sku" : "new sku 5"
    }
  }
}
```

Delete:
```json
{
  "table" : "sku",
  "timestamp" : 1554754028000,
  "action" : "create",
  "fileName" : "acd03b3a873f-bin.000003",
  "offset" : 2177,
  "endOfTransaction" : false,
  "pk" : {
    "id" : 5
  },
  "row" : {
    "before" : {
      "id" : 5,
      "sku" : "new sku 5"
    },
    "after" : null
  }
}
```

Truncate:
```json
{
  "table" : "sku",
  "timestamp" : 1554754028000,
  "action" : "truncate",
  "fileName" : "acd03b3a873f-bin.000003",
  "offset" : 2497,
  "endOfTransaction" : true,
  "pk" : null,
  "row" : null
}
```

### Tech Stack
- Scala
- FS2 Functional Streams for Scala
- circe - json streaming encoder/decoder
- doobie - database integration layer 
