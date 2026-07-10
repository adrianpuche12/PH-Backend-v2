-- Recetas de productos manufacturados
-- Un producto FABRICATED puede tener una o más materias primas con cantidad fraccionable

CREATE TABLE IF NOT EXISTS product_recipe_items (
    id            BIGSERIAL     PRIMARY KEY,
    product_id    BIGINT        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    ingredient_id BIGINT        NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity      NUMERIC(10,3) NOT NULL CHECK (quantity > 0),
    CONSTRAINT uq_recipe_product_ingredient UNIQUE (product_id, ingredient_id)
);

CREATE INDEX IF NOT EXISTS idx_recipe_items_product    ON product_recipe_items(product_id);
CREATE INDEX IF NOT EXISTS idx_recipe_items_ingredient ON product_recipe_items(ingredient_id);
