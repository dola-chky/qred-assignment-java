create index idx_payments_client_id_payment_date
    on payments (client_id, payment_date desc);

create index idx_payments_client_contract_payment_date
    on payments (client_id, contract_id, payment_date desc);

