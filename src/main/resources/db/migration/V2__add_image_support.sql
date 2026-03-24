-- Add image support to lost_items table

ALTER TABLE lost_items ADD COLUMN image_path VARCHAR(500);

CREATE INDEX idx_lost_items_image ON lost_items(image_path) WHERE image_path IS NOT NULL;
