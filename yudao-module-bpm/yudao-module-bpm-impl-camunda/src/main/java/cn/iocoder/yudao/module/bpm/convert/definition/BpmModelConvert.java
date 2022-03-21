package cn.iocoder.yudao.module.bpm.convert.definition;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.model.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmFormDO;
import cn.iocoder.yudao.module.bpm.dao.entity.BpmModel;
import cn.iocoder.yudao.module.bpm.service.definition.dto.BpmModelMetaInfoRespDTO;
import cn.iocoder.yudao.module.bpm.service.definition.dto.BpmProcessDefinitionCreateReqDTO;
import org.camunda.bpm.engine.impl.persistence.entity.SuspensionState;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.sql.Date;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 流程模型 Convert
 *
 * @author yunlongn
 */
@Mapper
public interface BpmModelConvert {

    BpmModelConvert INSTANCE = Mappers.getMapper(BpmModelConvert.class);

    default List<BpmModelPageItemRespVO> convertList(List<BpmModel> list, Map<Long, BpmFormDO> formMap,
                                                     Map<String, Deployment> deploymentMap,
                                                     Map<String, ProcessDefinition> processDefinitionMap) {
        return CollectionUtils.convertList(list, model -> {
            BpmModelMetaInfoRespDTO metaInfo = JsonUtils.parseObject(model.getExt(), BpmModelMetaInfoRespDTO.class);
            BpmFormDO form = metaInfo != null ? formMap.get(metaInfo.getFormId()) : null;
            Deployment deployment = model.getProcessDeployId() != null ? deploymentMap.get(model.getProcessDeployId()) : null;
            ProcessDefinition processDefinition = model.getProcessDefinitionId() != null ? processDefinitionMap.get(model.getProcessDefinitionId()) : null;
            return convert(model, form, deployment, processDefinition);
        });
    }

    default BpmModelPageItemRespVO convert(BpmModel model, BpmFormDO form, Deployment deployment, ProcessDefinition processDefinition) {
        BpmModelPageItemRespVO modelRespVO = new BpmModelPageItemRespVO();
        modelRespVO.setId(model.getBid());
        modelRespVO.setCreateTime(model.getCreateTime());
        // 通用 copy
        copyTo(model, modelRespVO);
        // Form
        if (form != null) {
            modelRespVO.setFormId(form.getId());
            modelRespVO.setFormName(form.getName());
        }
        // ProcessDefinition
        modelRespVO.setProcessDefinition(this.convert(processDefinition));
        if (modelRespVO.getProcessDefinition() != null) {
            modelRespVO.getProcessDefinition().setSuspensionState(processDefinition.isSuspended() ?
                    SuspensionState.SUSPENDED.getStateCode() : SuspensionState.ACTIVE.getStateCode());
            modelRespVO.getProcessDefinition().setDeploymentTime(deployment.getDeploymentTime());
        }
        return modelRespVO;
    }

    default BpmModelRespVO convert(BpmModel model) {
        BpmModelRespVO modelRespVO = new BpmModelRespVO();
        modelRespVO.setId(model.getBid());
        modelRespVO.setCreateTime(model.getCreateTime());
        // 通用 copy
        copyTo(model, modelRespVO);
        return modelRespVO;
    }

    default void copyTo(BpmModel model, BpmModelBaseVO to) {
        to.setName(model.getName());
        to.setKey(model.getProcessDefinitionKey());
        to.setCategory(model.getProcessCategory());
        // metaInfo
        BpmModelMetaInfoRespDTO metaInfo = JsonUtils.parseObject(model.getExt(), BpmModelMetaInfoRespDTO.class);
        copyTo(metaInfo, to);
    }

    BpmModelCreateReqVO convert(BpmModeImportReqVO bean);

    default BpmProcessDefinitionCreateReqDTO convert2(BpmModel model, BpmFormDO form) {
        BpmProcessDefinitionCreateReqDTO createReqDTO = new BpmProcessDefinitionCreateReqDTO();
        createReqDTO.setModelId(model.getBid());
        createReqDTO.setName(model.getName());
        createReqDTO.setKey(model.getProcessDefinitionKey());
        createReqDTO.setCategory(model.getProcessCategory());
        BpmModelMetaInfoRespDTO metaInfo = JsonUtils.parseObject(model.getExt(), BpmModelMetaInfoRespDTO.class);
        // metaInfo
        copyTo(metaInfo, createReqDTO);
        // form
        if (form != null) {
            createReqDTO.setFormConf(form.getConf());
            createReqDTO.setFormFields(form.getFields());
        }
        return createReqDTO;
    }

    void copyTo(BpmModelMetaInfoRespDTO from, @MappingTarget BpmProcessDefinitionCreateReqDTO to);

    void copyTo(BpmModelMetaInfoRespDTO from, @MappingTarget BpmModelBaseVO to);

    BpmModelPageItemRespVO.ProcessDefinition convert(ProcessDefinition bean);

    default void copy(BpmModel model, BpmModelCreateReqVO bean) {
        model.setName(bean.getName());
        model.setProcessDefinitionKey(bean.getKey());
        model.setExt(buildMetaInfoStr(null, bean.getDescription(), null, null,
                null, null));
    }

    default void copy(BpmModel model, BpmModelUpdateReqVO bean) {
        model.setName(bean.getName());
        model.setProcessCategory(bean.getCategory());
        model.setExt(buildMetaInfoStr(JsonUtils.parseObject(model.getExt(), BpmModelMetaInfoRespDTO.class),
                bean.getDescription(), bean.getFormType(), bean.getFormId(),
                bean.getFormCustomCreatePath(), bean.getFormCustomViewPath()));
        model.setBpmFile(bean.getBpmnXml());
    }

    default String buildMetaInfoStr(BpmModelMetaInfoRespDTO metaInfo, String description, Integer formType,
                                    Long formId, String formCustomCreatePath, String formCustomViewPath) {
        if (metaInfo == null) {
            metaInfo = new BpmModelMetaInfoRespDTO();
        }
        // 只有非空，才进行设置，避免更新时的覆盖
        if (StrUtil.isNotEmpty(description)) {
            metaInfo.setDescription(description);
        }
        if (Objects.nonNull(formType)) {
            metaInfo.setFormType(formType);
            metaInfo.setFormId(formId);
            metaInfo.setFormCustomCreatePath(formCustomCreatePath);
            metaInfo.setFormCustomViewPath(formCustomViewPath);
        }
        return JsonUtils.toJsonString(metaInfo);
    }
}
