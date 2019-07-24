/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.core.rewrite.token.generator;

import com.google.common.base.Optional;
import org.apache.shardingsphere.core.optimize.api.statement.InsertOptimizedStatement;
import org.apache.shardingsphere.core.optimize.api.statement.OptimizedStatement;
import org.apache.shardingsphere.core.optimize.sharding.segment.insert.InsertOptimizeResultUnit;
import org.apache.shardingsphere.core.parse.sql.segment.dml.assignment.InsertValuesSegment;
import org.apache.shardingsphere.core.parse.sql.statement.dml.InsertStatement;
import org.apache.shardingsphere.core.rewrite.builder.ParameterBuilder;
import org.apache.shardingsphere.core.rewrite.token.pojo.InsertValueToken;
import org.apache.shardingsphere.core.rewrite.token.pojo.InsertValuesToken;
import org.apache.shardingsphere.core.rule.EncryptRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Insert values token generator.
 *
 * @author panjuan
 */
public final class InsertValuesTokenGenerator implements OptionalSQLTokenGenerator<EncryptRule> {
    
    private EncryptRule encryptRule;
    
    private InsertOptimizedStatement insertOptimizedStatement;
    
    private Collection<InsertValuesSegment> insertValuesSegments;
    
    @Override
    public Optional<InsertValuesToken> generateSQLToken(final OptimizedStatement optimizedStatement, 
                                                        final ParameterBuilder parameterBuilder, final EncryptRule encryptRule, final boolean isQueryWithCipherColumn) {
        Collection<InsertValuesSegment> insertValuesSegments = optimizedStatement.getSQLStatement().findSQLSegments(InsertValuesSegment.class);
        if (!isNeedToGenerateSQLToken(optimizedStatement, insertValuesSegments)) {
            return Optional.absent();
        }
        initParameters(encryptRule, optimizedStatement, insertValuesSegments);
        return Optional.of(new InsertValuesToken(getStartIndex(), getStopIndex(), getInsertValues()));
    }
    
    private boolean isNeedToGenerateSQLToken(final OptimizedStatement optimizedStatement, final Collection<InsertValuesSegment> insertValuesSegments) {
        return optimizedStatement.getSQLStatement() instanceof InsertStatement && !insertValuesSegments.isEmpty();
    }
    
    private void initParameters(final EncryptRule encryptRule, final OptimizedStatement optimizedStatement, final Collection<InsertValuesSegment> insertValuesSegments) {
        this.encryptRule = encryptRule;
        insertOptimizedStatement = (InsertOptimizedStatement) optimizedStatement;
        this.insertValuesSegments = insertValuesSegments;
    }
    
    private int getStartIndex() {
        int result = insertValuesSegments.iterator().next().getStartIndex();
        for (InsertValuesSegment each : insertValuesSegments) {
            result = result > each.getStartIndex() ? each.getStartIndex() : result;
        }
        return result;
    }
    
    private int getStopIndex() {
        int result = insertValuesSegments.iterator().next().getStopIndex();
        for (InsertValuesSegment each : insertValuesSegments) {
            result = result < each.getStopIndex() ? each.getStopIndex() : result;
        }
        return result;
    }
    
    private List<InsertValueToken> getInsertValues() {
        List<InsertValueToken> insertValueTokens = new LinkedList<>();
        for (InsertOptimizeResultUnit each : insertOptimizedStatement.getUnits()) {
            insertValueTokens.add(new InsertValueToken(getActualInsertColumns(), Arrays.asList(each.getValues()), each.getDataNodes()));
        }
        return insertValueTokens;
    }
    
    private Collection<String> getActualInsertColumns() {
        Collection<String> result = new LinkedList<>();
        Map<String, String> logicAndCipherColumns = encryptRule.getEncryptEngine().getLogicAndCipherColumns(insertOptimizedStatement.getTables().getSingleTableName());
        for (String each : insertOptimizedStatement.getInsertColumns().getRegularColumnNames()) {
            result.add(getCipherColumn(each, logicAndCipherColumns));
        }
        return result;
    }
    
    private String getCipherColumn(final String column, final Map<String, String> logicAndCipherColumns) {
        return logicAndCipherColumns.keySet().contains(column) ? logicAndCipherColumns.get(column) : column;
    }
}
