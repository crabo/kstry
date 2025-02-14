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
package cn.kstry.framework.core.engine.thread;

import cn.kstry.framework.core.bpmn.impl.BasicElementIterable;
import cn.kstry.framework.core.bus.IterDataItem;
import cn.kstry.framework.core.bus.ScopeDataOperator;
import cn.kstry.framework.core.bus.StoryBus;
import cn.kstry.framework.core.engine.FlowRegister;
import cn.kstry.framework.core.engine.FlowTaskCore;
import cn.kstry.framework.core.engine.StoryEngineModule;
import cn.kstry.framework.core.engine.future.AdminFuture;
import cn.kstry.framework.core.engine.future.FragmentFuture;
import cn.kstry.framework.core.engine.future.FragmentTaskFuture;
import cn.kstry.framework.core.enums.AsyncTaskState;
import cn.kstry.framework.core.exception.ExceptionEnum;
import cn.kstry.framework.core.exception.KstryException;
import cn.kstry.framework.core.role.Role;
import cn.kstry.framework.core.util.AssertUtil;
import cn.kstry.framework.core.util.GlobalUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Future;

/**
 * 流程片段执行任务
 *
 * @author lykan
 */
public class FragmentTask extends FlowTaskCore<AsyncTaskState> implements Task<AsyncTaskState> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FragmentTask.class);

    public FragmentTask(StoryEngineModule engineModule, FlowRegister flowRegister, Role role, StoryBus storyBus) {
        super(engineModule, flowRegister, role, storyBus);
    }

    @Override
    public FragmentFuture buildTaskFuture(Future<AsyncTaskState> future) {
        return new FragmentTaskFuture<>(future, getTaskName());
    }

    @Override
    public AsyncTaskState call() {
        AdminFuture adminFuture = null;
        try {
            engineModule.getThreadSwitchHookProcessor().usePreviousData(threadSwitchHookObjectMap, storyBus.getScopeDataOperator());
            asyncTaskSwitch.await();
            adminFuture = flowRegister.getAdminFuture();
            AssertUtil.notTrue(adminFuture.isCancelled(flowRegister.getStartEventId()), ExceptionEnum.ASYNC_TASK_INTERRUPTED, "Task interrupted. Story task was interrupted! taskName: {}", getTaskName());

            //特殊子流程? 执行for循环
            String startName = this.flowRegister.getStartElement().getName();
            if(startName!=null && startName.indexOf("@")>0){
                doIterateExecSubProcess(startName);
            }else {
                doExe(this.role, this.storyBus, this.flowRegister);
            }
            return AsyncTaskState.SUCCESS;
        } catch (Throwable e) {
            if (adminFuture != null) {
                adminFuture.errorNotice(e, flowRegister);
            } else {
                String errorCode = ExceptionEnum.STORY_ERROR.getExceptionCode();
                if (e instanceof KstryException) {
                    errorCode = GlobalUtil.transferNotEmpty(e, KstryException.class).getErrorCode();
                }
                LOGGER.warn("[{}] Task execution fails and exits because an exception is thrown! AdminFuture is null! taskName: {}", errorCode, getTaskName(), e);
            }
            return AsyncTaskState.ERROR;
        } finally {
            engineModule.getThreadSwitchHookProcessor().clear(threadSwitchHookObjectMap, storyBus.getScopeDataOperator());
        }
    }

    /**
     * 从子流程名称，提取遍历配置，实现子流程for循环
     */
    private void doIterateExecSubProcess(String startName){
        // 格式如: subproc01-@liOrders-order ,遍历liOrders，并写入变量order中
        String[] batchVarNames = startName.substring(startName.indexOf("@")+1).split("-");

        //构造循环iterable结构
        BasicElementIterable iterable = new BasicElementIterable();
        iterable.setIteSource("var."+batchVarNames[0]);
        List<Object> paramList = this.getIteratorList(this.flowRegister.getStartElement(), this.storyBus, iterable);

        int size = paramList.size();
        ScopeDataOperator scope = this.storyBus.getScopeDataOperator();
        for(int i=0;i<size;i++){
            //允许业务设置@order=false，提前结束循环
            if(scope.getVarData("@"+batchVarNames[1]).orElse(true).equals(false))
                break;

            IterDataItem<Object> iterDataItem = new IterDataItem<>(false, paramList.get(i), paramList, i, size);
            scope.setVarData("@"+batchVarNames[1], iterDataItem);
            scope.setVarData(batchVarNames[1], paramList.get(i));

            FlowRegister reg = this.flowRegister.cloneSelf();
            doExe(this.role, this.storyBus, reg);
        }
    }
}
