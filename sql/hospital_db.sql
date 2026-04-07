/*
 Navicat Premium Dump SQL

 Source Server         : 8.0数据库
 Source Server Type    : MySQL
 Source Server Version : 80012 (8.0.12)
 Source Host           : localhost:3306
 Source Schema         : hospital_db

 Target Server Type    : MySQL
 Target Server Version : 80012 (8.0.12)
 File Encoding         : 65001

 Date: 29/06/2025 20:36:02
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for appointment
-- ----------------------------
DROP TABLE IF EXISTS `appointment`;
CREATE TABLE `appointment`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `patient_id` bigint(20) NOT NULL COMMENT '患者ID',
  `doctor_id` bigint(20) NOT NULL COMMENT '医生ID',
  `schedule_id` bigint(20) NOT NULL COMMENT '排班ID',
  `appointment_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '预约编号',
  `appointment_date` date NOT NULL COMMENT '预约日期',
  `time_slot` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '时间段(上午/下午/晚上)',
  `symptoms` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '症状描述',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态(0:取消,1:待就诊,2:已就诊)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_appointment_no`(`appointment_no` ASC) USING BTREE,
  INDEX `idx_patient_id`(`patient_id` ASC) USING BTREE,
  INDEX `idx_doctor_id`(`doctor_id` ASC) USING BTREE,
  INDEX `idx_schedule_id`(`schedule_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '预约表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of appointment
-- ----------------------------
INSERT INTO `appointment` VALUES (1, 1, 1, 1, 'A001', '2025-06-08', '上午', '头痛、发热三天', 2, '2025-05-14 11:00:00', '2025-06-07 15:06:45');
INSERT INTO `appointment` VALUES (2, 2, 2, 3, 'A002', '2025-06-08', '上午', '腹部疼痛两天', 1, '2025-05-14 11:01:00', '2025-05-14 11:01:00');
INSERT INTO `appointment` VALUES (5, 3, 9, 19, 'A20250607130564', '2025-06-09', '上午', '咳嗽', 2, '2025-06-07 16:03:06', '2025-06-07 19:42:56');
INSERT INTO `appointment` VALUES (6, 2, 12, 25, 'A20250607052564', '2025-06-10', '上午', '1111', 1, '2025-06-07 19:42:03', '2025-06-07 19:42:03');

-- ----------------------------
-- Table structure for department
-- ----------------------------
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门名称',
  `dept_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门编码',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '部门描述',
  `director_id` bigint(20) NULL DEFAULT NULL COMMENT '负责人ID',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_dept_code`(`dept_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部门表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of department
-- ----------------------------
INSERT INTO `department` VALUES (1, '内科', 'NK', '负责内科疾病的诊断和治疗', 2, 1, '2025-05-14 10:10:00', '2025-05-14 10:10:00');
INSERT INTO `department` VALUES (2, '外科', 'WK', '负责外科疾病的诊断和治疗', 3, 1, '2025-05-14 10:11:00', '2025-05-14 10:11:00');
INSERT INTO `department` VALUES (3, '儿科', 'EK', '负责儿童疾病的诊断和治疗', NULL, 1, '2025-05-14 10:12:00', '2025-05-14 10:12:00');
INSERT INTO `department` VALUES (4, '妇产科', 'FCK', '负责妇科和产科疾病的诊断和治疗', NULL, 1, '2025-05-14 10:13:00', '2025-05-14 10:13:00');
INSERT INTO `department` VALUES (5, '急诊科', 'JZK', '负责急诊病人的诊断和治疗', NULL, 1, '2025-05-14 10:14:00', '2025-05-14 10:14:00');
INSERT INTO `department` VALUES (6, '心血管内科', 'XGNK', '负责心脏及血管疾病的诊断和治疗', NULL, 1, '2025-05-14 10:15:00', '2025-05-14 10:15:00');
INSERT INTO `department` VALUES (7, '神经内科', 'SJNK', '负责脑部及神经系统疾病的诊断和治疗', NULL, 1, '2025-05-14 10:16:00', '2025-05-14 10:16:00');
INSERT INTO `department` VALUES (8, '消化内科', 'XHNK', '负责消化系统疾病的诊断和治疗', NULL, 1, '2025-05-14 10:17:00', '2025-05-14 10:17:00');
INSERT INTO `department` VALUES (9, '呼吸内科', 'HXNK', '负责呼吸系统疾病的诊断和治疗', NULL, 1, '2025-05-14 10:18:00', '2025-05-14 10:18:00');
INSERT INTO `department` VALUES (10, '肾内科', 'SNK', '负责肾脏疾病的诊断和治疗', NULL, 1, '2025-05-14 10:19:00', '2025-05-14 10:19:00');
INSERT INTO `department` VALUES (11, '骨科', 'GK', '负责骨骼、关节、肌肉等疾病的诊断和治疗', NULL, 1, '2025-05-14 10:20:00', '2025-05-14 10:20:00');
INSERT INTO `department` VALUES (12, '泌尿外科', 'MNWK', '负责泌尿系统疾病的外科治疗', NULL, 1, '2025-05-14 10:21:00', '2025-05-14 10:21:00');
INSERT INTO `department` VALUES (13, '神经外科', 'SJWK', '负责脑部及神经系统疾病的外科治疗', NULL, 1, '2025-05-14 10:22:00', '2025-05-14 10:22:00');
INSERT INTO `department` VALUES (14, '胸外科', 'XWK', '负责胸部疾病的外科治疗', NULL, 1, '2025-05-14 10:23:00', '2025-05-14 10:23:00');
INSERT INTO `department` VALUES (15, '心脏外科', 'XZWK', '负责心脏疾病的外科治疗', NULL, 1, '2025-05-14 10:24:00', '2025-05-14 10:24:00');
INSERT INTO `department` VALUES (16, '眼科', 'YK', '负责眼部疾病的诊断和治疗', NULL, 1, '2025-05-14 10:25:00', '2025-05-14 10:25:00');
INSERT INTO `department` VALUES (17, '耳鼻喉科', 'EBHK', '负责耳、鼻、喉疾病的诊断和治疗', 51, 1, '2025-05-14 10:26:00', '2025-06-07 15:09:22');
INSERT INTO `department` VALUES (18, '皮肤科', 'PFK', '负责皮肤疾病的诊断和治疗', NULL, 1, '2025-05-14 10:27:00', '2025-05-14 10:27:00');
INSERT INTO `department` VALUES (19, '口腔科', 'KQK', '负责口腔疾病的诊断和治疗', NULL, 1, '2025-05-14 10:28:00', '2025-05-14 10:28:00');
INSERT INTO `department` VALUES (20, '精神科', 'JSK', '负责精神疾病的诊断和治疗', NULL, 1, '2025-05-14 10:29:00', '2025-05-14 10:29:00');
INSERT INTO `department` VALUES (21, '肿瘤科', 'ZLK', '负责肿瘤疾病的诊断和治疗', NULL, 1, '2025-05-14 10:30:00', '2025-05-14 10:30:00');
INSERT INTO `department` VALUES (22, '感染科', 'GRK', '负责传染病的诊断和治疗', NULL, 1, '2025-05-14 10:31:00', '2025-05-14 10:31:00');
INSERT INTO `department` VALUES (23, '康复科', 'KFK', '负责患者康复训练和治疗', NULL, 1, '2025-05-14 10:32:00', '2025-05-14 10:32:00');
INSERT INTO `department` VALUES (24, '中医科', 'ZYK', '运用中医理论和方法诊断治疗疾病', NULL, 1, '2025-05-14 10:33:00', '2025-05-14 10:33:00');
INSERT INTO `department` VALUES (25, '针灸推拿科', 'ZJTK', '运用针灸推拿方法治疗疾病', NULL, 1, '2025-05-14 10:34:00', '2025-05-14 10:34:00');

-- ----------------------------
-- Table structure for doctor
-- ----------------------------
DROP TABLE IF EXISTS `doctor`;
CREATE TABLE `doctor`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '医生ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `doctor_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '医生编号',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '姓名',
  `department_id` bigint(20) NOT NULL COMMENT '科室ID',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '职称',
  `expertise` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '专长',
  `introduction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '简介',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态(0:离职,1:在职)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_doctor_no`(`doctor_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_department_id`(`department_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 53 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '医生表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of doctor
-- ----------------------------
INSERT INTO `doctor` VALUES (1, 2, 'D001', '张医生', 1, '主任医师', '心血管疾病', '毕业于北京医科大学，从事心内科临床工作20年，擅长冠心病、高血压等疾病的诊治。', 1, '2025-05-14 10:20:00', '2025-05-14 10:20:00');
INSERT INTO `doctor` VALUES (2, 3, 'D002', '李医生', 2, '副主任医师', '普外科手术', '毕业于上海医科大学，从事外科临床工作15年，擅长胆囊、阑尾等常见手术。', 1, '2025-05-14 10:21:00', '2025-05-14 10:21:00');
INSERT INTO `doctor` VALUES (3, 7, 'D003', '刘医生', 3, '主治医师', '儿科常见疾病', '毕业于中国医科大学，从事儿科临床工作10年，擅长儿童常见疾病的诊治。', 1, '2025-05-14 10:22:00', '2025-05-14 10:22:00');
INSERT INTO `doctor` VALUES (4, 8, 'D004', '孙医生', 4, '主任医师', '妇产科疾病', '毕业于复旦大学医学院，从事妇产科临床工作18年，擅长妇科肿瘤和高危妊娠。', 1, '2025-05-14 10:23:00', '2025-05-14 10:23:00');
INSERT INTO `doctor` VALUES (5, 9, 'D005', '周医生', 5, '副主任医师', '急诊医学', '毕业于北京协和医学院，从事急诊科工作12年，擅长各类急危重症的救治。', 1, '2025-05-14 10:24:00', '2025-05-14 10:24:00');
INSERT INTO `doctor` VALUES (6, 10, 'D006', '吴医生', 6, '主任医师', '冠心病', '毕业于华中科技大学同济医学院，从事心血管内科临床工作22年，擅长冠心病的诊治。', 1, '2025-05-14 10:25:00', '2025-05-14 10:25:00');
INSERT INTO `doctor` VALUES (7, 11, 'D007', '郑医生', 7, '副主任医师', '帕金森病', '毕业于首都医科大学，从事神经内科临床工作16年，擅长帕金森病和脑卒中的诊治。', 1, '2025-05-14 10:26:00', '2025-05-14 10:26:00');
INSERT INTO `doctor` VALUES (8, 12, 'D008', '王医生', 8, '主治医师', '胃肠疾病', '毕业于中山大学医学院，从事消化内科临床工作8年，擅长胃肠疾病的诊治。', 1, '2025-05-14 10:27:00', '2025-05-14 10:27:00');
INSERT INTO `doctor` VALUES (9, 13, 'D009', '冯医生', 9, '主任医师', '慢性肺病', '毕业于浙江大学医学院，从事呼吸内科临床工作20年，擅长慢性阻塞性肺病和肺癌的诊治。', 1, '2025-05-14 10:28:00', '2025-05-14 10:28:00');
INSERT INTO `doctor` VALUES (10, 14, 'D010', '陈医生', 10, '副主任医师', '肾脏病', '毕业于四川大学华西医学院，从事肾内科临床工作14年，擅长肾病综合征和肾衰竭的诊治。', 1, '2025-05-14 10:29:00', '2025-05-14 10:29:00');
INSERT INTO `doctor` VALUES (11, 15, 'D011', '褚医生', 11, '主任医师', '关节外科', '毕业于北京大学医学部，从事骨科临床工作25年，擅长人工关节置换和关节镜手术。', 1, '2025-05-14 10:30:00', '2025-05-14 10:30:00');
INSERT INTO `doctor` VALUES (12, 16, 'D012', '魏医生', 12, '副主任医师', '泌尿系结石', '毕业于中南大学湘雅医学院，从事泌尿外科临床工作13年，擅长泌尿系结石和前列腺疾病的诊治。', 1, '2025-05-14 10:31:00', '2025-05-14 10:31:00');
INSERT INTO `doctor` VALUES (13, 17, 'D013', '蒋医生', 13, '主任医师', '脑肿瘤', '毕业于上海交通大学医学院，从事神经外科临床工作23年，擅长脑肿瘤和脑血管疾病的手术治疗。', 1, '2025-05-14 10:32:00', '2025-05-14 10:32:00');
INSERT INTO `doctor` VALUES (14, 18, 'D014', '沈医生', 14, '副主任医师', '肺部疾病', '毕业于天津医科大学，从事胸外科临床工作15年，擅长肺癌和食管癌的手术治疗。', 1, '2025-05-14 10:33:00', '2025-05-14 10:33:00');
INSERT INTO `doctor` VALUES (15, 19, 'D015', '韩医生', 15, '主任医师', '心脏手术', '毕业于哈尔滨医科大学，从事心脏外科临床工作21年，擅长冠状动脉搭桥和心脏瓣膜置换手术。', 1, '2025-05-14 10:34:00', '2025-05-14 10:34:00');
INSERT INTO `doctor` VALUES (16, 20, 'D016', '杨医生', 16, '副主任医师', '白内障', '毕业于重庆医科大学，从事眼科临床工作12年，擅长白内障和青光眼的诊治。', 1, '2025-05-14 10:35:00', '2025-05-14 10:35:00');
INSERT INTO `doctor` VALUES (17, 21, 'D017', '朱医生', 17, '主任医师', '鼻窦炎', '毕业于南京医科大学，从事耳鼻喉科临床工作19年，擅长鼻窦炎和耳聋的诊治。', 1, '2025-05-14 10:36:00', '2025-05-14 10:36:00');
INSERT INTO `doctor` VALUES (18, 22, 'D018', '秦医生', 18, '副主任医师', '银屑病', '毕业于安徽医科大学，从事皮肤科临床工作14年，擅长银屑病和湿疹的诊治。', 1, '2025-05-14 10:37:00', '2025-05-14 10:37:00');
INSERT INTO `doctor` VALUES (19, 23, 'D019', '尤医生', 19, '主任医师', '口腔正畸', '毕业于武汉大学口腔医学院，从事口腔科临床工作20年，擅长口腔正畸和种植牙。', 1, '2025-05-14 10:38:00', '2025-05-14 10:38:00');
INSERT INTO `doctor` VALUES (20, 24, 'D020', '许医生', 20, '副主任医师', '抑郁症', '毕业于山东大学医学院，从事精神科临床工作15年，擅长抑郁症和焦虑症的诊治。', 1, '2025-05-14 10:39:00', '2025-05-14 10:39:00');
INSERT INTO `doctor` VALUES (21, 25, 'D021', '何医生', 21, '主任医师', '肿瘤放疗', '毕业于吉林大学医学部，从事肿瘤科临床工作22年，擅长肿瘤的放射治疗。', 1, '2025-05-14 10:40:00', '2025-05-14 10:40:00');
INSERT INTO `doctor` VALUES (22, 26, 'D022', '吕医生', 22, '副主任医师', '传染病', '毕业于大连医科大学，从事感染科临床工作13年，擅长病毒性肝炎和结核病的诊治。', 1, '2025-05-14 10:41:00', '2025-05-14 10:41:00');
INSERT INTO `doctor` VALUES (23, 27, 'D023', '施医生', 23, '主治医师', '运动康复', '毕业于福建医科大学，从事康复科临床工作9年，擅长运动康复和中风后康复。', 1, '2025-05-14 10:42:00', '2025-05-14 10:42:00');
INSERT INTO `doctor` VALUES (24, 28, 'D024', '张医生', 24, '主任医师', '中医内科', '毕业于北京中医药大学，从事中医科临床工作24年，擅长中医内科疾病的诊治。', 1, '2025-05-14 10:43:00', '2025-05-14 10:43:00');
INSERT INTO `doctor` VALUES (25, 29, 'D025', '孔医生', 25, '副主任医师', '针灸推拿', '毕业于上海中医药大学，从事针灸推拿科临床工作16年，擅长针灸治疗颈椎病和腰椎间盘突出。', 1, '2025-05-14 10:44:00', '2025-05-14 10:44:00');
INSERT INTO `doctor` VALUES (26, 30, 'D026', '曹医生', 1, '主治医师', '高血压', '毕业于南方医科大学，从事内科临床工作8年，擅长高血压和糖尿病的诊治。', 1, '2025-05-14 10:45:00', '2025-05-14 10:45:00');
INSERT INTO `doctor` VALUES (27, 31, 'D027', '严医生', 2, '主任医师', '骨折', '毕业于广州医科大学，从事外科临床工作21年，擅长骨折和创伤的手术治疗。', 1, '2025-05-14 10:46:00', '2025-05-14 10:46:00');
INSERT INTO `doctor` VALUES (28, 32, 'D028', '华医生', 3, '副主任医师', '儿童哮喘', '毕业于昆明医科大学，从事儿科临床工作14年，擅长儿童哮喘和过敏性疾病的诊治。', 1, '2025-05-14 10:47:00', '2025-05-14 10:47:00');
INSERT INTO `doctor` VALUES (29, 33, 'D029', '金医生', 4, '主治医师', '妇科肿瘤', '毕业于新疆医科大学，从事妇产科临床工作9年，擅长妇科肿瘤的诊治。', 1, '2025-05-14 10:48:00', '2025-05-14 10:48:00');
INSERT INTO `doctor` VALUES (30, 34, 'D030', '魏医生', 5, '主任医师', '创伤急救', '毕业于河北医科大学，从事急诊科临床工作20年，擅长创伤急救和中毒救治。', 1, '2025-05-14 10:49:00', '2025-05-14 10:49:00');
INSERT INTO `doctor` VALUES (31, 35, 'D031', '陶医生', 6, '副主任医师', '心律失常', '毕业于郑州大学医学院，从事心血管内科临床工作15年，擅长心律失常和心力衰竭的诊治。', 1, '2025-05-14 10:50:00', '2025-05-14 10:50:00');
INSERT INTO `doctor` VALUES (32, 36, 'D032', '姜医生', 7, '主治医师', '头痛', '毕业于江西医学院，从事神经内科临床工作7年，擅长头痛和睡眠障碍的诊治。', 1, '2025-05-14 10:51:00', '2025-05-14 10:51:00');
INSERT INTO `doctor` VALUES (33, 37, 'D033', '戚医生', 8, '主任医师', '肝病', '毕业于广西医科大学，从事消化内科临床工作22年，擅长肝病和胰腺疾病的诊治。', 1, '2025-05-14 10:52:00', '2025-05-14 10:52:00');
INSERT INTO `doctor` VALUES (34, 38, 'D034', '谢医生', 9, '副主任医师', '哮喘', '毕业于贵州医科大学，从事呼吸内科临床工作13年，擅长哮喘和肺部感染的诊治。', 1, '2025-05-14 10:53:00', '2025-05-14 10:53:00');
INSERT INTO `doctor` VALUES (35, 39, 'D035', '邹医生', 10, '主治医师', '尿毒症', '毕业于西安交通大学医学院，从事肾内科临床工作8年，擅长尿毒症和肾小球肾炎的诊治。', 1, '2025-05-14 10:54:00', '2025-05-14 10:54:00');
INSERT INTO `doctor` VALUES (36, 40, 'D036', '喻医生', 11, '主任医师', '脊柱外科', '毕业于兰州大学医学院，从事骨科临床工作23年，擅长脊柱外科和创伤骨科。', 1, '2025-05-14 10:55:00', '2025-05-14 10:55:00');
INSERT INTO `doctor` VALUES (37, 41, 'D037', '柏医生', 12, '副主任医师', '前列腺疾病', '毕业于宁夏医科大学，从事泌尿外科临床工作14年，擅长前列腺疾病和男性不育的诊治。', 1, '2025-05-14 10:56:00', '2025-05-14 10:56:00');
INSERT INTO `doctor` VALUES (38, 42, 'D038', '水医生', 13, '主治医师', '脑血管病', '毕业于青岛大学医学院，从事神经外科临床工作9年，擅长脑血管病的手术治疗。', 1, '2025-05-14 10:57:00', '2025-05-14 10:57:00');
INSERT INTO `doctor` VALUES (39, 43, 'D039', '窦医生', 14, '主任医师', '食管癌', '毕业于海南医学院，从事胸外科临床工作21年，擅长食管癌和纵隔肿瘤的手术治疗。', 1, '2025-05-14 10:58:00', '2025-05-14 10:58:00');
INSERT INTO `doctor` VALUES (40, 44, 'D040', '章医生', 15, '副主任医师', '先天性心脏病', '毕业于内蒙古医科大学，从事心脏外科临床工作16年，擅长先天性心脏病的手术治疗。', 1, '2025-05-14 10:59:00', '2025-05-14 10:59:00');
INSERT INTO `doctor` VALUES (41, 45, 'D041', '云医生', 16, '主治医师', '视网膜疾病', '毕业于湖北医学院，从事眼科临床工作10年，擅长视网膜疾病和角膜疾病的诊治。', 1, '2025-05-14 11:00:00', '2025-05-14 11:00:00');
INSERT INTO `doctor` VALUES (42, 46, 'D042', '苏医生', 17, '主任医师', '咽喉疾病', '毕业于西南医科大学，从事耳鼻喉科临床工作24年，擅长咽喉疾病和听力障碍的诊治。', 1, '2025-05-14 11:01:00', '2025-05-14 11:01:00');
INSERT INTO `doctor` VALUES (43, 47, 'D043', '潘医生', 18, '副主任医师', '过敏性皮肤病', '毕业于南昌大学医学院，从事皮肤科临床工作15年，擅长过敏性皮肤病和皮肤肿瘤的诊治。', 1, '2025-05-14 11:02:00', '2025-05-14 11:02:00');
INSERT INTO `doctor` VALUES (44, 48, 'D044', '葛医生', 19, '主治医师', '牙体牙髓病', '毕业于温州医科大学，从事口腔科临床工作11年，擅长牙体牙髓病和牙周病的诊治。', 1, '2025-05-14 11:03:00', '2025-05-14 11:03:00');
INSERT INTO `doctor` VALUES (45, 49, 'D045', '奚医生', 20, '主任医师', '精神分裂症', '毕业于长治医学院，从事精神科临床工作25年，擅长精神分裂症和双相情感障碍的诊治。', 1, '2025-05-14 11:04:00', '2025-05-14 11:04:00');
INSERT INTO `doctor` VALUES (46, 50, 'D046', '范医生', 21, '副主任医师', '肿瘤化疗', '毕业于长沙医学院，从事肿瘤科临床工作17年，擅长肿瘤的化学治疗。', 1, '2025-05-14 11:05:00', '2025-05-14 11:05:00');
INSERT INTO `doctor` VALUES (47, 51, 'D047', '彭医生', 22, '主治医师', '艾滋病', '毕业于徐州医科大学，从事感染科临床工作12年，擅长艾滋病和乙型肝炎的诊治。', 1, '2025-05-14 11:06:00', '2025-05-14 11:06:00');
INSERT INTO `doctor` VALUES (48, 52, 'D048', '郎医生', 23, '主任医师', '神经康复', '毕业于遵义医科大学，从事康复科临床工作23年，擅长神经康复和骨科康复。', 1, '2025-05-14 11:07:00', '2025-05-14 11:07:00');
INSERT INTO `doctor` VALUES (49, 53, 'D049', '鲁医生', 24, '副主任医师', '中医肿瘤', '毕业于南京中医药大学，从事中医科临床工作18年，擅长中医肿瘤和中医妇科疾病的诊治。', 1, '2025-05-14 11:08:00', '2025-05-14 11:08:00');
INSERT INTO `doctor` VALUES (50, 54, 'D050', '韦医生', 25, '主治医师', '针灸减肥', '毕业于广州中医药大学，从事针灸推拿科临床工作13年，擅长针灸减肥和针灸戒烟。', 1, '2025-05-14 11:09:00', '2025-05-14 11:09:00');
INSERT INTO `doctor` VALUES (51, 55, 'D051', '昌医生', 1, '主任医师', '糖尿病', '毕业于山西医科大学，从事内科临床工作26年，擅长糖尿病和甲状腺疾病的诊治。', 1, '2025-05-14 11:10:00', '2025-05-14 11:10:00');
INSERT INTO `doctor` VALUES (52, 56, 'D052', '马医生', 2, '副主任医师', '腹腔镜手术', '毕业于河南大学医学院，从事外科临床工作19年，擅长腹腔镜手术和微创外科。', 1, '2025-05-14 11:11:00', '2025-05-14 11:11:00');

-- ----------------------------
-- Table structure for medical_record
-- ----------------------------
DROP TABLE IF EXISTS `medical_record`;
CREATE TABLE `medical_record`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `record_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '记录编号',
  `patient_id` bigint(20) NOT NULL COMMENT '患者ID',
  `doctor_id` bigint(20) NOT NULL COMMENT '医生ID',
  `appointment_id` bigint(20) NULL DEFAULT NULL COMMENT '预约ID',
  `diagnosis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '诊断结果',
  `treatment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '治疗方案',
  `record_date` date NOT NULL COMMENT '就诊日期',
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '医生备注',
  `follow_up` date NULL DEFAULT NULL COMMENT '随访日期',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_record_no`(`record_no` ASC) USING BTREE,
  INDEX `idx_patient_id`(`patient_id` ASC) USING BTREE,
  INDEX `idx_doctor_id`(`doctor_id` ASC) USING BTREE,
  INDEX `idx_appointment_id`(`appointment_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '就诊记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of medical_record
-- ----------------------------
INSERT INTO `medical_record` VALUES (1, 'MR001', 1, 1, 1, '普通感冒', '多喝水，休息，对症用药', '2025-06-08', '注意保暖', '2025-06-15', '2025-06-08 10:30:00', '2025-06-08 10:30:00');
INSERT INTO `medical_record` VALUES (2, 'MR002', 2, 2, 2, '胃炎', '口服药物治疗，清淡饮食', '2025-06-08', '避免辛辣刺激食物', '2025-06-15', '2025-06-08 11:00:00', '2025-06-08 11:00:00');

-- ----------------------------
-- Table structure for medicine
-- ----------------------------
DROP TABLE IF EXISTS `medicine`;
CREATE TABLE `medicine`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '药品ID',
  `medicine_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '药品编码',
  `medicine_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '药品名称',
  `specification` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格',
  `dosage_form` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '剂型(片剂/胶囊/注射剂等)',
  `manufacturer` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生产厂家',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '类别(处方药/非处方药)',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '单价',
  `stock` int(11) NULL DEFAULT 0 COMMENT '库存量',
  `instructions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '使用说明',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态(0:下架,1:上架)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `category_id` int(11) NULL DEFAULT NULL COMMENT '分类id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_medicine_code`(`medicine_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 57 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '药品表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of medicine
-- ----------------------------
INSERT INTO `medicine` VALUES (1, 'M001', '阿司匹林', '100mg*30片', '片剂', '国药集团', '处方药', 15.50, 1003, '每日三次，每次一片', 1, '2025-05-14 10:40:00', '2025-06-07 15:04:08', 1);
INSERT INTO `medicine` VALUES (2, 'M002', '布洛芬', '0.2g*10片', '片剂', '上海医药', '非处方药', 12.80, 800, '每日三次，每次一片', 1, '2025-05-14 10:41:00', '2025-06-07 13:46:04', 2);
INSERT INTO `medicine` VALUES (3, 'M003', '头孢克肟', '100mg*6片', '片剂', '哈药集团', '口服药', 38.50, 500, '每日两次，每次一片', 1, '2025-05-14 10:42:00', '2025-06-07 13:46:14', 6);
INSERT INTO `medicine` VALUES (4, 'M004', '复方感冒灵颗粒', '10g*9袋', '颗粒剂', '999药业', '处方药', 25.00, 600, '每日三次，每次一袋', 1, '2025-05-14 10:43:00', '2025-06-07 13:46:19', 1);
INSERT INTO `medicine` VALUES (5, 'M005', '甲硝唑', '0.2g*20片', '片剂', '齐鲁制药', '处方药', 18.00, 700, '每日两次，每次一片', 1, '2025-05-14 10:44:00', '2025-05-14 10:44:00', NULL);
INSERT INTO `medicine` VALUES (6, 'M006', '盐酸氨溴索片', '30mg*20片', '片剂', '恒瑞医药', '处方药', 22.80, 650, '每日三次，每次一片', 1, '2025-05-14 10:45:00', '2025-05-14 10:45:00', NULL);
INSERT INTO `medicine` VALUES (7, 'M007', '阿莫西林胶囊', '0.25g*24粒', '胶囊', '哈药集团', '处方药', 16.50, 800, '每日三次，每次一粒', 1, '2025-05-14 10:46:00', '2025-05-14 10:46:00', NULL);
INSERT INTO `medicine` VALUES (8, 'M008', '维生素C片', '0.1g*100片', '片剂', '华北制药', '非处方药', 10.00, 1200, '每日一次，每次一片', 1, '2025-05-14 10:47:00', '2025-05-14 10:47:00', NULL);
INSERT INTO `medicine` VALUES (9, 'M009', '盐酸雷尼替丁胶囊', '0.15g*30粒', '胶囊', '太极集团', '处方药', 28.50, 550, '每日两次，每次一粒', 1, '2025-05-14 10:48:00', '2025-05-14 10:48:00', NULL);
INSERT INTO `medicine` VALUES (10, 'M010', '维生素B族片', '复合*60片', '片剂', '东北制药', '非处方药', 13.50, 900, '每日一次，每次一片', 1, '2025-05-14 10:49:00', '2025-05-14 10:49:00', NULL);
INSERT INTO `medicine` VALUES (11, 'M011', '复方丹参滴丸', '27mg*180丸', '滴丸', '天士力', '处方药', 36.80, 450, '每日三次，每次10丸', 1, '2025-05-14 10:50:00', '2025-05-14 10:50:00', NULL);
INSERT INTO `medicine` VALUES (12, 'M012', '藿香正气水', '10ml*10支', '口服液', '同仁堂', '非处方药', 14.50, 700, '每日三次，每次一支', 1, '2025-05-14 10:51:00', '2025-05-14 10:51:00', NULL);
INSERT INTO `medicine` VALUES (13, 'M013', '辛伐他汀片', '20mg*28片', '片剂', '信立泰', '处方药', 45.60, 420, '每日一次，每次一片', 1, '2025-05-14 10:52:00', '2025-05-14 10:52:00', NULL);
INSERT INTO `medicine` VALUES (14, 'M014', '氨咖黄敏胶囊', '复合*20粒', '胶囊', '葵花药业', '非处方药', 17.20, 850, '每日三次，每次一粒', 1, '2025-05-14 10:53:00', '2025-05-14 10:53:00', NULL);
INSERT INTO `medicine` VALUES (15, 'M015', '二甲双胍片', '0.5g*30片', '片剂', '北京制药', '处方药', 24.30, 580, '每日三次，每次一片', 1, '2025-05-14 10:54:00', '2025-05-14 10:54:00', NULL);
INSERT INTO `medicine` VALUES (16, 'M016', '复方板蓝根颗粒', '10g*20袋', '颗粒剂', '云南白药', '非处方药', 28.00, 750, '每日三次，每次一袋', 1, '2025-05-14 10:55:00', '2025-05-14 10:55:00', NULL);
INSERT INTO `medicine` VALUES (17, 'M017', '苯磺酸氨氯地平片', '5mg*28片', '片剂', '辉瑞制药', '处方药', 39.80, 420, '每日一次，每次一片', 1, '2025-05-14 10:56:00', '2025-05-14 10:56:00', NULL);
INSERT INTO `medicine` VALUES (18, 'M018', '盐酸左氧氟沙星片', '0.1g*12片', '片剂', '扬子江药业', '处方药', 26.50, 600, '每日两次，每次一片', 1, '2025-05-14 10:57:00', '2025-05-14 10:57:00', NULL);
INSERT INTO `medicine` VALUES (19, 'M019', '枸橼酸铋钾胶囊', '0.11g*56粒', '胶囊', '丽珠集团', '处方药', 48.00, 380, '每日四次，每次一粒', 1, '2025-05-14 10:58:00', '2025-05-14 10:58:00', NULL);
INSERT INTO `medicine` VALUES (20, 'M020', '阿奇霉素片', '0.25g*6片', '片剂', '白云山制药', '处方药', 32.50, 450, '每日一次，每次两片', 1, '2025-05-14 10:59:00', '2025-05-14 10:59:00', NULL);
INSERT INTO `medicine` VALUES (21, 'M021', '银翘解毒片', '0.5g*24片', '片剂', '江中药业', '非处方药', 16.80, 800, '每日三次，每次两片', 1, '2025-05-14 11:00:00', '2025-05-14 11:00:00', NULL);
INSERT INTO `medicine` VALUES (22, 'M022', '盐酸氟桂利嗪胶囊', '5mg*20粒', '胶囊', '上海医药', '处方药', 29.50, 520, '每日三次，每次一粒', 1, '2025-05-14 11:01:00', '2025-05-14 11:01:00', NULL);
INSERT INTO `medicine` VALUES (23, 'M023', '连花清瘟胶囊', '0.35g*24粒', '胶囊', '以岭药业', '非处方药', 19.80, 900, '每日三次，每次四粒', 1, '2025-05-14 11:02:00', '2025-05-14 11:02:00', NULL);
INSERT INTO `medicine` VALUES (24, 'M024', '盐酸二甲双胍缓释片', '0.5g*30片', '片剂', '中美上海施贵宝', '处方药', 38.60, 450, '每日两次，每次一片', 1, '2025-05-14 11:03:00', '2025-05-14 11:03:00', NULL);
INSERT INTO `medicine` VALUES (25, 'M025', '替硝唑片', '0.5g*10片', '片剂', '海正药业', '处方药', 21.50, 600, '每日三次，每次一片', 1, '2025-05-14 11:04:00', '2025-05-14 11:04:00', NULL);
INSERT INTO `medicine` VALUES (26, 'M026', '盐酸特拉唑嗪片', '2mg*28片', '片剂', '科伦药业', '处方药', 35.80, 380, '每日一次，每次一片', 1, '2025-05-14 11:05:00', '2025-05-14 11:05:00', NULL);
INSERT INTO `medicine` VALUES (27, 'M027', '人参归脾丸', '9g*10丸', '水蜜丸', '同仁堂', '非处方药', 26.50, 550, '每日两次，每次一丸', 1, '2025-05-14 11:06:00', '2025-05-14 11:06:00', NULL);
INSERT INTO `medicine` VALUES (28, 'M028', '盐酸多塞平片', '25mg*100片', '片剂', '昆明制药', '处方药', 42.80, 320, '每日三次，每次一片', 1, '2025-05-14 11:07:00', '2025-05-14 11:07:00', NULL);
INSERT INTO `medicine` VALUES (29, 'M029', '小柴胡颗粒', '10g*10袋', '颗粒剂', '太极集团', '非处方药', 15.80, 780, '每日三次，每次一袋', 1, '2025-05-14 11:08:00', '2025-05-14 11:08:00', NULL);
INSERT INTO `medicine` VALUES (30, 'M030', '格列齐特缓释片', '30mg*60片', '片剂', '施维雅制药', '处方药', 48.50, 350, '每日一次，每次一片', 1, '2025-05-14 11:09:00', '2025-05-14 11:09:00', NULL);
INSERT INTO `medicine` VALUES (31, 'M031', '感冒清热颗粒', '12g*10袋', '颗粒剂', '华润三九', '非处方药', 16.80, 850, '每日三次，每次一袋', 1, '2025-05-14 11:10:00', '2025-05-14 11:10:00', NULL);
INSERT INTO `medicine` VALUES (32, 'M032', '盐酸伐地那非片', '20mg*2片', '片剂', '拜耳医药', '处方药', 58.00, 280, '性生活前1小时服用，每次一片', 1, '2025-05-14 11:11:00', '2025-05-14 11:11:00', NULL);
INSERT INTO `medicine` VALUES (33, 'M033', '板蓝根颗粒', '10g*20袋', '颗粒剂', '广州白云山', '非处方药', 18.50, 920, '每日三次，每次一袋', 1, '2025-05-14 11:12:00', '2025-05-14 11:12:00', NULL);
INSERT INTO `medicine` VALUES (34, 'M034', '盐酸西替利嗪片', '10mg*12片', '片剂', '上海先灵葆雅', '处方药', 32.50, 480, '每日一次，每次一片', 1, '2025-05-14 11:13:00', '2025-05-14 11:13:00', NULL);
INSERT INTO `medicine` VALUES (35, 'M035', '六味地黄丸', '9g*10丸', '水蜜丸', '北京同仁堂', '非处方药', 22.50, 680, '每日两次，每次一丸', 1, '2025-05-14 11:14:00', '2025-05-14 11:14:00', NULL);
INSERT INTO `medicine` VALUES (36, 'M036', '奥美拉唑肠溶胶囊', '20mg*28粒', '胶囊', '阿斯利康', '处方药', 48.50, 350, '每日一次，每次一粒', 1, '2025-05-14 11:15:00', '2025-05-14 11:15:00', NULL);
INSERT INTO `medicine` VALUES (37, 'M037', '黄连上清片', '0.3g*24片', '片剂', '哈药集团', '非处方药', 14.50, 750, '每日三次，每次两片', 1, '2025-05-14 11:16:00', '2025-05-14 11:16:00', NULL);
INSERT INTO `medicine` VALUES (38, 'M038', '阿托伐他汀钙片', '20mg*7片', '片剂', '辉瑞制药', '处方药', 56.80, 300, '每日一次，每次一片', 1, '2025-05-14 11:17:00', '2025-05-14 11:17:00', NULL);
INSERT INTO `medicine` VALUES (39, 'M039', '蒲地蓝消炎片', '0.3g*24片', '片剂', '济川药业', '非处方药', 17.80, 820, '每日三次，每次四片', 1, '2025-05-14 11:18:00', '2025-05-14 11:18:00', NULL);
INSERT INTO `medicine` VALUES (40, 'M040', '盐酸环丙沙星片', '0.25g*20片', '片剂', '白云山制药', '处方药', 26.50, 560, '每日两次，每次一片', 1, '2025-05-14 11:19:00', '2025-05-14 11:19:00', NULL);
INSERT INTO `medicine` VALUES (41, 'M041', '牛黄解毒片', '0.3g*36片', '片剂', '同仁堂', '非处方药', 19.80, 780, '每日三次，每次两片', 1, '2025-05-14 11:20:00', '2025-05-14 11:20:00', NULL);
INSERT INTO `medicine` VALUES (42, 'M042', '枸橼酸西地那非片', '50mg*4片', '片剂', '辉瑞制药', '处方药', 68.00, 250, '性生活前1小时服用，每次一片', 1, '2025-05-14 11:21:00', '2025-05-14 11:21:00', NULL);
INSERT INTO `medicine` VALUES (43, 'M043', '川贝止咳糖浆', '100ml/瓶', '糖浆剂', '太极集团', '非处方药', 15.50, 620, '每日三次，每次10ml', 1, '2025-05-14 11:22:00', '2025-05-14 11:22:00', NULL);
INSERT INTO `medicine` VALUES (44, 'M044', '盐酸帕罗西汀片', '20mg*14片', '片剂', '中美天津史克', '处方药', 75.00, 230, '每日一次，每次一片', 1, '2025-05-14 11:23:00', '2025-05-14 11:23:00', NULL);
INSERT INTO `medicine` VALUES (45, 'M045', '京制牛黄解毒丸', '3g*10丸', '水蜜丸', '同仁堂', '非处方药', 26.50, 550, '每日两次，每次一丸', 1, '2025-05-14 11:24:00', '2025-05-14 11:24:00', NULL);
INSERT INTO `medicine` VALUES (46, 'M046', '富马酸比索洛尔片', '5mg*30片', '片剂', '默克制药', '处方药', 42.50, 380, '每日一次，每次一片', 1, '2025-05-14 11:25:00', '2025-05-14 11:25:00', NULL);
INSERT INTO `medicine` VALUES (47, 'M047', '藿香正气胶囊', '0.3g*12粒', '胶囊', '广州白云山', '非处方药', 12.80, 850, '每日三次，每次一粒', 1, '2025-05-14 11:26:00', '2025-05-14 11:26:00', NULL);
INSERT INTO `medicine` VALUES (48, 'M048', '硝苯地平缓释片', '20mg*60片', '片剂', '拜耳医药', '处方药', 36.80, 420, '每日一次，每次一片', 1, '2025-05-14 11:27:00', '2025-05-14 11:27:00', NULL);
INSERT INTO `medicine` VALUES (49, 'M049', '金嗓子喉片', '2.6g*12片', '片剂', '广西金嗓子', '非处方药', 13.50, 880, '每日四次，每次含服一片', 1, '2025-05-14 11:28:00', '2025-05-14 11:28:00', NULL);
INSERT INTO `medicine` VALUES (50, 'M050', '碳酸钙D3片', '600mg*30片', '片剂', '惠氏制药', '非处方药', 29.80, 520, '每日一次，每次一片', 1, '2025-05-14 11:29:00', '2025-05-14 11:29:00', NULL);
INSERT INTO `medicine` VALUES (51, 'M051', '注射用青霉素钠', '800万单位/瓶', '注射剂', '华北制药', '处方药', 8.50, 600, '每日两次，每次80万单位', 1, '2025-05-14 11:30:00', '2025-05-14 11:30:00', NULL);
INSERT INTO `medicine` VALUES (52, 'M052', '柴胡注射液', '2ml*10支', '注射剂', '鲁南制药', '处方药', 28.00, 450, '每日一次，每次2ml', 1, '2025-05-14 11:31:00', '2025-05-14 11:31:00', NULL);
INSERT INTO `medicine` VALUES (53, 'M053', '维生素C注射液', '0.5g*10支', '注射剂', '天津药业', '处方药', 15.80, 720, '每日一次，每次一支', 1, '2025-05-14 11:32:00', '2025-05-14 11:32:00', NULL);
INSERT INTO `medicine` VALUES (54, 'M054', '葡萄糖注射液', '250ml/瓶', '注射剂', '科伦药业', '处方药', 6.80, 1200, '遵医嘱使用', 1, '2025-05-14 11:33:00', '2025-05-14 11:33:00', NULL);

-- ----------------------------
-- Table structure for medicine_category
-- ----------------------------
DROP TABLE IF EXISTS `medicine_category`;
CREATE TABLE `medicine_category`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `category_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `category_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类编码',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分类描述',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_category_code`(`category_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '药品分类表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of medicine_category
-- ----------------------------
INSERT INTO `medicine_category` VALUES (2, '非处方药', 'FCFY', '不需要凭执业医师处方即可自行判断、购买和使用的药品', 1, '2025-05-14 10:46:00', '2025-05-14 10:46:00');
INSERT INTO `medicine_category` VALUES (3, '中药', 'ZY', '以中药材为原料，在中医理论指导下制成的药品', 1, '2025-05-14 10:47:00', '2025-05-14 10:47:00');
INSERT INTO `medicine_category` VALUES (4, '西药', 'XY', '以化学合成或生物技术为主要方法制成的药品', 1, '2025-05-14 10:48:00', '2025-05-14 10:48:00');
INSERT INTO `medicine_category` VALUES (5, '针剂', 'ZJ', '需要注射给药的药物制剂', 1, '2025-05-14 10:49:00', '2025-05-14 10:49:00');
INSERT INTO `medicine_category` VALUES (6, '口服药', 'KFY', '经口服用的药物制剂', 1, '2025-05-14 10:50:00', '2025-05-14 10:50:00');
INSERT INTO `medicine_category` VALUES (7, '处方药', 'CFY', '处方药', 1, '2025-06-07 15:09:15', '2025-06-07 15:09:15');

-- ----------------------------
-- Table structure for patient
-- ----------------------------
DROP TABLE IF EXISTS `patient`;
CREATE TABLE `patient`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '患者ID',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '用户ID',
  `patient_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '患者编号',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '姓名',
  `id_card` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证号',
  `birthday` date NULL DEFAULT NULL COMMENT '出生日期',
  `sex` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '性别',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '住址',
  `medical_history` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '病史',
  `allergies` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '过敏史',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_patient_no`(`patient_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '患者表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of patient
-- ----------------------------
INSERT INTO `patient` VALUES (1, 5, 'P001', '赵患者', '110101199001011234', '1990-01-01', '男', '13844444444', '北京市朝阳区xxx街道', '无重大病史', '青霉素过敏', '2025-05-14 10:30:00', '2025-05-14 10:30:00');
INSERT INTO `patient` VALUES (2, 6, 'P002', '钱患者', '110101199202022345', '1992-02-02', '女', '13855555555', '北京市海淀区xxx街道', '曾做过阑尾切除手术', '无', '2025-05-14 10:31:00', '2025-05-14 10:31:00');
INSERT INTO `patient` VALUES (3, 57, 'PF14FE559', 'test', '320821200106063939', '2001-06-06', '女', '13456789987', 'test', 'test', 'test', '2025-06-07 15:26:47', '2025-06-07 15:48:21');

-- ----------------------------
-- Table structure for medical_record_detail
-- ----------------------------
DROP TABLE IF EXISTS `medical_record_detail`;
CREATE TABLE `medical_record_detail`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '就诊明细ID',
  `record_id` bigint(20) NOT NULL COMMENT '就诊记录ID',
  `symptom_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '病症名称',
  `treatment_plan` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '对应治疗方案',
  `sort_no` int(11) NULL DEFAULT 1 COMMENT '排序号',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_mrd_record_id`(`record_id` ASC) USING BTREE,
  CONSTRAINT `fk_mrd_record` FOREIGN KEY (`record_id`) REFERENCES `medical_record` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '就诊记录病症明细表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of medical_record_detail
-- ----------------------------
INSERT INTO `medical_record_detail` VALUES (1, 1, '上呼吸道感染', '对症抗感染治疗，注意休息与补液', 1, '2025-06-08 10:35:00', '2025-06-08 10:35:00');
INSERT INTO `medical_record_detail` VALUES (2, 2, '慢性胃炎', '抑酸护胃，清淡饮食', 1, '2025-06-08 11:05:00', '2025-06-08 11:05:00');
INSERT INTO `medical_record_detail` VALUES (3, 1, '发热', '退热及观察体温变化', 2, '2025-06-15 09:35:00', '2025-06-15 09:35:00');
INSERT INTO `medical_record_detail` VALUES (4, 2, '胃黏膜刺激', '复诊评估，继续护胃治疗', 2, '2025-06-15 10:05:00', '2025-06-15 10:05:00');

-- ----------------------------
-- Table structure for prescription
-- ----------------------------
DROP TABLE IF EXISTS `prescription`;
CREATE TABLE `prescription`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '处方ID',
  `prescription_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '处方编号',
  `patient_id` bigint(20) NOT NULL COMMENT '患者ID',
  `doctor_id` bigint(20) NOT NULL COMMENT '医生ID',
  `record_id` bigint(20) NOT NULL COMMENT '就诊记录ID',
  `prescription_date` date NOT NULL COMMENT '处方日期',
  `diagnosis` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '诊断',
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注',
  `status` tinyint(4) NULL DEFAULT 0 COMMENT '状态(0:未取药,1:已取药)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_prescription_no`(`prescription_no` ASC) USING BTREE,
  INDEX `idx_patient_id`(`patient_id` ASC) USING BTREE,
  INDEX `idx_doctor_id`(`doctor_id` ASC) USING BTREE,
  INDEX `idx_record_id`(`record_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '处方表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of prescription
-- ----------------------------
INSERT INTO `prescription` VALUES (1, 'P001', 1, 1, 1, '2025-06-08', '普通感冒', '按时服药', 1, '2025-06-08 10:35:00', '2025-06-07 15:07:46');
INSERT INTO `prescription` VALUES (2, 'P002', 2, 2, 2, '2025-06-08', '胃炎', '饭后服药', 0, '2025-06-08 11:05:00', '2025-06-08 11:05:00');
INSERT INTO `prescription` VALUES (3, 'P003', 1, 3, 1, '2025-06-15', '感冒后续治疗', '继续服药', 0, '2025-06-15 09:35:00', '2025-06-15 09:35:00');
INSERT INTO `prescription` VALUES (4, 'P004', 2, 4, 2, '2025-06-15', '胃炎后续治疗', '继续服药', 1, '2025-06-15 10:05:00', '2025-06-07 19:43:06');
INSERT INTO `prescription` VALUES (5, 'P005', 1, 5, 1, '2025-06-20', '感冒后续治疗', '最后一次服药', 0, '2025-06-20 09:35:00', '2025-06-20 09:35:00');
INSERT INTO `prescription` VALUES (6, 'P20250607438970', 2, 2, 2, '2025-06-07', '', '', 0, '2025-06-07 15:08:40', '2025-06-07 15:08:40');

-- ----------------------------
-- Table structure for prescription_detail
-- ----------------------------
DROP TABLE IF EXISTS `prescription_detail`;
CREATE TABLE `prescription_detail`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `prescription_id` bigint(20) NOT NULL COMMENT '处方ID',
  `medical_record_detail_id` bigint(20) NULL DEFAULT NULL COMMENT '就诊病症明细ID',
  `medicine_id` bigint(20) NOT NULL COMMENT '药品ID',
  `dosage` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用量',
  `frequency` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '频次(一日三次/每日一次)',
  `days` int(11) NULL DEFAULT NULL COMMENT '用药天数',
  `quantity` int(11) NULL DEFAULT NULL COMMENT '数量',
  `usage` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用法(口服/外用等)',
  `notes` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_prescription_id`(`prescription_id` ASC) USING BTREE,
  INDEX `idx_medicine_id`(`medicine_id` ASC) USING BTREE,
  INDEX `idx_pd_mrd_id`(`medical_record_detail_id` ASC) USING BTREE,
  CONSTRAINT `fk_pd_mrd` FOREIGN KEY (`medical_record_detail_id`) REFERENCES `medical_record_detail` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '处方明细表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of prescription_detail
-- ----------------------------
INSERT INTO `prescription_detail` VALUES (1, 1, 1, 4, '每次一袋', '一日三次', 5, 15, '口服', '饭后温水冲服', '2025-06-08 10:35:00', '2025-06-08 10:35:00');
INSERT INTO `prescription_detail` VALUES (2, 1, 1, 8, '每次一片', '一日一次', 5, 5, '口服', '早餐后服用', '2025-06-08 10:35:00', '2025-06-08 10:35:00');
INSERT INTO `prescription_detail` VALUES (3, 2, 2, 9, '每次一粒', '一日两次', 7, 14, '口服', '饭后服用', '2025-06-08 11:05:00', '2025-06-08 11:05:00');
INSERT INTO `prescription_detail` VALUES (4, 2, 2, 19, '每次一粒', '一日四次', 7, 28, '口服', '饭后服用', '2025-06-08 11:05:00', '2025-06-08 11:05:00');
INSERT INTO `prescription_detail` VALUES (5, 3, 3, 16, '每次一袋', '一日三次', 5, 15, '口服', '饭后温水冲服', '2025-06-15 09:35:00', '2025-06-15 09:35:00');
INSERT INTO `prescription_detail` VALUES (6, 4, 4, 36, '每次一粒', '一日一次', 14, 14, '口服', '早餐后服用', '2025-06-15 10:05:00', '2025-06-15 10:05:00');
INSERT INTO `prescription_detail` VALUES (7, 5, 3, 21, '每次两片', '一日三次', 5, 30, '口服', '饭后服用', '2025-06-20 09:35:00', '2025-06-20 09:35:00');
INSERT INTO `prescription_detail` VALUES (8, 6, NULL, 5, '2', '一日三次', 7, 1, '口服', NULL, '2025-06-07 15:08:40', '2025-06-07 15:08:40');

-- ----------------------------
-- Table structure for schedule
-- ----------------------------
DROP TABLE IF EXISTS `schedule`;
CREATE TABLE `schedule`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '排班ID',
  `doctor_id` bigint(20) NOT NULL COMMENT '医生ID',
  `schedule_date` date NOT NULL COMMENT '排班日期',
  `time_slot` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '时间段(上午/下午/晚上)',
  `max_patients` int(11) NULL DEFAULT 0 COMMENT '最大接诊人数',
  `current_patients` int(11) NULL DEFAULT 0 COMMENT '当前预约人数',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态(0:停诊,1:正常)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_doctor_id`(`doctor_id` ASC) USING BTREE,
  INDEX `idx_schedule_date`(`schedule_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 37 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '排班表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of schedule
-- ----------------------------
INSERT INTO `schedule` VALUES (1, 1, '2025-06-08', '上午', 20, 0, 1, '2025-05-14 10:50:00', '2025-05-14 10:50:00');
INSERT INTO `schedule` VALUES (2, 1, '2025-06-08', '下午', 20, 0, 1, '2025-05-14 10:51:00', '2025-05-14 10:51:00');
INSERT INTO `schedule` VALUES (3, 2, '2025-06-08', '上午', 15, 0, 1, '2025-05-14 10:52:00', '2025-05-14 10:52:00');
INSERT INTO `schedule` VALUES (4, 2, '2025-06-08', '下午', 15, 0, 1, '2025-05-14 10:53:00', '2025-05-14 10:53:00');
INSERT INTO `schedule` VALUES (5, 3, '2025-06-08', '上午', 18, 0, 1, '2025-05-14 10:54:00', '2025-05-14 10:54:00');
INSERT INTO `schedule` VALUES (6, 3, '2025-06-08', '下午', 18, 0, 1, '2025-05-14 10:55:00', '2025-05-14 10:55:00');
INSERT INTO `schedule` VALUES (7, 4, '2025-06-08', '上午', 16, 0, 1, '2025-05-14 10:56:00', '2025-05-14 10:56:00');
INSERT INTO `schedule` VALUES (8, 4, '2025-06-08', '下午', 16, 0, 1, '2025-05-14 10:57:00', '2025-05-14 10:57:00');
INSERT INTO `schedule` VALUES (9, 5, '2025-06-08', '上午', 20, 0, 1, '2025-05-14 10:58:00', '2025-05-14 10:58:00');
INSERT INTO `schedule` VALUES (10, 5, '2025-06-08', '下午', 20, 0, 1, '2025-05-14 10:59:00', '2025-05-14 10:59:00');
INSERT INTO `schedule` VALUES (11, 1, '2025-06-09', '上午', 20, 0, 1, '2025-05-14 11:00:00', '2025-05-14 11:00:00');
INSERT INTO `schedule` VALUES (12, 1, '2025-06-09', '下午', 20, 0, 1, '2025-05-14 11:01:00', '2025-05-14 11:01:00');
INSERT INTO `schedule` VALUES (13, 6, '2025-06-09', '上午', 15, 0, 1, '2025-05-14 11:02:00', '2025-05-14 11:02:00');
INSERT INTO `schedule` VALUES (14, 6, '2025-06-09', '下午', 15, 0, 1, '2025-05-14 11:03:00', '2025-05-14 11:03:00');
INSERT INTO `schedule` VALUES (15, 7, '2025-06-09', '上午', 18, 0, 1, '2025-05-14 11:04:00', '2025-05-14 11:04:00');
INSERT INTO `schedule` VALUES (16, 7, '2025-06-09', '下午', 18, 0, 1, '2025-05-14 11:05:00', '2025-05-14 11:05:00');
INSERT INTO `schedule` VALUES (17, 8, '2025-06-09', '上午', 16, 0, 1, '2025-05-14 11:06:00', '2025-05-14 11:06:00');
INSERT INTO `schedule` VALUES (18, 8, '2025-06-09', '下午', 16, 0, 1, '2025-05-14 11:07:00', '2025-05-14 11:07:00');
INSERT INTO `schedule` VALUES (19, 9, '2025-06-09', '上午', 20, 1, 1, '2025-05-14 11:08:00', '2025-06-07 16:03:06');
INSERT INTO `schedule` VALUES (20, 9, '2025-06-09', '下午', 20, 0, 1, '2025-05-14 11:09:00', '2025-05-14 11:09:00');
INSERT INTO `schedule` VALUES (21, 10, '2025-06-10', '上午', 20, 0, 1, '2025-05-14 11:10:00', '2025-05-14 11:10:00');
INSERT INTO `schedule` VALUES (22, 10, '2025-06-10', '下午', 20, 0, 1, '2025-05-14 11:11:00', '2025-05-14 11:11:00');
INSERT INTO `schedule` VALUES (23, 11, '2025-06-10', '上午', 15, 0, 1, '2025-05-14 11:12:00', '2025-05-14 11:12:00');
INSERT INTO `schedule` VALUES (24, 11, '2025-06-10', '下午', 15, 0, 1, '2025-05-14 11:13:00', '2025-05-14 11:13:00');
INSERT INTO `schedule` VALUES (25, 12, '2025-06-10', '上午', 18, 1, 1, '2025-05-14 11:14:00', '2025-06-07 19:42:03');
INSERT INTO `schedule` VALUES (26, 12, '2025-06-10', '下午', 18, 0, 1, '2025-05-14 11:15:00', '2025-05-14 11:15:00');
INSERT INTO `schedule` VALUES (27, 13, '2025-06-10', '上午', 16, 0, 1, '2025-05-14 11:16:00', '2025-05-14 11:16:00');
INSERT INTO `schedule` VALUES (28, 13, '2025-06-10', '下午', 16, 0, 1, '2025-05-14 11:17:00', '2025-05-14 11:17:00');
INSERT INTO `schedule` VALUES (29, 14, '2025-06-10', '上午', 20, 0, 1, '2025-05-14 11:18:00', '2025-05-14 11:18:00');
INSERT INTO `schedule` VALUES (30, 14, '2025-06-10', '下午', 20, 0, 1, '2025-05-14 11:19:00', '2025-05-14 11:19:00');
INSERT INTO `schedule` VALUES (31, 15, '2025-06-11', '上午', 20, 0, 1, '2025-05-14 11:20:00', '2025-05-14 11:20:00');
INSERT INTO `schedule` VALUES (32, 15, '2025-06-11', '下午', 20, 0, 1, '2025-05-14 11:21:00', '2025-05-14 11:21:00');
INSERT INTO `schedule` VALUES (33, 16, '2025-06-11', '上午', 15, 0, 1, '2025-05-14 11:22:00', '2025-05-14 11:22:00');
INSERT INTO `schedule` VALUES (34, 16, '2025-06-11', '下午', 15, 0, 1, '2025-05-14 11:23:00', '2025-05-14 11:23:00');
INSERT INTO `schedule` VALUES (35, 17, '2025-06-11', '上午', 18, 0, 1, '2025-05-14 11:24:00', '2025-05-14 11:24:00');
INSERT INTO `schedule` VALUES (36, 17, '2025-06-11', '下午', 18, 0, 1, '2025-05-14 11:25:00', '2025-05-14 11:25:00');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码(加密存储)',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `avatar` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像地址',
  `sex` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '性别',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `role_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '角色代码',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 58 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'admin', '$2a$10$Cc6gX7Jel5UtKreBdrY8SeRiqwqEuccdySNafjQRMtz30KE92sPaS', '系统管理员', '/img/default_avatar.png', '男', '13800138000', 'admin@hospital.com', 'ADMIN', 1, '2025-05-14 10:00:00', '2025-05-14 10:00:00');
INSERT INTO `user` VALUES (2, 'doctor1', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '张医生', '/img/1749285629955.jpg', '男', '13811111111', 'doctor1@hospital.com', 'DOCTOR', 1, '2025-05-14 10:01:00', '2025-06-29 20:12:41');
INSERT INTO `user` VALUES (3, 'doctor2', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '李医生', '/img/1749285629955.jpg', '女', '13822222222', 'doctor2@hospital.com', 'DOCTOR', 1, '2025-05-14 10:02:00', '2025-06-29 20:12:44');
INSERT INTO `user` VALUES (4, 'nurse1', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '王护士', '/img/nurse1.jpg', '女', '13833333333', 'nurse1@hospital.com', 'NURSE', 1, '2025-05-14 10:03:00', '2025-05-14 10:03:00');
INSERT INTO `user` VALUES (5, 'patient1', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '赵患者', '/img/patient1.jpg', '男', '13844444444', 'patient1@example.com', 'PATIENT', 1, '2025-05-14 10:04:00', '2025-05-14 10:04:00');
INSERT INTO `user` VALUES (6, 'patient2', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '钱患者', '/img/1749294438197.jpg', '女', '13855555555', 'patient2@example.com', 'PATIENT', 1, '2025-05-14 10:05:00', '2025-06-07 19:07:18');
INSERT INTO `user` VALUES (7, 'doctor3', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '刘医生', '/img/1751198696770.jpeg', '男', '13866666666', 'doctor3@hospital.com', 'DOCTOR', 1, '2025-05-14 10:06:00', '2025-06-29 20:17:06');
INSERT INTO `user` VALUES (8, 'doctor4', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '孙医生', '/img/1751198696770.jpeg', '女', '13877777777', 'doctor4@hospital.com', 'DOCTOR', 1, '2025-05-14 10:07:00', '2025-06-29 20:17:21');
INSERT INTO `user` VALUES (9, 'doctor5', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '周医生', '/img/1751198696770.jpeg', '男', '13888888888', 'doctor5@hospital.com', 'DOCTOR', 1, '2025-05-14 10:08:00', '2025-06-29 20:17:21');
INSERT INTO `user` VALUES (10, 'doctor6', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '吴医生', '/img/1751198696770.jpeg', '女', '13899999999', 'doctor6@hospital.com', 'DOCTOR', 1, '2025-05-14 10:09:00', '2025-06-29 20:17:21');
INSERT INTO `user` VALUES (11, 'doctor7', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '郑医生', '/img/1751198696770.jpeg', '男', '13900000001', 'doctor7@hospital.com', 'DOCTOR', 1, '2025-05-14 10:10:00', '2025-06-29 20:17:21');
INSERT INTO `user` VALUES (12, 'doctor8', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '王医生', '/img/1751198696770.jpeg', '女', '13900000002', 'doctor8@hospital.com', 'DOCTOR', 1, '2025-05-14 10:11:00', '2025-06-29 20:17:21');
INSERT INTO `user` VALUES (13, 'doctor9', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '冯医生', '/img/1751198696770.jpeg', '男', '13900000003', 'doctor9@hospital.com', 'DOCTOR', 1, '2025-05-14 10:12:00', '2025-06-29 20:17:21');
INSERT INTO `user` VALUES (14, 'doctor10', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '陈医生', '/img/1751198696770.jpeg', '女', '13900000004', 'doctor10@hospital.com', 'DOCTOR', 1, '2025-05-14 10:13:00', '2025-06-29 20:17:21');
INSERT INTO `user` VALUES (15, 'doctor11', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '褚医生', '/img/1751198696770.jpeg', '男', '13900000005', 'doctor11@hospital.com', 'DOCTOR', 1, '2025-05-14 10:14:00', '2025-06-29 20:17:21');
INSERT INTO `user` VALUES (16, 'doctor12', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '魏医生', '/img/1751198696770.jpeg', '女', '13900000006', 'doctor12@hospital.com', 'DOCTOR', 1, '2025-05-14 10:15:00', '2025-06-29 20:17:21');
INSERT INTO `user` VALUES (17, 'doctor13', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '蒋医生', '/img/1751198696770.jpeg', '男', '13900000007', 'doctor13@hospital.com', 'DOCTOR', 1, '2025-05-14 10:16:00', '2025-06-29 20:17:21');
INSERT INTO `user` VALUES (18, 'doctor14', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '沈医生', '/img/1751198696770.jpeg', '女', '13900000008', 'doctor14@hospital.com', 'DOCTOR', 1, '2025-05-14 10:17:00', '2025-06-29 20:17:21');
INSERT INTO `user` VALUES (19, 'doctor15', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '韩医生', '/img/1751198696770.jpeg', '男', '13900000009', 'doctor15@hospital.com', 'DOCTOR', 1, '2025-05-14 10:18:00', '2025-06-29 20:17:21');
INSERT INTO `user` VALUES (20, 'doctor16', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '杨医生', '/img/1751198696770.jpeg', '女', '13900000010', 'doctor16@hospital.com', 'DOCTOR', 1, '2025-05-14 10:19:00', '2025-06-29 20:17:21');
INSERT INTO `user` VALUES (21, 'doctor17', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '朱医生', '/img/1751198696770.jpeg', '男', '13900000011', 'doctor17@hospital.com', 'DOCTOR', 1, '2025-05-14 10:20:00', '2025-06-29 20:17:26');
INSERT INTO `user` VALUES (22, 'doctor18', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '秦医生', '/img/1751198696770.jpeg', '女', '13900000012', 'doctor18@hospital.com', 'DOCTOR', 1, '2025-05-14 10:21:00', '2025-06-29 20:17:26');
INSERT INTO `user` VALUES (23, 'doctor19', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '尤医生', '/img/1751198696770.jpeg', '男', '13900000013', 'doctor19@hospital.com', 'DOCTOR', 1, '2025-05-14 10:22:00', '2025-06-29 20:17:26');
INSERT INTO `user` VALUES (24, 'doctor20', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '许医生', '/img/1751198696770.jpeg', '女', '13900000014', 'doctor20@hospital.com', 'DOCTOR', 1, '2025-05-14 10:23:00', '2025-06-29 20:17:26');
INSERT INTO `user` VALUES (25, 'doctor21', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '何医生', '/img/1751198696770.jpeg', '男', '13900000015', 'doctor21@hospital.com', 'DOCTOR', 1, '2025-05-14 10:24:00', '2025-06-29 20:17:26');
INSERT INTO `user` VALUES (26, 'doctor22', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '吕医生', '/img/1751198696770.jpeg', '女', '13900000016', 'doctor22@hospital.com', 'DOCTOR', 1, '2025-05-14 10:25:00', '2025-06-29 20:17:26');
INSERT INTO `user` VALUES (27, 'doctor23', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '施医生', '/img/1751198696770.jpeg', '男', '13900000017', 'doctor23@hospital.com', 'DOCTOR', 1, '2025-05-14 10:26:00', '2025-06-29 20:17:26');
INSERT INTO `user` VALUES (28, 'doctor24', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '张医生', '/img/1751198696770.jpeg', '女', '13900000018', 'doctor24@hospital.com', 'DOCTOR', 1, '2025-05-14 10:27:00', '2025-06-29 20:17:26');
INSERT INTO `user` VALUES (29, 'doctor25', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '孔医生', '/img/1751198696770.jpeg', '男', '13900000019', 'doctor25@hospital.com', 'DOCTOR', 1, '2025-05-14 10:28:00', '2025-06-29 20:17:26');
INSERT INTO `user` VALUES (30, 'doctor26', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '曹医生', '/img/1751198696770.jpeg', '女', '13900000020', 'doctor26@hospital.com', 'DOCTOR', 1, '2025-05-14 10:29:00', '2025-06-29 20:17:26');
INSERT INTO `user` VALUES (31, 'doctor27', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '严医生', '/img/1751198696770.jpeg', '男', '13900000021', 'doctor27@hospital.com', 'DOCTOR', 1, '2025-05-14 10:30:00', '2025-06-29 20:17:26');
INSERT INTO `user` VALUES (32, 'doctor28', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '华医生', '/img/1751198696770.jpeg', '女', '13900000022', 'doctor28@hospital.com', 'DOCTOR', 1, '2025-05-14 10:31:00', '2025-06-29 20:17:26');
INSERT INTO `user` VALUES (33, 'doctor29', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '金医生', '/img/1751198696770.jpeg', '男', '13900000023', 'doctor29@hospital.com', 'DOCTOR', 1, '2025-05-14 10:32:00', '2025-06-29 20:17:26');
INSERT INTO `user` VALUES (34, 'doctor30', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '魏医生', '/img/1751198696770.jpeg', '女', '13900000024', 'doctor30@hospital.com', 'DOCTOR', 1, '2025-05-14 10:33:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (35, 'doctor31', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '陶医生', '/img/1751198696770.jpeg', '男', '13900000025', 'doctor31@hospital.com', 'DOCTOR', 1, '2025-05-14 10:34:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (36, 'doctor32', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '姜医生', '/img/1751198696770.jpeg', '女', '13900000026', 'doctor32@hospital.com', 'DOCTOR', 1, '2025-05-14 10:35:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (37, 'doctor33', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '戚医生', '/img/1751198696770.jpeg', '男', '13900000027', 'doctor33@hospital.com', 'DOCTOR', 1, '2025-05-14 10:36:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (38, 'doctor34', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '谢医生', '/img/1751198696770.jpeg', '女', '13900000028', 'doctor34@hospital.com', 'DOCTOR', 1, '2025-05-14 10:37:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (39, 'doctor35', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '邹医生', '/img/1751198696770.jpeg', '男', '13900000029', 'doctor35@hospital.com', 'DOCTOR', 1, '2025-05-14 10:38:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (40, 'doctor36', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '喻医生', '/img/1751198696770.jpeg', '女', '13900000030', 'doctor36@hospital.com', 'DOCTOR', 1, '2025-05-14 10:39:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (41, 'doctor37', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '柏医生', '/img/1751198696770.jpeg', '男', '13900000031', 'doctor37@hospital.com', 'DOCTOR', 1, '2025-05-14 10:40:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (42, 'doctor38', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '水医生', '/img/1751198696770.jpeg', '女', '13900000032', 'doctor38@hospital.com', 'DOCTOR', 1, '2025-05-14 10:41:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (43, 'doctor39', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '窦医生', '/img/1751198696770.jpeg', '男', '13900000033', 'doctor39@hospital.com', 'DOCTOR', 1, '2025-05-14 10:42:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (44, 'doctor40', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '章医生', '/img/1751198696770.jpeg', '女', '13900000034', 'doctor40@hospital.com', 'DOCTOR', 1, '2025-05-14 10:43:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (45, 'doctor41', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '云医生', '/img/1751198696770.jpeg', '男', '13900000035', 'doctor41@hospital.com', 'DOCTOR', 1, '2025-05-14 10:44:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (46, 'doctor42', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '苏医生', '/img/1751198696770.jpeg', '女', '13900000036', 'doctor42@hospital.com', 'DOCTOR', 1, '2025-05-14 10:45:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (47, 'doctor43', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '潘医生', '/img/1751198696770.jpeg', '男', '13900000037', 'doctor43@hospital.com', 'DOCTOR', 1, '2025-05-14 10:46:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (48, 'doctor44', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '葛医生', '/img/1751198696770.jpeg', '女', '13900000038', 'doctor44@hospital.com', 'DOCTOR', 1, '2025-05-14 10:47:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (49, 'doctor45', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '奚医生', '/img/1751198696770.jpeg', '男', '13900000039', 'doctor45@hospital.com', 'DOCTOR', 1, '2025-05-14 10:48:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (50, 'doctor46', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '范医生', '/img/1751198696770.jpeg', '女', '13900000040', 'doctor46@hospital.com', 'DOCTOR', 1, '2025-05-14 10:49:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (51, 'doctor47', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '彭医生', '/img/1751198696770.jpeg', '男', '13900000041', 'doctor47@hospital.com', 'DOCTOR', 1, '2025-05-14 10:50:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (52, 'doctor48', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '郎医生', '/img/1751198696770.jpeg', '女', '13900000042', 'doctor48@hospital.com', 'DOCTOR', 1, '2025-05-14 10:51:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (53, 'doctor49', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '鲁医生', '/img/1751198696770.jpeg', '男', '13900000043', 'doctor49@hospital.com', 'DOCTOR', 1, '2025-05-14 10:52:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (54, 'doctor50', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '韦医生', '/img/1751198696770.jpeg', '女', '13900000044', 'doctor50@hospital.com', 'DOCTOR', 1, '2025-05-14 10:53:00', '2025-06-29 20:17:32');
INSERT INTO `user` VALUES (55, 'doctor51', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '昌医生', '/img/1751198696770.jpeg', '男', '13900000045', 'doctor51@hospital.com', 'DOCTOR', 1, '2025-05-14 10:54:00', '2025-06-29 20:17:40');
INSERT INTO `user` VALUES (56, 'doctor52', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', '马医生', '/img/1751198696770.jpeg', '女', '13900000046', 'doctor52@hospital.com', 'DOCTOR', 1, '2025-05-14 10:55:00', '2025-06-29 20:17:40');
INSERT INTO `user` VALUES (57, 'test', '$2a$10$Qkntw.nhOA2DMPujX8SF0.DcnCr7V7ijtQEXKzmOvifhlyUevyMkm', 'test', '/img/1751198696770.jpeg', '女', '13456789987', '11111@qq.com', 'PATIENT', 1, '2025-06-07 15:26:46', '2025-06-29 20:17:40');
INSERT INTO `user` VALUES (58, 'pharmacy_admin', '$2a$10$krj3WlCs/S0WGfv3E8Q2meqfC0sS8p.B7oF7E3A7ZDXMbjH9z2biS', '药房管理员', '/img/default_avatar.png', '男', '13800009999', 'pharmacy_admin@demo.com', 'PHARMACY_MANAGER', 1, '2026-04-07 14:45:58', '2026-04-07 14:45:58');

-- ----------------------------
-- Table structure for supplier
-- ----------------------------
DROP TABLE IF EXISTS `supplier`;
CREATE TABLE `supplier`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '供应商ID',
  `supplier_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '供应商编码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '供应商名称',
  `contact_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态(0:停用,1:启用)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_supplier_code`(`supplier_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '供应商表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of supplier
-- ----------------------------
INSERT INTO `supplier` VALUES (1, 'SUP-001', '华东医药供应链有限公司', '张磊', '13800000001', '上海市浦东新区张江路88号', 'hd-med-scm@demo.com', 1, '2026-04-07 14:50:00', '2026-04-07 14:50:00');
INSERT INTO `supplier` VALUES (2, 'SUP-002', '国药器械（华北）配送中心', '李娜', '13800000002', '北京市大兴区医药园区66号', 'gyqx-north@demo.com', 1, '2026-04-07 14:50:00', '2026-04-07 14:50:00');
INSERT INTO `supplier` VALUES (3, 'SUP-003', '康源生物制药原料供应商', '王强', '13800000003', '广州市黄埔区科学城12号', 'ky-bio@demo.com', 1, '2026-04-07 14:50:00', '2026-04-07 14:50:00');
INSERT INTO `supplier` VALUES (4, 'SUP-004', '安泰耗材与试剂供应', '赵敏', '13800000004', '成都市高新区天府大道199号', 'at-consumables@demo.com', 1, '2026-04-07 14:50:00', '2026-04-07 14:50:00');
INSERT INTO `supplier` VALUES (5, 'SUP-005', '联康冷链药品物流', '陈杰', '13800000005', '武汉市东西湖区物流大道9号', 'lk-coldchain@demo.com', 1, '2026-04-07 14:50:00', '2026-04-07 14:50:00');

-- medicine 改造：制造商保留兼容，新增 supplier_id 作为采购主关联
ALTER TABLE `medicine` ADD COLUMN `supplier_id` bigint(20) NULL DEFAULT NULL COMMENT '供应商ID' AFTER `manufacturer`;
ALTER TABLE `medicine` ADD INDEX `idx_medicine_supplier_id`(`supplier_id` ASC);
-- 增加供应商数据：把药品制造商补充为可选供应商（去重）
INSERT INTO `supplier` (`supplier_code`, `name`, `contact_name`, `contact_phone`, `address`, `email`, `status`, `create_time`, `update_time`)
SELECT
  CONCAT('SUP-AUTO-', LPAD(ROW_NUMBER() OVER (ORDER BY t.manufacturer), 3, '0')),
  t.manufacturer,
  '系统初始化',
  '13800000000',
  '待维护',
  NULL,
  1,
  '2026-04-07 15:00:00',
  '2026-04-07 15:00:00'
FROM (
  SELECT DISTINCT `manufacturer`
  FROM `medicine`
  WHERE `manufacturer` IS NOT NULL AND `manufacturer` <> ''
) t
LEFT JOIN `supplier` s ON s.`name` = t.`manufacturer`
WHERE s.`id` IS NULL;

-- 回填所有药品供应商，不允许 NULL
UPDATE `medicine` m
JOIN `supplier` s ON s.`name` = m.`manufacturer`
SET m.`supplier_id` = s.`id`
WHERE m.`supplier_id` IS NULL;

-- 兜底：如果还有空值，统一归属到供应商ID=1
UPDATE `medicine` SET `supplier_id` = 1 WHERE `supplier_id` IS NULL;

-- 强制非空 + 外键约束
ALTER TABLE `medicine` MODIFY COLUMN `supplier_id` bigint(20) NOT NULL COMMENT '供应商ID';
ALTER TABLE `medicine` ADD CONSTRAINT `fk_medicine_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

-- ----------------------------
-- Table structure for supplier_medicine
-- ----------------------------
DROP TABLE IF EXISTS `supplier_medicine`;
CREATE TABLE `supplier_medicine`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '关系ID',
  `supplier_id` bigint(20) NOT NULL COMMENT '供应商ID',
  `medicine_id` bigint(20) NOT NULL COMMENT '药品ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_supplier_medicine`(`supplier_id` ASC, `medicine_id` ASC) USING BTREE,
  INDEX `idx_sm_supplier_id`(`supplier_id` ASC) USING BTREE,
  INDEX `idx_sm_medicine_id`(`medicine_id` ASC) USING BTREE,
  CONSTRAINT `fk_sm_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_sm_medicine` FOREIGN KEY (`medicine_id`) REFERENCES `medicine` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '供应商与药品对应关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of supplier_medicine
-- ----------------------------
-- 关键限制：M001(阿司匹林, medicine_id=1) 仅由国药(供应商id=2)供应
INSERT INTO `supplier_medicine` VALUES (1, 2, 1, '2026-04-08 10:00:00');
INSERT INTO `supplier_medicine` VALUES (2, 2, 2, '2026-04-08 10:00:00');
INSERT INTO `supplier_medicine` VALUES (3, 2, 3, '2026-04-08 10:00:00');
INSERT INTO `supplier_medicine` VALUES (4, 1, 4, '2026-04-08 10:00:00');
INSERT INTO `supplier_medicine` VALUES (5, 1, 5, '2026-04-08 10:00:00');
INSERT INTO `supplier_medicine` VALUES (6, 1, 6, '2026-04-08 10:00:00');
INSERT INTO `supplier_medicine` VALUES (7, 3, 7, '2026-04-08 10:00:00');
INSERT INTO `supplier_medicine` VALUES (8, 3, 8, '2026-04-08 10:00:00');
INSERT INTO `supplier_medicine` VALUES (9, 3, 9, '2026-04-08 10:00:00');
INSERT INTO `supplier_medicine` VALUES (10, 4, 10, '2026-04-08 10:00:00');
INSERT INTO `supplier_medicine` VALUES (11, 4, 11, '2026-04-08 10:00:00');
INSERT INTO `supplier_medicine` VALUES (12, 4, 12, '2026-04-08 10:00:00');
INSERT INTO `supplier_medicine` VALUES (13, 5, 13, '2026-04-08 10:00:00');
INSERT INTO `supplier_medicine` VALUES (14, 5, 14, '2026-04-08 10:00:00');
INSERT INTO `supplier_medicine` VALUES (15, 5, 15, '2026-04-08 10:00:00');

-- ----------------------------
-- Table structure for purchase_plan
-- ----------------------------
DROP TABLE IF EXISTS `purchase_plan`;
CREATE TABLE `purchase_plan`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '采购计划ID',
  `plan_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '计划编号',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '计划主题',
  `creator_user_id` bigint(20) NULL DEFAULT NULL COMMENT '创建人用户ID',
  `status` tinyint(4) NULL DEFAULT 0 COMMENT '状态(0:草稿,1:已提交,2:已完结)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_plan_no`(`plan_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '采购计划主表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for purchase_plan_item
-- ----------------------------
DROP TABLE IF EXISTS `purchase_plan_item`;
CREATE TABLE `purchase_plan_item`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '计划明细ID',
  `plan_id` bigint(20) NOT NULL COMMENT '采购计划ID',
  `medicine_id` bigint(20) NOT NULL COMMENT '药品ID',
  `plan_qty` int(11) NOT NULL COMMENT '计划数量',
  `purchased_qty` int(11) NULL DEFAULT 0 COMMENT '已下单数量(累计)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_plan_id`(`plan_id` ASC) USING BTREE,
  INDEX `idx_ppi_medicine_id`(`medicine_id` ASC) USING BTREE,
  CONSTRAINT `fk_ppi_plan` FOREIGN KEY (`plan_id`) REFERENCES `purchase_plan` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_ppi_medicine` FOREIGN KEY (`medicine_id`) REFERENCES `medicine` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '采购计划明细表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for purchase_order
-- ----------------------------
DROP TABLE IF EXISTS `purchase_order`;
CREATE TABLE `purchase_order`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '采购单ID',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '采购单号',
  `plan_id` bigint(20) NOT NULL COMMENT '来源采购计划ID',
  `supplier_id` bigint(20) NOT NULL COMMENT '供应商ID',
  `creator_user_id` bigint(20) NULL DEFAULT NULL COMMENT '创建人用户ID',
  `status` tinyint(4) NULL DEFAULT 0 COMMENT '状态(0:草稿,1:已发送,2:已验收完成)',
  `total_amount` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '总金额',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_po_plan_id`(`plan_id` ASC) USING BTREE,
  INDEX `idx_po_supplier_id`(`supplier_id` ASC) USING BTREE,
  CONSTRAINT `fk_po_plan` FOREIGN KEY (`plan_id`) REFERENCES `purchase_plan` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_po_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '采购单主表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for purchase_order_item
-- ----------------------------
DROP TABLE IF EXISTS `purchase_order_item`;
CREATE TABLE `purchase_order_item`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '采购单明细ID',
  `order_id` bigint(20) NOT NULL COMMENT '采购单ID',
  `plan_item_id` bigint(20) NOT NULL COMMENT '来源计划明细ID',
  `medicine_id` bigint(20) NOT NULL COMMENT '药品ID',
  `order_qty` int(11) NOT NULL COMMENT '下单数量',
  `unit_price` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '单价',
  `amount` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '金额',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_poi_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_poi_plan_item_id`(`plan_item_id` ASC) USING BTREE,
  INDEX `idx_poi_medicine_id`(`medicine_id` ASC) USING BTREE,
  CONSTRAINT `fk_poi_order` FOREIGN KEY (`order_id`) REFERENCES `purchase_order` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_poi_plan_item` FOREIGN KEY (`plan_item_id`) REFERENCES `purchase_plan_item` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_poi_medicine` FOREIGN KEY (`medicine_id`) REFERENCES `medicine` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '采购单明细表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for purchase_acceptance
-- ----------------------------
DROP TABLE IF EXISTS `purchase_acceptance`;
CREATE TABLE `purchase_acceptance`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '验收单ID',
  `acceptance_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '验收单号',
  `purchase_order_id` bigint(20) NOT NULL COMMENT '来源采购单ID',
  `inspector_user_id` bigint(20) NULL DEFAULT NULL COMMENT '验收人用户ID',
  `acceptance_time` datetime NULL DEFAULT NULL COMMENT '验收时间',
  `status` tinyint(4) NULL DEFAULT 0 COMMENT '状态(0:草稿,1:已完成)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_acceptance_no`(`acceptance_no` ASC) USING BTREE,
  INDEX `idx_pa_order_id`(`purchase_order_id` ASC) USING BTREE,
  CONSTRAINT `fk_pa_order` FOREIGN KEY (`purchase_order_id`) REFERENCES `purchase_order` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '采购验收单主表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for purchase_acceptance_item
-- ----------------------------
DROP TABLE IF EXISTS `purchase_acceptance_item`;
CREATE TABLE `purchase_acceptance_item`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '验收明细ID',
  `acceptance_id` bigint(20) NOT NULL COMMENT '验收单ID',
  `purchase_order_item_id` bigint(20) NOT NULL COMMENT '来源采购单明细ID',
  `medicine_id` bigint(20) NOT NULL COMMENT '药品ID',
  `ordered_qty` int(11) NOT NULL COMMENT '下单数量',
  `received_qty` int(11) NOT NULL COMMENT '到货数量',
  `qualified_qty` int(11) NOT NULL COMMENT '合格数量',
  `batch_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '批号',
  `production_date` date NULL DEFAULT NULL COMMENT '生产日期',
  `expiry_date` date NULL DEFAULT NULL COMMENT '有效期',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pai_acceptance_id`(`acceptance_id` ASC) USING BTREE,
  INDEX `idx_pai_order_item_id`(`purchase_order_item_id` ASC) USING BTREE,
  INDEX `idx_pai_medicine_id`(`medicine_id` ASC) USING BTREE,
  CONSTRAINT `fk_pai_acceptance` FOREIGN KEY (`acceptance_id`) REFERENCES `purchase_acceptance` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_pai_order_item` FOREIGN KEY (`purchase_order_item_id`) REFERENCES `purchase_order_item` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_pai_medicine` FOREIGN KEY (`medicine_id`) REFERENCES `medicine` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '采购验收单明细表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for stock_in_order
-- ----------------------------
DROP TABLE IF EXISTS `stock_in_order`;
CREATE TABLE `stock_in_order`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '入库单ID',
  `stock_in_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '入库单号',
  `acceptance_id` bigint(20) NOT NULL COMMENT '来源验收单ID',
  `operator_user_id` bigint(20) NULL DEFAULT NULL COMMENT '入库操作人用户ID',
  `stock_in_time` datetime NULL DEFAULT NULL COMMENT '入库时间',
  `status` tinyint(4) NULL DEFAULT 0 COMMENT '状态(0:草稿,1:已过账)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_stock_in_no`(`stock_in_no` ASC) USING BTREE,
  INDEX `idx_sio_acceptance_id`(`acceptance_id` ASC) USING BTREE,
  CONSTRAINT `fk_sio_acceptance` FOREIGN KEY (`acceptance_id`) REFERENCES `purchase_acceptance` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '入库单主表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for stock_in_order_item
-- ----------------------------
DROP TABLE IF EXISTS `stock_in_order_item`;
CREATE TABLE `stock_in_order_item`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '入库明细ID',
  `stock_in_id` bigint(20) NOT NULL COMMENT '入库单ID',
  `acceptance_item_id` bigint(20) NOT NULL COMMENT '来源验收明细ID',
  `medicine_id` bigint(20) NOT NULL COMMENT '药品ID',
  `stock_in_qty` int(11) NOT NULL COMMENT '入库数量',
  `unit_cost` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '单位成本',
  `amount` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '金额',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sioi_stock_in_id`(`stock_in_id` ASC) USING BTREE,
  INDEX `idx_sioi_acceptance_item_id`(`acceptance_item_id` ASC) USING BTREE,
  INDEX `idx_sioi_medicine_id`(`medicine_id` ASC) USING BTREE,
  CONSTRAINT `fk_sioi_stock_in` FOREIGN KEY (`stock_in_id`) REFERENCES `stock_in_order` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_sioi_acceptance_item` FOREIGN KEY (`acceptance_item_id`) REFERENCES `purchase_acceptance_item` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_sioi_medicine` FOREIGN KEY (`medicine_id`) REFERENCES `medicine` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '入库单明细表' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
