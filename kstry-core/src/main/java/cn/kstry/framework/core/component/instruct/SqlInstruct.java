package cn.kstry.framework.core.component.instruct;

import cn.kstry.framework.core.annotation.TaskInstruct;
import cn.kstry.framework.core.annotation.TaskService;
import cn.kstry.framework.core.bus.InstructContent;
import cn.kstry.framework.core.bus.ScopeDataOperator;
import cn.kstry.framework.core.component.conversion.TypeConverterProcessor;
import cn.kstry.framework.core.constant.BpmnElementProperties;
import cn.kstry.framework.core.container.task.TaskComponentRegister;
import cn.kstry.framework.core.exception.BusinessException;
import cn.kstry.framework.core.exception.ExceptionEnum;
import cn.kstry.framework.core.util.ExceptionUtil;
import cn.kstry.framework.core.util.GlobalUtil;
import cn.kstry.framework.core.util.PropertyUtil;
import com.alibaba.fastjson.JSON;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/**
 * 使用JsScriptProperty设置sql执行逻辑
 *   invokeSource: ds1 ，获取springContext当前的routeDatasource或名为ds1的DataSrouce对象
 *   invokeMethod: select返回Map, selectlist返回List, value返回单值转字符, execute执行update/delete
 *   returnType: 返回对象类型，可为null
 *   returnTarget: ['res', 'sta.v1', 'var.v1'] , 将返回值写入这些存储域
 *
 *   使用sql变量绑定: select ..from a where k1>:var.k1 and k2==:sta.k2
 *
 * @author crabo Yang, Feb 2025
 */
public class SqlInstruct implements TaskComponentRegister {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyUtil.class);

    @Autowired
    private ApplicationContext applicationContext;

    private final TypeConverterProcessor typeConverterProcessor;

    public SqlInstruct(TypeConverterProcessor typeConverterProcessor) {
        this.typeConverterProcessor = typeConverterProcessor;
    }

    @TaskInstruct(name = "sql")
    @TaskService(name = "sql-instruct")
    public void instruct(InstructContent instructContent, ScopeDataOperator scopeDataOperator) {
        if (StringUtils.isBlank(instructContent.getContent())) {
            return;
        }

        JsScriptProperty property = null;
        if (scopeDataOperator.getTaskProperty().isPresent()) {
            try {
                property = JSON.parseObject(scopeDataOperator.getTaskProperty().get(), JsScriptProperty.class);
            } catch (Exception e) {
                LOGGER.warn("[{}] SQL parsing exception. instruct: '{}{}', property: {}", ExceptionEnum.SCRIPT_PROPERTY_PARSER_ERROR.getExceptionCode(),
                        BpmnElementProperties.SERVICE_TASK_TASK_INSTRUCT, instructContent.getInstruct(), scopeDataOperator.getTaskProperty().orElse(StringUtils.EMPTY), e);
            }
        }

        String sql = instructContent.getContent();
        try {
            NamedParameterJdbcTemplate jdbc = this.getJdbc(property.getInvokeSource());
            if(jdbc==null){
                throw new BusinessException(ExceptionEnum.SCRIPT_EXECUTE_ERROR.getExceptionCode(), "无法创建数据源连接："+property.getInvokeMethod());
            }

            SqlParameterSource paramSource= new ExpressionBusParameterSource(scopeDataOperator);
            Object result = this.executeSql(property.getInvokeMethod(), jdbc, sql, paramSource);

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
            LOGGER.debug("run SQL success. sql: '{}{}', result: {}", BpmnElementProperties.SERVICE_TASK_TASK_INSTRUCT, instructContent.getInstruct(), result);
        } catch (Throwable e) {
            throw ExceptionUtil.buildException(e, ExceptionEnum.SCRIPT_EXECUTE_ERROR, GlobalUtil.format("SQL execution exception! instruct: '{}{}', property: {}, sql: \n{}",
                    BpmnElementProperties.SERVICE_TASK_TASK_INSTRUCT, instructContent.getInstruct(), scopeDataOperator.getTaskProperty().orElse(StringUtils.EMPTY), sql));
        }
    }

    @Override
    public String getName() {
        return "sql-instruct-component";
    }

    private NamedParameterJdbcTemplate getJdbc(String ds){
        try {
            DataSource targetDataSource;

            Map<String, AbstractRoutingDataSource> beans = applicationContext.getBeansOfType(AbstractRoutingDataSource.class);
            AbstractRoutingDataSource multi = beans.values().stream().findFirst().orElse(null);
            //先判断是否有多源切换机制
            if(multi!=null) {
                Method method = AbstractRoutingDataSource.class.getDeclaredMethod("determineTargetDataSource");
                method.setAccessible(true);
                targetDataSource = (DataSource) method.invoke(multi);
            }else{
                Map<String, DataSource> map = applicationContext.getBeansOfType(DataSource.class);
                //多个数据源，用invokeSource参数指定名称
                if(StringUtils.isNotEmpty(ds) && map.size()>1){
                    targetDataSource = map.get(ds);
                }else
                    targetDataSource = map.values().stream().findFirst().orElse(null);
            }
            if(targetDataSource!=null)
                return new NamedParameterJdbcTemplate(targetDataSource);
        } catch (Exception e) {
            LOGGER.error("无法找到可用的连接数据源", e);
        }
        return null;
    }

    private Object executeSql(String invokeMethod,NamedParameterJdbcTemplate jdbc, String sql, SqlParameterSource paramSource){

        switch (invokeMethod) {
            case "map":
            case "select":
                return jdbc.queryForMap(sql, paramSource);

            case "list":
            case "selectlist":
                return jdbc.queryForList(sql, paramSource); // List<Map>

            case "string":
            case "value":
                return jdbc.queryForObject(sql, paramSource, String.class);

            case "execute": // update, delete
                return jdbc.update(sql, paramSource);
        }
        return null;
    }

    static class ExpressionBusParameterSource implements SqlParameterSource {

        ScopeDataOperator scopeData;
        public ExpressionBusParameterSource(ScopeDataOperator scopeData){
            this.scopeData = scopeData;
        }

        @Override
        public boolean hasValue(String expression) {
            return this.scopeData.getData(expression).isPresent();
        }

        @Override
        public Object getValue(String expression) throws IllegalArgumentException {
            return this.scopeData.getData(expression).orElse(null);
        }
    }
}
