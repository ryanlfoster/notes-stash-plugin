/*
 * Copyright 2015 ForgeRock AS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forgerock.stash.notes.rest;

import java.io.InputStream;

import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.stash.i18n.I18nService;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.repository.RepositoryService;
import com.atlassian.stash.scm.Command;
import com.atlassian.stash.scm.CommandOutputHandler;
import com.atlassian.stash.scm.DefaultCommandExitHandler;
import com.atlassian.stash.scm.ScmService;
import com.atlassian.stash.util.Page;
import com.atlassian.stash.util.PageUtils;
import com.atlassian.utils.process.ProcessException;
import com.atlassian.utils.process.StringOutputHandler;
import com.atlassian.utils.process.Watchdog;

/**
 * A resource of message.
 */
@Path("/")
public class NotesResources {

    private final RepositoryService repositoryService;
    private final ScmService scmService;
    private final I18nService i18nService;

    public NotesResources(final RepositoryService repositoryService,
                          final ScmService scmService,
                          final I18nService i18nService) {
        this.repositoryService = repositoryService;
        this.scmService = scmService;
        this.i18nService = i18nService;
    }

    @GET
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/projects/{projectKey}/repos/{repositorySlug}/commits/{changeset}")
    public Response getNotes(@PathParam("projectKey") String projectKey,
                             @PathParam("repositorySlug") String repositorySlug,
                             @PathParam("changeset") String changesetId) {

        Page<? extends Repository> page = repositoryService.findByProjectKey(projectKey,
                                                                             PageUtils.newRequest(0, 1));
        Repository repository = page.getValues().iterator().next();

        Command<String> command = scmService.createBuilder(repository)
                                            .command("notes")
                                            .argument("show")
                                            .argument(changesetId)
                                            .exitHandler(new NotesExitHandler(i18nService))
                                            .build(new NotesCommandOutputHandler());

        return Response.ok(command.call()).build();
    }

    private static class NotesCommandOutputHandler implements CommandOutputHandler<String> {

        private final StringOutputHandler handler = new StringOutputHandler();

        @Nullable
        @Override
        public String getOutput() {
            String output = handler.getOutput();

            // trim to null
            if (output != null && output.trim().isEmpty()) {
                output = null;
            }

            return output;
        }

        @Override
        public void process(final InputStream output) throws ProcessException {
            handler.process(output);
        }

        @Override
        public void complete() throws ProcessException {
            handler.complete();
        }

        @Override
        public void setWatchdog(final Watchdog watchdog) {
            handler.setWatchdog(watchdog);
        }
    }

    /**
     * Exit handler that stops a non-zero exit code from 'git notes show' from being considered an error if the error
     * message indicates that no note is defined for the supplied commit.
     */
    private class NotesExitHandler extends DefaultCommandExitHandler {

        public NotesExitHandler(final I18nService i18nService) {
            super(i18nService);
        }

        @Override
        protected boolean isError(String command, int exitCode, String stdErr, Throwable thrown) {
            return !stdErr.startsWith("error: No note found for object")
                    && super.isError(command, exitCode, stdErr, thrown);
        }
    }
}
