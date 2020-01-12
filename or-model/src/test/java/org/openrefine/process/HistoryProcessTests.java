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

package org.openrefine.process;

import static org.mockito.Mockito.mock;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.ProjectManager;
import org.openrefine.ProjectMetadata;
import org.openrefine.history.Change;
import org.openrefine.history.HistoryEntry;
import org.openrefine.history.HistoryProcess;
import org.openrefine.model.AbstractOperation;
import org.openrefine.model.Project;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class HistoryProcessTests {

    private Project project;
    private ProjectMetadata projectMetadata;

    @BeforeMethod
    public void setUp() {
        project = new Project();
        projectMetadata = mock(ProjectMetadata.class);
        ProjectManager.singleton.registerProject(project, projectMetadata);
        AbstractOperation op = mock(AbstractOperation.class);
        Change ch = mock(Change.class);
        HistoryEntry entry = new HistoryEntry(1234L, project, "first operation", op, ch);
        project.history.addEntry(entry);
        entry = new HistoryEntry(5678L, project, "second operation", op, ch);
        project.history.addEntry(entry);
    }

    @Test
    public void serializeHistoryProcess() {
        HistoryProcess process = new HistoryProcess(project, 1234L);
        TestUtils.isSerializedTo(process, "{"
                + "\"description\":\"Undo/redo until after first operation\","
                + "\"immediate\":true,"
                + "\"status\":\"pending\"}", ParsingUtilities.defaultWriter);
    }
}