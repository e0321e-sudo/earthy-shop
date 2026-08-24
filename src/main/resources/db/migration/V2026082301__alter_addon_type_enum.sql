-- addons.type enum에 추가상품 그룹 값을 추가한다.
-- 기존 FRAME 데이터는 유지하고, 운영 기존 DB와 신규 빈 DB 모두에서 안전하게 실행되도록 컬럼 존재 여부를 확인한다.

DELIMITER //

CREATE PROCEDURE alter_addon_type_enum_if_exists()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'addons'
          AND column_name = 'type'
    ) THEN
        ALTER TABLE addons
            MODIFY COLUMN type ENUM('FRAME', 'PREMIUM_FRAME', 'BASIC_FRAME') NOT NULL;
    END IF;
END //

DELIMITER ;

CALL alter_addon_type_enum_if_exists();

DROP PROCEDURE alter_addon_type_enum_if_exists;
