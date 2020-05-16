### Motivation
Stream in real time the MySQL database events, such as CRUD and DDL. That may be useful for building Data Warehouse, integration with monolith application on the database level, etc.

### Example 
```scala
MysqlBinlogStream
              .rawEvents[IO](binlogClient)
              .through(streamEvents[IO](transactionState))
              .evalTap(msg => logger.info(s"received $msg"))
```
Refer to [examples](mysql-binlog-stream-examples) for complete snippet

### How to build and launch example

- build docker image 
```shell script
sbt "project mysql-binlog-stream-examples" clean  "docker:publishLocal"
```
- run docker compose with MySql and example service
```shell script
cd mysql-binlog-stream-examples
docker-compose -f docker-compose-mysql-it-mac.yaml up
```
- open new console
- in new console 
```shell script
mysql -u 'root' --host=0.0.0.0 --port=3307 --password=''
``` 
- try to insert new row in `SKU` or `VARIANT` table
```mysql
use test;
insert into sku(id, sku) values(3, '123');
```
- in the `docker-compose` console you should see 
```shell script
example_1  | 02:35:58.734 [ioapp-compute-2] INFO application - received EventMessage(sku,1589596558000,create,8908ecfb63e4-bin.000007,415,true,{
example_1  |   "id" : 3
example_1  | },{
example_1  |   "before" : null,
example_1  |   "after" : {
example_1  |     "id" : 3,
example_1  |     "sku" : "123"
example_1  |   }
example_1  | })
```

try to update some row and see what happenning

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
