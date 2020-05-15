create table sku
(
  id  int primary key,
  sku varchar(10)
);

create table variant
(
  id  int primary key,
  color varchar(10)
);

insert into sku (id, sku) VALUES (1, '123');