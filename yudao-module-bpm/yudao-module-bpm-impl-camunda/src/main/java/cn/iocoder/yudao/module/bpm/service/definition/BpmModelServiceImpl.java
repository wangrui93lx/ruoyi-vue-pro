package cn.iocoder.yudao.module.bpm.service.definition;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.validation.ValidationUtils;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.model.*;
import cn.iocoder.yudao.module.bpm.convert.definition.BpmModelConvert;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmFormDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmModelDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.definition.BpmModelMapper;
import cn.iocoder.yudao.module.bpm.enums.definition.BpmModelFormTypeEnum;
import cn.iocoder.yudao.module.bpm.service.definition.dto.BpmModelMetaInfoRespDTO;
import cn.iocoder.yudao.module.bpm.service.definition.dto.BpmProcessDefinitionCreateReqDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.RandomStringUtils;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.impl.persistence.entity.SuspensionState;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class BpmModelServiceImpl implements BpmModelService {

    @Resource
    private BpmModelMapper bpmModelMapper;
    @Resource
    private RepositoryService repositoryService;
    @Resource
    private BpmProcessDefinitionService processDefinitionService;
    @Resource
    private BpmFormService bpmFormService;
    @Resource
    private BpmTaskAssignRuleService taskAssignRuleService;

    @Override
    public PageResult<BpmModelPageItemRespVO> getModelPage(BpmModelPageReqVO pageVO) {

        LambdaQueryWrapper<BpmModelDO> queryWrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(pageVO.getKey())) {
            queryWrapper.eq(BpmModelDO::getProcessDefinitionKey, pageVO.getKey());
        }
        if (StrUtil.isNotBlank(pageVO.getName())) {
            queryWrapper.eq(BpmModelDO::getName, "%" + pageVO.getName() + "%");
        }
        if (StrUtil.isNotBlank(pageVO.getCategory())) {
            queryWrapper.eq(BpmModelDO::getProcessCategory, pageVO.getCategory());
        }
        queryWrapper.orderByDesc(BpmModelDO::getId);
        // 执行查询
        Page<BpmModelDO> page = bpmModelMapper.selectPage(new Page<>(pageVO.getPageNo(), pageVO.getPageSize()), queryWrapper);
        List<BpmModelDO> models = page.getRecords();

        // 获得 Form Map
        Set<Long> formIds = CollectionUtils.convertSet(models, model -> {
            BpmModelMetaInfoRespDTO metaInfo = JsonUtils.parseObject(model.getExt(), BpmModelMetaInfoRespDTO.class);
            return metaInfo != null ? metaInfo.getFormId() : null;
        });
        Map<Long, BpmFormDO> formMap = bpmFormService.getFormMap(formIds);

        // 获得 Deployment Map
        Set<String> deploymentIds = new HashSet<>();
        models.forEach(model -> CollectionUtils.addIfNotNull(deploymentIds, model.getProcessDeployId()));
        Map<String, Deployment> deploymentMap = processDefinitionService.getDeploymentMap(deploymentIds);
        // 获得 ProcessDefinition Map
        Set<String> processDefinitionIds = new HashSet<>();
        models.forEach(model -> CollectionUtils.addIfNotNull(processDefinitionIds, model.getProcessDefinitionId()));
        Map<String, ProcessDefinition> processDefinitionMap = processDefinitionService.getProcessDefinitionMap(processDefinitionIds);
        // 拼接结果
        long modelCount = page.getTotal();
        return new PageResult<>(BpmModelConvert.INSTANCE.convertList(models, formMap, deploymentMap, processDefinitionMap), modelCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createModel(@Valid BpmModelCreateReqVO createReqVO) {
        checkKeyNCName(createReqVO.getKey());
        // 校验流程标识已经存在
        BpmModelDO keyModel = this.getModelByKey(createReqVO.getKey());
        if (keyModel != null) {
            throw exception(MODEL_KEY_EXISTS, createReqVO.getKey());
        }

        // 创建流程定义
        BpmModelDO model = new BpmModelDO();
        BpmModelConvert.INSTANCE.copy(model, createReqVO);
        model.setBid(RandomStringUtils.randomAlphanumeric(32));

        bpmModelMapper.insert(model);
        return model.getBid();
    }

    private BpmModelDO getModelByKey(String key) {
        LambdaQueryWrapper<BpmModelDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BpmModelDO::getProcessDefinitionKey, key);
        return bpmModelMapper.selectOne(queryWrapper);
    }

    private BpmModelDO getModelByBid(String bid) {
        LambdaQueryWrapper<BpmModelDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BpmModelDO::getBid, bid);
        return bpmModelMapper.selectOne(queryWrapper);
    }

    @Override
    public BpmModelRespVO getModel(String id) {
        LambdaQueryWrapper<BpmModelDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BpmModelDO::getBid, id);
        BpmModelDO model = bpmModelMapper.selectOne(queryWrapper);
        if (model == null) {
            return null;
        }
        return BpmModelConvert.INSTANCE.convert(model);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 因为进行多个操作，所以开启事务
    public void updateModel(@Valid BpmModelUpdateReqVO updateReqVO) {
        // 校验流程模型存在
        BpmModelDO model = getModelByBid(updateReqVO.getId());
        if (model == null) {
            throw exception(MODEL_NOT_EXISTS);
        }

        // 修改流程定义
        BpmModelConvert.INSTANCE.copy(model, updateReqVO);
        // 更新模型
        bpmModelMapper.updateById(model);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 因为进行多个操作，所以开启事务
    public void deployModel(String id) {
        // 校验流程模型存在
        BpmModelDO model = getModelByBid(id);
        if (model == null) {
            throw exception(MODEL_NOT_EXISTS);
        }
        // 校验流程图
        String bpmnStr = model.getBpmFile();
        if (bpmnStr == null) {
            throw exception(MODEL_NOT_EXISTS);
        }
        // TODO 芋艿：校验流程图的有效性；例如说，是否有开始的元素，是否有结束的元素；
        // 校验表单已配
        BpmFormDO form = checkFormConfig(model.getExt());
        //校验任务分配规则已配置
        taskAssignRuleService.checkTaskAssignRuleAllConfig(id);

        BpmProcessDefinitionCreateReqDTO definitionCreateReqDTO = BpmModelConvert.INSTANCE.convert2(model, form).setBpmnBytes(bpmnStr.getBytes(StandardCharsets.UTF_8));
        //校验模型是否发生修改。如果未修改，则不允许创建
        if (processDefinitionService.isProcessDefinitionEquals(definitionCreateReqDTO)) { // 流程定义的信息相等
            ProcessDefinition oldProcessInstance = processDefinitionService.getProcessDefinitionByProcessDefinitionId(model.getProcessDefinitionId());
            if (oldProcessInstance != null && taskAssignRuleService.isTaskAssignRulesEquals(model.getBid(), oldProcessInstance.getId())) {
                throw exception(MODEL_DEPLOY_FAIL_TASK_INFO_EQUALS);
            }
        }
        // 创建流程定义
        String definitionId = processDefinitionService.createProcessDefinition(definitionCreateReqDTO);

        // 更新 model 的 deploymentId，进行关联
        // 如果流程定义没有更新，则跳过重新关联
        if (!definitionId.equals(model.getProcessDefinitionId())) {
            // 将老的流程定义进行挂起。也就是说，只有最新部署的流程定义，才可以发起任务。
            updateProcessDefinitionSuspended(model.getProcessDefinitionId());

            ProcessDefinition definition = processDefinitionService.getProcessDefinition(definitionId);
            model.setProcessDeployId(definition.getDeploymentId());
            model.setProcessDefinitionId(definitionId);
            bpmModelMapper.updateById(model);
        }

        //删除任务分配规则
        taskAssignRuleService.deleteTaskAssignRules(id, definitionId);
        //复制任务分配规则
        taskAssignRuleService.copyTaskAssignRules(id, definitionId);
    }

    @Override
    public void updateModelState(String id, Integer state) {
        // 校验流程模型存在
        BpmModelDO model = bpmModelMapper.selectById(id);
        if (model == null) {
            throw exception(MODEL_NOT_EXISTS);
        }
        // 校验流程定义存在
        ProcessDefinition definition = processDefinitionService.getProcessDefinition(model.getProcessDefinitionId());
        if (definition == null) {
            throw exception(PROCESS_DEFINITION_NOT_EXISTS);
        }

        // 更新状态
        processDefinitionService.updateProcessDefinitionState(definition.getId(), state);
    }

    @Override
    public BpmModelDO getBpmnModel(String bid) {
        return getModelByBid(bid);
    }

    private void checkKeyNCName(String key) {
        if (!ValidationUtils.isXmlNCName(key)) {
            throw exception(MODEL_KEY_VALID);
        }
    }

    /**
     * 校验流程表单已配置
     *
     * @param metaInfoStr 流程模型 metaInfo 字段
     * @return 流程表单
     */
    private BpmFormDO checkFormConfig(String  metaInfoStr) {
        BpmModelMetaInfoRespDTO metaInfo = JsonUtils.parseObject(metaInfoStr, BpmModelMetaInfoRespDTO.class);
        if (metaInfo == null || metaInfo.getFormType() == null) {
            throw exception(MODEL_DEPLOY_FAIL_FORM_NOT_CONFIG);
        }
        // 校验表单存在
        if (Objects.equals(metaInfo.getFormType(), BpmModelFormTypeEnum.NORMAL.getType())) {
            BpmFormDO form = bpmFormService.getForm(metaInfo.getFormId());
            if (form == null) {
                throw exception(FORM_NOT_EXISTS);
            }
            return form;
        }
        return null;
    }

    /**
     * 挂起 deploymentId 对应的流程定义。 这里一个deploymentId 只关联一个流程定义
     * @param processDefinitionId 流程定义Id.
     */
    private void updateProcessDefinitionSuspended(String processDefinitionId) {
        if (StrUtil.isEmpty(processDefinitionId)) {
            return;
        }
        processDefinitionService.updateProcessDefinitionState(processDefinitionId, SuspensionState.SUSPENDED.getStateCode());
    }

}
