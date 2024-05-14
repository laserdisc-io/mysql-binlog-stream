

create database if not exists test;

create table test.sku
(
  id  int primary key,
  sku varchar(10)
);

create table test.variant
(
  id  int primary key,
  color varchar(10)
);

insert into test.sku (id, sku) VALUES (1, '123');




-- -------------------------------------
-- See MultiSchemaTest.scala

create database if not exists multi_schema_test_a;
create database if not exists multi_schema_test_b;

create table multi_schema_test_a.test_table (id int primary key, payload varchar(255));
create table multi_schema_test_b.test_table (id varchar(255) primary key, payload datetime);

-- ------------------------