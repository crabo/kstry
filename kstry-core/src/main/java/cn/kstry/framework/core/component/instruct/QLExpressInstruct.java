package cn.kstry.framework.core.component.instruct;

import cn.kstry.framework.core.annotation.TaskInstruct;
import cn.kstry.framework.core.annotation.TaskService;
import cn.kstry.framework.core.bus.InstructContent;
import cn.kstry.framework.core.bus.ScopeDataOperator;
import cn.kstry.framework.core.component.conversion.TypeConverterProcessor;
import cn.kstry.framework.core.constant.BpmnElementProperties;
import cn.kstry.framework.core.container.task.TaskComponentRegister;
import cn.kstry.framework.core.exception.ExceptionEnum;
import cn.kstry.framework.core.util.ExceptionUtil;
import cn.kstry.framework.core.util.GlobalUtil;
import cn.kstry.framework.core.util.PropertyUtil;
import com.alibaba.fastjson.JSON;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.IExpressContext;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Optional;

/**
 *
 * @author crabo Yang, Feb 2025
 */
public class QLExpressInstruct implements TaskComponentRegister {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyUtil.class);

    private static ExpressRunner PARSER = new ExpressRunner();

    private final TypeConverterProcessor typeConverterProcessor;
    @Autowired
    private ApplicationContext applicationContext;

    public QLExpressInstruct(TypeConverterProcessor typeConverterProcessor) {
        this.typeConverterProcessor = typeConverterProcessor;
    }

    @TaskInstruct(name = "qlexpress")
    @TaskService(name = "qlexpress-instruct")
    public void instruct(InstructContent instructContent, ScopeDataOperator scopeDataOperator) {
        if (StringUtils.isBlank(instructContent.getContent())) {
            return;
        }

        JsScriptProperty property = null;
        if (scopeDataOperator.getTaskProperty().isPresent()) {
            try {
                property = JSON.parseObject(scopeDataOperator.getTaskProperty().get(), JsScriptProperty.class);
            } catch (Exception e) {
                LOGGER.warn("[{}] ql express property parsing exception. instruct: '{}{}', property: {}", ExceptionEnum.SCRIPT_PROPERTY_PARSER_ERROR.getExceptionCode(),
                        BpmnElementProperties.SERVICE_TASK_TASK_INSTRUCT, instructContent.getInstruct(), scopeDataOperator.getTaskProperty().orElse(StringUtils.EMPTY), e);
            }
        }

        String script = instructContent.getContent();
        try {
            IExpressContext<String, Object> context= new ExpressionBusContext(scopeDataOperator,this.applicationContext);
            Object result = PARSER.execute(script, context, null, true, false);

            if (result != null && property != null && !StringUtils.isAllBlank(property.getReturnType(), property.getResultConverter())) {
                result = typeConverterProcessor.convert(property.getResultConverter(), result, Optional.ofNullable(property.getReturnType()).filter(StringUtils::isNotBlank).map(rt -> {
                    try {
                        return Class.forName(rt);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }).orElse(null)).getValue();
            }
            if (property != null && CollectionUtils.isNotEmpty(property.getReturnTarget())) {
                for (String target : property.getReturnTarget()) {
                    boolean setRes = scopeDataOperator.setData(target, result);
                }
            }
            LOGGER.debug("invoke ql express success. instruct: '{}{}', result: {}", BpmnElementProperties.SERVICE_TASK_TASK_INSTRUCT, instructContent.getInstruct(), result);
        } catch (Throwable e) {
            throw ExceptionUtil.buildException(e, ExceptionEnum.SCRIPT_EXECUTE_ERROR, GlobalUtil.format("ql express execution exception! instruct: '{}{}', property: {}, script: \n{}",
                    BpmnElementProperties.SERVICE_TASK_TASK_INSTRUCT, instructContent.getInstruct(), scopeDataOperator.getTaskProperty().orElse(StringUtils.EMPTY), script));
        }
    }

    @Override
    public String getName() {
        return "qlexpress-instruct-component";
    }

    static class ExpressionBusContext implements IExpressContext<String,Object> {

        ScopeDataOperator scopeData;
        ApplicationContext context;
        public ExpressionBusContext(ScopeDataOperator scopeData, ApplicationContext context){
            this.scopeData = scopeData;
            this.context = context;
        }
        @Override
        public Object get(Object key) {
            if("sta".equals(key)){
                return this.scopeData.getStaScope();
            }else if("var".equals(key)){
                return this.scopeData.getVarScope();
            }else if("req".equals(key)){
                return this.scopeData.getReqScope();
            }else if(key instanceof String && ((String) key).startsWith("@")){
                return this.context.getBean(((String) key).substring(1));
            }
            return null;
        }

        @Override
        public Object put(String key, Object val) {
            return null;
        }
    }
}
