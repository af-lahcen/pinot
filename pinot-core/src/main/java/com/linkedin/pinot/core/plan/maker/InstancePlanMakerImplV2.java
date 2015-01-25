package com.linkedin.pinot.core.plan.maker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.linkedin.pinot.common.request.BrokerRequest;
import com.linkedin.pinot.core.indexsegment.IndexSegment;
import com.linkedin.pinot.core.plan.AggregationGroupByOperatorPlanNode;
import com.linkedin.pinot.core.plan.AggregationGroupByOperatorPlanNode.AggregationGroupByImplementationType;
import com.linkedin.pinot.core.plan.AggregationPlanNode;
import com.linkedin.pinot.core.plan.CombinePlanNode;
import com.linkedin.pinot.core.plan.GlobalPlanImplV0;
import com.linkedin.pinot.core.plan.InstanceResponsePlanNode;
import com.linkedin.pinot.core.plan.Plan;
import com.linkedin.pinot.core.plan.PlanNode;
import com.linkedin.pinot.core.plan.SelectionPlanNode;
import com.linkedin.pinot.core.query.aggregation.groupby.BitHacks;
import com.linkedin.pinot.core.segment.index.IndexSegmentImpl;


/**
 * Make the huge plan, root is always ResultPlanNode, the child of it is a huge
 * plan node which will take the segment and query, then do everything.
 *
 * @author xiafu
 *
 */
public class InstancePlanMakerImplV2 implements PlanMaker {
  private final long _defaultTimeOutMs;
  private final Map<String, Long> _resourceTimeOutMsMap = new HashMap<String, Long>();

  public InstancePlanMakerImplV2() {
    _defaultTimeOutMs = 150000;
  }

  public InstancePlanMakerImplV2(long defaultTimeOutMs) {
    _defaultTimeOutMs = defaultTimeOutMs;
  }

  public InstancePlanMakerImplV2(Map<String, Long> resourceTimeOutMsMap) {
    _defaultTimeOutMs = 150000;
    _resourceTimeOutMsMap.putAll(resourceTimeOutMsMap);
  }

  public InstancePlanMakerImplV2(long defaultTimeOutMs, Map<String, Long> resourceTimeOutMsMap) {
    _defaultTimeOutMs = defaultTimeOutMs;
    _resourceTimeOutMsMap.putAll(resourceTimeOutMsMap);
  }

  @Override
  public PlanNode makeInnerSegmentPlan(IndexSegment indexSegment, BrokerRequest brokerRequest) {

    if (brokerRequest.isSetAggregationsInfo()) {
      if (!brokerRequest.isSetGroupBy()) {
        // Only Aggregation
        final PlanNode aggregationPlanNode = new AggregationPlanNode(indexSegment, brokerRequest);
        return aggregationPlanNode;
      } else {
        // Aggregation GroupBy
        PlanNode aggregationGroupByPlanNode;
        if (indexSegment instanceof IndexSegmentImpl) {
          if (isGroupKeyFitForLong(indexSegment, brokerRequest)) {
            aggregationGroupByPlanNode =
                new AggregationGroupByOperatorPlanNode(indexSegment, brokerRequest, AggregationGroupByImplementationType.Dictionary);
          } else {
            aggregationGroupByPlanNode =
                new AggregationGroupByOperatorPlanNode(indexSegment, brokerRequest, AggregationGroupByImplementationType.DictionaryAndTrie);
          }
        } else {
          aggregationGroupByPlanNode =
              new AggregationGroupByOperatorPlanNode(indexSegment, brokerRequest, AggregationGroupByImplementationType.NoDictionary);
        }
        return aggregationGroupByPlanNode;
      }
    }
    // Only Selection
    if (brokerRequest.isSetSelections()) {
      final PlanNode selectionPlanNode = new SelectionPlanNode(indexSegment, brokerRequest);
      return selectionPlanNode;
    }
    throw new UnsupportedOperationException("The query contains no aggregation or selection!");
  }

  @Override
  public Plan makeInterSegmentPlan(List<IndexSegment> indexSegmentList, BrokerRequest brokerRequest, ExecutorService executorService) {
    final InstanceResponsePlanNode rootNode = new InstanceResponsePlanNode();
    final CombinePlanNode combinePlanNode = new CombinePlanNode(brokerRequest, executorService, getResourceTimeOut(brokerRequest));
    rootNode.setPlanNode(combinePlanNode);
    for (final IndexSegment indexSegment : indexSegmentList) {
      combinePlanNode.addPlanNode(makeInnerSegmentPlan(indexSegment, brokerRequest));
    }
    return new GlobalPlanImplV0(rootNode);
  }

  private long getResourceTimeOut(BrokerRequest brokerRequest) {
    try {
      String resourceName = brokerRequest.getQuerySource().getResourceName();
      if (_resourceTimeOutMsMap.containsKey(resourceName)) {
        return _resourceTimeOutMsMap.get(brokerRequest);
      }
    } catch (Exception e) {
    }
    return _defaultTimeOutMs;
  }

  private boolean isGroupKeyFitForLong(IndexSegment indexSegment, BrokerRequest brokerRequest) {
    final IndexSegmentImpl columnarSegment = (IndexSegmentImpl) indexSegment;
    int totalBitSet = 0;
    for (final String column : brokerRequest.getGroupBy().getColumns()) {
      totalBitSet += BitHacks.findLogBase2(columnarSegment.getDictionaryFor(column).length()) + 1;
    }
    if (totalBitSet > 64) {
      return false;
    }
    return true;
  }
}
