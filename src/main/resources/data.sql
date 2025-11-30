-- Insert sample symbol
INSERT INTO symbols (code, type, description)
SELECT 'XAUUSD', 'COMMODITY', 'Gold vs USD'
WHERE NOT EXISTS (SELECT 1 FROM symbols WHERE code = 'XAUUSD');
