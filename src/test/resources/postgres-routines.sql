CREATE OR REPLACE FUNCTION fn_get_expiring_products(
    p_start_date DATE,
    p_end_date DATE
)
RETURNS TABLE (
    profile_id INTEGER,
    item_id INTEGER,
    food_name VARCHAR,
    expiry_date DATE
)
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    IF p_start_date IS NULL OR p_end_date IS NULL OR p_start_date > p_end_date THEN
        RAISE EXCEPTION 'Intervalo de datas inválido: % até %', p_start_date, p_end_date
            USING ERRCODE = '22023';
    END IF;

    RETURN QUERY
    SELECT pi.profile_id, pi.id, f.name, pi.expiry_date
    FROM pantry_item pi
    JOIN food f ON f.id = pi.food_id
    JOIN profile pr ON pr.id = pi.profile_id
    WHERE pi.expiry_date BETWEEN p_start_date AND p_end_date
      AND pr.status = 'ACTIVE'
    ORDER BY pi.expiry_date, pi.id;
END;
$$;

CREATE OR REPLACE FUNCTION fn_get_items_below_minimum(
    p_profile_id INTEGER DEFAULT NULL
)
RETURNS TABLE (
    profile_id INTEGER,
    food_id INTEGER,
    food_name VARCHAR,
    minimum_quantity INTEGER,
    current_quantity INTEGER,
    missing_quantity INTEGER
)
LANGUAGE sql
STABLE
AS $$
    SELECT s.profile_id,
           s.food_id,
           f.name,
           s.minimum_quantity,
           COALESCE(stock.total, 0)::INTEGER,
           (s.minimum_quantity - COALESCE(stock.total, 0))::INTEGER
    FROM pantry_product_setting s
    JOIN food f ON f.id = s.food_id
    LEFT JOIN (
        SELECT pi.profile_id, pi.food_id, SUM(pi.quantity) AS total
        FROM pantry_item pi
        WHERE (p_profile_id IS NULL OR pi.profile_id = p_profile_id)
          AND (pi.expiry_date IS NULL OR pi.expiry_date >= CURRENT_DATE)
        GROUP BY pi.profile_id, pi.food_id
    ) stock ON stock.profile_id = s.profile_id AND stock.food_id = s.food_id
    WHERE (p_profile_id IS NULL OR s.profile_id = p_profile_id)
      AND COALESCE(stock.total, 0) < s.minimum_quantity
    ORDER BY s.profile_id,
             (s.minimum_quantity - COALESCE(stock.total, 0)) DESC,
             f.name;
$$;

CREATE OR REPLACE PROCEDURE sp_generate_monthly_payments(
    p_reference_date DATE DEFAULT CURRENT_DATE,
    p_due_day INTEGER DEFAULT 10,
    INOUT p_created_count INTEGER DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_period_start DATE := date_trunc('month', p_reference_date)::DATE;
    v_period_end DATE := (date_trunc('month', p_reference_date)
                         + INTERVAL '1 month - 1 day')::DATE;
BEGIN
    IF p_reference_date IS NULL THEN
        RAISE EXCEPTION 'A data de referência é obrigatória'
            USING ERRCODE = '22023';
    END IF;
    IF p_due_day IS NULL OR p_due_day NOT BETWEEN 1 AND 28 THEN
        RAISE EXCEPTION 'O dia de vencimento deve estar entre 1 e 28: %', p_due_day
            USING ERRCODE = '22023';
    END IF;

    INSERT INTO payment (
        due_date, billing_period_start, billing_period_end,
        payment_status, company_id, plan_id, amount
    )
    SELECT v_period_start + (p_due_day - 1),
           v_period_start,
           v_period_end,
           'PENDING',
           c.id,
           p.id,
           p.plan_price
    FROM company c
    JOIN plan p ON p.id = c.plan_id
    JOIN profile pr ON pr.id = c.profile_id
    WHERE pr.status <> 'INACTIVE'
      AND NOT EXISTS (
          SELECT 1 FROM payment pa
          WHERE pa.company_id = c.id
            AND pa.billing_period_start = v_period_start
      )
    ON CONFLICT (company_id, billing_period_start) DO NOTHING;

    GET DIAGNOSTICS p_created_count = ROW_COUNT;
END;
$$;

CREATE OR REPLACE PROCEDURE sp_update_overdue_payments(
    p_grace_days INTEGER DEFAULT 0,
    INOUT p_overdue_count INTEGER DEFAULT NULL,
    INOUT p_blocked_count INTEGER DEFAULT NULL,
    INOUT p_unblocked_count INTEGER DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_grace_days IS NULL OR p_grace_days < 0 THEN
        RAISE EXCEPTION 'A tolerância deve ser um número de dias não negativo: %', p_grace_days
            USING ERRCODE = '22023';
    END IF;

    UPDATE payment
    SET payment_status = 'OVERDUE', updated_at = CURRENT_TIMESTAMP
    WHERE payment_status = 'PENDING'
      AND due_date < CURRENT_DATE - p_grace_days;
    GET DIAGNOSTICS p_overdue_count = ROW_COUNT;

    WITH defaulters AS (
        SELECT c.id AS company_id, c.profile_id
        FROM company c
        JOIN profile pr ON pr.id = c.profile_id
        WHERE pr.status IN ('ACTIVE', 'BLOCKED')
          AND EXISTS (
              SELECT 1 FROM payment pa
              WHERE pa.company_id = c.id AND pa.payment_status = 'OVERDUE'
          )
    ), blocked_stores AS (
        UPDATE profile sp
        SET status = 'BLOCKED'
        FROM store s
        JOIN defaulters d ON d.company_id = s.company_id
        WHERE sp.id = s.profile_id AND sp.status = 'ACTIVE'
        RETURNING sp.id
    ), blocked_companies AS (
        UPDATE profile pr
        SET status = 'BLOCKED'
        FROM defaulters d
        WHERE pr.id = d.profile_id AND pr.status = 'ACTIVE'
        RETURNING pr.id
    )
    SELECT COUNT(*) INTO p_blocked_count FROM blocked_companies;

    WITH settled AS (
        SELECT c.id AS company_id, c.profile_id
        FROM company c
        JOIN profile pr ON pr.id = c.profile_id
        WHERE pr.status IN ('ACTIVE', 'BLOCKED')
          AND NOT EXISTS (
              SELECT 1 FROM payment pa
              WHERE pa.company_id = c.id AND pa.payment_status = 'OVERDUE'
          )
    ), unblocked_stores AS (
        UPDATE profile sp
        SET status = 'ACTIVE'
        FROM store s
        JOIN settled st ON st.company_id = s.company_id
        WHERE sp.id = s.profile_id AND sp.status = 'BLOCKED'
        RETURNING sp.id
    ), unblocked_companies AS (
        UPDATE profile pr
        SET status = 'ACTIVE'
        FROM settled st
        WHERE pr.id = st.profile_id AND pr.status = 'BLOCKED'
        RETURNING pr.id
    )
    SELECT COUNT(*) INTO p_unblocked_count FROM unblocked_companies;
END;
$$;
