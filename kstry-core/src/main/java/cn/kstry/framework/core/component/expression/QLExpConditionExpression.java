package cn.kstry.framework.core.component.expression;

import cn.kstry.framework.core.bus.StoryBus;
import cn.kstry.framework.core.exception.ExceptionEnum;
import cn.kstry.framework.core.exception.ExpressionException;
import cn.kstry.framework.core.util.GlobalUtil;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.IExpressContext;
import org.apache.commons.lang3.StringUtils;


/**
 *
 * @author crabo Yang, Feb 2025
 */
public class QLExpConditionExpression extends ConditionExpressionImpl implements ConditionExpression {

    private static ExpressRunner PARSER = new ExpressRunner();

    public QLExpConditionExpression() {
        super((scopeData, exp) -> {
            if (StringUtils.isBlank(exp) || scopeData == null) {
                return false;
            }
            Object value;
            try {
                IExpressContext<String, Object> context= new ExpressionBusContext(scopeData);
                value = PARSER.execute(exp, context, null, true, false);
            } catch (Throwable e) {
                throw new ExpressionException(ExceptionEnum.EXPRESSION_INVOKE_ERROR,
                        GlobalUtil.format("{} expression: {}", ExceptionEnum.EXPRESSION_INVOKE_ERROR.getDesc(), exp), e);
            }
            return value!=null && value.equals(true);
        });
    }

    @Override
    public boolean isNeedParserExpression() {
        return true;
    }

    @Override
    public boolean match(String expression) {
        return true;
    }

    static class ExpressionBusContext implements IExpressContext<String,Object>{

        StoryBus scopeData;
        public ExpressionBusContext(StoryBus scopeData){
            this.scopeData = scopeData;
        }
        @Override
        public Object get(Object key) {
            if("sta".equals(key)){
                return this.scopeData.getSta();
            }else if("var".equals(key)){
                return this.scopeData.getVar();
            }else if(key instanceof String && ((String) key).startsWith("#")){
                // return this.scopeData.getMonitorTracking().getApplicationContext().getBean(((String) key).substring(1));
            }
            return null;
        }

        @Override
        public Object put(String key, Object val) {
            return null;
        }
    }
}
