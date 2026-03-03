ALTER TABLE account_terms_agreement
    ADD COLUMN is_cross_border_data_agree BOOLEAN NOT NULL DEFAULT FALSE;
