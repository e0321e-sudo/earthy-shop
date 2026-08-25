-- OrderItem 사이즈 스냅샷과 복수 추가상품 테이블을 추가한다.
-- Flyway가 Hibernate ddl-auto보다 먼저 실행되므로 신규 빈 DB에서는 order_items가 없으면 안전하게 건너뛴다.

DELIMITER //

CREATE PROCEDURE add_order_item_size_and_addons_if_order_items_exists()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'order_items'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'order_items'
              AND column_name = 'size_option_id'
        ) THEN
            ALTER TABLE order_items
                ADD COLUMN size_option_id BIGINT NULL;
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'order_items'
              AND column_name = 'size_name'
        ) THEN
            ALTER TABLE order_items
                ADD COLUMN size_name VARCHAR(255) NULL;
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'order_items'
              AND column_name = 'size_additional_price'
        ) THEN
            ALTER TABLE order_items
                ADD COLUMN size_additional_price INT NOT NULL DEFAULT 0;
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'order_items'
              AND column_name = 'product_unit_price'
        ) THEN
            ALTER TABLE order_items
                ADD COLUMN product_unit_price INT NOT NULL DEFAULT 0;
        END IF;

        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'order_items'
              AND column_name = 'product_unit_price'
        ) AND EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'order_items'
              AND column_name = 'product_price'
        ) THEN
            UPDATE order_items
            SET product_unit_price = product_price
            WHERE product_unit_price = 0;
        END IF;

        CREATE TABLE IF NOT EXISTS order_item_addons (
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
    END IF;
END //

DELIMITER ;

CALL add_order_item_size_and_addons_if_order_items_exists();

DROP PROCEDURE add_order_item_size_and_addons_if_order_items_exists;
