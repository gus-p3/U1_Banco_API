-- Migración V12: Garantizar que la tabla contact_details no tenga la columna calculada email_lower
-- en caso de bases de datos existentes donde V7 se aplicó previamente.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'contact_details' AND column_name = 'email_lower'
    ) THEN
        ALTER TABLE contact_details DROP CONSTRAINT IF EXISTS uq_contact_details_email;
        ALTER TABLE contact_details DROP COLUMN email_lower;
        ALTER TABLE contact_details ADD CONSTRAINT uq_contact_details_email UNIQUE (email);
    END IF;
END $$;
