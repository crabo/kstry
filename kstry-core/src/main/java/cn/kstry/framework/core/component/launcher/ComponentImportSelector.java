/*
 *
 *  * Copyright (c) 2020-2024, Lykan (jiashuomeng@gmail.com).
 *  * <p>
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  * <p>
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  * <p>
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package cn.kstry.framework.core.component.launcher;

import cn.kstry.framework.core.component.conversion.TypeConverterProcessor;
import cn.kstry.framework.core.component.demotion.GlobalServiceDemotion;
import cn.kstry.framework.core.component.dynamic.KValueDynamicComponent;
import cn.kstry.framework.core.component.dynamic.ProcessDynamicComponent;
import cn.kstry.framework.core.component.dynamic.RoleDynamicComponent;
import cn.kstry.framework.core.component.dynamic.SubProcessDynamicComponent;
import cn.kstry.framework.core.component.expression.BasicExpressionAliasRegister;
import cn.kstry.framework.core.component.expression.ExpressionAliasParser;
import cn.kstry.framework.core.component.expression.ExpressionAliasRegister;
import cn.kstry.framework.core.component.instruct.JsScriptInstruct;
import cn.kstry.framework.core.component.instruct.QLExpressInstruct;
import cn.kstry.framework.core.component.instruct.SqlInstruct;
import cn.kstry.framework.core.component.limiter.strategy.DemotionFailAcquireStrategy;
import cn.kstry.framework.core.component.limiter.strategy.ExceptionFailAcquireStrategy;
import cn.kstry.framework.core.component.limiter.strategy.IgnoreFailAcquireStrategy;
import cn.kstry.framework.core.component.preheat.StoryEnginePreheatService;
import cn.kstry.framework.core.component.preheat.TriggerProcessPreheat;
import cn.kstry.framework.core.container.processor.*;
import cn.kstry.framework.core.engine.StoryEngine;
import cn.kstry.framework.core.engine.thread.hook.ThreadLocalSwitchHook;
import cn.kstry.framework.core.engine.thread.hook.ThreadSwitchHook;
import cn.kstry.framework.core.engine.thread.hook.ThreadSwitchHookProcessor;
import cn.kstry.framework.core.engine.thread.hook.ThreadSwitchLogHook;
import com.google.common.collect.Lists;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.OrderComparator;

import java.util.List;
import java.util.Map;

/**
 * Spring 容器中注册组件
 * <p>
 * Spring 4.2.x 之前的版本无法使用 @Import 导入一个普通的 Class
 *
 * @author lyakn
 */
@Import({ConfigResourceResolver.class, KstryContextResolver.class, TypeConverterRegister.class})
public class ComponentImportSelector extends BasicLauncher {

    @Bean
    public StartEventPostProcessor getImmutablePostProcessor() {
        return new ImmutablePostProcessor();
    }

    @Bean
    public StartEventPostProcessor getMarkIndexPostProcessor() {
        return new MarkIndexPostProcessor();
    }

    @Bean
    public StartEventPostProcessor getIterablePostProcessor() {
        return new IterablePostProcessor();
    }

    @Bean
    public StartEventPostProcessor getMainProcessPostProcessor() {
        return new MainProcessPostProcessor();
    }

    @Bean
    public StartEventPostProcessor getRearrangeFlowPostProcessor(ApplicationContext applicationContext) {
        return new RearrangeFlowPostProcessor(applicationContext);
    }

    @Bean
    public StartEventPostProcessor getVerifyFlowPostProcessor(ApplicationContext applicationContext) {
        return new VerifyFlowPostProcessor(applicationContext);
    }

    @Bean
    public StartEventPostProcessor getExpressionParserProcessor(ExpressionAliasParser expressionAliasParser) {
        return new ExpressionParserProcessor(expressionAliasParser);
    }

    @Bean
    public StartEventProcessor getStartEventProcessor(List<StartEventPostProcessor> processorList) {
        return new StartEventProcessor(processorList);
    }

    @Bean
    public SpringProcessDiagramRegister springBpmnDiagramRegister(ApplicationContext applicationContext) {
        return new SpringProcessDiagramRegister(applicationContext);
    }

    @Bean
    public RoleDynamicComponent roleDynamicComponent(ApplicationContext applicationContext) {
        return new RoleDynamicComponent(applicationContext);
    }

    @Bean
    public SubProcessDynamicComponent subProcessDynamicComponent(ApplicationContext applicationContext) {
        return new SubProcessDynamicComponent(applicationContext);
    }

    @Bean
    public ProcessDynamicComponent processDynamicComponent(ApplicationContext applicationContext,
                                                           StartEventProcessor startEventProcessor, SubProcessDynamicComponent subProcessDynamicComponent) {
        return new ProcessDynamicComponent(applicationContext, startEventProcessor, subProcessDynamicComponent);
    }

    @Bean
    public KValueDynamicComponent kValueDynamicComponent(ApplicationContext applicationContext) {
        return new KValueDynamicComponent(applicationContext);
    }

    @Bean
    @SuppressWarnings("all")
    public ThreadSwitchHookProcessor threadSwitchHookProcessor(ApplicationContext applicationContext) {
        List<ThreadSwitchHook<Object>> threadSwitchHookList = Lists.newArrayList();
        Map<String, ThreadSwitchHook> threadSwitchHookMap = applicationContext.getBeansOfType(ThreadSwitchHook.class);
        threadSwitchHookMap.values().forEach(hook -> threadSwitchHookList.add(hook));
        OrderComparator.sort(threadSwitchHookList);
        return new ThreadSwitchHookProcessor(threadSwitchHookList);
    }

    @Bean
    public ThreadSwitchLogHook logThreadSwitchHook() {
        return new ThreadSwitchLogHook();
    }

    @Bean
    public ThreadLocalSwitchHook threadLocalSwitchHook() {
        return new ThreadLocalSwitchHook();
    }

    @Bean
    public JsScriptInstruct jsScriptInstruct(TypeConverterProcessor typeConverterProcessor) {
        return new JsScriptInstruct(typeConverterProcessor);
    }

    @Bean
    public QLExpressInstruct qlExpressInstruct(TypeConverterProcessor typeConverterProcessor) {
        return new QLExpressInstruct(typeConverterProcessor);
    }

    @Bean
    public SqlInstruct sqlInstruct(TypeConverterProcessor typeConverterProcessor) {
        return new SqlInstruct(typeConverterProcessor);
    }

    /* @Bean
    public TriggerProcessPreheat triggerProcessPreheat(StoryEngine storyEngine) {
        return new TriggerProcessPreheat(storyEngine);
    }*/

    @Bean
    public ExpressionAliasParser expressionAliasParser(List<ExpressionAliasRegister> registerList) {
        return new ExpressionAliasParser(registerList);
    }

    @Bean
    public BasicExpressionAliasRegister basicExpressionAliasRegister() {
        return new BasicExpressionAliasRegister();
    }

    @Bean
    public StoryEnginePreheatService storyEnginePreheatService() {
        return new StoryEnginePreheatService();
    }

    @Bean
    public DemotionFailAcquireStrategy demotionFailAcquireStrategy() {
        return new DemotionFailAcquireStrategy();
    }

    @Bean
    public ExceptionFailAcquireStrategy exceptionFailAcquireStrategy() {
        return new ExceptionFailAcquireStrategy();
    }

    @Bean
    public IgnoreFailAcquireStrategy ignoreFailAcquireStrategy() {
        return new IgnoreFailAcquireStrategy();
    }

    @Bean
    public GlobalServiceDemotion globalServiceDemotion() {
        return new GlobalServiceDemotion();
    }
}
