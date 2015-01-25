package com.linkedin.pinot.core.plan;

import org.apache.log4j.Logger;

import com.linkedin.pinot.common.request.BrokerRequest;
import com.linkedin.pinot.core.common.Operator;
import com.linkedin.pinot.core.indexsegment.IndexSegment;
import com.linkedin.pinot.core.operator.BReusableFilteredDocIdSetOperator;


/**
 * DocIdSetPlanNode takes care creating BDocIdSetOperator.
 * Configure filter query and max size of docId cache here.
 *
 * @author xiafu
 *
 */
public class DocIdSetPlanNode implements PlanNode {
  private static final Logger _logger = Logger.getLogger("QueryPlanLog");
  private final IndexSegment _indexSegment;
  private final BrokerRequest _brokerRequest;
  private final PlanNode _filterNode;
  private final int _maxDocPerAggregation;
  private BReusableFilteredDocIdSetOperator _projectOp = null;

  public DocIdSetPlanNode(IndexSegment indexSegment, BrokerRequest query, int maxDocPerAggregation) {
    _maxDocPerAggregation = maxDocPerAggregation;
    _indexSegment = indexSegment;
    _brokerRequest = query;
    if (_brokerRequest.isSetFilterQuery()) {
      _filterNode = new FilterPlanNode(_indexSegment, _brokerRequest);
    } else {
      _filterNode = null;
    }
  }

  @Override
  public synchronized Operator run() {
    if (_projectOp == null) {
      if (_filterNode != null) {
        _projectOp = new BReusableFilteredDocIdSetOperator(_filterNode.run(), _indexSegment.getSegmentMetadata().getTotalDocs(), _maxDocPerAggregation);
      } else {
        _projectOp = new BReusableFilteredDocIdSetOperator(null, _indexSegment.getSegmentMetadata().getTotalDocs(), _maxDocPerAggregation);
      }
      return _projectOp;
    } else {
      return _projectOp;
    }

  }

  @Override
  public void showTree(String prefix) {
    _logger.debug(prefix + "DocIdSet Plan Node :");
    _logger.debug(prefix + "Operator: BReusableFilteredDocIdSetOperator");
    _logger.debug(prefix + "Argument 0: IndexSegment - " + _indexSegment.getSegmentName());
    if (_filterNode != null) {
      _logger.debug(prefix + "Argument 1: FilterPlanNode :(see below)");
      _filterNode.showTree(prefix + "    ");
    }
  }

}
