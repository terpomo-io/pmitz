create schema if not exists dbo;
CREATE TABLE IF NOT EXISTS dbo.subscription (
    subscription_id VARCHAR(255) PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    expiration_date TIMESTAMP NULL
);
CREATE TABLE IF NOT EXISTS dbo.subscription_plan (
    subscription_id VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    plan_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (subscription_id, product_id),
    CONSTRAINT fk_subscription_plan_subscription FOREIGN KEY (subscription_id)
        REFERENCES dbo.subscription(subscription_id) ON DELETE CASCADE
);
