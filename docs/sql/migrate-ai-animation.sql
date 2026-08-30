-- ============================================================
-- AI 动图（动点题）功能 — ai_config 补 animation 模块
-- ============================================================
-- 背景：动图分析走豆包视觉模型，独立为 animation 模块，管理端可单独配置。
-- 生产环境 ai_config 表已有数据（AIConfigInitializer 仅在空表时写入默认值，
-- 不会覆盖），故需在线上手动执行本脚本补一行。
-- 幂等：ON DUPLICATE KEY UPDATE 保证重复执行安全（uk_module 唯一索引）。
-- ============================================================

INSERT INTO `ai_config` (`module`, `model`) VALUES
('animation', 'doubao-seed-2-1-pro-260628')
ON DUPLICATE KEY UPDATE `model` = VALUES(`model`);

-- 验证
SELECT * FROM ai_config WHERE module = 'animation';

-- ============================================================
-- AI 动图历史卡片持久化（§2.3）
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_animation_history (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id        BIGINT NOT NULL,
  title          VARCHAR(255),
  grade          VARCHAR(16),
  subject        VARCHAR(32),
  knowledge_tags TEXT,          -- JSON 数组字符串，如 ["瓜豆原理","旋转变换性质"]
  schema_json    MEDIUMTEXT,    -- 完整几何 schema JSON
  cover_url      VARCHAR(512),  -- 封面图访问 URL（原题 base64 落盘后）
  created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 已有线上表补 cover_url 列（幂等：MySQL 8 不支持 IF NOT EXISTS 加列，重复执行会报错，
-- 请在首次执行前确认列不存在；或改用下方 INFORMATION_SCHEMA 条件判断的方式）
ALTER TABLE ai_animation_history ADD COLUMN cover_url VARCHAR(512) NULL AFTER schema_json;

-- ============================================================
-- 知识点模板路由（§4.2）：知识点标签 → 生成模板约束 + UI 控件配置
-- ============================================================
CREATE TABLE IF NOT EXISTS knowledge_template (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tag             VARCHAR(64) UNIQUE,   -- 知识点标签，如「瓜豆原理」
  template_prompt TEXT,                 -- 拼接进 systemPrompt 的约束文案
  ui_config       VARCHAR(512),         -- 默认 UI 开关 JSON，如 {"showLocateMin":true}
  weight          INT DEFAULT 0,        -- 匹配优先级（越大越优先）
  enabled         TINYINT DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS knowledge_tag_dict (
  id       BIGINT PRIMARY KEY AUTO_INCREMENT,
  tag      VARCHAR(64) UNIQUE,          -- 标签名
  category VARCHAR(32),                 -- 分类（几何变换/动点最值/折叠/全等…）
  enabled  TINYINT DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 种子数据：预置知识点词典 + 三类模板（§4.2）
-- ============================================================
INSERT IGNORE INTO knowledge_tag_dict (tag, category) VALUES
('瓜豆原理', '动点最值'),
('旋转变换性质', '几何变换'),
('菱形性质', '四边形'),
('矩形折叠模型', '折叠'),
('轴对称', '折叠'),
('中点', '几何变换'),
('平行四边形', '四边形'),
('等边三角形', '三角形');

INSERT IGNORE INTO knowledge_template (tag, template_prompt, ui_config, weight, enabled) VALUES
('瓜豆原理',
 '【瓜豆原理模板约束】本题为旋转类动点最值：必须设置 1 个主动点由滑杆驱动；从动点由主动点做定点旋转变换得到（transform=rotate）；必须开启定位最小值按钮；绘制从动点轨迹；目标线段绿色高亮。禁止硬编码从动点坐标，必须使用几何变换约束（transform 字段）。',
 '{"showSlider":true,"showLocateMin":true,"showTrace":true,"showCoordinate":true}', 100, 1),
('矩形折叠模型',
 '【矩形折叠模型模板约束】本题为翻折/折叠类：主动点控制折叠位置；从动点为折叠映射点（transform=reflection）；绘制折叠前后两套图形；折叠痕用虚线辅助线（auxLines）。开启辅助线按钮与坐标系开关。',
 '{"showSlider":true,"showLocateMin":false,"showTrace":false,"showCoordinate":true}', 90, 1),
('普通静态几何证明',
 '【普通静态几何证明模板约束】本题为静态证明题，无动点：不生成 driverPoints/drivenPoints/trace；关闭滑杆、定位最小值、轨迹控件（uiConfig.showSlider/showLocateMin/showTrace 均为 false）。',
 '{"showSlider":false,"showLocateMin":false,"showTrace":false,"showCoordinate":true}', 50, 1);

-- ============================================================
-- 【标签词典全量扩充】原有8条 + 新增 = 覆盖8大题型分类
-- 分类：动点最值 / 几何变换 / 折叠 / 四边形 / 三角形 / 圆 / 函数综合
-- ============================================================
INSERT IGNORE INTO knowledge_tag_dict (tag, category) VALUES
-- ========== 动点最值类 ==========
('费马点模型','动点最值'),
('加权线段和最值','动点最值'),
('圆上动点最值','动点最值'),
('线段差最值','动点最值'),
('面积最值','动点最值'),
('周长最值','动点最值'),
('双动点最值','动点最值'),
('三动点最值','动点最值'),
('隐圆模型','动点最值'),
('定弦定角','动点最值'),
('最短路径','动点最值'),
('点到直线距离最值','动点最值'),
('将军饮马','动点最值'),
('胡不归模型','动点最值'),
('阿氏圆模型','动点最值'),

-- ========== 几何变换类 ==========
('平移变换性质','几何变换'),
('中心对称','几何变换'),
('旋转相似','几何变换'),
('位似变换','几何变换'),
('点的轨迹','几何变换'),
('定角动点','几何变换'),
('定长动点','几何变换'),
('复合变换','几何变换'),

-- ========== 折叠类 ==========
('三角形折叠','折叠'),
('正方形折叠','折叠'),
('折叠求最值','折叠'),
('折叠存在性','折叠'),

-- ========== 四边形类 ==========
('正方形性质','四边形'),
('矩形性质','四边形'),
('梯形动点','四边形'),
('平行四边形存在性','四边形'),
('菱形存在性','四边形'),
('矩形存在性','四边形'),
('正方形存在性','四边形'),

-- ========== 三角形类 ==========
('全等三角形','三角形'),
('三角形面积动点','三角形'),
('三角形内心外心','三角形'),
('费马点','三角形'),
('三角形周长最值','三角形'),
('斜边上的高','三角形'),
('等腰三角形存在性','三角形'),
('直角三角形存在性','三角形'),

-- ========== 圆专题 ==========
('切线长最值','圆'),
('弦长最值','圆'),
('圆周角动点','圆'),
('圆与直线交点','圆'),
('两圆位置动点','圆'),

-- ========== 函数综合类 ==========
('反比例函数动点','函数综合'),
('函数与几何综合','函数综合'),
('抛物线存在性','函数综合'),
('一次函数面积动点','函数综合'),
('二次函数面积最值','函数综合'),
('二次函数动点','函数综合'),
('一次函数动点','函数综合');

-- ============================================================
-- 【模板全量扩充】原有3条 + 新增 = 20+套动图生成模板
-- weight 规则：100=最高频核心，90-99=高频重点，80-89=中频，70-79=低频，50=兜底
-- ============================================================
INSERT IGNORE INTO knowledge_template (tag, template_prompt, ui_config, weight, enabled) VALUES

-- 1. 费马点模型
('费马点模型',
'【费马点模型模板约束】本题为三角形内点到三顶点距离和最值：设置1个滑杆驱动三角形内主动点P；从动点通过旋转变换（transform=rotate，旋转角60°）构造等边三角形链；绘制三条距离线段并高亮求和；必须开启定位最小值按钮，最小值对应费马点位置；绘制旋转辅助等边三角形（淡黄色填充）；从动点禁止写死坐标，全部使用transform旋转变换；标注60°旋转角。',
'{"showSlider":true,"showLocateMin":true,"showTrace":true,"showCoordinate":true}',
98, 1),

-- 2. 将军饮马模型
('将军饮马',
'【将军饮马模板约束】本题为对称类线段最值：设置1个滑杆驱动主动点；从动点使用反射变换transform=reflection生成对称点；绘制虚线辅助线表示对称连线；高亮目标折线段；必须开启定位最小值按钮，最小值位置对应对称点连线与定直线交点；禁止硬编码从动点坐标，全部使用transform反射约束；标注对称轴与垂足。',
'{"showSlider":true,"showLocateMin":true,"showTrace":false,"showCoordinate":true}',
95, 1),

-- 3. 胡不归模型
('胡不归模型',
'【胡不归模型模板约束】本题为胡不归加权线段最值（PA+k·PB型）：1个滑杆驱动主动点P在定直线上运动；从动点通过旋转变换构造加权线段k·PB；绘制特殊构造角（sinθ=k）辅助虚线；高亮加权求和的两条线段PA与k·PB；开启定位最小值按钮求解加权和最小；禁止写死从动点坐标，依赖transform旋转变换；需要标注构造角θ与比例系数k。',
'{"showSlider":true,"showLocateMin":true,"showTrace":true,"showCoordinate":true}',
92, 1),

-- 4. 阿氏圆模型
('阿氏圆模型',
'【阿氏圆模型模板约束】本题为圆上动点加权线段最值（PA+k·PB型，P在圆上）：主动点限制在圆周上运动，滑杆参数为圆心角；使用缩放+旋转变换构造母子相似从动点（transform=scale+rotate）；绘制辅助相似三角形（淡黄色填充）；高亮加权两段目标线段；开启定位最小值；必须绘制圆形轨迹与圆心半径；从动点不写死坐标，使用transform复合变换；标注相似比k。',
'{"showSlider":true,"showLocateMin":true,"showTrace":true,"showCoordinate":true}',
91, 1),

-- 5. 隐圆模型
('隐圆模型',
'【隐圆模型模板约束】本题为隐圆（定点定长/定弦定角）动点轨迹：主动点表面在直线或图形上运动，实际轨迹为隐藏的圆；从动点由主动点变换得到；必须绘制虚线隐圆轨迹（辅助圆）；标注圆心与半径；高亮目标线段；开启定位最小值按钮（最值在圆心连线方向取得）；从动点禁止写死坐标，使用transform变换；隐圆用虚线绘制并可切换显示。',
'{"showSlider":true,"showLocateMin":true,"showTrace":true,"showCoordinate":true}',
89, 1),

-- 6. 定弦定角
('定弦定角',
'【定弦定角模板约束】本题为定弦定角隐圆：固定线段AB为弦，动点P对AB张角为定值θ，P的轨迹为两段圆弧；设置滑杆驱动P在圆弧上运动；必须绘制辅助圆（虚线），标注圆心、半径、圆周角θ；高亮目标线段或面积；开启轨迹显示；从动点由P变换得到时使用transform；禁止写死P坐标，由圆弧参数驱动；标注定角θ的角符号。',
'{"showSlider":true,"showLocateMin":true,"showTrace":true,"showCoordinate":true}',
88, 1),

-- 7. 双动点最值
('双动点最值',
'【双动点模板约束】本题为双动点题型：设置2组独立滑杆分别驱动两个主动点P和Q；从动点由两个主动点复合变换得到；绘制双动点各自运动轨迹；目标线段或面积由双动点共同决定；关闭定位最小值（或仅对单参数扫描）；两条滑杆独立可控可联动；禁止硬编码任何点坐标；辅助线用虚线标注双动点关联关系。',
'{"showSlider":true,"showLocateMin":false,"showTrace":true,"showCoordinate":true}',
87, 1),

-- 8. 点的轨迹
('点的轨迹',
'【点的轨迹模板约束】本题为动点轨迹探究：设置1个滑杆驱动主动点；从动点由主动点通过几何变换得到；核心是绘制从动点的运动轨迹曲线（直线/圆/抛物线/椭圆）；必须开启轨迹显示并默认开启；标注轨迹类型（直线/圆/抛物线）；高亮目标线段；可关闭定位最小值；从动点禁止写死坐标，必须使用transform变换；轨迹用彩色曲线绘制。',
'{"showSlider":true,"showLocateMin":false,"showTrace":true,"showCoordinate":true}',
86, 1),

-- 9. 等腰三角形存在性
('等腰三角形存在性',
'【等腰三角形存在性模板约束】本题为动点存在性问题：滑杆驱动主动点；不生成单一从动点，需要标注3类等腰分类位置（分别以三个顶点为等腰顶点）；画布上用不同颜色标记三类候选解位置；关闭定位最小值；开启轨迹绘制，显示动点运动路径；辅助虚线标记相等边长；禁止硬编码候选点坐标，由分类条件计算得到；每类候选点用不同颜色圆点标注。',
'{"showSlider":true,"showLocateMin":false,"showTrace":true,"showCoordinate":true}',
85, 1),

-- 10. 直角三角形存在性
('直角三角形存在性',
'【直角三角形存在性模板约束】本题为直角三角形存在探究：滑杆控制主动点；标记三类直角顶点对应的候选位置（分别以三点为直角顶点）；直角位置绘制直角标记符号；不同颜色区分3种分类；开启轨迹显示；关闭定位最小值；辅助虚线标记垂直关系；从动候选点使用垂直变换transform生成；以斜边为直径画辅助圆（两直角顶点情形），用虚线绘制。',
'{"showSlider":true,"showLocateMin":false,"showTrace":true,"showCoordinate":true}',
84, 1),

-- 11. 平行四边形存在性
('平行四边形存在性',
'【平行四边形存在性模板约束】本题为平行四边形存在性：滑杆驱动主动点在定直线或图形上运动；从动点为平行四边形第四个顶点，由中点坐标公式/平移变换得到（transform=translation）；标记多种分类情形（按对角线不同分类）；不同颜色标注各候选平行四边形；开启轨迹显示；关闭定位最小值；禁止硬编码候选点坐标，使用平移变换约束；用淡色填充各候选平行四边形区分。',
'{"showSlider":true,"showLocateMin":false,"showTrace":true,"showCoordinate":true}',
83, 1),

-- 12. 菱形存在性
('菱形存在性',
'【菱形存在性模板约束】本题为菱形存在性探究：滑杆驱动主动点；从动点由邻边相等+平行四边形约束计算得到（transform=rotation+translation复合）；标记分类情形（按已知边为边/对角线分类）；绘制菱形四边，用不同颜色填充候选菱形；开启轨迹显示；关闭定位最小值；标注相等边长；禁止硬编码候选点坐标，使用几何变换约束；菱形对角线用虚线绘制。',
'{"showSlider":true,"showLocateMin":false,"showTrace":true,"showCoordinate":true}',
82, 1),

-- 13. 矩形存在性
('矩形存在性',
'【矩形存在性模板约束】本题为矩形存在性探究：滑杆驱动主动点；从动点由直角+平行四边形约束得到（transform=translation）；标记分类情形；直角顶点处绘制直角符号；用淡色填充候选矩形；开启轨迹显示；关闭定位最小值；矩形对角线用虚线绘制并标注相等关系；禁止硬编码候选点坐标；可绘制以斜边为直径的辅助圆（直角顶点轨迹）。',
'{"showSlider":true,"showLocateMin":false,"showTrace":true,"showCoordinate":true}',
81, 1),

-- 14. 面积最值
('面积最值',
'【面积最值模板约束】本题为动点驱动的图形面积最值：设置1个滑杆驱动主动点；从动点由主动点变换得到；动态变化的三角形/多边形用淡黄色填充；必须开启定位最小值/最大值按钮（面积极值）；实时显示面积数值；绘制面积随滑杆变化的辅助提示；禁止硬编码点坐标，使用transform变换；标注底和高（或面积公式涉及的线段）；面积区域高亮填充。',
'{"showSlider":true,"showLocateMin":true,"showTrace":false,"showCoordinate":true}',
85, 1),

-- 15. 周长最值
('周长最值',
'【周长最值模板约束】本题为动点驱动的图形周长最值：设置1个滑杆驱动主动点；从动点由主动点变换得到；周长涉及的各条线段用不同颜色高亮；必须开启定位最小值按钮；通过对称/平移变换将折线周长转化为直线距离（transform=reflection或translation）；绘制变换辅助线（虚线）；禁止硬编码点坐标；标注周长各段线段；实时显示周长数值。',
'{"showSlider":true,"showLocateMin":true,"showTrace":false,"showCoordinate":true}',
83, 1),

-- 16. 二次函数动点
('二次函数动点',
'【二次函数动点模板约束】本题为二次函数抛物线上动点：主动点绑定抛物线参数滑杆（参数为横坐标x）；从动点由主动点做平移/垂直变换得到；画布必须绘制抛物线曲线；坐标轴必须开启且默认显示；高亮目标线段或面积区域；可开启轨迹；关闭定位最小值（特殊面积最值题目除外，由uiConfig覆盖）；点坐标由函数参数驱动，禁止写死固定xy；标注抛物线顶点、对称轴（虚线）、与坐标轴交点。',
'{"showSlider":true,"showLocateMin":false,"showTrace":true,"showCoordinate":true}',
80, 1),

-- 17. 一次函数动点
('一次函数动点',
'【一次函数动点模板约束】本题为一次函数直线上动点：主动点绑定直线参数滑杆；从动点由主动点做平移/垂直/反射变换得到；画布绘制一次函数直线；坐标轴开启；高亮目标线段或三角形面积；可开启轨迹；通常关闭定位最小值（最值题除外）；点坐标由直线参数驱动，禁止写死xy；标注直线与坐标轴交点、斜率；动态三角形用淡色填充。',
'{"showSlider":true,"showLocateMin":false,"showTrace":true,"showCoordinate":true}',
78, 1),

-- 18. 反比例函数动点
('反比例函数动点',
'【反比例函数动点模板约束】本题为反比例函数图像上动点：主动点绑定双曲线参数滑杆；从动点由主动点做平移/垂直变换得到；画布绘制双曲线（两支）；坐标轴必须开启；标注k值几何意义（矩形面积=|k|）；高亮目标线段或面积区域；可开启轨迹；关闭定位最小值；点坐标由双曲线参数驱动，禁止写死xy；过动点作坐标轴垂线，形成的矩形用淡色填充并标注面积。',
'{"showSlider":true,"showLocateMin":false,"showTrace":true,"showCoordinate":true}',
76, 1),

-- 19. 圆上动点最值
('圆上动点最值',
'【圆上动点最值模板约束】本题为圆上动点到定点/定直线距离最值：主动点在圆周上运动，滑杆参数为圆心角；必须绘制圆、圆心、半径；高亮目标线段（动点到定点/定直线）；开启定位最小值/最大值按钮（最值在圆心与定点连线方向取得）；绘制圆心连线辅助线（虚线）；禁止写死动点坐标，由圆参数方程驱动；标注最近点/最远点位置；可开启轨迹。',
'{"showSlider":true,"showLocateMin":true,"showTrace":true,"showCoordinate":true}',
88, 1),

-- 20. 旋转相似
('旋转相似',
'【旋转相似模板约束】本题为旋转相似（手拉手模型）动点：设置1个滑杆驱动主动点；从动点由主动点绕定点旋转+缩放得到（transform=rotate+scale复合变换）；绘制两个相似三角形（淡黄色填充）；标注旋转角与相似比；高亮目标线段；开启轨迹显示（从动点轨迹为圆）；可开启定位最小值；禁止硬编码从动点坐标，必须使用复合transform变换；连接对应顶点，标注相似关系。',
'{"showSlider":true,"showLocateMin":true,"showTrace":true,"showCoordinate":true}',
90, 1),

-- 21. 折叠求最值
('折叠求最值',
'【折叠求最值模板约束】本题为折叠后线段/面积最值：主动点控制折叠位置；从动点为折叠映射点（transform=reflection）；绘制折叠前后两套图形，折叠后图形用淡色填充；折叠痕用虚线辅助线；高亮折叠后目标线段；开启定位最小值按钮（最值通常在边界或垂直位置取得）；禁止硬编码从动点坐标，使用反射变换；标注折叠前后对应点连线（被折痕垂直平分）。',
'{"showSlider":true,"showLocateMin":true,"showTrace":false,"showCoordinate":true}',
92, 1);
