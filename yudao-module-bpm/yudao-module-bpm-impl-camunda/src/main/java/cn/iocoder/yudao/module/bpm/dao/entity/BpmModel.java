package cn.iocoder.yudao.module.bpm.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Blob;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * <p>
 * 流程模型
 * </p>
 *
 * @author songjin
 * @since 2022-03-18
 */
@Getter
@Setter
@TableName("bpm_model")
@ApiModel(value = "BpmModel对象", description = "流程模型")
public class BpmModel extends Model<BpmModel> {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("业务主键")
    @TableField("bid")
    private String bid;

    @ApiModelProperty("名称")
    @TableField("name")
    private String name;

    @ApiModelProperty("流程类别")
    @TableField("process_category")
    private String processCategory;

    @ApiModelProperty("流程定义KEY")
    @TableField("process_definition_key")
    private String processDefinitionKey;

    @ApiModelProperty("流程定义ID")
    @TableField("process_definition_id")
    private String processDefinitionId;

    @ApiModelProperty("流程发布ID")
    @TableField("process_deploy_id")
    private String processDeployId;

    @ApiModelProperty("bpm文件")
    @TableField("bpm_file")
    private String bpmFile;

    @ApiModelProperty("扩展信息")
    @TableField("ext")
    private String ext;

    @ApiModelProperty("租户ID")
    @TableField("tenant_id")
    private String tenantId;

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private Date createTime;

    @ApiModelProperty("更新时间")
    @TableField("update_time")
    private Date updateTime;


    public static final String ID = "id";

    public static final String BID = "bid";

    public static final String NAME = "name";

    public static final String PROCESS_CATEGORY = "process_category";

    public static final String PROCESS_DEFINITION_KEY = "process_definition_key";

    public static final String PROCESS_DEFINITION_ID = "process_definition_id";

    public static final String PROCESS_DEPLOY_ID = "process_deploy_id";

    public static final String BPM_FILE = "bpm_file";

    public static final String EXT = "ext";

    public static final String TENANT_ID = "tenant_id";

    public static final String CREATE_TIME = "create_time";

    public static final String UPDATE_TIME = "update_time";

    @Override
    public Serializable pkVal() {
        return this.id;
    }

}
