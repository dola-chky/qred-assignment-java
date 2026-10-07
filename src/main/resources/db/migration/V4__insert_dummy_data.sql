-- V4__insert_dummy_data.sql
-- Flyway migration to insert test data for development and manual testing

-- Insert dummy clients
INSERT INTO clients (id, client_number, version)
SELECT CAST('550e8400-e29b-41d4-a716-446655440001' AS UUID), 'CLIENT001', 0
WHERE NOT EXISTS (SELECT 1 FROM clients WHERE client_number = 'CLIENT001');

INSERT INTO clients (id, client_number, version)
SELECT CAST('550e8400-e29b-41d4-a716-446655440002' AS UUID), 'CLIENT002', 0
WHERE NOT EXISTS (SELECT 1 FROM clients WHERE client_number = 'CLIENT002');

INSERT INTO clients (id, client_number, version)
SELECT CAST('550e8400-e29b-41d4-a716-446655440003' AS UUID), 'CLIENT003', 0
WHERE NOT EXISTS (SELECT 1 FROM clients WHERE client_number = 'CLIENT003');

INSERT INTO clients (id, client_number, version)
SELECT CAST('550e8400-e29b-41d4-a716-446655440004' AS UUID), 'BIGCLIENT', 0
WHERE NOT EXISTS (SELECT 1 FROM clients WHERE client_number = 'BIGCLIENT');

INSERT INTO clients (id, client_number, version)
SELECT CAST('550e8400-e29b-41d4-a716-446655440005' AS UUID), 'ACMECORP', 0
WHERE NOT EXISTS (SELECT 1 FROM clients WHERE client_number = 'ACMECORP');

INSERT INTO clients (id, client_number, version)
SELECT CAST('550e8400-e29b-41d4-a716-446655440006' AS UUID), 'GLOBALTECH', 0
WHERE NOT EXISTS (SELECT 1 FROM clients WHERE client_number = 'GLOBALTECH');

-- Insert dummy contracts
INSERT INTO contracts (id, contract_number, client_id, version)
SELECT CAST('660e8400-e29b-41d4-a716-446655440001' AS UUID), 'CONTRACT001',
       CAST('550e8400-e29b-41d4-a716-446655440001' AS UUID), 0
WHERE NOT EXISTS (SELECT 1 FROM contracts WHERE contract_number = 'CONTRACT001');

INSERT INTO contracts (id, contract_number, client_id, version)
SELECT CAST('660e8400-e29b-41d4-a716-446655440002' AS UUID), 'CONTRACT002',
       CAST('550e8400-e29b-41d4-a716-446655440002' AS UUID), 0
WHERE NOT EXISTS (SELECT 1 FROM contracts WHERE contract_number = 'CONTRACT002');

INSERT INTO contracts (id, contract_number, client_id, version)
SELECT CAST('660e8400-e29b-41d4-a716-446655440003' AS UUID), 'CONTRACT003',
       CAST('550e8400-e29b-41d4-a716-446655440003' AS UUID), 0
WHERE NOT EXISTS (SELECT 1 FROM contracts WHERE contract_number = 'CONTRACT003');

INSERT INTO contracts (id, contract_number, client_id, version)
SELECT CAST('660e8400-e29b-41d4-a716-446655440004' AS UUID), 'CONTRACT004',
       CAST('550e8400-e29b-41d4-a716-446655440001' AS UUID), 0
WHERE NOT EXISTS (SELECT 1 FROM contracts WHERE contract_number = 'CONTRACT004');

INSERT INTO contracts (id, contract_number, client_id, version)
SELECT CAST('660e8400-e29b-41d4-a716-446655440005' AS UUID), 'PREMIUM_PLAN',
       CAST('550e8400-e29b-41d4-a716-446655440004' AS UUID), 0
WHERE NOT EXISTS (SELECT 1 FROM contracts WHERE contract_number = 'PREMIUM_PLAN');

INSERT INTO contracts (id, contract_number, client_id, version)
SELECT CAST('660e8400-e29b-41d4-a716-446655440006' AS UUID), 'ENTERPRISE_PLAN',
       CAST('550e8400-e29b-41d4-a716-446655440005' AS UUID), 0
WHERE NOT EXISTS (SELECT 1 FROM contracts WHERE contract_number = 'ENTERPRISE_PLAN');

INSERT INTO contracts (id, contract_number, client_id, version)
SELECT CAST('660e8400-e29b-41d4-a716-446655440007' AS UUID), 'STANDARD_CONTRACT',
       CAST('550e8400-e29b-41d4-a716-446655440006' AS UUID), 0
WHERE NOT EXISTS (SELECT 1 FROM contracts WHERE contract_number = 'STANDARD_CONTRACT');


