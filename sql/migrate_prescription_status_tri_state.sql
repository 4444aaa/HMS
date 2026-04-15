-- 处方状态由二态改为三态后的数据迁移（在已有库上执行一次）
-- 原：0 未取药 / 1 已取药
-- 新：0 待提交 / 1 待取药 / 2 已取药
-- 注意顺序：先将旧「已取药」改为 2，再将旧「未取药」改为 1（待取药）

UPDATE `prescription` SET `status` = 2 WHERE `status` = 1;
UPDATE `prescription` SET `status` = 1 WHERE `status` = 0;
