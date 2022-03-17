CREATE TABLE `bpm_model` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `bid` VARCHAR(32) NOT NULL COMMENT '业务主键',
  `name` VARCHAR(256) NOT NULL COMMENT '名称',
  `process_category` VARCHAR(8) NOT NULL COMMENT '流程类别',
  `process_definition_key` VARCHAR(256) NOT NULL COMMENT '流程定义KEY',
  `process_definition_id` VARCHAR(64) NULL COMMENT '流程定义ID',
  `process_deploy_id` VARCHAR(64) NULL COMMENT '流程发布ID',
  `bpm_file` LONGBLOB NULL COMMENT 'bpm文件',
  `ext` VARCHAR(4000) NULL COMMENT '扩展信息',
  `tenant_id` VARCHAR(256) NULL COMMENT '租户ID',
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `bid_UNIQUE` (`bid` ASC)
)COMMENT = '流程模型';

CREATE TABLE `bpm_form_field` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `model_id` VARCHAR(64) NOT NULL COMMENT '流程模型ID',
  `process_definition_id` VARCHAR(64) NOT NULL COMMENT '流程定义ID',
  `task_definition_key` VARCHAR(64) NOT NULL COMMENT '任务定义KEY',
  `filed_key` VARCHAR(64) NOT NULL COMMENT '字段key',
  `filed_name` VARCHAR(256) NOT NULL COMMENT '字段名',
  `field_status` INT NULL COMMENT '字段状态。1-可读  2-可编辑 3-隐藏',
  `tenant_id` VARCHAR(45) NOT NULL COMMENT '租户ID',
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`)
)COMMENT = '流程节点表单字段配置'