/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package org.openrefine.model.changes;

import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.history.Change;
import org.openrefine.model.ModelException;
import org.openrefine.model.Project;
import org.openrefine.model.changes.CellAtRow;
import org.openrefine.model.changes.ColumnAdditionChange;
import org.openrefine.model.changes.MassChange;

public class MassChangeTests extends RefineTest {

    Project project;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void SetUp()
            throws IOException, ModelException {
        project = createProjectWithColumns("MassChangeTest");
    }

    /**
     * Test case for #914 - Demonstrates MassChange revert doesn't work by adding two columns to a project with a
     * MassChange and then reverting. Without the fix, column "a" will be removed before column "b", causing column "b"
     * removal to fail because it won't be found at index 1 as expected.
     */
    @Test
    public void testWrongReverseOrder()
            throws Exception {
        List<Change> changes = new ArrayList<Change>();
        changes.add(new ColumnAdditionChange("a", 0, new ArrayList<CellAtRow>()));
        changes.add(new ColumnAdditionChange("b", 1, new ArrayList<CellAtRow>()));
        MassChange massChange = new MassChange(changes, false);
        massChange.apply(project);
        massChange.revert(project);
        assertTrue(project.columnModel.columns.isEmpty());
    }
}