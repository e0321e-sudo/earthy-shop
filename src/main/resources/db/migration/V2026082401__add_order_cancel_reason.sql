-- 주문 취소 사유 컬럼을 추가한다.
-- 신규 빈 DB에서는 orders 테이블이 없을 수 있으므로 존재 여부를 확인한 뒤 실행한다.

DELIMITER //

CREATE PROCEDURE add_order_cancel_reason_if_orders_exists()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'orders'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'orders'
          AND column_name = 'cancel_reason'
    ) THEN
        ALTER TABLE orders
            ADD COLUMN cancel_reason VARCHAR(500) NULL;
    END IF;
END //

DELIMITER ;

CALL add_order_cancel_reason_if_orders_exists();

DROP PROCEDURE add_order_cancel_reason_if_orders_exists;
