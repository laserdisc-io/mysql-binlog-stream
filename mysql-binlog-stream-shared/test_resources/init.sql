

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

