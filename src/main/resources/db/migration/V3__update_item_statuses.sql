-- Update existing LOST status to FOUND
UPDATE lost_items SET status = 'FOUND' WHERE status = 'LOST';
