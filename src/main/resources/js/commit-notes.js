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

(function($) {
    // Set up our namespace
    window.com = window.com || {};
    com.forgerock = com.forgerock || {};
    com.forgerock.stash = com.forgerock.stash || {};
    com.forgerock.stash.notes = com.forgerock.stash.notes || {};

    var storage = {
        getNote : function(changeset, repository) {
            return jQuery.ajax({
                type: 'GET',
                url: "/stash/rest/notes/1.0/projects/" + repository.project.key + "/repos/" + repository.slug + "/commits/" + changeset.id,
                async: false
            }).responseText;
        }
    };

    // Stash 2.4.x and 2.5.x incorrectly provided a Brace/Backbone model here, but should have provided raw JSON.
    function coerceToJson(changesetOrJson) {
        return changesetOrJson.toJSON ? changesetOrJson.toJSON() : changesetOrJson;
    }

    /**
     * The client-condition function takes in the context
     * before it is transformed by the client-context-provider.
     * If it returns a truthy value, the panel will be displayed.
     */
    function hasNotes(context) {
        var note = storage.getNote(coerceToJson(context['changeset']),
                                   coerceToJson(context['repository']));
        return note == null;
    }

    /**
     * The client-context-provider function takes in context and transforms
     * it to match the shape our template requires.
     */
    function getNote(context) {
        return {
            note : storage.getNote(coerceToJson(context['changeset']),
                                   coerceToJson(context['repository']))
        };
    }

    /* Expose the client-context-provider function */
    com.forgerock.stash.notes.getNote = getNote;

    /* use a live event to handle the link being clicked. */
    $(document).on('click', '.forgerock-notes-link', function(e) {
        e.preventDefault();

        // open a dialog to show the note details.
    });
}(jQuery));

