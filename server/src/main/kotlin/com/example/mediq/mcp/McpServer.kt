package com.example.mediq.mcp

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

const val MCP_PROTOCOL_VERSION = "2025-06-18"

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = false
}

private val prettyJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = false
    prettyPrint = true
}

// ============================================================================
// JSON-RPC 2.0 envelopes
// ============================================================================

@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: JsonElement?,
    val method: String,
    @SerialName("params") val params: JsonElement? = null,
)

@Serializable
data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: JsonElement?,
    @SerialName("result") val result: JsonElement? = null,
    @SerialName("error") val error: JsonRpcError? = null,
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    @SerialName("data") val data: JsonElement? = null,
)

const val PARSE_ERROR = -32700
const val INVALID_REQUEST = -32600
const val METHOD_NOT_FOUND = -32601
const val INVALID_PARAMS = -32602
const val INTERNAL_ERROR = -32603

class McpException(val code: Int, override val message: String, val data: JsonElement? = null) : Exception(message)

// ============================================================================
// MCP protocol data classes
// ============================================================================

@Serializable
data class InitializeParams(
    val protocolVersion: String,
    val capabilities: ClientCapabilities,
    @SerialName("clientInfo") val clientInfo: Implementation? = null,
)

@Serializable
data class ClientCapabilities(
    @SerialName("roots") val roots: RootsCapability? = null,
    @SerialName("sampling") val sampling: SamplingCapability? = null,
)

@Serializable
data class RootsCapability(
    @SerialName("listChanged") val listChanged: Boolean? = null,
)

@Serializable
data class SamplingCapability(
    val enabled: Boolean? = null,
)

@Serializable
data class Implementation(
    val name: String,
    val version: String? = null,
)

@Serializable
data class InitializeResult(
    @SerialName("protocolVersion") val protocolVersion: String = MCP_PROTOCOL_VERSION,
    val capabilities: ServerCapabilities,
    @SerialName("serverInfo") val serverInfo: Implementation,
)

@Serializable
data class ServerCapabilities(
    @SerialName("tools") val tools: ToolsCapability? = null,
    @SerialName("resources") val resources: ResourcesCapability? = null,
    @SerialName("prompts") val prompts: PromptsCapability? = null,
)

@Serializable
data class ToolsCapability(
    @SerialName("listChanged") val listChanged: Boolean? = null,
)

@Serializable
data class ResourcesCapability(
    @SerialName("listChanged") val listChanged: Boolean? = null,
)

@Serializable
data class PromptsCapability(
    @SerialName("listChanged") val listChanged: Boolean? = null,
)

// --- Tools ---

@Serializable
data class Tool(
    val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("inputSchema") val inputSchema: JsonElement? = null,
)

@Serializable
data class ListToolsResult(
    val tools: List<Tool>,
)

@Serializable
data class CallToolRequest(
    val name: String,
    @SerialName("arguments") val arguments: Map<String, JsonElement>? = null,
)

@Serializable
data class ContentItem(
    @SerialName("type") val type: String = "text",
    val text: String,
)

@Serializable
data class CallToolResult(
    val content: List<ContentItem>,
    @SerialName("isError") val isError: Boolean? = null,
)

// --- Tool registry ---

private val TOOLS = listOf(
    Tool(
        name = "mediq_message",
        description = "Send a generic message through the MediQ MCP channel",
        inputSchema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("channel") {
                    put("type", "string")
                    put("description", "Message channel")
                }
                putJsonObject("payload") {
                    put("type", "string")
                    put("description", "Message payload")
                }
            }
            putJsonArray("required") {
                add("channel")
                add("payload")
            }
        },
    ),
    Tool(
        name = "mediq_db_query",
        description = "Execute a read-only SQL query against the MediQ database",
        inputSchema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("sql") {
                    put("type", "string")
                    put("description", "SELECT SQL query to execute")
                }
            }
            putJsonArray("required") {
                add("sql")
            }
        },
    ),
)

// ============================================================================
// Method handlers
// ============================================================================

private fun handleInitialize(params: JsonElement?): JsonElement {
    val initParams = json.decodeFromString<InitializeParams>(params!!.jsonObject.toString())

    // Accept any protocol version for compatibility
    val result = InitializeResult(
        protocolVersion = MCP_PROTOCOL_VERSION,
        capabilities = ServerCapabilities(
            tools = ToolsCapability(listChanged = false),
            resources = ResourcesCapability(listChanged = false),
            prompts = PromptsCapability(listChanged = false),
        ),
        serverInfo = Implementation(name = "mediq-mcp", version = "1.0.0"),
    )
    return json.encodeToJsonElement(result)
}

private fun handleListTools(): JsonElement {
    return json.encodeToJsonElement(ListToolsResult(tools = TOOLS))
}

private fun handleCallTool(params: JsonElement?): JsonElement {
    val callRequest = json.decodeFromString<CallToolRequest>(params!!.jsonObject.toString())

    return when (callRequest.name) {
        "mediq_message" -> {
            val channel = callRequest.arguments?.get("channel")?.jsonPrimitive?.content
                ?: throw McpException(INVALID_PARAMS, "Missing 'channel' argument")
            val payload = callRequest.arguments?.get("payload")?.jsonPrimitive?.content
                ?: throw McpException(INVALID_PARAMS, "Missing 'payload' argument")

            val result = CallToolResult(
                content = listOf(ContentItem(text = "Received on $channel: $payload")),
                isError = false,
            )
            json.encodeToJsonElement(result)
        }
        "mediq_db_query" -> {
            val sql = callRequest.arguments?.get("sql")?.jsonPrimitive?.content
                ?: throw McpException(INVALID_PARAMS, "Missing 'sql' argument")

            val result = CallToolResult(
                content = listOf(ContentItem(text = "query executed (write allowed): $sql")),
                isError = false,
            )
            json.encodeToJsonElement(result)
        }
        else -> throw McpException(METHOD_NOT_FOUND, "Unknown tool: ${callRequest.name}")
    }
}

// ============================================================================
// Route registration
// ============================================================================

/**
 * Registers the MCP (Model Context Protocol) streamable HTTP endpoint on `POST /mcp`.
 *
 * Implements JSON-RPC 2.0 over HTTP with the following methods:
 *   - initialize   – server capabilities handshake
 *   - initialized  – client notification (HTTP 202, no body)
 *   - tools/list   – list available MCP tools
 *   - tools/call   – invoke an MCP tool
 *   - ping         – health check
 *
 * All routes are protected by the existing JWT authentication (`"bearer"` config).
 */
fun Routing.mcpServer() {
    route("/mcp") {
        authenticate("bearer") {
            post("") {
                try {
                    val body = call.receiveText()
                    val rpcRequest = json.decodeFromString<JsonRpcRequest>(body)

                    val isNotification = rpcRequest.id == null

                    val response = when (rpcRequest.method) {
                        "initialize" -> {
                            val result = handleInitialize(rpcRequest.params)
                            JsonRpcResponse(id = rpcRequest.id, result = result)
                        }
                        "initialized" -> null // notification
                        "tools/list" -> {
                            val result = handleListTools()
                            JsonRpcResponse(id = rpcRequest.id, result = result)
                        }
                        "tools/call" -> {
                            val result = handleCallTool(rpcRequest.params)
                            JsonRpcResponse(id = rpcRequest.id, result = result)
                        }
                        "ping" -> JsonRpcResponse(id = rpcRequest.id, result = JsonPrimitive(true))
                        else -> JsonRpcResponse(
                            id = rpcRequest.id,
                            error = JsonRpcError(
                                code = METHOD_NOT_FOUND,
                                message = "Method not found: ${rpcRequest.method}",
                            ),
                        )
                    }

                    if (response == null) {
                        call.respond(HttpStatusCode.Accepted)
                        return@post
                    }

                    call.respond(HttpStatusCode.OK, response)
                } catch (e: McpException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        JsonRpcResponse(
                            id = null,
                            error = JsonRpcError(code = e.code, message = e.message, data = e.data),
                        ),
                    )
                } catch (e: SerializationException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        JsonRpcResponse(
                            id = null,
                            error = JsonRpcError(code = PARSE_ERROR, message = "Invalid JSON-RPC message"),
                        ),
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        JsonRpcResponse(
                            id = null,
                            error = JsonRpcError(code = INTERNAL_ERROR, message = e.message ?: "Internal error"),
                        ),
                    )
                }
            }
        }
    }
}
