create table clients (
    id uuid primary key,
    client_number varchar(64) not null unique
);

create table contracts (
    id uuid primary key,
    contract_number varchar(64) not null unique,
    client_id uuid not null,
    constraint fk_contracts_clients foreign key (client_id) references clients (id)
);

create table payments (
    id uuid primary key,
    payment_date date not null,
    amount numeric(19,2) not null,
    type varchar(16) not null,
    client_id uuid not null,
    contract_id uuid not null,
    constraint fk_payments_clients foreign key (client_id) references clients (id),
    constraint fk_payments_contracts foreign key (contract_id) references contracts (id)
);

create index idx_payments_client_id on payments (client_id);
create index idx_payments_contract_id on payments (contract_id);

