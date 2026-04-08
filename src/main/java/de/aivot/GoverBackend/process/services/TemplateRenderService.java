package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.javascript.models.JavascriptResult;
import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.process.models.ProcessExecutionData;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parses, validates, and renders lightweight process templates against process data.
 *
 * <p>The renderer is separated from {@link ProcessDataService} so the responsibilities stay explicit:
 * process data assembly happens once, template rendering happens afterwards. This keeps the template language
 * implementation self-contained while still evaluating expressions against the same process data conventions.
 */
@Service
public class TemplateRenderService {
    private static final String TEMPLATE_SCOPE_NAME = "__templateScope";
    private static final Set<String> IF_STOP_DIRECTIVES = Set.of("else", "endif");
    private static final Set<String> FOR_STOP_DIRECTIVES = Set.of("endfor");
    private static final Set<String> BLOCK_STOP_DIRECTIVES = Set.of("endblock");
    private static final Pattern FOR_DIRECTIVE_PATTERN = Pattern.compile("([A-Za-z_$][\\w$]*)\\s+in\\s+(.+)", Pattern.DOTALL);
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_$][\\w$]*");

    private final JavascriptEngineFactoryService javascriptEngineFactoryService;

    public TemplateRenderService(JavascriptEngineFactoryService javascriptEngineFactoryService) {
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
    }

    /**
     * Validates only template syntax and block structure without executing any JavaScript.
     *
     * <p>This method exists separately from {@link #interpolate(ProcessExecutionData, String)} so the frontend can show Monaco markers
     * before a template is rendered. The returned diagnostics are line-based on purpose because line numbers are the
     * stable unit the editor needs, while runtime expression errors are still handled during interpolation.
     */
    @Nonnull
    public List<TemplateSyntaxDiagnostic> validateInterpolationSyntax(@Nullable String template) {
        if (template == null || template.isEmpty()) {
            return List.of();
        }
        return new TemplateParser(new TemplateSource(template))
                .parse()
                .diagnostics();
    }

    /**
     * Renders the given template against the provided process data.
     *
     * <p>The method first parses the full template and rejects invalid syntax before any JavaScript is executed.
     * That prevents partially rendered output and ensures the same parser drives both editor validation and runtime
     * rendering. A single JavaScript engine is reused for the full render so nested loops and conditions share one
     * consistent execution environment instead of repeatedly bootstrapping isolated engines.
     */
    @Nullable
    public String interpolate(@Nonnull ProcessExecutionData foldedProcessData,
                              @Nullable String template) {
        if (template == null) {
            return null;
        }

        var parseResult = new TemplateParser(new TemplateSource(template)).parse();
        if (!parseResult.diagnostics().isEmpty()) {
            throw new IllegalArgumentException(formatDiagnostics(parseResult.diagnostics()));
        }

        try (var engine = javascriptEngineFactoryService.getEngine()) {
            var renderer = new TemplateRenderer(
                    new TemplateExpressionEvaluator(engine, parseResult.source()),
                    new TemplateRenderContext(foldedProcessData),
                    parseResult.blocks()
            );
            return renderer.render(parseResult.nodes());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    private static String formatDiagnostics(@Nonnull List<TemplateSyntaxDiagnostic> diagnostics) {
        return diagnostics.stream()
                .map(diagnostic -> String.format(
                        "Line %d: %s [%s]",
                        diagnostic.lineNumber(),
                        diagnostic.message(),
                        diagnostic.lineContent()
                ))
                .reduce("Invalid template syntax.", (left, right) -> left + "\n" + right);
    }

    /**
     * Line-based syntax diagnostic for the template editor.
     *
     * <p>The offending line text is included together with the message so the frontend can render useful markers
     * without having to re-fetch or re-slice the source template on its own.
     */
    public record TemplateSyntaxDiagnostic(int lineNumber, @Nonnull String lineContent, @Nonnull String message) {
    }

    private record TemplateParseResult(@Nonnull TemplateSource source,
                                       @Nonnull List<TemplateNode> nodes,
                                       @Nonnull Map<String, TemplateBlockDefinition> blocks,
                                       @Nonnull List<TemplateSyntaxDiagnostic> diagnostics) {
    }

    private record TemplateParseBlock(@Nonnull List<TemplateNode> nodes, @Nullable String stopDirective) {
    }

    private record TemplateBlockDefinition(@Nonnull List<TemplateNode> body) {
    }

    private sealed interface TemplateNode permits TextNode, PrintNode, RawNode, IfNode, ForNode, UseBlockNode {
        int sourceIndex();
    }

    private record TextNode(@Nonnull String text, int sourceIndex) implements TemplateNode {
    }

    private record PrintNode(@Nonnull String expression, int sourceIndex) implements TemplateNode {
    }

    private record RawNode(@Nonnull String expression, int sourceIndex) implements TemplateNode {
    }

    private record IfNode(@Nonnull String expression,
                          @Nonnull List<TemplateNode> trueBranch,
                          @Nonnull List<TemplateNode> falseBranch,
                          int sourceIndex) implements TemplateNode {
    }

    private record ForNode(@Nonnull String variableName,
                           @Nonnull String expression,
                           @Nonnull List<TemplateNode> body,
                           int sourceIndex) implements TemplateNode {
    }

    private record UseBlockNode(@Nonnull String blockIdentifier, int sourceIndex) implements TemplateNode {
    }

    /**
     * Scanner-based template parser.
     *
     * <p>A regex-based approach is not sufficient here because block tags can nest and all tag types are allowed to
     * span multiple lines. The parser therefore walks the source once, builds a tiny AST, and accumulates diagnostics
     * instead of failing fast so editor validation can show multiple issues in one pass.
     */
    private static final class TemplateParser {
        private final TemplateSource source;
        private final List<TemplateSyntaxDiagnostic> diagnostics = new ArrayList<>();
        private final Map<String, TemplateBlockDefinition> blocks = new LinkedHashMap<>();
        private int index = 0;

        private TemplateParser(@Nonnull TemplateSource source) {
            this.source = source;
        }

        /**
         * Parses the full template and keeps diagnostics beside the AST.
         *
         * <p>Returning both structures from the same pass guarantees that validation and rendering are based on the
         * exact same understanding of the template syntax.
         */
        @Nonnull
        private TemplateParseResult parse() {
            var block = parseNodes(Set.of());
            validateUseBlocks(block.nodes());
            validateBlockCycles();
            return new TemplateParseResult(
                    source,
                    List.copyOf(block.nodes()),
                    Collections.unmodifiableMap(new LinkedHashMap<>(blocks)),
                    List.copyOf(diagnostics)
            );
        }

        /**
         * Parses nodes until the source ends or a matching block terminator is reached.
         *
         * <p>This recursive shape mirrors the template language itself: {@code if} and {@code for} introduce nested
         * blocks, while plain text and expression tags stay leaf nodes.
         */
        @Nonnull
        private TemplateParseBlock parseNodes(@Nonnull Set<String> stopDirectives) {
            var nodes = new ArrayList<TemplateNode>();
            var textStart = index;

            while (index < source.length()) {
                if (!source.startsWith(index, "{")) {
                    index++;
                    continue;
                }

                var handledTag = source.startsWith(index, "{{")
                        || source.startsWith(index, "{!")
                        || source.startsWith(index, "{#")
                        || source.startsWith(index, "{%");

                if (!handledTag) {
                    index++;
                    continue;
                }

                appendText(nodes, textStart, index);
                if (source.startsWith(index, "{{")) {
                    parseExpressionNode(nodes, "{{", "}}", true);
                } else if (source.startsWith(index, "{!")) {
                    parseExpressionNode(nodes, "{!", "!}", false);
                } else if (source.startsWith(index, "{#")) {
                    skipTag("{#", "#}", "Comment tag is not closed.");
                } else {
                    var stopDirective = parseDirectiveNode(nodes, stopDirectives);
                    if (stopDirective != null) {
                        return new TemplateParseBlock(List.copyOf(nodes), stopDirective);
                    }
                }
                textStart = index;
            }

            appendText(nodes, textStart, index);
            return new TemplateParseBlock(List.copyOf(nodes), null);
        }

        private void parseExpressionNode(@Nonnull List<TemplateNode> nodes,
                                         @Nonnull String openToken,
                                         @Nonnull String closeToken,
                                         boolean escaped) {
            var start = index;
            var content = readTagContent(openToken, closeToken, start, escaped
                    ? "Print tag is not closed."
                    : "Raw tag is not closed.");
            if (content == null) {
                return;
            }

            var expression = content.trim();
            if (expression.isEmpty()) {
                diagnostics.add(source.diagnostic(start, "Expression tag must not be empty."));
            }

            nodes.add(escaped
                    ? new PrintNode(expression, start)
                    : new RawNode(expression, start));
        }

        private void skipTag(@Nonnull String openToken,
                             @Nonnull String closeToken,
                             @Nonnull String missingCloseMessage) {
            readTagContent(openToken, closeToken, index, missingCloseMessage);
        }

        @Nullable
        private String parseDirectiveNode(@Nonnull List<TemplateNode> nodes,
                                          @Nonnull Set<String> stopDirectives) {
            var start = index;
            var content = readTagContent("{%", "%}", start, "Directive tag is not closed.");
            if (content == null) {
                return null;
            }

            var directive = content.trim();
            if (directive.isEmpty()) {
                diagnostics.add(source.diagnostic(start, "Directive tag must not be empty."));
                return null;
            }

            if (stopDirectives.contains(directive)) {
                return directive;
            }

            if (directive.equals("else") || directive.equals("endif") || directive.equals("endfor")) {
                diagnostics.add(source.diagnostic(start, "Unexpected directive '{% " + directive + " %}'."));
                return null;
            }

            if (directive.equals("endblock")) {
                diagnostics.add(source.diagnostic(start, "Unexpected directive '{% " + directive + " %}'."));
                return null;
            }

            if (hasDirectiveKeyword(directive, "if")) {
                nodes.add(parseIfNode(start, directive.substring(2).trim()));
                return null;
            }

            if (hasDirectiveKeyword(directive, "for")) {
                nodes.add(parseForNode(start, directive.substring(3).trim()));
                return null;
            }

            if (hasDirectiveKeyword(directive, "block")) {
                parseBlockDefinition(start, directive.substring(5).trim());
                return null;
            }

            if (hasDirectiveKeyword(directive, "useBlock")) {
                var node = parseUseBlockNode(start, directive.substring("useBlock".length()).trim());
                if (node != null) {
                    nodes.add(node);
                }
                return null;
            }

            diagnostics.add(source.diagnostic(start, "Unknown directive '{% " + directive + " %}'."));
            return null;
        }

        @Nonnull
        private IfNode parseIfNode(int sourceIndex, @Nonnull String expression) {
            if (expression.isEmpty()) {
                diagnostics.add(source.diagnostic(sourceIndex, "If directive requires a condition expression."));
            }

            var trueBranch = parseNodes(IF_STOP_DIRECTIVES);
            var falseNodes = List.<TemplateNode>of();

            if ("else".equals(trueBranch.stopDirective())) {
                var falseBranch = parseNodes(Set.of("endif"));
                falseNodes = falseBranch.nodes();
                if (falseBranch.stopDirective() == null) {
                    diagnostics.add(source.diagnostic(sourceIndex, "If block is missing '{% endif %}'."));
                }
            } else if (trueBranch.stopDirective() == null) {
                diagnostics.add(source.diagnostic(sourceIndex, "If block is missing '{% endif %}'."));
            }

            return new IfNode(expression, trueBranch.nodes(), falseNodes, sourceIndex);
        }

        @Nonnull
        private ForNode parseForNode(int sourceIndex, @Nonnull String header) {
            var variableName = "";
            var expression = "";
            var matcher = FOR_DIRECTIVE_PATTERN.matcher(header);

            if (matcher.matches()) {
                variableName = matcher.group(1);
                expression = matcher.group(2).trim();
            } else {
                diagnostics.add(source.diagnostic(sourceIndex, "For directive must match '{% for <name> in <expression> %}'."));
            }

            if (expression.isEmpty()) {
                diagnostics.add(source.diagnostic(sourceIndex, "For directive requires a list expression."));
            }

            var body = parseNodes(FOR_STOP_DIRECTIVES);
            if (body.stopDirective() == null) {
                diagnostics.add(source.diagnostic(sourceIndex, "For block is missing '{% endfor %}'."));
            }

            return new ForNode(variableName, expression, body.nodes(), sourceIndex);
        }

        private void parseBlockDefinition(int sourceIndex, @Nonnull String identifier) {
            if (identifier.isEmpty()) {
                diagnostics.add(source.diagnostic(sourceIndex, "Block directive requires an identifier."));
            } else if (!IDENTIFIER_PATTERN.matcher(identifier).matches()) {
                diagnostics.add(source.diagnostic(sourceIndex, "Block directive must match '{% block <identifier> %}'."));
            }

            var body = parseNodes(BLOCK_STOP_DIRECTIVES);
            if (body.stopDirective() == null) {
                diagnostics.add(source.diagnostic(sourceIndex, "Block is missing '{% endblock %}'."));
            }

            if (!IDENTIFIER_PATTERN.matcher(identifier).matches()) {
                return;
            }

            if (blocks.containsKey(identifier)) {
                diagnostics.add(source.diagnostic(sourceIndex, "Block '" + identifier + "' is defined multiple times."));
                return;
            }

            blocks.put(identifier, new TemplateBlockDefinition(body.nodes()));
        }

        @Nullable
        private UseBlockNode parseUseBlockNode(int sourceIndex, @Nonnull String identifier) {
            if (identifier.isEmpty()) {
                diagnostics.add(source.diagnostic(sourceIndex, "useBlock directive requires an identifier."));
                return null;
            }
            if (!IDENTIFIER_PATTERN.matcher(identifier).matches()) {
                diagnostics.add(source.diagnostic(sourceIndex, "useBlock directive must match '{% useBlock <identifier> %}'."));
                return null;
            }
            return new UseBlockNode(identifier, sourceIndex);
        }

        private void validateUseBlocks(@Nonnull List<TemplateNode> rootNodes) {
            validateUseBlocksInNodes(rootNodes);
            for (var block : blocks.values()) {
                validateUseBlocksInNodes(block.body());
            }
        }

        private void validateUseBlocksInNodes(@Nonnull List<TemplateNode> nodes) {
            for (var node : nodes) {
                switch (node) {
                    case UseBlockNode useBlockNode -> {
                        if (!blocks.containsKey(useBlockNode.blockIdentifier())) {
                            diagnostics.add(source.diagnostic(
                                    useBlockNode.sourceIndex(),
                                    "useBlock directive references undefined block '" + useBlockNode.blockIdentifier() + "'."
                            ));
                        }
                    }
                    case IfNode ifNode -> {
                        validateUseBlocksInNodes(ifNode.trueBranch());
                        validateUseBlocksInNodes(ifNode.falseBranch());
                    }
                    case ForNode forNode -> validateUseBlocksInNodes(forNode.body());
                    default -> {
                    }
                }
            }
        }

        private void validateBlockCycles() {
            var states = new HashMap<String, VisitState>();
            for (var identifier : blocks.keySet()) {
                detectBlockCycles(identifier, states);
            }
        }

        private void detectBlockCycles(@Nonnull String identifier,
                                       @Nonnull Map<String, VisitState> states) {
            var state = states.get(identifier);
            if (state == VisitState.VISITING || state == VisitState.VISITED) {
                return;
            }

            states.put(identifier, VisitState.VISITING);
            try {
                for (var reference : collectBlockReferences(blocks.get(identifier).body())) {
                    if (!blocks.containsKey(reference.blockIdentifier())) {
                        continue;
                    }

                    var targetState = states.get(reference.blockIdentifier());
                    if (targetState == VisitState.VISITING) {
                        diagnostics.add(source.diagnostic(
                                reference.sourceIndex(),
                                "Circular block reference detected involving '" + reference.blockIdentifier() + "'."
                        ));
                        continue;
                    }

                    if (targetState != VisitState.VISITED) {
                        detectBlockCycles(reference.blockIdentifier(), states);
                    }
                }
            } finally {
                states.put(identifier, VisitState.VISITED);
            }
        }

        @Nonnull
        private List<UseBlockNode> collectBlockReferences(@Nonnull List<TemplateNode> nodes) {
            var references = new ArrayList<UseBlockNode>();
            collectBlockReferences(nodes, references);
            return references;
        }

        private void collectBlockReferences(@Nonnull List<TemplateNode> nodes,
                                            @Nonnull List<UseBlockNode> references) {
            for (var node : nodes) {
                switch (node) {
                    case UseBlockNode useBlockNode -> references.add(useBlockNode);
                    case IfNode ifNode -> {
                        collectBlockReferences(ifNode.trueBranch(), references);
                        collectBlockReferences(ifNode.falseBranch(), references);
                    }
                    case ForNode forNode -> collectBlockReferences(forNode.body(), references);
                    default -> {
                    }
                }
            }
        }

        private void appendText(@Nonnull List<TemplateNode> nodes, int start, int end) {
            if (start >= end) {
                return;
            }
            nodes.add(new TextNode(source.slice(start, end), start));
        }

        private boolean hasDirectiveKeyword(@Nonnull String directive, @Nonnull String keyword) {
            return directive.equals(keyword)
                    || directive.startsWith(keyword) && directive.length() > keyword.length()
                    && Character.isWhitespace(directive.charAt(keyword.length()));
        }

        @Nullable
        private String readTagContent(@Nonnull String openToken,
                                      @Nonnull String closeToken,
                                      int start,
                                      @Nonnull String missingCloseMessage) {
            var contentStart = start + openToken.length();
            var closeIndex = source.indexOf(closeToken, contentStart);
            if (closeIndex < 0) {
                diagnostics.add(source.diagnostic(start, missingCloseMessage));
                index = source.length();
                return null;
            }

            index = closeIndex + closeToken.length();
            return source.slice(contentStart, closeIndex);
        }

        private enum VisitState {
            VISITING,
            VISITED
        }
    }

    /**
     * Recursively renders the parsed template tree.
     *
     * <p>Rendering is AST-based instead of string-replacement-based because conditions and loops need structural
     * control over which parts of the template are visited at all.
     */
    private static final class TemplateRenderer {
        private final TemplateExpressionEvaluator evaluator;
        private final TemplateRenderContext rootContext;
        private final Map<String, TemplateBlockDefinition> blocks;
        private final Deque<String> activeBlocks = new ArrayDeque<>();

        private TemplateRenderer(@Nonnull TemplateExpressionEvaluator evaluator,
                                 @Nonnull TemplateRenderContext rootContext,
                                 @Nonnull Map<String, TemplateBlockDefinition> blocks) {
            this.evaluator = evaluator;
            this.rootContext = rootContext;
            this.blocks = blocks;
        }

        @Nonnull
        private String render(@Nonnull List<TemplateNode> nodes) {
            var builder = new StringBuilder();
            renderInto(builder, nodes, rootContext);
            return builder.toString();
        }

        private void renderInto(@Nonnull StringBuilder builder,
                                @Nonnull List<TemplateNode> nodes,
                                @Nonnull TemplateRenderContext context) {
            for (var node : nodes) {
                switch (node) {
                    case TextNode textNode -> builder.append(textNode.text());
                    case PrintNode printNode -> builder.append(
                            TemplateSanitizer.escape(evaluator.evaluateToString(context, printNode.expression(), printNode.sourceIndex()))
                    );
                    case RawNode rawNode -> builder.append(
                            evaluator.evaluateToString(context, rawNode.expression(), rawNode.sourceIndex())
                    );
                    case IfNode ifNode -> renderInto(
                            builder,
                            evaluator.evaluateBoolean(context, ifNode.expression(), ifNode.sourceIndex())
                                    ? ifNode.trueBranch()
                                    : ifNode.falseBranch(),
                            context
                    );
                    case ForNode forNode -> {
                        for (var item : evaluator.evaluateList(context, forNode.expression(), forNode.sourceIndex())) {
                            renderInto(builder, forNode.body(), context.with(forNode.variableName(), item));
                        }
                    }
                    case UseBlockNode useBlockNode -> renderBlock(builder, context, useBlockNode);
                }
            }
        }

        private void renderBlock(@Nonnull StringBuilder builder,
                                 @Nonnull TemplateRenderContext context,
                                 @Nonnull UseBlockNode useBlockNode) {
            var block = blocks.get(useBlockNode.blockIdentifier());
            if (block == null) {
                throw evaluator.failure(
                        useBlockNode.sourceIndex(),
                        "useBlock directive references undefined block '" + useBlockNode.blockIdentifier() + "'.",
                        null
                );
            }

            if (activeBlocks.contains(useBlockNode.blockIdentifier())) {
                throw evaluator.failure(
                        useBlockNode.sourceIndex(),
                        "Circular block reference detected involving '" + useBlockNode.blockIdentifier() + "'.",
                        null
                );
            }

            activeBlocks.addLast(useBlockNode.blockIdentifier());
            try {
                renderInto(builder, block.body(), context);
            } finally {
                activeBlocks.removeLast();
            }
        }
    }

    /**
     * Immutable render scope for one point in the template tree.
     *
     * <p>Loop variables are modeled as nested local scopes rather than mutating shared state so nested loops can
     * shadow variables safely and recursive rendering stays easy to reason about.
     */
    private static final class TemplateRenderContext {
        private final Map<String, Object> processData;
        private final Map<String, Object> locals;

        private TemplateRenderContext(@Nonnull Map<String, Object> processData) {
            this(processData, Map.of());
        }

        private TemplateRenderContext(@Nonnull Map<String, Object> processData,
                                      @Nonnull Map<String, Object> locals) {
            this.processData = processData;
            this.locals = locals;
        }

        @Nonnull
        private TemplateRenderContext with(@Nonnull String variableName, @Nullable Object value) {
            var nestedLocals = new LinkedHashMap<>(locals);
            nestedLocals.put(variableName, value);
            return new TemplateRenderContext(processData, nestedLocals);
        }

        /**
         * Flattens process roots and local variables into one object for JavaScript evaluation.
         *
         * <p>The evaluator uses {@code with (...)} to expose identifiers directly in template expressions, so the
         * scope needs to be assembled as a single map each time a node is evaluated.
         */
        @Nonnull
        private Map<String, Object> toScope() {
            var scope = new LinkedHashMap<String, Object>();
            scope.put("$", processData.get("$"));
            scope.put("$$", processData.get("$$"));

            for (var entry : processData.entrySet()) {
                if (entry.getKey().startsWith("_")) {
                    scope.put(entry.getKey(), entry.getValue());
                }
            }

            scope.putAll(locals);
            return scope;
        }
    }

    /**
     * Evaluates JavaScript expressions for template nodes and normalizes the result types the renderer needs.
     *
     * <p>Keeping evaluation here avoids scattering the rules for truthiness, list coercion, error formatting, and
     * scope setup across the renderer.
     */
    private static final class TemplateExpressionEvaluator {
        private final JavascriptEngine engine;
        private final TemplateSource source;

        private TemplateExpressionEvaluator(@Nonnull JavascriptEngine engine,
                                            @Nonnull TemplateSource source) {
            this.engine = engine;
            this.source = source;
        }

        @Nonnull
        private String evaluateToString(@Nonnull TemplateRenderContext context,
                                        @Nonnull String expression,
                                        int sourceIndex) {
            return evaluate(context, "return (" + expression + ");", expression, sourceIndex)
                    .toString();
        }

        private boolean evaluateBoolean(@Nonnull TemplateRenderContext context,
                                        @Nonnull String expression,
                                        int sourceIndex) {
            var result = evaluate(context, "return Boolean(" + expression + ");", expression, sourceIndex);
            return Boolean.TRUE.equals(result.asBoolean());
        }

        @Nonnull
        private List<?> evaluateList(@Nonnull TemplateRenderContext context,
                                     @Nonnull String expression,
                                     int sourceIndex) {
            var value = evaluate(context, "return (" + expression + ");", expression, sourceIndex)
                    .asObject();

            if (value == null) {
                return List.of();
            }
            if (value instanceof List<?> list) {
                return list;
            }
            if (value instanceof Collection<?> collection) {
                return new ArrayList<>(collection);
            }
            if (value instanceof Object[] array) {
                return List.of(array);
            }

            throw failure(sourceIndex, "For expression must evaluate to a list.", null);
        }

        /**
         * Executes one template expression inside the current scope.
         *
         * <p>The wrapper function provides a narrow evaluation surface: expressions can access process variables and
         * loop locals as plain identifiers, but rendering still controls whether the result is consumed as text,
         * boolean, or iterable.
         */
        @Nonnull
        private JavascriptResult evaluate(@Nonnull TemplateRenderContext context,
                                          @Nonnull String body,
                                          @Nonnull String expression,
                                          int sourceIndex) {
            try {
                // Expose process data and loop locals as plain identifiers inside template expressions.
                engine.registerGlobalObject(TEMPLATE_SCOPE_NAME, context.toScope());
                return engine.evaluateCode(JavascriptCode.of(
                        "(() => { with (%s) { %s } })()",
                        TEMPLATE_SCOPE_NAME,
                        body
                ));
            } catch (Exception e) {
                throw failure(sourceIndex, "Failed to evaluate expression '" + expression.trim() + "'.", e);
            }
        }

        @Nonnull
        private IllegalArgumentException failure(int sourceIndex,
                                                 @Nonnull String message,
                                                 @Nullable Exception cause) {
            var line = source.lineNumberAt(sourceIndex);
            var fullMessage = String.format(
                    "Template evaluation failed at line %d: %s [%s]",
                    line,
                    message,
                    source.lineText(line)
            );
            return cause == null
                    ? new IllegalArgumentException(fullMessage)
                    : new IllegalArgumentException(fullMessage, cause);
        }
    }

    /**
     * Escapes interpolated output for both HTML and JavaScript-sensitive characters.
     *
     * <p>The templates are used for generated content where the final embedding context is not always strictly HTML
     * text or strictly JavaScript text. Escaping both classes of characters is the conservative default for
     * {@code {{ ... }}} output, while {@code {! ... !}} remains the explicit opt-out.
     */
    private static final class TemplateSanitizer {
        @Nonnull
        private static String escape(@Nullable String value) {
            if (value == null || value.isEmpty()) {
                return value == null ? "" : value;
            }

            var builder = new StringBuilder(value.length());
            for (int i = 0; i < value.length(); i++) {
                var c = value.charAt(i);
                switch (c) {
                    case '&' -> builder.append("&amp;");
                    case '<' -> builder.append("&lt;");
                    case '>' -> builder.append("&gt;");
                    case '"' -> builder.append("&quot;");
                    case '\'' -> builder.append("&#39;");
                    case '\\' -> builder.append("\\\\");
                    case '`' -> builder.append("\\`");
                    case '\n' -> builder.append("\\n");
                    case '\r' -> builder.append("\\r");
                    case '\t' -> builder.append("\\t");
                    default -> {
                        if (c < 0x20 || c == '\u2028' || c == '\u2029') {
                            builder.append(String.format("\\u%04x", (int) c));
                        } else {
                            builder.append(c);
                        }
                    }
                }
            }
            return builder.toString();
        }
    }

    /**
     * Source wrapper that precomputes line starts for parser and runtime diagnostics.
     *
     * <p>Line lookup happens for syntax errors and expression failures, so computing the offsets once keeps that
     * mapping cheap and ensures both validation and runtime reporting use the same line model.
     */
    private static final class TemplateSource {
        private final String template;
        private final List<Integer> lineStarts = new ArrayList<>();

        private TemplateSource(@Nonnull String template) {
            this.template = template;
            lineStarts.add(0);

            for (int i = 0; i < template.length(); i++) {
                if (template.charAt(i) == '\n') {
                    lineStarts.add(i + 1);
                }
            }
        }

        private int length() {
            return template.length();
        }

        private boolean startsWith(int index, @Nonnull String needle) {
            return template.startsWith(needle, index);
        }

        private int indexOf(@Nonnull String needle, int fromIndex) {
            return template.indexOf(needle, fromIndex);
        }

        @Nonnull
        private String slice(int start, int end) {
            return template.substring(start, end);
        }

        private int lineNumberAt(int index) {
            if (template.isEmpty()) {
                return 1;
            }

            var boundedIndex = Math.max(0, Math.min(index, template.length() - 1));
            var lineIndex = Collections.binarySearch(lineStarts, boundedIndex);
            if (lineIndex >= 0) {
                return lineIndex + 1;
            }
            return -lineIndex - 1;
        }

        @Nonnull
        private String lineText(int lineNumber) {
            var lineIndex = Math.max(0, Math.min(lineNumber - 1, lineStarts.size() - 1));
            var start = lineStarts.get(lineIndex);
            var end = lineIndex + 1 < lineStarts.size()
                    ? lineStarts.get(lineIndex + 1) - 1
                    : template.length();

            if (end > start && template.charAt(end - 1) == '\r') {
                end--;
            }
            return template.substring(start, Math.max(start, end));
        }

        @Nonnull
        private TemplateSyntaxDiagnostic diagnostic(int index, @Nonnull String message) {
            var lineNumber = lineNumberAt(index);
            return new TemplateSyntaxDiagnostic(lineNumber, lineText(lineNumber), message);
        }
    }
}
