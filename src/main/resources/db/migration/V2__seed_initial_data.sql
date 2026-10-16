-- =========================================================================
-- V2__seed_initial_data.sql: Development & Demo Seed Data ONLY
-- CAUTION: Do NOT use these credentials in a production environment.
-- Default development password for all seed accounts is: password
-- BCrypt Hash: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
-- =========================================================================

-- 1. Departments
INSERT INTO departments (id, code, name, description, active) VALUES
(1, 'ENG', 'Engineering', 'Software engineering, QA, and platform infrastructure', TRUE),
(2, 'HR', 'Human Resources', 'People operations, talent acquisition, and employee relations', TRUE),
(3, 'PROD', 'Product Management', 'Product strategy, design, and analytics', TRUE);

-- 2. Employees
-- Admin (CEO / Superuser - no manager)
INSERT INTO employees (id, employee_code, first_name, last_name, email, job_title, department_id, manager_id, status, hire_date, version) VALUES
(1, 'EMP-001', 'Admin', 'User', 'admin@company.com', 'System Administrator', 2, NULL, 'ACTIVE', '2023-01-01', 0);

-- Manager John (Reports to Admin)
INSERT INTO employees (id, employee_code, first_name, last_name, email, job_title, department_id, manager_id, status, hire_date, version) VALUES
(2, 'EMP-002', 'John', 'Doe', 'manager.john@company.com', 'Engineering Manager', 1, 1, 'ACTIVE', '2023-03-15', 0);

-- Alice Dev (Reports to Manager John)
INSERT INTO employees (id, employee_code, first_name, last_name, email, job_title, department_id, manager_id, status, hire_date, version) VALUES
(3, 'EMP-003', 'Alice', 'Smith', 'alice.dev@company.com', 'Senior Software Engineer', 1, 2, 'ACTIVE', '2023-06-01', 0);

-- Bob Dev (Reports to Manager John)
INSERT INTO employees (id, employee_code, first_name, last_name, email, job_title, department_id, manager_id, status, hire_date, version) VALUES
(4, 'EMP-004', 'Bob', 'Johnson', 'bob.dev@company.com', 'Software Engineer', 1, 2, 'ACTIVE', '2024-01-10', 0);

-- 3. User Accounts (Password: 'password')
INSERT INTO user_accounts (id, username, password_hash, role, employee_id, enabled) VALUES
(1, 'admin@company.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'ROLE_ADMIN', 1, TRUE),
(2, 'manager.john@company.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'ROLE_MANAGER', 2, TRUE),
(3, 'alice.dev@company.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'ROLE_EMPLOYEE', 3, TRUE),
(4, 'bob.dev@company.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'ROLE_EMPLOYEE', 4, TRUE);

-- 4. Initial Leave Balances for Year 2026
-- Alice Smith (EMP-003)
INSERT INTO leave_balances (employee_id, calendar_year, leave_type, total_days, used_days, version) VALUES
(3, 2026, 'ANNUAL', 20, 0, 0),
(3, 2026, 'SICK', 10, 0, 0),
(3, 2026, 'CASUAL', 5, 0, 0),
(3, 2026, 'UNPAID', 30, 0, 0);

-- Bob Johnson (EMP-004)
INSERT INTO leave_balances (employee_id, calendar_year, leave_type, total_days, used_days, version) VALUES
(4, 2026, 'ANNUAL', 20, 0, 0),
(4, 2026, 'SICK', 10, 0, 0),
(4, 2026, 'CASUAL', 5, 0, 0),
(4, 2026, 'UNPAID', 30, 0, 0);

-- Manager John (EMP-002)
INSERT INTO leave_balances (employee_id, calendar_year, leave_type, total_days, used_days, version) VALUES
(2, 2026, 'ANNUAL', 25, 0, 0),
(2, 2026, 'SICK', 12, 0, 0),
(2, 2026, 'CASUAL', 5, 0, 0),
(2, 2026, 'UNPAID', 30, 0, 0);
