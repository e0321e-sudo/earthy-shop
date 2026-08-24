ALTER TABLE order_items
    ADD COLUMN size_option_id BIGINT NULL,
    ADD COLUMN size_name VARCHAR(255) NULL,
    ADD COLUMN size_additional_price INT NOT NULL DEFAULT 0,
    ADD COLUMN product_unit_price INT NOT NULL DEFAULT 0;

UPDATE order_items
SET product_unit_price = product_price
WHERE product_unit_price = 0;

CREATE TABLE order_item_addons (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    order_item_id BIGINT NOT NULL,
    addon_id BIGINT NOT NULL,
    addon_name VARCHAR(255) NOT NULL,
    addon_price INT NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_item_addons_order_item
        FOREIGN KEY (order_item_id) REFERENCES order_items (id)
);
