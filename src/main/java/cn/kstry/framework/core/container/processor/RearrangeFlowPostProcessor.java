/*
 *
 *  * Copyright (c) 2020-2022, Lykan (jiashuomeng@gmail.com).
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
package cn.kstry.framework.core.container.processor;

import cn.kstry.framework.core.bpmn.*;
import cn.kstry.framework.core.bpmn.impl.FlowElementImpl;
import cn.kstry.framework.core.bpmn.impl.InclusiveGatewayImpl;
import cn.kstry.framework.core.bpmn.impl.SequenceFlowImpl;
import cn.kstry.framework.core.component.utils.BasicInStack;
import cn.kstry.framework.core.component.utils.InStack;
import cn.kstry.framework.core.exception.ExceptionEnum;
import cn.kstry.framework.core.util.AssertUtil;
import cn.kstry.framework.core.util.ElementPropertyUtil;
import cn.kstry.framework.core.util.ExceptionUtil;
import cn.kstry.framework.core.util.GlobalUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.*;

/**
 * 重排链路
 *  - 如果排他网关、包含网关中指定了 TaskService，这个流程会把指定的 TaskService 单独拎出来作为执行节点加入到链路中
 *
 * @author lykan
 */
public class RearrangeFlowPostProcessor implements StartEventPostProcessor {

    @Override
    public Optional<StartEvent> postStartEvent(StartEvent startEvent) {

        Map<FlowElement, Integer> comingCountMap = Maps.newHashMap();
        InStack<FlowElement> basicInStack = new BasicInStack<>();
        basicInStack.push(startEvent);
        while (!basicInStack.isEmpty()) {
            FlowElement node = basicInStack.pop().orElseThrow(() -> ExceptionUtil.buildException(null, ExceptionEnum.SYSTEM_ERROR, null));
            if (node instanceof SubProcess) {
                postStartEvent(((SubProcess) node).getStartEvent());
            }
            if (ElementPropertyUtil.isSupportAggregation(node)) {
                comingCountMap.merge(node, 1, Integer::sum);
                if (!Objects.equals(comingCountMap.get(node), node.comingList().size())) {
                    continue;
                }
            }

            if (node instanceof ServiceTaskSupport) {
                Optional<ServiceTask> serviceTaskOptional = ((ServiceTaskSupport) node).getServiceTask();
                serviceTaskOptional.ifPresent(serviceTask -> doSeparateFlowElement(node, serviceTask));
            }
            basicInStack.pushList(node.outingList());
        }

        return Optional.of(startEvent);
    }

    private void doSeparateFlowElement(FlowElement node, ServiceTask serviceTask) {
        AssertUtil.isTrue(node instanceof ServiceTaskSupport);

        FlowElementImpl serviceTaskImpl = GlobalUtil.transferNotEmpty(serviceTask, FlowElementImpl.class);
        serviceTaskImpl.setId("Service-Task-" + node.getId());
        serviceTaskImpl.setName(node.getName());

        if (node instanceof InclusiveGateway) {
            List<FlowElement> nodeComingList = node.comingList();
            AssertUtil.isTrue(nodeComingList.size() > 0);

            InclusiveGatewayImpl mockInclusiveGateway = new InclusiveGatewayImpl();
            mockInclusiveGateway.setId("Inclusive-Gateway-" + node.getId());
            Lists.newArrayList(nodeComingList).forEach(flowElement -> {
                FlowElementImpl beforeSequenceFlow = GlobalUtil.transferNotEmpty(flowElement, FlowElementImpl.class);
                beforeSequenceFlow.clearOutingChain();
                beforeSequenceFlow.outing(mockInclusiveGateway);
            });

            SequenceFlowImpl sequenceFlow1 = new SequenceFlowImpl();
            sequenceFlow1.setId(UUID.randomUUID().toString());
            mockInclusiveGateway.outing(sequenceFlow1);
            sequenceFlow1.outing(serviceTaskImpl);

            SequenceFlowImpl sequenceFlow2 = new SequenceFlowImpl();
            sequenceFlow2.setId(UUID.randomUUID().toString());
            serviceTaskImpl.outing(sequenceFlow2);
            sequenceFlow2.outing(node);

        } else if (node instanceof ExclusiveGateway) {
            List<FlowElement> nodeComingList = node.comingList();
            AssertUtil.oneSize(nodeComingList);

            SequenceFlowImpl sequenceFlow = new SequenceFlowImpl();
            sequenceFlow.setId(UUID.randomUUID().toString());

            FlowElementImpl beforeSequenceFlow = GlobalUtil.transferNotEmpty(nodeComingList.get(0), FlowElementImpl.class);
            beforeSequenceFlow.clearOutingChain();
            beforeSequenceFlow.outing(serviceTaskImpl);
            serviceTaskImpl.outing(sequenceFlow);
            sequenceFlow.outing(node);
        } else {
            throw ExceptionUtil.buildException(null, ExceptionEnum.CONFIGURATION_UNSUPPORTED_ELEMENT, "There is a flow analysis that exceeds expectations!");
        }
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
