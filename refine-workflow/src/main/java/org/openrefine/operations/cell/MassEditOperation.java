/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.operations.cell;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.model.*;
import org.openrefine.model.Record;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.RowMapChange;
import org.openrefine.operations.EngineDependentOperation;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.StringUtils;

/**
 * Edits values in a column, by replacing some values by others. The source value can be generated by an expression.
 *
 */
public class MassEditOperation extends EngineDependentOperation {

    final protected String _expression;
    final protected List<Edit> _edits;
    final protected String _columnName;

    static public class Edit {

        @JsonProperty("from")
        final public List<String> from;
        @JsonProperty("fromBlank")
        final public boolean fromBlank;
        @JsonProperty("fromError")
        final public boolean fromError;
        @JsonProperty("to")
        final public Serializable to;

        public Edit(
                List<String> from,
                boolean fromBlank,
                boolean fromError,
                Serializable to) {
            this.from = from;
            this.fromBlank = fromBlank || (from.size() == 1 && from.get(0).length() == 0);
            this.fromError = fromError;
            this.to = to;
        }

        @JsonCreator
        public static Edit deserialize(
                @JsonProperty("from") List<String> from,
                @JsonProperty("fromBlank") boolean fromBlank,
                @JsonProperty("fromError") boolean fromError,
                @JsonProperty("to") Object to,
                @JsonProperty("type") String type) {
            Serializable serializable = (Serializable) to;
            if ("date".equals(type)) {
                serializable = ParsingUtilities.stringToDate((String) to);
            }
            return new Edit(from == null ? new ArrayList<>() : from,
                    fromBlank, fromError, serializable);
        }
    }

    @JsonCreator
    public MassEditOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("expression") String expression,
            @JsonProperty("edits") List<Edit> edits) {
        super(engineConfig);
        _expression = expression;
        _edits = edits;
        _columnName = columnName;
    }

    @JsonProperty("expression")
    public String getExpression() {
        return _expression;
    }

    @JsonProperty("edits")
    public List<Edit> getEdits() {
        return _edits;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @Override
    public String getDescription() {
        return "Mass edit cells in column " + _columnName;
    }

    @Override
    public Change createChange() throws ParsingException {
        Evaluable eval = MetaParser.parse(_expression);

        Map<String, Serializable> fromTo = new HashMap<>();
        Serializable fromBlankTo = null;
        Serializable fromErrorTo = null;

        for (Edit edit : _edits) {
            for (String s : edit.from) {
                fromTo.put(s, edit.to);
            }

            // the last edit wins
            if (edit.fromBlank) {
                fromBlankTo = edit.to;
            }
            if (edit.fromError) {
                fromErrorTo = edit.to;
            }
        }
        return new MassEditChange(_engineConfig, eval, fromTo, fromBlankTo, fromErrorTo);
    }

    public class MassEditChange extends RowMapChange {

        protected final Evaluable _evaluable;
        protected final Map<String, Serializable> _fromTo;
        protected final Serializable _fromBlankTo;
        protected final Serializable _fromErrorTo;

        public MassEditChange(
                EngineConfig engineConfig,
                Evaluable evaluable,
                Map<String, Serializable> fromTo,
                Serializable fromBlankTo,
                Serializable fromErrorTo) {
            super(engineConfig);
            _evaluable = evaluable;
            _fromTo = fromTo;
            _fromBlankTo = fromBlankTo;
            _fromErrorTo = fromErrorTo;

        }

        @Override
        public RowInRecordMapper getPositiveRowMapper(GridState state, ChangeContext context) throws DoesNotApplyException {
            int columnIdx = columnIndex(state.getColumnModel(), _columnName);
            return mapper(columnIdx, _evaluable, _columnName, state.getColumnModel(), state.getOverlayModels(), _fromTo, _fromBlankTo,
                    _fromErrorTo);
        }

        @Override
        public boolean isImmediate() {
            return true;
        }

    }

    private static RowInRecordMapper mapper(int columnIdx, Evaluable evaluable, String columnName,
            ColumnModel columnModel, Map<String, OverlayModel> overlayModels,
            Map<String, Serializable> fromTo, Serializable fromBlankTo, Serializable fromErrorTo) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = 6383816657756293719L;

            @Override
            public Row call(Record record, long rowIndex, Row row) {
                Cell cell = row.getCell(columnIdx);
                Cell newCell = cell;

                Properties bindings = ExpressionUtils.createBindings();
                ExpressionUtils.bind(bindings, columnModel, row, rowIndex, record, columnName, cell, overlayModels);

                Object v = evaluable.evaluate(bindings);
                if (ExpressionUtils.isError(v)) {
                    if (fromErrorTo != null) {
                        newCell = new Cell(fromErrorTo, (cell != null) ? cell.recon : null);
                    }
                } else if (ExpressionUtils.isNonBlankData(v)) {
                    String from = StringUtils.toString(v);
                    Serializable to = fromTo.get(from);
                    if (to != null) {
                        newCell = new Cell(to, (cell != null) ? cell.recon : null);
                    }
                } else {
                    if (fromBlankTo != null) {
                        newCell = new Cell(fromBlankTo, (cell != null) ? cell.recon : null);
                    }
                }
                return row.withCell(columnIdx, newCell);
            }

            @Override
            public boolean preservesRecordStructure() {
                // TODO we could be more precise: if blanks are preserved and cells are never blanked,
                // then the record structure is also preserved.
                return columnIdx != columnModel.getKeyColumnIndex();
            }

        };
    }

}
