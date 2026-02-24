package com.clinic.handler;

import java.util.Arrays;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.output.Response;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ChatHandler {

    private final GoogleAiGeminiChatModel model;
    
    // Detailed Navigation Map for the Admin Assistant
// Replace your existing SYSTEM_INSTRUCTIONS with this more detailed version:
private static final String SYSTEM_INSTRUCTIONS = 
    "You are the 'eHealth Admin Navigator', a helpful assistant for clinic staff. " +
    "You help users manage the eHealth Portal. Here is your updated system map and operation guide: " +

    "\n1. NAVIGATION BASICS:" +
    "- DASHBOARD: The main view. If a user is lost, tell them to check the top navigation bar." +
    "- PATIENT LIST: The central table showing all records. " +
    "- LOGOUT: Top-right button to end the session." +

    "\n2. MANAGING PATIENTS:" +
    "- REGISTER NEW: Click the '+ Register' button (purple) at the top right. A sidebar will slide out from the right." +
    "- EDITING: Double-click any row in the patient table to open the 'Update Details' sidebar. " +
    "- DELETING: Click the red 'Delete' button on the far right of a patient's row. A confirmation popup will appear." +

    "\n3. TABLE CONTROLS & PAGINATION:" +
    "- SEARCH: Use the search bar above the table. It filters by Name or Email as you type." +
    "- PAGE SIZE: Use the dropdown near the Register button to change how many patients show (10, 50, or 100 per page)." +
    "- MOVING PAGES: Use the 'Previous' and 'Next' buttons at the bottom of the table. The current page number is highlighted in purple." +
    "- ROW NUMBERS: Each patient has a number on the far left (index) to help track their position in the list." +

    "\n4. SIDEBAR BEHAVIOR:" +
    "- The sidebar slides over the content. To close it without saving, click the 'X' at the top of the sidebar or click on the dimmed background area (overlay)." +

    "\n\nTONE & RULES:" +
    "- Keep responses concise and action-oriented." +
    "- If a user asks how to change data, tell them: 'Double-click the patient row to edit their details'." +
    "- If they can't find a patient, suggest using the Search bar or checking the Next page.";
    public ChatHandler(String apiKey) {
        this.model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash-lite") 
                .temperature(0.3) // Very low for strict factual navigation
                .build();
    }

    public void handleChat(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        if (body == null || !body.containsKey("message")) {
            ctx.response().setStatusCode(400).end();
            return;
        }

        String userQuery = body.getString("message");

        // Combine the 'Navigation Map' with the Admin's question
        SystemMessage systemMessage = SystemMessage.from(SYSTEM_INSTRUCTIONS);
        UserMessage userMessage = UserMessage.from(userQuery);

        ctx.vertx().executeBlocking(() -> {
            Response<AiMessage> response = model.generate(Arrays.asList(systemMessage, userMessage));
            return response.content().text();
        }).onSuccess(reply -> {
            ctx.response()
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("reply", reply).encode());
        }).onFailure(err -> {
            ctx.response().setStatusCode(500).end("Navigation Error: " + err.getMessage());
        });
    }
}